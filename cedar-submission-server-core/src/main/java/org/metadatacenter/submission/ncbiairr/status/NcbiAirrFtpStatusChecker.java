package org.metadatacenter.submission.ncbiairr.status;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.metadatacenter.config.FTPConfig;
import org.metadatacenter.submission.status.SubmissionState;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusDescriptor;
import org.metadatacenter.submission.status.SubmissionStatusManager;
import org.metadatacenter.submission.upload.ftp.UploaderCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NcbiAirrFtpStatusChecker {

  final static Logger logger = LoggerFactory.getLogger(NcbiAirrFtpStatusChecker.class);

  public static SubmissionStatus getNcbiAirrSubmissionStatus(String submissionID) throws IOException,
      UploaderCreationException {

    SubmissionStatusDescriptor submissionStatusDescriptor =
        SubmissionStatusManager.getInstance().getCurrentSubmissions().get(submissionID);
    NcbiAirrSubmissionStatusTask submissionStatusTask =
        (NcbiAirrSubmissionStatusTask) submissionStatusDescriptor.getSubmissionStatusTask();

    FTPConfig ftpConfig = submissionStatusTask.getFtpConfig();
    String submissionPath = ftpConfig.getSubmissionDirectory() + "/" + submissionStatusTask.getSubmissionFolder();

    logger.info("Checking NCBI submission status (submissionPath: " + submissionPath + ")");

    // Open the FTP connection
    FTPClient ftpClient = connect(ftpConfig.getHost(), ftpConfig.getUser(),
        ftpConfig.getPassword());

    // TODO: remove this temp path


    // Go to the submission folder
    if (!ftpClient.changeWorkingDirectory(submissionPath)) {
      throw new IOException("Couldn't go to the submission folder (path: " + submissionPath + ")");
    }

    FTPFile[] files = ftpClient.listFiles();
    System.out.println("----------------------------------------");
    for (FTPFile file : files) {
      String details = file.getName();
      if (file.isDirectory()) {
        details = "[" + details + "]";
      }
      details += "\t\t" + file.getSize();
      System.out.println(details);
    }
    System.out.println("----------------------------------------");

    // Get the submission status
    // ...

    // Close the FTP connection
    ftpClient.disconnect();



    return new SubmissionStatus(submissionID, SubmissionState.COMPLETED, "Simulated submission completed");
  }

  public static FTPClient connect(String host, String user, String password) throws UploaderCreationException {
    FTPClient ftpClient = new FTPClient();
    try {
      ftpClient.connect(host);
      int replyCode = ftpClient.getReplyCode();
      if (!FTPReply.isPositiveCompletion(replyCode)) {
        showServerReply(ftpClient);
        throw new UploaderCreationException("Failed to connect to the FTP server: " + host);
      }
      boolean success = ftpClient.login(user, password);
      if (!success) {
        ftpClient.disconnect();
        showServerReply(ftpClient);
        throw new UploaderCreationException("Invalid username and password to login to the FTP server: " + host);
      }
      ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      ftpClient.enterLocalPassiveMode();
      ftpClient.setControlKeepAliveTimeout(300); // set timeout to 5 minutes

      return ftpClient;
    } catch (IOException ex) {
      logger.error(ex.getMessage());
      throw new UploaderCreationException("Error while creating the FTP client", ex);
    }
  }


  private static void showServerReply(FTPClient ftpClient) {
    String[] replies = ftpClient.getReplyStrings();
    if (replies != null && replies.length > 0) {
      for (String reply : replies) {
        logger.error(reply);
      }
    }
  }
}

