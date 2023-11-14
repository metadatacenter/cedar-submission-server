package org.metadatacenter.submission.notifications;

import org.glassfish.jersey.client.ClientProperties;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.submission.status.SubmissionStatusDescriptor;
import org.metadatacenter.submission.status.SubmissionType;
import org.metadatacenter.util.test.TestUserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class StatusNotifier {

  final static Logger logger = LoggerFactory.getLogger(StatusNotifier.class);

  private static StatusNotifier singleInstance = null;
  private static boolean initialized = false;

  private static CedarConfig cedarConfig = null;
  private static Client client = null;

  private StatusNotifier(CedarConfig cfg) {
    cedarConfig = cfg;
    client = ClientBuilder.newClient();
    client.property(ClientProperties.CONNECT_TIMEOUT, 3000);
    client.property(ClientProperties.READ_TIMEOUT, 30000);
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

    Entity postContent = Entity.entity(content, MediaType.APPLICATION_JSON);

    String adminUserAuthHeader = TestUserUtil.getAdminUserAuthHeader(cedarConfig);

    Response response = client.target(url).request().header("Authorization", adminUserAuthHeader).post(postContent);

    if (response.getStatus() != CedarResponseStatus.OK.getStatusCode()) {
      logger.warn("Internal error, statusCode=" + response.getStatus() + " postContent=" + postContent);
      throw new InternalError("Error sending message to user");
    }
  }
}
