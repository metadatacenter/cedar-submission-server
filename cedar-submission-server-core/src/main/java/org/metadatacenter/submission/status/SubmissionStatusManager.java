package org.metadatacenter.submission.status;

import org.glassfish.jersey.client.ClientProperties;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.util.http.ProxyUtil;
import org.metadatacenter.util.test.TestUserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TODO Use Redis queue
// TODO Need to add insertion time in descriptor and clean old ones

public class SubmissionStatusManager
{
  final static Logger logger = LoggerFactory.getLogger(SubmissionStatusManager.class);

  private final ExecutorService executor;

  private final ConcurrentHashMap<String, SubmissionStatusDescriptor> submissions = new ConcurrentHashMap<>();

  // TODO: this is not nice. Find another way of having these variable available to call the messaging server
  private CedarConfig cedarConfig;
  public void setCedarConfig(CedarConfig cedarConfig) {
    this.cedarConfig = cedarConfig;
  }

  private SubmissionStatusManager()
  {
    this.executor = Executors.newFixedThreadPool(10);
    cedarConfig = null;

  }

  private static SubmissionStatusManager singleInstance;

  public static synchronized SubmissionStatusManager getInstance()
  {
    if (singleInstance == null) {
      singleInstance = new SubmissionStatusManager();
      singleInstance.start();
    }
    return singleInstance;
  }

  private void start()
  {
    logger.info("Starting the submission status manager");
    executor.submit(new SubmissionStatusManagerRunnable(this));
  }

  public void stop()
  {
    logger.info("Stopping the submission status manager");
    executor.shutdown();
    try {
      executor.awaitTermination(100, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      logger.warn("Submission status manager shutdown interrupted: " + e.getMessage());
    }
  }

  public String addSubmission(SubmissionStatusTask submissionStatusTask)
  {
    String submissionID = submissionStatusTask.getSubmissionID();
    SubmissionStatus submissionStatus = new SubmissionStatus(submissionID, SubmissionState.STARTED,
      "Received submission with ID " + submissionID);
    SubmissionStatusDescriptor submissionStatusDescriptor = new SubmissionStatusDescriptor(submissionID,
      submissionStatusTask.getUserID(), submissionStatusTask.getStatusURL(), submissionStatus, submissionStatusTask);

    this.submissions.put(submissionID, submissionStatusDescriptor);

    notifyUser(submissionStatusDescriptor);

    return submissionID;
  }

  public void updateSubmission(SubmissionStatus submissionStatus)
  {
    String submissionID = submissionStatus.getSubmissionID();

    if (!this.submissions.containsKey(submissionID))
      logger.warn("Attempt to update unknown submission " + submissionID);
    else {
      SubmissionStatusDescriptor currentSubmissionStatusDescriptor = submissions.get(submissionID);
      SubmissionStatusDescriptor newSubmissionStatusDescriptor = new SubmissionStatusDescriptor(submissionID,
        currentSubmissionStatusDescriptor.getUserID(), currentSubmissionStatusDescriptor.getStatusURL(),
        submissionStatus, currentSubmissionStatusDescriptor.getSubmissionStatusTask());

      notifyUser(newSubmissionStatusDescriptor);

      this.submissions.put(submissionID, newSubmissionStatusDescriptor);

      if (submissionStatus.getSubmissionState() == SubmissionState.COMPLETED
        || submissionStatus.getSubmissionState() == SubmissionState.REJECTED
        || submissionStatus.getSubmissionState() == SubmissionState.ERROR)
        removeSubmission(submissionID);
    }
  }

  public void removeSubmission(String submissionID)
  {
    if (!this.submissions.containsKey(submissionID))
      logger.warn("Attempt to remove unknown submission " + submissionID);
    else {
      SubmissionStatusDescriptor submissionStatusDescriptor = this.submissions.get(submissionID);

      if (submissionStatusDescriptor.getSubmissionStatus().getSubmissionState() != SubmissionState.COMPLETED)
        logger.warn("Removing incomplete submission " + submissionID);

      logger.info("Removing submission " + submissionID);

      this.submissions.remove(submissionID);
    }
  }

  public Map<String, SubmissionStatusDescriptor> getCurrentSubmissions()
  {
    synchronized (submissions) {
      return Collections.unmodifiableMap(submissions);
    }
  }

  private void notifyUser(SubmissionStatusDescriptor submissionStatusDescriptor)
  {

    // TODO: reuse this client
    Client client = ClientBuilder.newClient();
    client.property(ClientProperties.CONNECT_TIMEOUT, 3000);
    client.property(ClientProperties.READ_TIMEOUT, 30000);

    logger.info("Notifying user for submission " + submissionStatusDescriptor.getSubmissionID() + "; status = "
      + submissionStatusDescriptor.getSubmissionStatus().getSubmissionState() + ", message = "
      + submissionStatusDescriptor.getSubmissionStatus().getStatusMessage());

    // TODO: read from cedarConfig
    //String url = cedarConfig.getMicroserviceUrlUtil().getMessaging().getMessages();
    String url = "https://messaging.metadatacenter.orgx/messages";

    Map<String, Object> content = new HashMap<>();
    content.put("subject", submissionStatusDescriptor.getSubmissionStatusTask().getSubmissionType() + "Submission Notification");
    content.put("body", submissionStatusDescriptor.getSubmissionStatus().getStatusMessage());

    Map<String, Object> to = new HashMap<>();
    to.put("recipientType", "user");
    //TODO
    //to.put("@id", submissionStatusDescriptor.getUserID());
    to.put("@id", "https://metadatacenter.org/users/" + submissionStatusDescriptor.getUserID());
    content.put("to", to);

    Map<String, Object> from = new HashMap<>();
    from.put("senderType", "process");

    String processId = null;
    if (submissionStatusDescriptor.getSubmissionStatusTask().getSubmissionType().equals(SubmissionType.IMMPORT)) {
      processId = "submission.IMMPORT";
    }
    else if (submissionStatusDescriptor.getSubmissionStatusTask().getSubmissionType().equals(SubmissionType.NCBI_AIRR)) {
      processId = "submission.AIRR";
    }

    from.put("processId", processId);
    content.put("from", from);

    Entity postContent = Entity.entity(content, MediaType.APPLICATION_JSON);

    // TODO: is it fine to get this header from TestUtil?
    //String adminUserAuthHeader = TestUserUtil.getAdminUserAuthHeader(cedarConfig);
    String adminUserAuthHeader = "apiKey 58c4f22b9ea1548047682f3112f2f1bcedcb5e40443ddb5e6a11bda0629c2f20";

    Response response = client.target(url).request().header("Authorization", adminUserAuthHeader).post(postContent);
    logger.info("******* RESPONSE ********");
    logger.info(response.toString());

  }
}
