package org.metadatacenter.cedar.submission.core;

public class SubmissionStatusTaskDescriptor
{
  private final String submissionID;
  private final String userID;
  private final String statusURL;
  private final SubmissionStatus submissionStatus;
  private final SubmissionStatusTask submissionStatusTask;

  public SubmissionStatusTaskDescriptor(String submissionID, String userID, String statusURL,
    SubmissionStatus submissionStatus, SubmissionStatusTask submissionStatusTask)
  {
    this.submissionID = submissionID;
    this.userID = userID;
    this.statusURL = statusURL;
    this.submissionStatus = submissionStatus;
    this.submissionStatusTask = submissionStatusTask;
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

  public SubmissionStatusTask getSubmissionStatusTask()
  {
    return submissionStatusTask;
  }

  public SubmissionStatus getSubmissionStatus()
  {
    return submissionStatus;
  }
}
