package org.metadatacenter.submission.status;

import java.util.concurrent.Callable;

/**
 * Subclasses must specialize this call to implement a submission status call.
 */
public abstract class SubmissionStatusTask implements Callable<SubmissionStatus>
{
  private final String submissionID;
  private final String userID;
  private final String statusURL;

  public SubmissionStatusTask(String submissionID, String userID, String statusURL)
  {
    this.submissionID = submissionID;
    this.userID = userID;
    this.statusURL = statusURL;
  }

  public String getSubmissionID()
  {
    return submissionID;
  }

  public String getUserID()
  {
    return userID;
  }

  public String getStatusURL()
  {
    return statusURL;
  }

  @Override public SubmissionStatus call() throws Exception {
    return callSubmissionStatusEndpoint();
  }

  abstract protected SubmissionStatus callSubmissionStatusEndpoint() throws Exception;
}
