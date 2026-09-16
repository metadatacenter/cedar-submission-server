package org.metadatacenter.submission.notifications;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.server.security.CedarApiKeyAuthRequest;
import org.metadatacenter.submission.status.SubmissionStatusDescriptor;
import org.metadatacenter.submission.status.SubmissionType;
import org.metadatacenter.util.http.HttpTimeouts;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StatusNotifier {

  final static Logger logger = LoggerFactory.getLogger(StatusNotifier.class);

  private static StatusNotifier singleInstance = null;
  private static boolean initialized = false;

  private static CedarConfig cedarConfig = null;

  private StatusNotifier(CedarConfig cfg) {
    cedarConfig = cfg;
  }

  public static void initialize(CedarConfig cedarConfig) {
    singleInstance = new StatusNotifier(cedarConfig);
    initialized = true;
  }

  public static StatusNotifier getInstance() {
    if (!initialized) {
      throw new IllegalStateException("Cannot return instance. The instance needs to be initialized first");
    } else {
      return singleInstance;
    }
  }

  public void sendMessage(SubmissionStatusDescriptor submissionStatusDescriptor) {

    String url = cedarConfig.getMicroserviceUrlUtil().getMessaging().getMessages();
    logger.info("Notification url:" + url);

    Map<String, Object> content = new HashMap<>();
    content.put("subject", submissionStatusDescriptor.submissionStatusTask().getSubmissionType().getValue()
        + " Submission " + submissionStatusDescriptor.submissionStatus().submissionState());
    content.put("body", submissionStatusDescriptor.submissionStatus().statusMessage());

    Map<String, Object> to = new HashMap<>();
    to.put("recipientType", "user");
    to.put("@id", submissionStatusDescriptor.userID());
    content.put("to", to);

    Map<String, Object> from = new HashMap<>();
    from.put("senderType", "process");

    String processId = null;
    if (submissionStatusDescriptor.submissionStatusTask().getSubmissionType().equals(SubmissionType.IMMPORT)) {
      processId = "submission.IMMPORT";
    } else if (submissionStatusDescriptor.submissionStatusTask().getSubmissionType().equals(SubmissionType
        .NCBI)) {
      processId = "submission.NCBI"; // It aligns with name in messaging server
    }

    from.put("processId", processId);
    content.put("from", from);

    String adminUserAuthHeader = new CedarApiKeyAuthRequest(
        cedarConfig.getAdminUserConfig().getApiKey()).getAuthHeader();

    // The messaging server is the next CEDAR service, so this is the interactive class of outbound
    // call: its timeouts, its pool and its retry policy. It used to be a JAX-RS client of its own,
    // built here with two timeouts written into the code that no configuration could reach.
    int status;
    try {
      String postContent = JsonMapper.STRICT_MAPPER.writeValueAsString(content);
      try (ClassicHttpResponse response = HttpTimeouts.INTERACTIVE.execute(
          Request.post(url)
              .setHeader(HttpHeaders.AUTHORIZATION, adminUserAuthHeader)
              .bodyString(postContent, ContentType.APPLICATION_JSON))) {
        status = response.getCode();
        if (status != CedarResponseStatus.OK.getStatusCode()) {
          logger.warn("Internal error, statusCode=" + status + " postContent=" + postContent);
        }
      }
    } catch (IOException e) {
      // A failed downstream notification is a genuine server-side fault (500), but not JVM
      // corruption: a RuntimeException flows through catch (Exception) and the CedarExceptionMapper.
      throw new IllegalStateException("Error sending message to user", e);
    }

    if (status != CedarResponseStatus.OK.getStatusCode()) {
      throw new IllegalStateException("Error sending message to user");
    }
  }
}
