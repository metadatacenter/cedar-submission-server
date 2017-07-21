package org.metadatacenter.submission.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TODO Need to add insertion time in descriptor and clean old ones

public class SubmissionStatusManager
{
  final static Logger logger = LoggerFactory.getLogger(SubmissionStatusManager.class);

  private final ExecutorService executor;

  private final ConcurrentHashMap<String, SubmissionStatusDescriptor> submissions = new ConcurrentHashMap<>();

  public SubmissionStatusManager()
  {

    this.executor = Executors.newFixedThreadPool(10);
  }

  public void start()
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

  public String addSubmission(String userID, String statusURL, SubmissionStatusTask submissionStatusTask)
  {
    String submissionID = UUID.randomUUID().toString();
    SubmissionStatus submissionStatus = new SubmissionStatus(submissionID, SubmissionState.IN_PROGRESS, "");
    SubmissionStatusDescriptor submissionStatusDescriptor = new SubmissionStatusDescriptor(submissionID, userID,
      statusURL, submissionStatus, submissionStatusTask);

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
    // TODO
    logger.info("Notifying user");
  }
}
