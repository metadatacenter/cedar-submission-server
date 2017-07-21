package org.metadatacenter.submission.immport;

import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImmPortSubmissionStatusTask extends SubmissionStatusTask
{
  final static Logger logger = LoggerFactory.getLogger(ImmPortSubmissionStatusTask.class);

  public ImmPortSubmissionStatusTask(String submissionID, String userID, String statusURL)
  {
    super(submissionID, userID, statusURL);
  }

  @Override protected SubmissionStatus callSubmissionStatusEndpoint()
  {
    return ImmPortUtil.getImmPortSubmissionStatus(getSubmissionID());
  }
}
