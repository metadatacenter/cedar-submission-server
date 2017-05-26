package org.metadatacenter.cedar.submission.resources;

import com.codahale.metrics.annotation.Timed;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.HttpConnectionConstants;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.CedarProcessingException;
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
import java.io.IOException;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
public class LincsSubmissionServerResource extends CedarMicroserviceResource {

  private static final Logger logger = LoggerFactory.getLogger(LincsSubmissionServerResource.class);

  private static final String LINCS_VALIDATION_ENDPOINT = "http://dev3.ccs.miami.edu:8080/dcic/api/dataset-validation";

  public LincsSubmissionServerResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  @POST
  @Timed
  @Path("/validate-lincs")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response validateInstance() throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);

    String payload = c.request().getRequestBody().asJsonString();

    HttpResponse lincsResponse = sendPostRequest(LINCS_VALIDATION_ENDPOINT, payload);
    Response response = createServiceResponse(lincsResponse);
    return response;
  }

  private HttpResponse sendPostRequest(String url, String content) throws
      CedarProcessingException {
    Request proxyRequest = Request.Post(url)
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

  private Response createServiceResponse(HttpResponse httpResponse) throws CedarProcessingException {
    try {
      HttpEntity entity = httpResponse.getEntity();
      int statusCode = httpResponse.getStatusLine().getStatusCode();
      String mediaType = entity.getContentType().getValue();
      return Response.status(statusCode).type(mediaType).entity(entity.getContent()).build();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new CedarProcessingException(e);
    }
  }
}
