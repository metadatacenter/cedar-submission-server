package org.metadatacenter.submission.ncbiairr.status;

import org.metadatacenter.config.FTPConfig;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusTask;

public class NcbiAirrSubmissionStatusTask extends SubmissionStatusTask {

  private FTPConfig ftpConfig;

  // TODO: keep the statusURL in the parent. Not needed here.
  public NcbiAirrSubmissionStatusTask(String submissionID, String userID, String statusURL, FTPConfig ftpConfig) {
    super(submissionID, userID, statusURL);
    this.ftpConfig = ftpConfig;
  }

  public FTPConfig getFtpConfig() {
    return ftpConfig;
  }

  @Override
  protected SubmissionStatus callSubmissionStatusEndpoint() {
    return NcbiAirrSubmissionStatusUtil.getNcbiAirrSubmissionStatus(getSubmissionID());
  }
}
