package org.metadatacenter.submission.resources;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.HttpConnectionConstants;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.util.http.CedarResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;

import static javax.ws.rs.core.Response.Status.*;
import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
public class LincsSubmissionServerResource extends CedarMicroserviceResource {

  private static final Logger logger = LoggerFactory.getLogger(LincsSubmissionServerResource.class);

  private static final String LINCS_VALIDATION_ENDPOINT = "http://dev3.ccs.miami.edu:8080/dcic/api/dataset-validation";

  public LincsSubmissionServerResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  private static void loggingError(String errorMessage, String detailedMessage) {
    logger.error(errorMessage);
    logger.error("Message from the upstream server: " + detailedMessage);
  }

  @POST
  @Timed
  @Path("/validate-lincs")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response validateInstance() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    String payload = c.request().getRequestBody().asJsonString();

    HttpResponse lincsResponse = sendPostRequestToLincsServer(payload);
    return unpackLincsResponseAndForwardIt(lincsResponse);
  }

  private HttpResponse sendPostRequestToLincsServer(String content) throws CedarProcessingException {
    Request proxyRequest = Request.Post(LINCS_VALIDATION_ENDPOINT)
        .connectTimeout(HttpConnectionConstants.CONNECTION_TIMEOUT)
        .socketTimeout(HttpConnectionConstants.SOCKET_TIMEOUT)
        .bodyString(content, ContentType.APPLICATION_JSON);
    try {
      return proxyRequest.execute().returnResponse();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new CedarProcessingException(e);
    }
  }

  private Response unpackLincsResponseAndForwardIt(HttpResponse httpResponse) throws CedarProcessingException {
    int statusCode = httpResponse.getStatusLine().getStatusCode();
    HttpEntity responseEntity = httpResponse.getEntity();
    try {
      Response response = null;
      if (statusCode == OK.getStatusCode()) {
        response = handleSuccessResponse(responseEntity);
      } else if (statusCode == BAD_REQUEST.getStatusCode()) {
        response = handleClientErrorResponse(responseEntity);
      } else if (statusCode == UNAUTHORIZED.getStatusCode()) {
        response = handleUnauthorizedResponse(responseEntity);
      } else if (statusCode == INTERNAL_SERVER_ERROR.getStatusCode()) {
        response = handleServerErrorResponse(responseEntity);
      } else {
        response = handleOtherErrorResponse(statusCode, responseEntity);
      }
      return response;
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new CedarProcessingException(e);
    }
  }

  private Response handleSuccessResponse(final HttpEntity responseEntity) throws IOException {
    return CedarResponse.ok()
        .entity(responseEntity.getContent())
        .build();
  }

  private Response handleClientErrorResponse(final HttpEntity responseEntity) throws IOException {
    String errorMessage = "The validation service from 'dev3.ccs.miami.edu' returns a Bad Request (400) status";
    String detailedMessage = IOUtils.toString(responseEntity.getContent());
    loggingError(errorMessage, detailedMessage);
    return CedarResponse.badGateway()
        .errorKey(CedarErrorKey.UPSTREAM_SERVER_ERROR)
        .errorMessage(errorMessage)
        .parameter("upstreamErrorMessage", detailedMessage)
        .build();
  }

  private Response handleUnauthorizedResponse(HttpEntity responseEntity) throws IOException {
    String errorMessage = "The validation service from 'dev3.ccs.miami.edu' returns an Unauthorized (401) status";
    String detailedMessage = IOUtils.toString(responseEntity.getContent());
    loggingError(errorMessage, detailedMessage);
    return CedarResponse.badGateway()
        .errorKey(CedarErrorKey.UPSTREAM_SERVER_ERROR)
        .errorMessage(errorMessage)
        .parameter("upstreamErrorMessage", detailedMessage)
        .build();
  }

  private Response handleServerErrorResponse(HttpEntity responseEntity) throws IOException {
    String errorMessage = "The validation service from 'dev3.ccs.miami.edu' returns an Internal Server Error (500) " +
        "status";
    String detailedMessage = IOUtils.toString(responseEntity.getContent());
    loggingError(errorMessage, detailedMessage);
    return CedarResponse.badGateway()
        .errorKey(CedarErrorKey.UPSTREAM_SERVER_ERROR)
        .errorMessage(errorMessage)
        .parameter("upstreamErrorMessage", detailedMessage)
        .build();
  }

  private Response handleOtherErrorResponse(int statusCode, HttpEntity responseEntity) throws IOException {
    String errorMessage = String.format("The validation service from 'dev3.ccs.miami.edu' returns (%s) status",
        statusCode);
    String detailedMessage = IOUtils.toString(responseEntity.getContent());
    loggingError(errorMessage, detailedMessage);
    return CedarResponse.status(Status.fromStatusCode(statusCode))
        .errorKey(CedarErrorKey.UPSTREAM_SERVER_ERROR)
        .errorMessage(errorMessage)
        .parameter("upstreamErrorMessage", detailedMessage)
        .build();
  }
}
