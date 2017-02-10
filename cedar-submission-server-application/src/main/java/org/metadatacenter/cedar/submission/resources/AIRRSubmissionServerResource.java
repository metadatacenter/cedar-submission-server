package org.metadatacenter.cedar.submission.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.io.Files;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.FTPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.List;
import java.util.Optional;

/**
 * See here for submission instructions to NCBI:
 * <p>
 * https://docs.google.com/document/d/1tmPinCgaTwBkTsOwjitquFc0ZUN65w5xZs30q5phRkY/edit
 */
@Path("/command") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.MULTIPART_FORM_DATA) public class AIRRSubmissionServerResource
{
  private static final String NCBI_FTP_HOST = "ftp-private.ncbi.nlm.nih.gov";
  private static final String NCBI_FTP_USER = "CEDAR";
  private static final String NCBI_FTP_PASSWORD = "N7DD5Hs9";
  private static final String REMOTE_SUBMISSION_DIRECTORY = "submit/Test";

  final static Logger logger = LoggerFactory.getLogger(AIRRSubmissionServerResource.class);

  protected @Context UriInfo uriInfo;

  protected @Context HttpServletRequest request;

  protected @Context HttpServletResponse response;

  private final CedarConfig cedarConfig;

  public AIRRSubmissionServerResource(CedarConfig cedarConfig)
  {
    this.cedarConfig = cedarConfig;
  }

  @POST @Timed @Path("/submit-airr") public Response submitAIRR()
  {

    Optional<FTPClient> ftpClient = createFTPClient(cedarConfig.getSubmissionConfig().getNcbi().getSra().getFtp());

    try {
      if (ServletFileUpload.isMultipartContent(request)) {
        File tempDir = Files.createTempDir();
        List<FileItem> fileItems = new ServletFileUpload(new DiskFileItemFactory(1024 * 1024, tempDir)).
          parseRequest(request);

        if (ftpClient.isPresent()) {
          for (FileItem fileItem : fileItems) {
            String fileName = fileItem.getName();
            String contentType = fileItem.getContentType();

            if (!fileItem.isFormField()) {
              InputStream is = fileItem.getInputStream();
              logger.info("Uploading file " + fileName);
              ftpClient.get().storeFile(fileName, is);
              is.close();
            }
          }
        } else {
          logger.warn("Failed to connect to FTP host " + NCBI_FTP_HOST);
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
      }
      return Response.ok().build();
    } catch (IOException e) {
      logger.warn("IO exception " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } catch (FileUploadException e) {
      logger.warn("File upload exception " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      try {
        if (ftpClient.isPresent())
          ftpClient.get().disconnect();
      } catch (IOException e) {
        logger.warn("Exception disconnecting from FTP host: " + e.getMessage());
      }
    }
  }

  public Optional<FTPClient> createFTPClient(FTPConfig ftpConfig)
  {
    FTPClient ftpClient = new FTPClient();

    try {
      ftpClient.connect(ftpConfig.getHost());

      if (!ftpClient.login(ftpConfig.getUser(), ftpConfig.getPassword())) {
        ftpClient.logout();
        logger.warn("Failure logging in to FTP host " + ftpConfig.getHost());
        return Optional.empty();
      } else {
        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
          ftpClient.disconnect();
          logger.warn("Failed to connect to FTP host " + ftpConfig.getHost() + ", reply = " + reply);
          return Optional.empty();
        } else {
          ftpClient.enterLocalPassiveMode();
          ftpClient.changeWorkingDirectory(ftpConfig.getSubmissionDirectory());
          logger.info("Connected to FTP host " + ftpConfig.getHost() + "; current directory is " + ftpClient
            .printWorkingDirectory());
          return Optional.of(ftpClient);
        }
      }
    } catch (SocketException e) {
      logger.warn("Socket exception connecting to FTP host: " + e.getMessage());
      return Optional.empty();
    } catch (IOException e) {
      logger.warn("IO exception connecting to FTP host: " + e.getMessage());
      return Optional.empty();
    }
  }
}
