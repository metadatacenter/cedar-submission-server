package org.metadatacenter.submission.ncbiairr.status;

import org.metadatacenter.config.FTPConfig;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusTask;

public class NcbiAirrSubmissionStatusTask extends SubmissionStatusTask {

  private FTPConfig ftpConfig;
  private String submissionFolder;
  private String lastStatusReportFile;

  // TODO: keep the statusURL in the parent. Not needed here.
  public NcbiAirrSubmissionStatusTask(String submissionID, String userID, String statusURL,
                                      FTPConfig ftpConfig, String submissionFolder) {
    super(submissionID, userID, statusURL);
    this.ftpConfig = ftpConfig;
    this.submissionFolder = submissionFolder;
  }

  @Override
  protected SubmissionStatus callSubmissionStatusEndpoint() throws Exception {
    return NcbiAirrFtpStatusChecker.getNcbiAirrSubmissionStatus(getSubmissionID(), ftpConfig, submissionFolder, lastStatusReportFile);
  }

  public FTPConfig getFtpConfig() {
    return ftpConfig;
  }

  public String getSubmissionFolder() {
    return submissionFolder;
  }

  public String getLastStatusReportFile() {
    return lastStatusReportFile;
  }

  public synchronized void setLastStatusReportFile(String lastStatusReportFile) {
    this.lastStatusReportFile = lastStatusReportFile;
  }
}
