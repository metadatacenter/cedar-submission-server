package org.metadatacenter.submission;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.submission.resources.AMIA2016DemoBioSampleServerResource;
import org.metadatacenter.submission.resources.ImmPortSubmissionServerResource;
import org.metadatacenter.submission.resources.LincsSubmissionServerResource;
import org.metadatacenter.submission.resources.NcbiCairrSubmissionServerResource;
import org.metadatacenter.submission.resources.NcbiGenericSubmissionServerResource;
import org.metadatacenter.util.test.RouteSurface;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Route safety net: probes every endpoint the submission resources declare, unauthenticated, and
 * requires each to answer 401. Every submission endpoint asserts {@code LoggedIn} before doing
 * anything, so a 404/405 means the route vanished or changed verb, and any other status means an
 * endpoint lost its authentication assertion. No fixtures and no backend are involved.
 */
public class SubmissionRoutesRespondTest {

  static {
    // Must run before the test support boots the server, which reads the port env vars. Ports are
    // distinct from the dev server and from every other booting test class.
    Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
    environment.put("CEDAR_SUBMISSION_HTTP_PORT", "19021");
    environment.put("CEDAR_SUBMISSION_ADMIN_PORT", "19121");
    environment.put("CEDAR_SUBMISSION_STOP_PORT", "19221");
    CedarEnvironmentSource.setOverride(environment);
  }

  private static final DropwizardTestSupport<SubmissionServerConfiguration> SERVER =
      new DropwizardTestSupport<>(SubmissionServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  @BeforeAll
  public static void startServer() throws Exception {
    SERVER.before();
  }

  @AfterAll
  public static void stopServer() {
    SERVER.after();
  }

  private static final List<Class<?>> RESOURCE_CLASSES = List.of(
      ImmPortSubmissionServerResource.class,
      LincsSubmissionServerResource.class,
      NcbiGenericSubmissionServerResource.class,
      NcbiCairrSubmissionServerResource.class,
      AMIA2016DemoBioSampleServerResource.class);

  @Test
  public void everyRouteRejectsAnUnauthenticatedRequest() {
    RouteSurface.assertEveryRouteAnswers(
        "http://localhost:" + SERVER.getLocalPort(),
        RouteSurface.endpoints(RESOURCE_CLASSES),
        401);
  }

  /**
   * A body whose Content-Type does not match the endpoint's {@code @Consumes} must be rejected as
   * 415 Unsupported Media Type. Media-type matching happens during dispatch, before the endpoint's
   * authentication assertion, so this is observable without credentials.
   *
   * <p>This pins a corrected mapping: {@code CedarExceptionMapper} used to translate JAX-RS's
   * NotSupportedException (which means "unsupported media type") into 505 HTTP Version Not
   * Supported, reporting a client mistake as a retryable server fault. The submission endpoints all
   * consume multipart/form-data, so sending JSON exercises exactly that path.
   */
  @Test
  public void aMismatchedContentTypeIsRejectedAsUnsupportedMediaType() throws Exception {
    List<RouteSurface.Endpoint> endpoints = RouteSurface.endpoints(RESOURCE_CLASSES);
    Assertions.assertFalse(endpoints.isEmpty(), "No submission endpoints found by reflection");
    HttpClient client = HttpClient.newHttpClient();
    StringBuilder failures = new StringBuilder();
    for (RouteSurface.Endpoint endpoint : endpoints) {
      if (endpoint.consumes == null || endpoint.consumes.startsWith("application/json")) {
        continue; // JSON is what we send, so there would be no mismatch to observe
      }
      HttpResponse<String> response = client.send(
          HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + RouteSurface.resolvedPath(endpoint)))
              .header("Content-Type", "application/json")
              .method(endpoint.verb, HttpRequest.BodyPublishers.ofString("{}"))
              .build(),
          HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 415) {
        failures.append(endpoint.key()).append(" (consumes ").append(endpoint.consumes)
            .append("): expected 415 for a JSON body but got ").append(response.statusCode()).append('\n');
      }
    }
    Assertions.assertEquals(0, failures.length(),
        "A mismatched Content-Type was not reported as 415 Unsupported Media Type:\n" + failures);
  }

}
