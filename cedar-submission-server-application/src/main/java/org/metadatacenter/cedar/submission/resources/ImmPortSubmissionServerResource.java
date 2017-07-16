package org.metadatacenter.cedar.submission.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.submission.biosample.CEDARWorkspaceResponse;
import org.metadatacenter.submission.biosample.ImmPortGetTokenResponse;
import org.metadatacenter.submission.biosample.Workspace;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;
import static org.metadatacenter.util.json.JsonMapper.MAPPER;

@Path("/command") @Produces(MediaType.APPLICATION_JSON) public class ImmPortSubmissionServerResource
  extends CedarMicroserviceResource
{
  private static final ObjectMapper objectMapper = new ObjectMapper();

  final static Logger logger = LoggerFactory.getLogger(ImmPortSubmissionServerResource.class);

  // TODO Get from environment variables
  private static final String IMMPORT_BASE_URL = "https://auth.dev.immport.org";
  private static final String IMMPORT_CEDAR_USER_NAME = "cedaruser";
  private static final String IMMPORT_CEDAR_USER_PASSWORD = "GoCedar2017#";

  private static final String IMMPORT_TOKEN_URL = IMMPORT_BASE_URL + "/auth/token";
  private static final String IMMPORT_WORKSPACES_URL_BASE = IMMPORT_BASE_URL + "/users/";
  private static final String IMMPORT_WORKSPACES_URL =
    IMMPORT_WORKSPACES_URL_BASE + IMMPORT_CEDAR_USER_NAME + "/workspaces";
  private static final String IMMPORT_SUBMISSION_URL = IMMPORT_BASE_URL + "/data/upload";

  public ImmPortSubmissionServerResource(CedarConfig cedarConfig)
  {
    super(cedarConfig);
  }

  @POST @Timed @Path("/immport-workspaces") @Consumes(MediaType.MULTIPART_FORM_DATA) public Response immPortWorkspaces()
    throws CedarException
  {
    CloseableHttpResponse response = null;

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    Optional<String> token = getImmPortToken();
    if (!token.isPresent())
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

    try {
      CloseableHttpClient client = HttpClientBuilder.create().build();
      HttpGet get = new HttpGet(IMMPORT_WORKSPACES_URL);
      get.setHeader("Authorization", "Bearer " + token.get());
      response = client.execute(get);

      if (response.getStatusLine().getStatusCode() == 200) {
        HttpEntity entity = response.getEntity();
        return Response.ok(immPortWorkspaces2CEDARWorkspaces(entity)).build();
      } else {
        logger.warn("Unexpected status code calling " + IMMPORT_WORKSPACES_URL + ";status=" + response.getStatusLine()
          .getStatusCode());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    } catch (IOException e) {
      logger.warn("IO exception connecting to host " + IMMPORT_WORKSPACES_URL + ": " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      if (response != null)
        try {
          response.close();
        } catch (IOException e) {
          logger.warn("Error closing HTTP response for ImmPort workspaces call");
        }
    }
  }

  @POST @Timed @Path("/immport-submit") @Consumes(MediaType.MULTIPART_FORM_DATA) public Response submitImmPort()
    throws CedarException
  {
    CloseableHttpResponse response = null;

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    Optional<String> token = getImmPortToken();
    if (!token.isPresent())
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

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
          return null; //generateUnexpectedStatusCodeSubmitResponse(response.getStatusLine().getStatusCode());
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

  public Optional<String> getImmPortToken()
  {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpPost post = new HttpPost(IMMPORT_TOKEN_URL);
    CloseableHttpResponse response = null;

    try {
      List<NameValuePair> params = new ArrayList<>(2);
      params.add(new BasicNameValuePair("username", IMMPORT_CEDAR_USER_NAME));
      params.add(new BasicNameValuePair("password", IMMPORT_CEDAR_USER_PASSWORD));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
      post.setHeader("Accept", "application/json");

      response = client.execute(post);

      if (response.getStatusLine().getStatusCode() == 200) {
        HttpEntity entity = response.getEntity();
        // Get ImmPortGetTokenResponse from stream
        ImmPortGetTokenResponse immportGetTokenResponse = objectMapper
          .readValue(entity.getContent(), ImmPortGetTokenResponse.class);

        if (immportGetTokenResponse.getStatus().intValue() == 200)
          return Optional.of(immportGetTokenResponse.getToken());
        else {
          logger.warn("Failed to get token from host " + IMMPORT_SUBMISSION_URL + "; ImmPort response code="
            + immportGetTokenResponse.getStatus().intValue() + ", error+" + immportGetTokenResponse.getError());
          return Optional.empty();
        }
      } else {
        logger.warn(
          "Failed to get token from host " + IMMPORT_SUBMISSION_URL + "; status code=" + response.getStatusLine()
            .getStatusCode());
        return Optional.empty();
      }
    } catch (IOException e) {
      logger.warn("IO error getting token from host " + IMMPORT_SUBMISSION_URL + "; error=" + e.getMessage());
      return Optional.empty();
    } finally {
      if (response != null)
        try {
          response.close();
        } catch (IOException e) {
          logger.warn("Error closing HTTP response for ImmPort token get call");
        }
    }
  }

  private CEDARWorkspaceResponse immPortWorkspaces2CEDARWorkspaces(HttpEntity responseEntity) throws IOException
  {
    CEDARWorkspaceResponse cedarWorkspaceResponse = new CEDARWorkspaceResponse();

    if (responseEntity != null) {
      String responseBody = EntityUtils.toString(responseEntity);
      JsonNode immPortWorkspaces = MAPPER.readTree(responseBody);

      if (immPortWorkspaces.has("error")) {
        cedarWorkspaceResponse.setError(immPortWorkspaces.get("error").textValue());
        return cedarWorkspaceResponse;
      } else {
        List<Workspace> workspaces = new ArrayList<>();
        Iterator<String> fieldNames = immPortWorkspaces.fieldNames();
        while (fieldNames.hasNext()) {
          String fieldName = fieldNames.next();
          String fieldValue = immPortWorkspaces.get(fieldName).asText();
          Workspace workspace = new Workspace();
          workspace.setWorkspaceID(fieldName);
          workspace.setWorkspaceName(fieldValue);
          workspaces.add(workspace);
        }
        cedarWorkspaceResponse.setWorkspaces(workspaces);
      }
      return cedarWorkspaceResponse;
    } else {
      cedarWorkspaceResponse.setError("No body in ImmPort response");
      return cedarWorkspaceResponse;
    }
  }
}