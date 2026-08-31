package org.metadatacenter.submission.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Files;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.io.Closer;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.http.CedarResponseStatus;
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
import org.metadatacenter.util.http.CedarResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
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
@Tag(name = "ImmPort")
@SecurityRequirement(name = "api_key")
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

  private HttpEntity getMultipartContentFromSubmission(String userId, String submissionID, String workspaceID)
      throws IOException, JAXBException, DatatypeConfigurationException {
    List<String> submissionMetadataFilePaths = getSubmissionMetadataFilePaths(userId, submissionID);
    List<String> submissionDataFilePaths = getSubmissionDataFilePaths(userId, submissionID);
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

  private static List<String> getSubmissionMetadataFilePaths(String userId, String submissionId)
      throws IOException, JAXBException, DatatypeConfigurationException {
    List<String> submissionMetadataFilePaths = new ArrayList<>();

    Map<String, FileUploadStatus> submissionUploadStatus = SubmissionUploadManager.getInstance()
        .getSubmissionsUploadStatus(userId, submissionId).getFilesUploadStatus();

    for (Map.Entry<String, FileUploadStatus> entry : submissionUploadStatus.entrySet()) {
      FileUploadStatus fileUploadStatus = entry.getValue();
      if (fileUploadStatus.isMetadataFile()) {
        submissionMetadataFilePaths.add(fileUploadStatus.getFileLocalPath());
      }
    }
    return submissionMetadataFilePaths;
  }

  private static List<String> getSubmissionDataFilePaths(String userId, String submissionId)
      throws IOException, JAXBException, DatatypeConfigurationException {
    List<String> submissionDataFilePaths = new ArrayList<>();

    Map<String, FileUploadStatus> submissionUploadStatus = SubmissionUploadManager.getInstance()
        .getSubmissionsUploadStatus(userId, submissionId).getFilesUploadStatus();

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
  @Operation(summary = "List the caller's ImmPort workspaces",
      description = "Ask ImmPort which workspaces the configured account can submit to, and return "
          + "them. A submission names one of these.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The available ImmPort workspaces"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "ImmPort could not be reached, or no ImmPort token is configured")
  })
  public Response immPortWorkspaces() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    Optional<String> immPortBearerToken = immPortUtil.getImmPortBearerToken();
    if (immPortBearerToken.isEmpty()) {
      logger.warn("Could not get an ImmPort token");
      return CedarResponse.status(CedarResponseStatus.INTERNAL_SERVER_ERROR).build();  // TODO CEDAR error response
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

      if (response.getCode() == 200) {
        HttpEntity entity = response.getEntity();
        return Response.ok(immPortWorkspacesResponseBody2CEDARWorkspaceResponse(entity)).build();
      } else {
        logger.warn("Unexpected status code calling " + workspaceUrl + "; status=" + response
            .getCode());
        return CedarResponse.status(CedarResponseStatus.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
      }
    } catch (IOException | ParseException e) {
      logger.warn("IO exception connecting to host " + workspaceUrl + ": " + e.getMessage());
      return CedarResponse.status(CedarResponseStatus.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
    } finally {
      Closer.closeQuietly(response);
      Closer.closeQuietly(client);
    }
  }

  @POST
  @Timed
  @Path("/immport-submit")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(summary = "Upload a submission package and send it to ImmPort",
      description = "Receive the files of an ImmPort submission, assemble them in CEDAR, and once "
          + "they are all present submit them to the named workspace. The package arrives a chunk at a time, so a caller sends this repeatedly; the submission starts by itself once the last chunk lands. A 200 therefore means the chunk was stored and any submission it completed was started, not that the repository has accepted anything. While the package is "
          + "still arriving the response is an empty object rather than a submission result.")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "The chunk was stored; ImmPort's result if this completed the submission, an empty object if not"),
      @ApiResponse(responseCode = "400", description = "Not a multipart upload, or no workspace was named"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "ImmPort could not be reached, or no ImmPort token is configured")
  })
  public Response submitImmPort() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    Optional<String> immPortBearerToken = immPortUtil.getImmPortBearerToken();
    if (immPortBearerToken.isEmpty()) {
      logger.warn("No ImmPort token found");
      return CedarResponse.status(CedarResponseStatus.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
    }

    CloseableHttpResponse response = null;
    CloseableHttpClient client = null;

    try {
      if (JakartaServletFileUpload.isMultipartContent(request)) {
        String userId = FlowUploadUtil.getLastFragmentOfUrl(c.getCedarUser().getId());
        FlowData data = FlowUploadUtil.getFlowData(request);

        String workspaceID = null;
        if (data.getAdditionalParameters().containsKey("workspaceId")) {
          workspaceID = data.getAdditionalParameters().get("workspaceId");
        } else {
          logger.warn("No workspaceId parameter specified");
          return CedarResponse.status(CedarResponseStatus.BAD_REQUEST).build();  // TODO CEDAR error response
        }
        String submissionLocalFolderPath = FlowUploadUtil
            .getSubmissionLocalFolderPath(ImmPortConstants.IMMPORT_LOCAL_FOLDER_NAME, userId, data.getSubmissionId());
        String filePath = FlowUploadUtil.saveToLocalFile(data, userId, request.getContentLength(),
            submissionLocalFolderPath);
        logger.info("File created. Path: " + filePath);
        SubmissionUploadManager.getInstance().updateStatus(data, userId, submissionLocalFolderPath);

        if (SubmissionUploadManager.getInstance().isSubmissionUploadComplete(userId, data.getSubmissionId())) {
          HttpEntity multiPartEntity = getMultipartContentFromSubmission(userId, data.submissionId, workspaceID);
          HttpPost post = new HttpPost(immPortSubmissionUrl);
          post.setHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_BEARER_PREFIX + immPortBearerToken.get());
          post.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
          post.setEntity(multiPartEntity);
          client = HttpClientBuilder.create().build();
          response = client.execute(post);
          int statusCode = response.getCode();

          if (statusCode == CedarResponseStatus.OK.getStatusCode()) {
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
                .getCode());
            return CedarResponse.status(CedarResponseStatus.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
          }
        } else {
          return Response.ok(new HashMap()).build(); // We are still building the request
        }
      } else {
        logger.warn("No form data supplied");
        return CedarResponse.status(CedarResponseStatus.BAD_REQUEST).build(); // TODO CEDAR error response
      }
    } catch (IOException | ParseException | SubmissionInstanceNotFoundException | IllegalAccessException |
             JAXBException |
             DatatypeConfigurationException e) {
      logger.warn("Exception submitting to ImmPort: " + e.getMessage());
      return CedarResponse.status(CedarResponseStatus.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
    } finally {
      Closer.closeQuietly(response);
      Closer.closeQuietly(client);
    }
  }

  // Original implementation with single multipart form upload. Keep for the moment for command line testing.
  @POST
  @Timed
  @Path("/immport-submit-old")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(summary = "Send a submission to ImmPort in one request",
      description = "The earlier ImmPort path, taking the whole submission as a single multipart "
          + "request rather than in chunks. Kept for testing from the command line; the chunked "
          + "route is what the workbench uses.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "ImmPort's submission result"),
      @ApiResponse(responseCode = "400", description = "Not a multipart upload, or no workspaceId parameter"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "500", description = "ImmPort could not be reached, or no ImmPort token is configured")
  })
  public Response submitImmPortOld() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    String workspaceID = request.getParameter("workspaceId"); // TODO CEDAR constant for parameter
    if (workspaceID == null || workspaceID.isEmpty()) {
      logger.warn("No workspaceId parameter specified");
      return CedarResponse.status(CedarResponseStatus.BAD_REQUEST).build();  // TODO CEDAR error response
    }

    Optional<String> immPortBearerToken = immPortUtil.getImmPortBearerToken();
    if (immPortBearerToken.isEmpty()) {
      logger.warn("No ImmPort token found");
      return CedarResponse.status(CedarResponseStatus.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
    }

    CloseableHttpResponse response = null;
    CloseableHttpClient client = null;

    try {
      if (JakartaServletFileUpload.isMultipartContent(request)) {

        HttpEntity multiPartEntity = getMultipartContentFromRequest(workspaceID);

        HttpPost post = new HttpPost(immPortSubmissionUrl);
        post.setHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_BEARER_PREFIX + immPortBearerToken.get());
        post.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
        post.setEntity(multiPartEntity);
        client = HttpClientBuilder.create().build();
        response = client.execute(post);

        int statusCode = response.getCode();

        if (statusCode == CedarResponseStatus.OK.getStatusCode()) {
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
              .getCode());
          return CedarResponse.status(CedarResponseStatus.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
        }
      } else {
        logger.warn("No form data supplied");
        return CedarResponse.status(CedarResponseStatus.BAD_REQUEST).build(); // TODO CEDAR error response
      }
    } catch (IOException | ParseException e) {
      logger.warn("Exception submitting to ImmmPort " + immPortSubmissionUrl + ": " + e.getMessage());
      return CedarResponse.status(CedarResponseStatus.INTERNAL_SERVER_ERROR).build(); // TODO CEDAR error response
    } finally {
      Closer.closeQuietly(response);
      Closer.closeQuietly(client);
    }
  }

  private HttpEntity getMultipartContentFromRequest(String workspaceID) throws FileUploadException, IOException {
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();

    builder.addTextBody(ImmPortConstants.IMMPORT_WORKSPACE_ID_FIELD, workspaceID);
    builder.addTextBody(ImmPortConstants.IMMPORT_USERNAME_FIELD, immPortUserName);

    File tempDir = Files.createTempDir();
    List<DiskFileItem> fileItems = new JakartaServletFileUpload<>(
        DiskFileItemFactory.builder().setThreshold(1024 * 1024).setPath(tempDir.toPath()).get())
        .parseRequest(request);

    for (DiskFileItem fileItem : fileItems) {
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
      throws IOException, ParseException {
    if (responseEntity != null) {
      String responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
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
      throws IOException, ParseException {
    if (responseEntity != null) {
      String responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
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
