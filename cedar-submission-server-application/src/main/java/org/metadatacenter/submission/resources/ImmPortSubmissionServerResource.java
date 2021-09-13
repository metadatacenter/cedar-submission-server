package org.metadatacenter.submission.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Files;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.trimmer.JsonLdDocument;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.submission.CEDARSubmitResponse;
import org.metadatacenter.submission.CEDARWorkspaceResponse;
import org.metadatacenter.submission.Workspace;
import org.metadatacenter.submission.exception.SubmissionInstanceNotFoundException;
import org.metadatacenter.submission.immport.ImmPortConstants;
import org.metadatacenter.submission.immport.ImmPortSubmissionStatusTask;
import org.metadatacenter.submission.immport.ImmPortUtil;
import org.metadatacenter.submission.status.SubmissionStatusManager;
import org.metadatacenter.submission.status.SubmissionType;
import org.metadatacenter.submission.upload.flow.FileUploadStatus;
import org.metadatacenter.submission.upload.flow.FlowData;
import org.metadatacenter.submission.upload.flow.FlowUploadUtil;
import org.metadatacenter.submission.upload.flow.SubmissionUploadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.metadatacenter.constant.HttpConstants.*;
import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;
import static org.metadatacenter.util.json.JsonMapper.MAPPER;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
public class ImmPortSubmissionServerResource extends CedarMicroserviceResource {
  private static final Logger logger = LoggerFactory.getLogger(ImmPortSubmissionServerResource.class);

  private final String immPortSubmissionUrl;
  private final String immPortUserName;
  private final ImmPortUtil immPortUtil;

  public ImmPortSubmissionServerResource(CedarConfig cedarConfig) {
    super(cedarConfig);
    immPortSubmissionUrl = cedarConfig.getSubmissionConfig().getImmPort().getSubmissionEndpoint().getUrl();
    immPortUserName = cedarConfig.getSubmissionConfig().getImmPort().getAuthentication().getUser();
    immPortUtil = new ImmPortUtil(cedarConfig);
  }

  private HttpEntity getMultipartContentFromSubmission(String submissionID, String workspaceID)
      throws IOException, JAXBException, DatatypeConfigurationException {
    List<String> submissionMetadataFilePaths = getSubmissionMetadataFilePaths(submissionID);
    List<String> submissionDataFilePaths = getSubmissionDataFilePaths(submissionID);
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();

    builder.addTextBody(ImmPortConstants.IMMPORT_WORKSPACE_ID_FIELD, workspaceID);
    builder.addTextBody(ImmPortConstants.IMMPORT_USERNAME_FIELD, immPortUserName);

    for (String submissionMetadataFilePath : submissionMetadataFilePaths) {
      File submissionMetadataFile = new File(submissionMetadataFilePath);
      InputStream submissionMetadataJSONLDFileInputStream = new FileInputStream(submissionMetadataFile);
      // TODO Need more checking here to ensure it is a JSON file
      JsonNode jsonLDNode = MAPPER.readTree(submissionMetadataJSONLDFileInputStream);
      JsonNode jsonNode = new JsonLdDocument(jsonLDNode).asJson(); // Translate from JSON-LD to JSON
      InputStream submissionMetadataJSONFileInputStream = IOUtils
          .toInputStream(jsonNode.toString(), StandardCharsets.UTF_8);
      builder.addBinaryBody("file", submissionMetadataJSONFileInputStream, ContentType.APPLICATION_JSON,
          submissionMetadataFile.getName());
    }

    for (String submissionDataFilePath : submissionDataFilePaths) {
      File submissionDataFile = new File(submissionDataFilePath);
      InputStream submissionFileInputStream = new FileInputStream(submissionDataFile);
      builder
          .addBinaryBody("file", submissionFileInputStream, ContentType.DEFAULT_BINARY, submissionDataFile.getName());
    }

    return builder.build();
  }

  private static List<String> getSubmissionMetadataFilePaths(String submissionId)
      throws IOException, JAXBException, DatatypeConfigurationException {
    List<String> submissionMetadataFilePaths = new ArrayList<>();

    Map<String, FileUploadStatus> submissionUploadStatus = SubmissionUploadManager.getInstance()
        .getSubmissionsUploadStatus(submissionId).getFilesUploadStatus();

    for (Map.Entry<String, FileUploadStatus> entry : submissionUploadStatus.entrySet()) {
      FileUploadStatus fileUploadStatus = entry.getValue();
      if (fileUploadStatus.isMetadataFile()) {
        submissionMetadataFilePaths.add(fileUploadStatus.getFileLocalPath());
      }
    }
    return submissionMetadataFilePaths;
  }

  private static List<String> getSubmissionDataFilePaths(String submissionId)
      throws IOException, JAXBException, DatatypeConfigurationException {
    List<String> submissionDataFilePaths = new ArrayList<>();

    Map<String, FileUploadStatus> submissionUploadStatus = SubmissionUploadManager.getInstance()
        .getSubmissionsUploadStatus(submissionId).getFilesUploadStatus();

    for (Map.Entry<String, FileUploadStatus> entry : submissionUploadStatus.entrySet()) {
      FileUploadStatus fileUploadStatus = entry.getValue();
      if (!fileUploadStatus.isMetadataFile()) {
        submissionDataFilePaths.add(fileUploadStatus.getFileLocalPath());
      }
    }
    return submissionDataFilePaths;
  }

  @GET
  @Timed
  @Path("/immport-workspaces")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response immPortWorkspaces() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    Optional<String> immPortBearerToken = immPortUtil.getImmPortBearerToken();
    if (!immPortBearerToken.isPresent()) {
      logger.warn("Could not get an ImmPort token");
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();  // TODO CEDAR error response
    }

    CloseableHttpResponse response = null;
    CloseableHttpClient client = null;

    String workspaceUrl = immPortUtil.getWorkspaceUrl();

    try {
      HttpGet get = new HttpGet(workspaceUrl);
      get.setHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_BEARER_PREFIX + immPortBearerToken.get());
      get.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
      client = HttpClientBuilder.create().build();
      response = client.execute(get);

      if (response.getStatusLine().getStatusCode() == 200) {
        HttpEntity entity = response.getEntity();
        return Response.ok(immPortWorkspacesResponseBody2CEDARWorkspaceResponse(entity)).build();
      } else {
        logger.warn("Unexpected status code calling " + workspaceUrl + "; status=" + response
            .getStatusLine().getStatusCode());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
      }
    } catch (IOException e) {
      logger.warn("IO exception connecting to host " + workspaceUrl + ": " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
    } finally {
      HttpClientUtils.closeQuietly(response);
      HttpClientUtils.closeQuietly(client);
    }
  }

  @POST
  @Timed
  @Path("/immport-submit")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response submitImmPort() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    Optional<String> immPortBearerToken = immPortUtil.getImmPortBearerToken();
    if (!immPortBearerToken.isPresent()) {
      logger.warn("No ImmPort token found");
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
    }

    CloseableHttpResponse response = null;
    CloseableHttpClient client = null;

    try {
      if (ServletFileUpload.isMultipartContent(request)) {
        String userId = FlowUploadUtil.getLastFragmentOfUrl(c.getCedarUser().getId());
        FlowData data = FlowUploadUtil.getFlowData(request);

        String workspaceID = null;
        if (data.getAdditionalParameters().containsKey("workspaceId")) {
          workspaceID = data.getAdditionalParameters().get("workspaceId");
        } else {
          logger.warn("No workspaceId parameter specified");
          return Response.status(Response.Status.BAD_REQUEST).build();  // TODO CEDAR error response
        }
        String submissionLocalFolderPath = FlowUploadUtil
            .getSubmissionLocalFolderPath(ImmPortConstants.IMMPORT_LOCAL_FOLDER_NAME, userId, data.getSubmissionId());
        String filePath = FlowUploadUtil.saveToLocalFile(data, userId, request.getContentLength(),
            submissionLocalFolderPath);
        logger.info("File created. Path: " + filePath);
        SubmissionUploadManager.getInstance().updateStatus(data, submissionLocalFolderPath);

        if (SubmissionUploadManager.getInstance().isSubmissionUploadComplete(data.getSubmissionId())) {
          HttpEntity multiPartEntity = getMultipartContentFromSubmission(data.submissionId, workspaceID);
          HttpPost post = new HttpPost(immPortSubmissionUrl);
          post.setHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_BEARER_PREFIX + immPortBearerToken.get());
          post.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
          post.setEntity(multiPartEntity);
          client = HttpClientBuilder.create().build();
          response = client.execute(post);
          int statusCode = response.getStatusLine().getStatusCode();

          if (statusCode == Response.Status.OK.getStatusCode()) {
            CEDARSubmitResponse cedarSubmitResponse = immPortSubmissionResponseBody2CEDARSubmissionResponse(
                response.getEntity());
            SubmissionStatusManager.getInstance().addSubmission(
                new ImmPortSubmissionStatusTask(cedarSubmitResponse.getSubmissionID(), SubmissionType.IMMPORT, c
                    .getCedarUser().getId(),
                    cedarSubmitResponse.getStatusURL(), immPortUtil));
            SubmissionStatusManager.getInstance().setCedarConfig(cedarConfig);

            return Response.ok(cedarSubmitResponse).build();
          } else {
            logger.warn("Unexpected status code returned from " + immPortSubmissionUrl + ": " + response
                .getStatusLine().getStatusCode());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
          }
        } else {
          return Response.ok(new HashMap()).build(); // We are still building the request
        }
      } else {
        logger.warn("No form data supplied");
        return Response.status(Response.Status.BAD_REQUEST).build(); // TODO CEDAR error response
      }
    } catch (IOException | SubmissionInstanceNotFoundException | IllegalAccessException | FileUploadException |
        JAXBException |
        DatatypeConfigurationException e) {
      logger.warn("Exception submitting to ImmPort: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
    } finally {
      HttpClientUtils.closeQuietly(response);
      HttpClientUtils.closeQuietly(client);
    }
  }

  // Original implementation with single multipart form upload. Keep for the moment for command line testing.
  @POST
  @Timed
  @Path("/immport-submit-old")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response submitImmPortOld() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    String workspaceID = request.getParameter("workspaceId"); // TODO CEDAR constant for parameter
    if (workspaceID == null || workspaceID.isEmpty()) {
      logger.warn("No workspaceId parameter specified");
      return Response.status(Response.Status.BAD_REQUEST).build();  // TODO CEDAR error response
    }

    Optional<String> immPortBearerToken = immPortUtil.getImmPortBearerToken();
    if (!immPortBearerToken.isPresent()) {
      logger.warn("No ImmPort token found");
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
    }

    CloseableHttpResponse response = null;
    CloseableHttpClient client = null;

    try {
      if (ServletFileUpload.isMultipartContent(request)) {

        HttpEntity multiPartEntity = getMultipartContentFromRequest(workspaceID);

        HttpPost post = new HttpPost(immPortSubmissionUrl);
        post.setHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_BEARER_PREFIX + immPortBearerToken.get());
        post.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
        post.setEntity(multiPartEntity);
        client = HttpClientBuilder.create().build();
        response = client.execute(post);

        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == Response.Status.OK.getStatusCode()) {
          CEDARSubmitResponse cedarSubmitResponse = immPortSubmissionResponseBody2CEDARSubmissionResponse(
              response.getEntity());
          String submissionID = cedarSubmitResponse.getSubmissionID();
          String userID = c.getCedarUser().getId();
          String statusURL = cedarSubmitResponse.getStatusURL();
          SubmissionStatusManager.getInstance()
              .addSubmission(new ImmPortSubmissionStatusTask(submissionID, SubmissionType.IMMPORT, userID, statusURL,
                  immPortUtil));
          return Response.ok(cedarSubmitResponse).build();
        } else {
          logger.warn("Unexpected status code returned from " + immPortSubmissionUrl + ": " + response
              .getStatusLine().getStatusCode());
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
        }
      } else {
        logger.warn("No form data supplied");
        return Response.status(Response.Status.BAD_REQUEST).build(); // TODO CEDAR error response
      }
    } catch (IOException | FileUploadException e) {
      logger.warn("Exception submitting to ImmmPort " + immPortSubmissionUrl + ": " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
    } finally {
      HttpClientUtils.closeQuietly(response);
      HttpClientUtils.closeQuietly(client);
    }
  }

  private HttpEntity getMultipartContentFromRequest(String workspaceID) throws FileUploadException, IOException {
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();

    builder.addTextBody(ImmPortConstants.IMMPORT_WORKSPACE_ID_FIELD, workspaceID);
    builder.addTextBody(ImmPortConstants.IMMPORT_USERNAME_FIELD, immPortUserName);

    File tempDir = Files.createTempDir();
    List<FileItem> fileItems = new ServletFileUpload(new DiskFileItemFactory(1024 * 1024, tempDir)).
        parseRequest(request);

    for (FileItem fileItem : fileItems) {
      String fileName = fileItem.getName();
      String fieldName = fileItem.getFieldName();
      if (!fileItem.isFormField()) {
        if ("instance".equals(fieldName)) {
          InputStream submissionMetadataJSONLDFileInputStream = fileItem.getInputStream();
          //TODO Need more checking here to ensure it is a JSON file
          JsonNode jsonLDNode = MAPPER.readTree(submissionMetadataJSONLDFileInputStream);
          JsonNode jsonNode = new JsonLdDocument(jsonLDNode).asJson(); // Translate from JSON-LD to JSON
          InputStream submissionMetadataJSONFileInputStream = IOUtils
              .toInputStream(jsonNode.toString(), StandardCharsets.UTF_8);
          builder.addBinaryBody("file", submissionMetadataJSONFileInputStream, ContentType.APPLICATION_JSON, fileName);
        } else { // The user-supplied files
          InputStream is = fileItem.getInputStream();
          builder.addBinaryBody("file", is, ContentType.DEFAULT_BINARY, fileName);
        }
      }
    }
    return builder.build();
  }

  private CEDARWorkspaceResponse immPortWorkspacesResponseBody2CEDARWorkspaceResponse(HttpEntity responseEntity)
      throws IOException {
    if (responseEntity != null) {
      String responseBody = EntityUtils.toString(responseEntity, CharEncoding.UTF_8);
      JsonNode immPortWorkspaces = MAPPER.readTree(responseBody);

      if (immPortWorkspaces.has("error")) {
        return createCEDARWorkspaceResponseWithError(immPortWorkspaces.get("error").textValue());
      } else {
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
    } else {
      return createCEDARWorkspaceResponseWithError("No body in ImmPort response");
    }
  }

  private CEDARSubmitResponse immPortSubmissionResponseBody2CEDARSubmissionResponse(HttpEntity responseEntity)
      throws IOException {
    if (responseEntity != null) {
      String responseBody = EntityUtils.toString(responseEntity, CharEncoding.UTF_8);
      JsonNode immPortSubmissionResponseBody = MAPPER.readTree(responseBody);

      if (immPortSubmissionResponseBody.has("error")) {
        return createCEDARSubmitResponseWithError(immPortSubmissionResponseBody.get("error").textValue());
      } else {
        CEDARSubmitResponse cedarSubmitResponse = new CEDARSubmitResponse();
        if (!immPortSubmissionResponseBody.has("uploadTicketStatusUiUrl")) {
          return createCEDARSubmitResponseWithError("No uploadTicketStatusUiURL field in ImmPort submit response");
        } else if (!immPortSubmissionResponseBody.has("status")) {
          return createCEDARSubmitResponseWithError("No status field in ImmPort submit response");
        } else if (!immPortSubmissionResponseBody.has("uploadTicketNumber")) {
          return createCEDARSubmitResponseWithError("No uploadTicketNumber field in ImmPort submit response");
        }

        cedarSubmitResponse.setStatusURL(immPortSubmissionResponseBody.get("uploadTicketStatusUiUrl").textValue());
        cedarSubmitResponse.setStatus(immPortSubmissionResponseBody.get("status").textValue());
        cedarSubmitResponse.setSubmissionID(immPortSubmissionResponseBody.get("uploadTicketNumber").textValue());
        cedarSubmitResponse.setSuccess(true);
        return cedarSubmitResponse;
      }
    } else {
      return createCEDARSubmitResponseWithError("No JSON in ImmPort submit response");
    }
  }

  private CEDARSubmitResponse createCEDARSubmitResponseWithError(String errorMessage) {
    CEDARSubmitResponse cedarSubmitResponse = new CEDARSubmitResponse();

    cedarSubmitResponse.setError(errorMessage);
    cedarSubmitResponse.setSuccess(false);

    return cedarSubmitResponse;
  }

  private CEDARWorkspaceResponse createCEDARWorkspaceResponseWithError(String errorMessage) {
    CEDARWorkspaceResponse cedarWorkspaceResponse = new CEDARWorkspaceResponse();

    cedarWorkspaceResponse.setError(errorMessage);
    cedarWorkspaceResponse.setSuccess(false);

    return cedarWorkspaceResponse;
  }
}
