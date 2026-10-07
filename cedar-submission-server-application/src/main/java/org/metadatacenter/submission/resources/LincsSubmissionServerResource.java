package org.metadatacenter.submission.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.exception.CedarDependencyUnavailableException;
import org.metadatacenter.util.json.JsonMapper;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import java.nio.charset.StandardCharsets;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.HttpTimeouts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "LINCS")
@SecurityRequirement(name = "api_key")
public class LincsSubmissionServerResource extends CedarMicroserviceResource {

  private static final Logger logger = LoggerFactory.getLogger(LincsSubmissionServerResource.class);

  private static final String LINCS_VALIDATION_ENDPOINT = "http://dev3.ccs.miami.edu:8080/dcic/api/dataset-validation";

  private final String validationEndpoint;
  private final HttpTimeouts timeouts;

  public LincsSubmissionServerResource(CedarConfig cedarConfig) {
    this(cedarConfig, LINCS_VALIDATION_ENDPOINT, HttpTimeouts.EXTERNAL);
  }

  LincsSubmissionServerResource(CedarConfig config, String validationEndpoint, HttpTimeouts timeouts) {
    super(config);
    this.validationEndpoint = validationEndpoint;
    this.timeouts = timeouts;
  }

  @POST
  @Timed
  @Path("/validate-lincs")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Validate an instance against the LINCS validator",
      description = "Forward a CEDAR instance to the LINCS dataset validator and return what it "
          + "says. Validator 400, 401, 403 and 500 responses are reported as gateway failures; other "
          + "refusals retain their status. Transport failures return 503.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The LINCS validation report"),
      @ApiResponse(responseCode = "400", description = "Invalid CEDAR request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "502", description = "The LINCS validator refused the request or returned an unreadable answer"),
      @ApiResponse(responseCode = "503", description = "The LINCS validator could not be reached")
  })
  public Response validateInstance() throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    String payload = c.request().getRequestBody().asJsonString();

    try (ClassicHttpResponse lincsResponse = sendPostRequestToLincsServer(payload)) {
      return unpackLincsResponseAndForwardIt(lincsResponse);
    } catch (IOException e) {
      throw new CedarDependencyUnavailableException("LINCS validator is unavailable", e);
    }
  }

  /**
   * Sends one validation request to the LINCS validator.
   *
   * <p>The validator is a service CEDAR does not operate, so the call takes the external class of
   * outbound call: a connect timeout that allows for a handshake across the internet, and that
   * class's own pool. It previously set two timeouts by hand and then executed on the fluent API's
   * process-wide default executor, which pools nothing and reads no configuration.
   */
  private ClassicHttpResponse sendPostRequestToLincsServer(String content) throws CedarProcessingException {
    Request proxyRequest = Request.post(validationEndpoint)
        .bodyString(content, ContentType.APPLICATION_JSON);
    try {
      return timeouts.execute(proxyRequest);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new CedarDependencyUnavailableException("LINCS validator is unavailable", e);
    }
  }

  private Response unpackLincsResponseAndForwardIt(ClassicHttpResponse response) {
    int status = response.getCode();
    try {
      String body = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      if (status == 200) {
        var report = JsonMapper.STRICT_MAPPER.readTree(body);
        if (report == null) throw new IOException("Empty validation report");
        return CedarResponse.ok().entity(report).build();
      }
      // Retain the established validator refusal policy while preserving unfamiliar statuses.
      int outward = status == 400 || status == 401 || status == 403 || status == 500 ? 502 : status;
      var result = CedarResponse.status(outward)
          .errorKey(CedarErrorKey.UPSTREAM_SERVER_ERROR)
          .message("The LINCS validator answered " + status)
          .parameter("upstreamStatusCode", status)
          .parameter("upstreamService", "LINCS");
      var retry = response.getFirstHeader("Retry-After");
      if (retry != null) result.header("Retry-After", retry.getValue());
      return result.build();
    } catch (IOException | org.apache.hc.core5.http.ParseException e) {
      return CedarResponse.badGateway().errorKey(CedarErrorKey.UPSTREAM_SERVER_ERROR)
          .message("The LINCS validator returned an unreadable response").exception(e).build();
    }
  }
}
