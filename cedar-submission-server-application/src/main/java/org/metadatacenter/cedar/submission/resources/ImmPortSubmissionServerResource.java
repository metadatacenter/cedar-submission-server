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
import org.metadatacenter.submission.biosample.CEDARSubmissionStatus;
import org.metadatacenter.submission.biosample.CEDARSubmitResponse;
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

import static org.metadatacenter.constant.HttpConstants.CONTENT_TYPE_APPLICATION_JSON;
import static org.metadatacenter.constant.HttpConstants.HTTP_AUTH_HEADER_BEARER_PREFIX;
import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_ACCEPT;
import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_AUTHORIZATION;
import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;
import static org.metadatacenter.util.json.JsonMapper.MAPPER;

@Path("/command") @Produces(MediaType.APPLICATION_JSON) public class ImmPortSubmissionServerResource
  extends CedarMicroserviceResource
{
  private static final ObjectMapper objectMapper = new ObjectMapper();

  final static Logger logger = LoggerFactory.getLogger(ImmPortSubmissionServerResource.class);

  // TODO Get from environment variables
  private static final String IMMPORT_CEDAR_USER_NAME = "cedaruser";
  private static final String IMMPORT_CEDAR_USER_PASSWORD = "GoCedar2017#";

  private static final String IMMPORT_TOKEN_URL = "https://auth.dev.immport.org/auth/token";
  private static final String IMMPORT_STATUS_URL_BASE = "https://api.dev.immport.org/data/upload/registration/";
  private static final String IMMPORT_WORKSPACES_URL_BASE = "https://api.dev.immport.org/users/";
  private static final String IMMPORT_WORKSPACES_URL =
    IMMPORT_WORKSPACES_URL_BASE + IMMPORT_CEDAR_USER_NAME + "/workspaces";
  private static final String IMMPORT_SUBMISSION_URL = "https://api.dev.immport.org/data/upload";

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
    if (!token.isPresent()) {
      logger.warn("Could not get an ImmPort token");
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    try {
      CloseableHttpClient client = HttpClientBuilder.create().build();
      HttpGet get = new HttpGet(IMMPORT_WORKSPACES_URL);
      get.setHeader("Authorization", "bearer " + token.get());
      get.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
      response = client.execute(get);

      if (response.getStatusLine().getStatusCode() == 200) {
        HttpEntity entity = response.getEntity();
        return Response.ok(immPortWorkspacesResponseBody2CEDARWorkspaceResponse(entity)).build();
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
    CloseableHttpClient client = HttpClientBuilder.create().build();
    CloseableHttpResponse response = null;

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    Optional<String> token = getImmPortToken();
    if (!token.isPresent())
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

    try {
      if (ServletFileUpload.isMultipartContent(request)) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        String workspaceID = request.getParameter("workspaceId");
        if (workspaceID == null || workspaceID.isEmpty()) {
          logger.warn("No workspaceId parameter specified");
          return Response.status(Response.Status.BAD_REQUEST).build();
        }
        builder.addTextBody("workspaceId", workspaceID);
        builder.addTextBody("username", IMMPORT_CEDAR_USER_NAME);

        File tempDir = Files.createTempDir();
        List<FileItem> fileItems = new ServletFileUpload(new DiskFileItemFactory(1024 * 1024, tempDir)).
          parseRequest(request);

        for (FileItem fileItem : fileItems) {
          String fileName = fileItem.getName();
          String fieldName = fileItem.getFieldName();
          if (!fileItem.isFormField()) {
            if ("instance".equals(fieldName)) {
              InputStream is = fileItem.getInputStream();
              builder.addBinaryBody("file", is, ContentType.DEFAULT_BINARY, fileName);
            } else { // The user-supplied files
              InputStream is = fileItem.getInputStream();
              builder.addBinaryBody("file", is, ContentType.DEFAULT_BINARY, fileName);
            }
          }
        }

        HttpEntity multiPartRequestEntity = builder.build();
        HttpPost post = new HttpPost(IMMPORT_SUBMISSION_URL);
        post.setEntity(multiPartRequestEntity);
        post.setHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_BEARER_PREFIX + token.get());
        post.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
        response = client.execute(post);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == Response.Status.OK.getStatusCode()) {
          CEDARSubmitResponse todo = immPortSubmissionResponseBody2CEDARSubmissionResponse(response.getEntity());
          getImmPortSubmissionStatus(todo.getSubmissionID());
          return Response.ok(immPortSubmissionResponseBody2CEDARSubmissionResponse(response.getEntity())).build();
        } else if (statusCode == Response.Status.BAD_REQUEST.getStatusCode()) {
          HttpEntity entity = response.getEntity();
          String responseBody = EntityUtils.toString(entity);
          logger.warn("Unexpected status code returned from " + IMMPORT_SUBMISSION_URL + ": " + response.getStatusLine()
            .getStatusCode() + "JSON " + responseBody);
          return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
          logger.warn("Unexpected status code returned from " + IMMPORT_SUBMISSION_URL + ": " + response.getStatusLine()
            .getStatusCode());
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
      } else {
        logger.warn("No form data supplied");
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
    } catch (IOException e) {
      logger.warn("IO exception connecting to host " + IMMPORT_SUBMISSION_URL + ": " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } catch (FileUploadException e) {
      logger.warn("File upload exception uploading to host " + IMMPORT_SUBMISSION_URL + ": " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      try {
        client.close();
      } catch (IOException e) {
        logger.warn("Error closing HTTP client for ImmPort submission call: " + e.getMessage());
      }
      if (response != null)
        try {
          response.close();
        } catch (IOException e) {
          logger.warn("Error closing HTTP response for ImmPort submission: " + e.getMessage());
        }
    }
  }

  private Optional<CEDARSubmissionStatus> getImmPortSubmissionStatus(String submissionID) throws CedarException
  {
    CloseableHttpResponse response = null;

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    Optional<String> token = getImmPortToken();
    if (!token.isPresent()) {
      logger.warn("Could not get an ImmPort token");
      return Optional.empty();
    }

    try {
      CloseableHttpClient client = HttpClientBuilder.create().build();
      HttpGet get = new HttpGet(IMMPORT_STATUS_URL_BASE + submissionID + "/status");
      get.setHeader("Authorization", "bearer " + token.get());
      get.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
      response = client.execute(get);

      if (response.getStatusLine().getStatusCode() == 200) {
        HttpEntity entity = response.getEntity();
        return Optional.of(immPortSubmissionStatusResponseBody2CEDARSubmissionStatusResponse(entity));
      } else {
        logger.warn("Unexpected status code calling " + IMMPORT_WORKSPACES_URL + ";status=" + response.getStatusLine()
          .getStatusCode());
        return Optional.empty();
      }
    } catch (IOException e) {
      logger.warn("IO exception connecting to host " + IMMPORT_WORKSPACES_URL + ": " + e.getMessage());
      return Optional.empty();
    } finally {
      if (response != null)
        try {
          response.close();
        } catch (IOException e) {
          logger.warn("Error closing HTTP response for ImmPort workspaces call");
        }
    }
  }

  public Optional<String> getImmPortToken()
  {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpPost post = new HttpPost(IMMPORT_TOKEN_URL);
    CloseableHttpResponse response = null;

    try {
      List<NameValuePair> parameters = new ArrayList<>(2);
      parameters.add(new BasicNameValuePair("username", IMMPORT_CEDAR_USER_NAME));
      parameters.add(new BasicNameValuePair("password", IMMPORT_CEDAR_USER_PASSWORD));
      post.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
      post.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);

      response = client.execute(post);

      if (response.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode()) {
        HttpEntity entity = response.getEntity();
        // Get ImmPortGetTokenResponse from stream
        ImmPortGetTokenResponse immportGetTokenResponse = objectMapper
          .readValue(entity.getContent(), ImmPortGetTokenResponse.class);

        if (immportGetTokenResponse.getStatus().intValue() == 200)
          return Optional.of(immportGetTokenResponse.getToken());
        else {
          logger.warn("Failed to get token from host " + IMMPORT_SUBMISSION_URL + "; ImmPort status code="
            + immportGetTokenResponse.getStatus().intValue() + ", error=" + immportGetTokenResponse.getError());
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
      try {
        client.close();
      } catch (IOException e) {
        logger.warn("Error closing HTTP client for ImmPort token get call: " + e.getMessage());
      }
      if (response != null)
        try {
          response.close();
        } catch (IOException e) {
          logger.warn("Error closing HTTP response for ImmPort token get call: " + e.getMessage());
        }
    }
  }

  private CEDARWorkspaceResponse immPortWorkspacesResponseBody2CEDARWorkspaceResponse(HttpEntity responseEntity)
    throws IOException
  {
    if (responseEntity != null) {
      String responseBody = EntityUtils.toString(responseEntity);
      JsonNode immPortWorkspaces = MAPPER.readTree(responseBody);

      if (immPortWorkspaces.has("error"))
        return createCEDARWorkspaceResponseWithError(immPortWorkspaces.get("error").textValue());
      else {
        CEDARWorkspaceResponse cedarWorkspaceResponse = new CEDARWorkspaceResponse();
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
        cedarWorkspaceResponse.setSuccess(true);
        return cedarWorkspaceResponse;
      }
    } else
      return createCEDARWorkspaceResponseWithError("No body in ImmPort response");
  }

  private CEDARSubmissionStatus immPortSubmissionStatusResponseBody2CEDARSubmissionStatusResponse(HttpEntity responseEntity)
    throws IOException
  {
    if (responseEntity != null) {
      String responseBody = EntityUtils.toString(responseEntity);
      JsonNode immPortSubmissionStatusResponseBody = MAPPER.readTree(responseBody);

      if (immPortSubmissionStatusResponseBody.has("error"))
        return createCEDARSubmissionStatus("error", immPortSubmissionStatusResponseBody.get("error").textValue());
      else {
        CEDARSubmissionStatus cedarSubmissionStatus = new CEDARSubmissionStatus();
        return cedarSubmissionStatus;
      }
    } else
      return createCEDARSubmissionStatus("error", "no response body from ImmPort");
  }

  private CEDARSubmitResponse immPortSubmissionResponseBody2CEDARSubmissionResponse(HttpEntity responseEntity)
    throws IOException
  {
    if (responseEntity != null) {
      String responseBody = EntityUtils.toString(responseEntity);
      JsonNode immPortSubmissionResponseBody = MAPPER.readTree(responseBody);

      if (immPortSubmissionResponseBody.has("error"))
        return createCEDARSubmitResponseWithError(immPortSubmissionResponseBody.get("error").textValue());
      else {
        CEDARSubmitResponse cedarSubmitResponse = new CEDARSubmitResponse();
        if (!immPortSubmissionResponseBody.has("uploadTicketStatusUiUrl"))
          return createCEDARSubmitResponseWithError("No uploadTicketStatusUiURL field in ImmPort submit response");
        else if (!immPortSubmissionResponseBody.has("status"))
          return createCEDARSubmitResponseWithError("No status field in ImmPort submit response");
        else if (!immPortSubmissionResponseBody.has("uploadTicketNumber"))
          return createCEDARSubmitResponseWithError("No uploadTicketNumber field in ImmPort submit response");

        cedarSubmitResponse.setStatusURL(immPortSubmissionResponseBody.get("uploadTicketStatusUiUrl").textValue());
        cedarSubmitResponse.setStatus(immPortSubmissionResponseBody.get("status").textValue());
        cedarSubmitResponse.setSubmissionID(immPortSubmissionResponseBody.get("uploadTicketNumber").textValue());
        cedarSubmitResponse.setSuccess(true);
        return cedarSubmitResponse;
      }
    } else
      return createCEDARSubmitResponseWithError("No JSON in ImmPort submit response");
  }

  private CEDARSubmitResponse createCEDARSubmitResponseWithError(String errorMessage)
  {
    CEDARSubmitResponse cedarSubmitResponse = new CEDARSubmitResponse();

    cedarSubmitResponse.setError(errorMessage);
    cedarSubmitResponse.setSuccess(false);

    return cedarSubmitResponse;
  }

  private CEDARWorkspaceResponse createCEDARWorkspaceResponseWithError(String errorMessage)
  {
    CEDARWorkspaceResponse cedarWorkspaceResponse = new CEDARWorkspaceResponse();

    cedarWorkspaceResponse.setError(errorMessage);
    cedarWorkspaceResponse.setSuccess(false);

    return cedarWorkspaceResponse;
  }

  private CEDARSubmissionStatus createCEDARSubmissionStatus(String status, String message)
  {
    CEDARSubmissionStatus cedarSubmissionStatus = new CEDARSubmissionStatus();

    cedarSubmissionStatus.setStatus(status);
    cedarSubmissionStatus.setMessage(message);

    return cedarSubmissionStatus;
  }
}