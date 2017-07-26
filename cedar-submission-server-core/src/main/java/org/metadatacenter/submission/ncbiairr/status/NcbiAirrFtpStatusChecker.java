package org.metadatacenter.submission.ncbiairr.status;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.metadatacenter.config.FTPConfig;
import org.metadatacenter.submission.Constants;
import org.metadatacenter.submission.status.SubmissionState;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusDescriptor;
import org.metadatacenter.submission.status.SubmissionStatusManager;
import org.metadatacenter.submission.upload.ftp.UploaderCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Optional;

public class NcbiAirrFtpStatusChecker {

  final static Logger logger = LoggerFactory.getLogger(NcbiAirrFtpStatusChecker.class);

  public static SubmissionStatus getNcbiAirrSubmissionStatus(String submissionID, FTPConfig ftpConfig,
                                                             String submissionFolder, String lastStatusReportFile)
      throws IOException,
      UploaderCreationException, ParserConfigurationException, SAXException {

    String submissionPath = ftpConfig.getSubmissionDirectory() + "/" + submissionFolder;

    logger.info("Checking NCBI submission status (submissionPath: " + submissionPath + ")");

    // Open the FTP connection
    FTPClient ftpClient = connect(ftpConfig.getHost(), ftpConfig.getUser(),
        ftpConfig.getPassword());

    // TODO: remove this temp path
    submissionPath = "submit/Test/2017-07-24T20-48-57.829Z_test";

    // Go to the submission folder
    if (!ftpClient.changeWorkingDirectory(submissionPath)) {
      SubmissionStatusManager.getInstance().removeSubmission(submissionID);
      throw new IOException("Couldn't go to the submission folder (path: " + submissionPath + ")");
    }

    FTPFile[] files = ftpClient.listFiles();
    System.out.println("----------------------------------------");

    Optional<String> mostRecentReportFileName = getMostRecentReportFileName(files);

    SubmissionStatus status = null;
    if (mostRecentReportFileName.isPresent()) { // the folder contains a report file (at the minimum)

      if (!mostRecentReportFileName.get().equals(lastStatusReportFile)) { // there is a new report
        // update the variable that stores the name of the last report checked
        SubmissionStatusDescriptor submissionStatusDescriptor = SubmissionStatusManager.getInstance()
            .getCurrentSubmissions().get(submissionID);
        NcbiAirrSubmissionStatusTask statusTask = (NcbiAirrSubmissionStatusTask) submissionStatusDescriptor
            .getSubmissionStatusTask();
        statusTask.setLastStatusReportFile(mostRecentReportFileName.get());


        // check the content of the report files
        status = new SubmissionStatus(submissionID, SubmissionState.STARTED, "THE STATUS HAS BEEN UPDATED!!");
        // TODO: Updates found. Notify the user.
      } else { // the report file has already been checked
        status = new SubmissionStatus(submissionID, SubmissionState.STARTED, "no changes");
      }

    } else { // the folder does not contain any report file yet
      status = new SubmissionStatus(submissionID, SubmissionState.STARTED, "no changes");
    }

    System.out.println("----------------------------------------");

    // Close the FTP connection
    ftpClient.disconnect();
    return status;
  }


  private static Optional<String> getMostRecentReportFileName(FTPFile[] files) throws ParserConfigurationException {
    // create a new DocumentBuilderFactory
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // use the factory to create a documentbuilder
    DocumentBuilder builder = factory.newDocumentBuilder();
    String lastReportFileName = null;
    int lastReportNumber = -1;
    for (FTPFile file : files) {
      if (file.isFile()) {
        if (file.getName().matches(Constants.NCBI_AIRR_REPORT_REGEX)) {
          int currentReportNumber = getReportNumber(file.getName());
          if (currentReportNumber > lastReportNumber) {
            lastReportFileName = file.getName();
            lastReportNumber = currentReportNumber;
          }
        }
      }
    }
    return Optional.ofNullable(lastReportFileName);
  }

  /**
   * Returns the number of the status report from the file name (e.g., report.2.xml -> 2)
   */
  private static int getReportNumber(String fileName) {
    int index1 = 6; // index of the first '.'
    int index2 = fileName.indexOf(".xml");
    return Integer.parseInt(fileName.substring(index1 + 1, index2));
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

