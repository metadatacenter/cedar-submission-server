package org.metadatacenter.cedar.submission.core;

import java.util.concurrent.Callable;

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
