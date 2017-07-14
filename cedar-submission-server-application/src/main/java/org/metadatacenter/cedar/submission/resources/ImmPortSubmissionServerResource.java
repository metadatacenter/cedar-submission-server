package org.metadatacenter.cedar.submission.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.io.Files;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command") @Produces(MediaType.APPLICATION_JSON) public class ImmPortSubmissionServerResource
  extends CedarMicroserviceResource
{
  final static Logger logger = LoggerFactory.getLogger(ImmPortSubmissionServerResource.class);

  private static final String IMMPORT_TOKEN_URL = "https://auth.dev.immport.org/auth/token";
  private static final String IMMPORT_SUBMISSION_URL = " https://api.dev.immport.org/data/upload";

  public ImmPortSubmissionServerResource(CedarConfig cedarConfig)
  {
    super(cedarConfig);
  }

  @POST @Timed @Path("/immport-submit") @Consumes(MediaType.MULTIPART_FORM_DATA) public Response submitImmPort()
    throws CedarException
  {
    CloseableHttpResponse response = null;

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    try {
      if (ServletFileUpload.isMultipartContent(request)) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        File tempDir = Files.createTempDir();
        List<FileItem> fileItems = new ServletFileUpload(new DiskFileItemFactory(1024 * 1024, tempDir)).
          parseRequest(request);

        for (FileItem fileItem : fileItems) {
          String fileName = fileItem.getName();
          String fieldName = fileItem.getFieldName();
          if (!fileItem.isFormField()) {
            if ("instance".equals(fieldName)) {
              InputStream is = fileItem.getInputStream();
              builder.addBinaryBody(fieldName, is, ContentType.APPLICATION_JSON, fileName);
            } else { // The user-supplied files
              InputStream is = fileItem.getInputStream();
              builder.addBinaryBody(fieldName, is, ContentType.DEFAULT_BINARY, fileName);
            }
          }
        }

        HttpEntity multiPartRequestEntity = builder.build();
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(IMMPORT_SUBMISSION_URL);
        post.setEntity(multiPartRequestEntity);
        response = client.execute(post);

        if (response.getStatusLine().getStatusCode() == 200) {
          HttpEntity entity = response.getEntity();
          InputStream responseStream = entity.getContent();
          return null;
        } else
          return null; //generateUnexpectedStatusCodeCEDARBioSampleValidationResponse(response.getStatusLine().getStatusCode());
      }
      return Response.ok().build();
    } catch (IOException e) {
      logger.warn("IO exception connecting to host " + IMMPORT_SUBMISSION_URL + ": " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } catch (FileUploadException e) {
      logger.warn("File upload exception uploading to host " + IMMPORT_SUBMISSION_URL + ": " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      if (response != null)
        try {
          response.close();
        } catch (IOException e) {
          logger.warn("Error closing HTTP response for ImmPort submission");
        }
    }
  }
}