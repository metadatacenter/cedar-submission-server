package org.metadatacenter.submission.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every submission route documents every answer it gives, each refusal in the shared error body. Each
 * chunked upload documents its refusal of a chunk as the shared error body, which names the problem.
 */
class OpenApiContractTest {

  @ParameterizedTest
  @ValueSource(strings = {"/command/upload-ncbi-to-cedar", "/command/upload-cairr-to-cedar", "/command/immport-submit"})
  void theUploadDocumentsARefusedChunkAsTheSharedErrorBody(String path) throws IOException {
    JsonNode refusal = readSpec().path("paths").path(path).path("post").path("responses").path("400");
    assertEquals("#/components/schemas/CedarError",
        refusal.at("/content/application~1json/schema/$ref").asText(), refusal.toString());
    assertTrue(refusal.path("description").asText().contains("message names the problem"), refusal.toString());
  }

  /**
   * The routes documented their 401 and 500 with no schema, although both answer the shared error body,
   * and left out the refusals every authenticated route can give. A route relaying an upstream refusal
   * documents the statuses SubmissionAnswerMatrixTest shows it relaying.
   */
  @ParameterizedTest(name = "{1} {0}")
  @CsvSource(delimiter = '|', value = {
      "/command/validate-ncbi         | post | 400,401,406,429,500,503",
      "/command/upload-ncbi-to-cedar  | post | 400,401,406,415,429,500,503",
      "/command/validate-cairr        | post | 400,401,406,429,500,503",
      "/command/upload-cairr-to-cedar | post | 400,401,406,415,429,500,503",
      "/command/immport-workspaces    | get  | 400,401,404,406,409,412,415,422,429,500,502,503",
      "/command/immport-submit        | post | 400,401,406,415,429,500,503",
      "/command/immport-submit-old    | post | 400,401,406,415,429,500,503",
      "/command/validate-lincs        | post | 400,401,404,406,409,412,415,422,429,500,502,503,504",
      "/command/validate-biosample    | post | 400,401,406,415,429,500,503"})
  void everyRouteDocumentsEveryAnswer(String path, String method, String refusals) throws IOException {
    JsonNode responses = readSpec().path("paths").path(path).path(method).path("responses");
    Set<String> documented = new TreeSet<>();
    responses.fieldNames().forEachRemaining(documented::add);
    Set<String> expected = new TreeSet<>(Set.of(refusals.split(",")));
    expected.add("200");
    assertEquals(expected, documented, path);
    for (String status : refusals.split(",")) {
      assertEquals("#/components/schemas/CedarError",
          responses.at("/" + status + "/content/application~1json/schema/$ref").asText(), path + " " + status);
      assertFalse(responses.path(status).path("description").asText().isBlank(), path + " " + status);
    }
  }

  @Test
  void everySubmissionRouteIsListedAbove() throws IOException {
    Set<String> routes = new TreeSet<>();
    readSpec().path("paths").fieldNames().forEachRemaining(path -> {
      if (path.startsWith("/command/")) routes.add(path);
    });
    assertEquals(new TreeSet<>(Set.of("/command/validate-ncbi", "/command/upload-ncbi-to-cedar",
        "/command/validate-cairr", "/command/upload-cairr-to-cedar", "/command/immport-workspaces",
        "/command/immport-submit", "/command/immport-submit-old", "/command/validate-lincs",
        "/command/validate-biosample")), routes);
  }

  private static JsonNode readSpec() throws IOException {
    try (InputStream input = OpenApiContractTest.class.getResourceAsStream("/assets/swagger-api/swagger.json")) {
      assertNotNull(input, "generated OpenAPI document");
      return JsonMapper.STRICT_MAPPER.readTree(input);
    }
  }
}
