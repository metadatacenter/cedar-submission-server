package org.metadatacenter.cedar.submission.status;

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

  @Override public SubmissionStatus call()
  {
    return callSubmissionStatusEndpoint(this.submissionID, this.userID, this.statusURL);
  }

  abstract protected SubmissionStatus callSubmissionStatusEndpoint(String submissionID, String userID,
    String statusURL);
}
