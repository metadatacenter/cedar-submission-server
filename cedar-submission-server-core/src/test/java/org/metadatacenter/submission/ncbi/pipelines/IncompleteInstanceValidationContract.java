package org.metadatacenter.submission.ncbi.pipelines;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.metadatacenter.submission.CEDARValidationResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An incomplete instance is reported, never a crash, by the validation of each NCBI pipeline.
 *
 * <p>An instance without its BioSample or SRA element, or with an SRA entry that lacks its list of file-name
 * fields or leaves one of them empty, made the validation throw, so the route answered a 500 instead of the
 * report. Each case starts from an instance the pipeline accepts and takes away one thing, so the report
 * concerns that thing alone.
 */
public abstract class IncompleteInstanceValidationContract {

  private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

  private static final String SAMPLE = "sample 1";
  private static final String FILE_NAME_FIELD = "file 1";
  private static final String FILE = "reads.fastq";

  /** The element and field names a pipeline's validation reads from an instance. */
  public record Pipeline(String bioprojectElement, List<String> bioprojectFields, String biosampleElement,
                            List<String> biosampleFields, String biosampleSampleField, String sraElement,
                            List<String> sraFields, String sraSampleField, String sraFileNameField) {
  }

  protected abstract Pipeline pipeline();

  protected abstract CEDARValidationResponse validateInstance(JsonNode instance);

  protected abstract CEDARValidationResponse validateFilenames(JsonNode instance, List<String> userFileNames);

  @Test
  public void aCompleteInstanceIsValid() {
    ObjectNode instance = completeInstance();
    assertReport(List.of(), validateInstance(instance));
    assertReport(List.of(), validateFilenames(instance, List.of(FILE)));
  }

  @Test
  public void anInstanceWithoutItsBioSampleElementIsReportedAsSuch() {
    ObjectNode instance = completeInstance();
    instance.remove(pipeline().biosampleElement());
    assertReport(List.of("Missing element: " + pipeline().biosampleElement()), validateInstance(instance));
  }

  /** A multi-instance element with no instances counts as missing. */
  @Test
  public void anInstanceWithAnEmptyBioSampleElementIsReportedAsMissingIt() {
    ObjectNode instance = completeInstance();
    instance.set(pipeline().biosampleElement(), NODES.arrayNode());
    assertReport(List.of("Missing element: " + pipeline().biosampleElement()), validateInstance(instance));
  }

  @Test
  public void anInstanceWithoutItsSraElementIsReportedAsSuch() {
    ObjectNode instance = completeInstance();
    instance.remove(pipeline().sraElement());
    assertReport(List.of("Missing element: " + pipeline().sraElement()), validateInstance(instance));
    assertReport(List.of(notInTheSra(FILE)), validateFilenames(instance, List.of(FILE)));
  }

  @Test
  public void anSraEntryWithoutItsFileNameFieldsIsReportedAsSuch() {
    ObjectNode instance = completeInstance();
    sraEntry(instance).remove(pipeline().sraFileNameField());
    assertReport(List.of(pipeline().sraFileNameField() + " value must be supplied for SRA"),
        validateInstance(instance));
    assertReport(List.of(notInTheSra(FILE)), validateFilenames(instance, List.of(FILE)));
  }

  @Test
  public void anSraFileNameFieldWithoutAValueIsReportedAsSuch() {
    ObjectNode instance = completeInstance();
    sraEntry(instance).set(FILE_NAME_FIELD, NODES.objectNode().putNull("@value"));
    assertReport(List.of("File name field not present: " + FILE_NAME_FIELD), validateInstance(instance));
    assertReport(List.of(notInTheSra(FILE)), validateFilenames(instance, List.of(FILE)));
  }

  @Test
  public void anEmptyInstanceIsReportedAsMissingEveryElement() {
    ObjectNode instance = NODES.objectNode();
    assertReport(List.of("Missing element: " + pipeline().bioprojectElement(),
        "Missing element: " + pipeline().biosampleElement(), "Missing element: " + pipeline().sraElement()),
        validateInstance(instance));
    assertReport(List.of(notInTheSra(FILE)), validateFilenames(instance, List.of(FILE)));
  }

  // Helpers

  /**
   * Builds an instance the pipeline accepts. Every field it reads holds a value, the one BioSample and the one
   * SRA entry identify the same sample, and the SRA entry's one file-name field holds {@link #FILE}.
   */
  private ObjectNode completeInstance() {
    Pipeline pipeline = pipeline();
    ObjectNode instance = NODES.objectNode();
    instance.set(pipeline.bioprojectElement(), filled(pipeline.bioprojectFields()));

    ObjectNode biosample = filled(pipeline.biosampleFields());
    biosample.set(pipeline.biosampleSampleField(), value(SAMPLE));
    instance.set(pipeline.biosampleElement(), NODES.arrayNode().add(biosample));

    ObjectNode sra = filled(pipeline.sraFields());
    sra.set(pipeline.sraSampleField(), value(SAMPLE));
    sra.set(pipeline.sraFileNameField(), NODES.arrayNode().add(FILE_NAME_FIELD));
    sra.set(FILE_NAME_FIELD, value(FILE));
    instance.set(pipeline.sraElement(), NODES.arrayNode().add(sra));
    return instance;
  }

  private ObjectNode sraEntry(ObjectNode instance) {
    return (ObjectNode) ((ArrayNode) instance.get(pipeline().sraElement())).get(0);
  }

  private static ObjectNode filled(List<String> fields) {
    ObjectNode element = NODES.objectNode();
    fields.forEach(field -> element.set(field, value(field + " value")));
    return element;
  }

  private static ObjectNode value(String value) {
    return NODES.objectNode().put("@value", value);
  }

  private static String notInTheSra(String fileName) {
    return "File name selected by the user is not present in the SRA section: '" + fileName + "'";
  }

  private static void assertReport(List<String> messages, CEDARValidationResponse response) {
    assertEquals(messages, response.getMessages());
    if (messages.isEmpty()) {
      assertTrue(response.getIsValid());
    } else {
      assertFalse(response.getIsValid());
    }
  }
}
