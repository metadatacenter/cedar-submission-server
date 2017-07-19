package org.metadatacenter.submission.ncbiairr.upload;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class FtpUploader implements FileUploader {

  final static Logger logger = LoggerFactory.getLogger(FtpUploader.class);

  private final FTPClient ftpClient;
  private final String userDirectory;

  public FtpUploader(@Nonnull FTPClient ftpClient) {
    this(ftpClient, "/");
  }

  public FtpUploader(@Nonnull FTPClient ftpClient, @Nonnull String userDirectory) {
    this.ftpClient = checkNotNull(ftpClient);
    this.userDirectory = checkNotNull(userDirectory);
  }

  private void changeToUserDirectory() throws IOException {
    ftpClient.changeToParentDirectory();
    ftpClient.changeWorkingDirectory(userDirectory);
  }

  public static FtpUploader createNewUploader(@Nonnull String hostname, @Nonnull String username,
                                              @Nonnull String password, @Nonnull Optional<String> userDirectory)
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
      return new FtpUploader(ftpClient, userDirectory.orElse("/"));
    } catch (IOException ex) {
      logger.error(ex.getMessage());
      throw new UploaderCreationException("Error while creating the FTP client", ex);
    }
  }

  @Override
  public void store(File file) throws IOException {
    changeToUserDirectory();
    storeFile(file);
  }

  @Override
  public void store(String directory, File file) throws IOException {
    createAndChangeToTargetDirectory(directory);
    storeFile(file);
  }

  private void storeFile(File file) throws IOException {
    InputStream is = new FileInputStream(file);
    try {
      ftpClient.storeFile(file.getName(), is);
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }

  private void createAndChangeToTargetDirectory(String directory) throws IOException {
    changeToUserDirectory();
    boolean dirExists = true;
    String[] directories = directory.split("/");
    for (String dir : directories ) {
      if (!dir.isEmpty() ) {
        if (dirExists) {
          dirExists = ftpClient.changeWorkingDirectory(dir);
        }
        if (!dirExists) {
          if (!ftpClient.makeDirectory(dir)) {
            showServerReply(ftpClient);
            throw new IOException("Unable to create remote directory: " + directory);
          }
          if (!ftpClient.changeWorkingDirectory(dir)) {
            showServerReply(ftpClient);
            throw new IOException("Unable to change the working remote directory: " + directory);
          }
        }
      }
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

  @Override
  public void disconnect() throws IOException {
    ftpClient.disconnect();
  }
}
