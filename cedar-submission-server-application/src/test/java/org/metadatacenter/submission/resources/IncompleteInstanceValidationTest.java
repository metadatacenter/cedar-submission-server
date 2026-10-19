package org.metadatacenter.submission.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.submission.CEDARValidationResponse;
import org.metadatacenter.submission.SubmissionServerApplication;
import org.metadatacenter.submission.SubmissionServerConfiguration;
import org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrTemplateInstance2XMLConverter;
import org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrValidator;
import org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericTemplateInstance2XMLConverter;
import org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericValidator;
import org.metadatacenter.submission.ncbi.validation.BioSampleValidator;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * validate-ncbi and validate-cairr answer an incomplete instance with its validation report.
 *
 * <p>A missing BioSample or SRA element, an SRA entry without its list of file-name fields, and a file-name
 * field without a value each made the validation throw, so the client received a 500 instead of the report.
 * The routes run authenticated in the real application with their real validators. Only the BioSample
 * validator, which sends XML to NCBI, is a counter, and an incomplete instance never reaches it.
 */
@Tag("integration")
class IncompleteInstanceValidationTest {

  private static final String FILE = "reads.fastq";

  static final AtomicInteger bioSampleCalls = new AtomicInteger();
  static DropwizardTestSupport<SubmissionServerConfiguration> server;
  static String auth;
  static final HttpClient CLIENT = HttpClient.newHttpClient();

  public static class ValidationApplication extends SubmissionServerApplication {
    @Override
    public void initializeApp() {
    }

    @Override
    public void runApp(SubmissionServerConfiguration configuration, Environment environment) {
      environment.jersey().register(new NcbiGenericSubmissionServerResource(cedarConfig, new NcbiGenericValidator(),
          new NcbiGenericTemplateInstance2XMLConverter(), countingBioSampleValidator()));
      environment.jersey().register(new NcbiCairrSubmissionServerResource(cedarConfig, new NcbiCairrValidator(),
          new NcbiCairrTemplateInstance2XMLConverter(), countingBioSampleValidator()));
    }
  }

  @BeforeAll
  static void start() throws Exception {
    Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
    environment.put("CEDAR_SUBMISSION_HTTP_PORT", "0");
    environment.put("CEDAR_SUBMISSION_ADMIN_PORT", "0");
    environment.put("CEDAR_SUBMISSION_STOP_PORT", "0");
    CedarEnvironmentSource.setOverride(environment);
    server = new DropwizardTestSupport<>(ValidationApplication.class,
        ResourceHelpers.resourceFilePath("test-config.yml"));
    server.before();
    CedarConfig config =
        CedarConfig.getInstance(CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_SUBMISSION));
    TestAuthUtil.installInMemoryUserService(config);
    auth = TestAuthUtil.getTestUser1AuthHeader(config);
  }

  @AfterEach
  void reset() {
    bioSampleCalls.set(0);
  }

  @AfterAll
  static void stop() {
    if (server != null) server.after();
  }

  /** The element names each route reads, and the field of an SRA entry that lists its file-name fields. */
  static Stream<Arguments> pipelines() {
    return Stream.of(
        Arguments.of("/command/validate-ncbi", "NCBI BioProject for Human Tissue", "NCBI BioSample for Human Tissue",
            "NCBI SRA for Human Tissue", "File Name"),
        Arguments.of("/command/validate-cairr", "BioProject for AIRR NCBI", "BioSample for AIRR NCBI",
            "Sequence Read Archive for AIRR NCBI", "filename"));
  }

  /** An instance with no elements lacks both the BioSample and the SRA element the reference check reads. */
  @ParameterizedTest
  @MethodSource("pipelines")
  void anInstanceWithoutItsElementsIsReportedAsMissingThem(String route, String bioproject, String biosample,
                                                           String sra, String fileNames) throws Exception {
    assertEquals(List.of("Missing element: " + bioproject, "Missing element: " + biosample,
        "Missing element: " + sra), validate(route, "{\"instance\":{}}"));
  }

  /** The file names are checked against the SRA element, which the instance lacks. */
  @ParameterizedTest
  @MethodSource("pipelines")
  void fileNamesForAnInstanceWithoutItsSraElementAreReportedAsAbsentFromIt(String route, String bioproject,
                                                                          String biosample, String sra,
                                                                          String fileNames) throws Exception {
    assertEquals(List.of("Missing element: " + bioproject, "Missing element: " + biosample,
        "Missing element: " + sra, notInTheSra(FILE)),
        validate(route, "{\"instance\":{},\"userFileNames\":[\"" + FILE + "\"]}"));
  }

  @ParameterizedTest
  @MethodSource("pipelines")
  void anSraEntryWithoutItsFileNameFieldsIsReportedAsSuch(String route, String bioproject, String biosample,
                                                          String sra, String fileNames) throws Exception {
    List<String> messages = validate(route, "{\"instance\":{\"" + sra + "\":[{}]}}");
    assertTrue(messages.contains(fileNames + " value must be supplied for SRA"), messages.toString());
  }

  @ParameterizedTest
  @MethodSource("pipelines")
  void anSraFileNameFieldWithoutAValueIsReportedAsSuch(String route, String bioproject, String biosample,
                                                       String sra, String fileNames) throws Exception {
    String entry = "{\"" + fileNames + "\":[\"file 1\"],\"file 1\":{\"@value\":null}}";
    List<String> messages = validate(route,
        "{\"instance\":{\"" + sra + "\":[" + entry + "]},\"userFileNames\":[\"" + FILE + "\"]}");
    assertTrue(messages.contains("File name field not present: file 1"), messages.toString());
    assertTrue(messages.contains(notInTheSra(FILE)), messages.toString());
  }

  // Helpers

  private static BioSampleValidator countingBioSampleValidator() {
    return new BioSampleValidator() {
      @Override
      public CEDARValidationResponse validateBioSampleSubmission(String bioSampleSubmissionXML) {
        bioSampleCalls.incrementAndGet();
        CEDARValidationResponse response = new CEDARValidationResponse();
        response.setIsValid(true);
        response.setMessages(new ArrayList<>());
        return response;
      }
    };
  }

  private static String notInTheSra(String fileName) {
    return "File name selected by the user is not present in the SRA section: '" + fileName + "'";
  }

  /** Posts the body and returns the report's messages, requiring a 200 that reports the instance invalid. */
  private static List<String> validate(String route, String body) throws Exception {
    HttpResponse<String> response = CLIENT.send(HttpRequest.newBuilder(
            URI.create("http://127.0.0.1:" + server.getLocalPort() + route))
        .timeout(Duration.ofSeconds(10)).header("Authorization", auth).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode(), response.body());
    JsonNode report = JsonMapper.STRICT_MAPPER.readTree(response.body());
    assertFalse(report.path("isValid").asBoolean(true), response.body());
    assertEquals(0, bioSampleCalls.get(), "an incomplete instance must not reach the BioSample validator");
    List<String> messages = new ArrayList<>();
    report.path("messages").forEach(message -> messages.add(message.asText()));
    return messages;
  }
}
