package org.metadatacenter.submission.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.MultipartInput;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.submission.CEDARValidationResponse;
import org.metadatacenter.submission.SubmissionServerApplication;
import org.metadatacenter.submission.SubmissionServerConfiguration;
import org.metadatacenter.submission.immport.ImmPortUtil;
import org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrTemplateInstance2XMLConverter;
import org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrValidator;
import org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericTemplateInstance2XMLConverter;
import org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericValidator;
import org.metadatacenter.submission.ncbi.validation.BioSampleValidator;
import org.metadatacenter.util.http.HttpTimeouts;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.TestAuthUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every refusal the submission routes build names its problem in the shared error body.
 *
 * <p>These refusals were built from a status alone, so each reached the client as the shared error body
 * with no message. The routes run authenticated in the real application, and stand-ins replace only the
 * services this server calls. ImmPort is a loopback server, and the BioSample validator, which sends XML to
 * NCBI, is a counter. The NCBI routes also take an instance validator that accepts every instance
 * and a converter that fails on every one, so a request reaches the conversion without a complete
 * submission.
 */
class SubmissionRefusalTest {

  private static final String BOUNDARY = "refusal";

  /** The converter fails with this text, which belongs in the log and never in the answer. */
  private static final String CONVERSION_DETAIL = "Unparseable date: \"next spring\"";

  static HttpServer immPort;
  static final AtomicInteger immPortAnswer = new AtomicInteger(422);
  static final AtomicInteger immPortCalls = new AtomicInteger();
  static final AtomicInteger bioSampleCalls = new AtomicInteger();
  static volatile boolean tokenAvailable = true;
  static DropwizardTestSupport<SubmissionServerConfiguration> server;
  static String auth;
  static final HttpClient CLIENT = HttpClient.newHttpClient();

  public static class RefusalApplication extends SubmissionServerApplication {
    @Override
    public void initializeApp() {
    }

    @Override
    public void runApp(SubmissionServerConfiguration configuration, Environment environment) {
      String base = "http://127.0.0.1:" + immPort.getAddress().getPort();
      ImmPortUtil immPortUtil = new ImmPortUtil(cedarConfig) {
        @Override
        public Optional<String> getImmPortBearerToken() {
          return tokenAvailable ? Optional.of("stub-token") : Optional.empty();
        }

        @Override
        public String getSubmissionUrl() {
          return base + "/data/upload";
        }
      };
      environment.jersey().register(new ImmPortSubmissionServerResource(cedarConfig, immPortUtil,
          HttpTimeouts.EXTERNAL));
      environment.jersey().register(new NcbiGenericSubmissionServerResource(cedarConfig,
          new NcbiGenericValidator() {
            @Override
            public CEDARValidationResponse validateInstance(JsonNode instance) {
              return valid();
            }
          },
          new NcbiGenericTemplateInstance2XMLConverter() {
            @Override
            public String convertTemplateInstanceToXML(JsonNode instance) throws ParseException {
              throw new ParseException(CONVERSION_DETAIL, 0);
            }
          },
          countingBioSampleValidator()));
      environment.jersey().register(new NcbiCairrSubmissionServerResource(cedarConfig,
          new NcbiCairrValidator() {
            @Override
            public CEDARValidationResponse validateInstance(JsonNode instance) {
              return valid();
            }
          },
          new NcbiCairrTemplateInstance2XMLConverter() {
            @Override
            public String convertTemplateInstanceToXML(JsonNode instance) throws ParseException {
              throw new ParseException(CONVERSION_DETAIL, 0);
            }
          },
          countingBioSampleValidator()));
      environment.jersey().register(new AMIA2016DemoBioSampleServerResource(cedarConfig,
          countingBioSampleValidator()));
    }
  }

  @BeforeAll
  static void start() throws Exception {
    immPort = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    immPort.createContext("/", exchange -> {
      immPortCalls.incrementAndGet();
      exchange.getRequestBody().readAllBytes();
      int status = immPortAnswer.get();
      if (status == -1) { // ImmPort drops the connection without answering
        exchange.close();
        return;
      }
      byte[] body = "{\"error\":\"refused by the stand-in\"}".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    immPort.start();
    Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
    environment.put("CEDAR_SUBMISSION_HTTP_PORT", "0");
    environment.put("CEDAR_SUBMISSION_ADMIN_PORT", "0");
    environment.put("CEDAR_SUBMISSION_STOP_PORT", "0");
    CedarEnvironmentSource.setOverride(environment);
    server = new DropwizardTestSupport<>(RefusalApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));
    server.before();
    CedarConfig config =
        CedarConfig.getInstance(CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_SUBMISSION));
    TestAuthUtil.installInMemoryUserService(config);
    auth = TestAuthUtil.getTestUser1AuthHeader(config);
  }

  @AfterEach
  void reset() {
    tokenAvailable = true;
    immPortAnswer.set(422);
    immPortCalls.set(0);
    bioSampleCalls.set(0);
  }

  @AfterAll
  static void stop() {
    if (server != null) server.after();
    if (immPort != null) immPort.stop(0);
  }

  // The NCBI and AIRR validation routes

  @ParameterizedTest
  @ValueSource(strings = {"/command/validate-ncbi", "/command/validate-cairr"})
  void aValidationBodyOfTheWrongShapeIsRefusedWithItsShape(String route) throws Exception {
    assertRefused(postJson(route, "{\"instance\":{},\"comment\":\"extra\"}"), 400,
        "The body must be an object holding instance and, optionally, userFileNames, and no other field");
  }

  /** File names that are not strings are the caller's mistake. They were answered as a 500. */
  @ParameterizedTest
  @ValueSource(strings = {"/command/validate-ncbi", "/command/validate-cairr"})
  void fileNamesThatAreNotStringsAreRefusedWithTheirField(String route) throws Exception {
    assertRefused(postJson(route, "{\"instance\":{},\"userFileNames\":[{\"name\":\"a.fastq\"}]}"),
        400, "userFileNames could not be read as a list of file names");
  }

  /**
   * A conversion failure is answered as a 500. The route built that answer and discarded it, so the request
   * went on with no XML and failed later, in the BioSample validator.
   */
  @ParameterizedTest
  @ValueSource(strings = {"/command/validate-ncbi", "/command/validate-cairr"})
  void aConversionFailureIsAnsweredWithoutValidatingAnything(String route) throws Exception {
    HttpResponse<String> response = postJson(route, "{\"instance\":{}}");
    JsonNode error = assertRefused(response, 500, "The instance could not be converted to the NCBI submission XML");
    assertTrue(error.hasNonNull("errorId"), error.toString());
    assertFalse(response.body().contains("next spring"), response.body());
    assertEquals(0, bioSampleCalls.get(), "a conversion failure must not reach the BioSample validator");
  }

  // The NCBI and AIRR uploads

  @ParameterizedTest
  @ValueSource(strings = {"/command/upload-ncbi-to-cedar", "/command/upload-cairr-to-cedar"})
  void aPackageWithoutExactlyOneMetadataFileIsRefusedWithItsCount(String route) throws Exception {
    assertRefused(postMultipart(route, chunk("a.json, b.json", Map.of())), 400,
        "metadataFiles must name exactly one metadata file, but names 2");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/command/upload-ncbi-to-cedar", "/command/upload-cairr-to-cedar",
      "/command/immport-submit", "/command/immport-submit-old?workspaceId=ws"})
  void aRequestThatIsNotMultipartIsRefusedAsSuch(String route) throws Exception {
    assertRefused(postWithoutBodyType(route), 400, "The request is not a multipart/form-data upload");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/command/upload-ncbi-to-cedar", "/command/upload-cairr-to-cedar"})
  void anUnreadableUploadIsAnsweredWithAMessageAndAnErrorId(String route) throws Exception {
    JsonNode error = assertRefused(postMultipart(route, "--" + BOUNDARY + "\r\nnot a part"), 500,
        "The chunk could not be stored, or the submission could not be prepared");
    assertTrue(error.hasNonNull("errorId"), error.toString());
  }

  // The chunked ImmPort upload

  @Test
  void anImmPortUploadWithoutCredentialsSaysSo() throws Exception {
    tokenAvailable = false;
    assertRefused(postMultipart("/command/immport-submit", chunk("instance.json", Map.of("workspaceId", "ws"))),
        500, "ImmPort authentication is unavailable");
    assertEquals(0, immPortCalls.get());
  }

  @Test
  void anImmPortUploadThatNamesNoWorkspaceIsRefusedWithTheField() throws Exception {
    assertRefused(postMultipart("/command/immport-submit", chunk("instance.json", Map.of())), 400,
        "Missing field: workspaceId");
    assertEquals(0, immPortCalls.get());
  }

  @Test
  void anImmPortRefusalOfTheUploadIsReportedWithImmPortsStatus() throws Exception {
    HttpResponse<String> response =
        postMultipart("/command/immport-submit", chunk("instance.json", Map.of("workspaceId", "ws")));
    assertRefused(response, 500, "ImmPort answered the submission with status 422");
    assertEquals(1, immPortCalls.get(), "the completed upload must reach ImmPort");
    assertFalse(response.body().contains("127.0.0.1"), response.body());
  }

  @Test
  void anImmPortUploadThatCannotBeSentIsAnsweredWithAMessageAndAnErrorId() throws Exception {
    immPortAnswer.set(-1);
    JsonNode error = assertRefused(
        postMultipart("/command/immport-submit", chunk("instance.json", Map.of("workspaceId", "ws"))), 500,
        "The chunk could not be stored, or the submission could not be prepared or sent to ImmPort");
    assertTrue(error.hasNonNull("errorId"), error.toString());
  }

  // The single-request ImmPort submission

  @Test
  void anImmPortSubmissionThatNamesNoWorkspaceIsRefusedWithTheParameter() throws Exception {
    assertRefused(postMultipart("/command/immport-submit-old", instancePart()), 400,
        "Missing parameter: workspaceId");
  }

  @Test
  void anImmPortSubmissionWithoutCredentialsSaysSo() throws Exception {
    tokenAvailable = false;
    assertRefused(postMultipart("/command/immport-submit-old?workspaceId=ws", instancePart()), 500,
        "ImmPort authentication is unavailable");
    assertEquals(0, immPortCalls.get());
  }

  @Test
  void anImmPortRefusalOfTheSubmissionIsReportedWithImmPortsStatus() throws Exception {
    HttpResponse<String> response = postMultipart("/command/immport-submit-old?workspaceId=ws", instancePart());
    assertRefused(response, 500, "ImmPort answered the submission with status 422");
    assertEquals(1, immPortCalls.get());
    assertFalse(response.body().contains("127.0.0.1"), response.body());
  }

  /**
   * A body the multipart parser cannot read is the caller's mistake. It was answered as a 500, as a failure
   * to reach ImmPort still is.
   */
  @Test
  void aMalformedImmPortSubmissionIsRefusedAsTheCallersMistake() throws Exception {
    assertRefused(postMultipart("/command/immport-submit-old?workspaceId=ws", "--" + BOUNDARY + "\r\nnot a part"),
        400, "The request could not be read, or the submission could not be sent to ImmPort");
    assertEquals(0, immPortCalls.get());
  }

  /**
   * The parser reports a failure to read the request, or to write a part to disk, as a plain
   * FileUploadException caused by that IOException. Only that failure stays a 500.
   */
  @Test
  void onlyAnInputFailureOfTheMultipartParserIsTheServersFault() {
    assertFalse(ImmPortSubmissionServerResource.isMalformedBody(
        new FileUploadException("Request 'multipart/form-data' failed: No space left on device",
            new IOException("No space left on device"))));
    assertTrue(ImmPortSubmissionServerResource.isMalformedBody(
        new FileUploadException("the request was rejected because no multipart boundary was found")));
    assertTrue(ImmPortSubmissionServerResource.isMalformedBody(
        new MultipartInput.MalformedStreamException("Stream ended unexpectedly", new IOException("No more data"))));
  }

  @Test
  void anImmPortSubmissionThatCannotBeSentIsAnsweredWithAMessageAndAnErrorId() throws Exception {
    immPortAnswer.set(-1);
    JsonNode error = assertRefused(postMultipart("/command/immport-submit-old?workspaceId=ws", instancePart()),
        500, "The request could not be read, or the submission could not be sent to ImmPort");
    assertTrue(error.hasNonNull("errorId"), error.toString());
  }

  // The AMIA 2016 demo validation

  /** A body that does not fit the template is the caller's mistake. It was answered as a 500. */
  @ParameterizedTest
  @ValueSource(strings = {"[]", "{\"sampleName\":[]}", "{\"comment\":\"extra\"}"})
  void anAmiaBodyThatDoesNotFitTheTemplateIsRefusedWithoutValidatingAnything(String body) throws Exception {
    assertRefused(postJson("/command/validate-biosample", body), 400,
        "The instance could not be converted to BioSample XML");
    assertEquals(0, bioSampleCalls.get());
  }

  // Helpers

  private static CEDARValidationResponse valid() {
    CEDARValidationResponse response = new CEDARValidationResponse();
    response.setIsValid(true);
    response.setMessages(new ArrayList<>());
    return response;
  }

  private static BioSampleValidator countingBioSampleValidator() {
    return new BioSampleValidator() {
      @Override
      public CEDARValidationResponse validateBioSampleSubmission(String bioSampleSubmissionXML) {
        bioSampleCalls.incrementAndGet();
        return valid();
      }
    };
  }

  private static JsonNode assertRefused(HttpResponse<String> response, int status, String message)
      throws IOException {
    assertEquals(status, response.statusCode(), response.body());
    assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"),
        response.headers().toString());
    JsonNode error = JsonMapper.STRICT_MAPPER.readTree(response.body());
    assertEquals(status, error.path("statusCode").asInt(), response.body());
    assertEquals(message, error.path("message").asText(), response.body());
    return error;
  }

  /** Builds one complete single-chunk upload of a JSON file under a fresh upload identifier. */
  private static String chunk(String metadataFiles, Map<String, String> extraFields) {
    String content = "{}";
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("submissionId", UUID.randomUUID().toString());
    fields.put("numberOfFiles", "1");
    fields.put("metadataFiles", metadataFiles);
    fields.put("flowChunkNumber", "1");
    fields.put("flowChunkSize", "1024");
    fields.put("flowCurrentChunkSize", String.valueOf(content.length()));
    fields.put("flowTotalSize", String.valueOf(content.length()));
    fields.put("flowIdentifier", "instance");
    fields.put("flowFilename", "instance.json");
    fields.put("flowRelativePath", "instance.json");
    fields.put("flowTotalChunks", "1");
    fields.putAll(extraFields);
    StringBuilder body = new StringBuilder();
    fields.forEach((name, value) -> body.append("--").append(BOUNDARY).append("\r\n")
        .append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
        .append(value).append("\r\n"));
    return body.append(filePart("file", content)).append("--").append(BOUNDARY).append("--\r\n").toString();
  }

  private static String instancePart() {
    return filePart("instance", "{}") + "--" + BOUNDARY + "--\r\n";
  }

  private static String filePart(String name, String content) {
    return "--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"instance.json\"\r\n"
        + "Content-Type: application/json\r\n\r\n" + content + "\r\n";
  }

  private static HttpResponse<String> postJson(String route, String body) throws Exception {
    return send(request(route).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body)));
  }

  private static HttpResponse<String> postMultipart(String route, String body) throws Exception {
    return send(request(route).header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
        .POST(HttpRequest.BodyPublishers.ofString(body)));
  }

  /** Sends a request that declares no body type. Jersey routes it to the multipart route, which refuses it. */
  private static HttpResponse<String> postWithoutBodyType(String route) throws Exception {
    return send(request(route).POST(HttpRequest.BodyPublishers.noBody()));
  }

  private static HttpRequest.Builder request(String route) {
    return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getLocalPort() + route))
        .timeout(Duration.ofSeconds(10)).header("Authorization", auth);
  }

  private static HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
    return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}
