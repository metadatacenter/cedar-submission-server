package org.metadatacenter.submission.immport;

import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusTask;

public class ImmPortSubmissionStatusTask extends SubmissionStatusTask
{
  public ImmPortSubmissionStatusTask(String submissionID, String userID, String statusURL)
  {
    super(submissionID, userID, statusURL);
  }

  @Override protected SubmissionStatus callSubmissionStatusEndpoint()
  {
    return ImmPortUtil.getImmPortSubmissionStatus(getSubmissionID());
  }
}
