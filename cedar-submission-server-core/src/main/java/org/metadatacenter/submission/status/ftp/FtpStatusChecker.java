package org.metadatacenter.submission.status.ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.metadatacenter.submission.upload.ftp.UploaderCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class FtpStatusChecker {

    final static Logger logger = LoggerFactory.getLogger(org.metadatacenter.submission.upload.ftp.FtpUploader.class);

    private final FTPClient ftpClient;
    private final String userDirectory;

//    public FtpStatusChecker(@Nonnull FTPClient ftpClient) {
//      this(ftpClient, "/");
//    }

    private FtpStatusChecker(@Nonnull FTPClient ftpClient, @Nonnull String userDirectory) {
      this.ftpClient = checkNotNull(ftpClient);
      this.userDirectory = checkNotNull(userDirectory);
    }

    private void changeToUserDirectory() throws IOException {
      ftpClient.changeToParentDirectory();
      ftpClient.changeWorkingDirectory(userDirectory);
    }

    public static FtpStatusChecker getStatusChecker(@Nonnull String hostname, @Nonnull String username, @Nonnull String password, @Nonnull Optional<String> userDirectory)
        throws UploaderCreationException {

      FTPClient ftpClient = new FTPClient();
      try {
        ftpClient.connect(hostname);
        int replyCode = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(replyCode)) {
          showServerReply(ftpClient);
          throw new UploaderCreationException("Failed to connect to the FTP server: " + hostname);
        }
        boolean success = ftpClient.login(username, password);
        if (!success) {
          ftpClient.disconnect();
          showServerReply(ftpClient);
          throw new UploaderCreationException("Invalid username and password to login to the FTP server: " + hostname);
        }
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setControlKeepAliveTimeout(300); // set timeout to 5 minutes
        return new FtpStatusChecker(ftpClient, userDirectory.orElse("/"));
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

    public void disconnect() throws IOException {
      ftpClient.disconnect();
    }
  }

