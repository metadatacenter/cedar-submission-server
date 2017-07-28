package org.metadatacenter.submission.immport;

import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusTask;
import org.metadatacenter.submission.status.SubmissionType;

public class ImmPortSubmissionStatusTask extends SubmissionStatusTask
{
  public ImmPortSubmissionStatusTask(String submissionID, SubmissionType submissionType, String userID, String statusURL)
  {
    super(submissionID, submissionType, userID, statusURL);
  }

  @Override protected SubmissionStatus callSubmissionStatusEndpoint()
  {
    return ImmPortUtil.getImmPortSubmissionStatus(getSubmissionID());
  }
}
