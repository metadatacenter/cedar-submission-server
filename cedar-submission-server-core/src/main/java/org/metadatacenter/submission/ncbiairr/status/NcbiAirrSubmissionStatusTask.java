package org.metadatacenter.submission.ncbiairr.status;

import org.metadatacenter.config.FTPConfig;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusTask;
import org.metadatacenter.submission.upload.ftp.UploaderCreationException;

import java.io.IOException;

public class NcbiAirrSubmissionStatusTask extends SubmissionStatusTask {

  private FTPConfig ftpConfig;
  private String submissionFolder;

  // TODO: keep the statusURL in the parent. Not needed here.
  public NcbiAirrSubmissionStatusTask(String submissionID, String userID, String statusURL,
                                      FTPConfig ftpConfig, String submissionFolder) {
    super(submissionID, userID, statusURL);
    this.ftpConfig = ftpConfig;
    this.submissionFolder = submissionFolder;
  }

  @Override
  protected SubmissionStatus callSubmissionStatusEndpoint() throws IOException, UploaderCreationException {
    return NcbiAirrFtpStatusChecker.getNcbiAirrSubmissionStatus(getSubmissionID());
  }

  public FTPConfig getFtpConfig() {
    return ftpConfig;
  }

  public String getSubmissionFolder() {
    return submissionFolder;
  }
}
