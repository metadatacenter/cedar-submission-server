package org.metadatacenter.submission.ncbi.pipelines.generic;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.Optional;

import static org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericConstants.*;

public class NcbiGenericUtil {

  public static Optional<String> getBioSampleTemplateFieldValue(JsonNode node, String fieldName) {
    return getTemplateFieldValue(node, fieldName, true, BIOSAMPLE_REQUIRED);
  }

  public static Optional<String> getBioProjectTemplateFieldValue(JsonNode node, String fieldName) {
    return getTemplateFieldValue(node, fieldName, true, BIOPROJECT_REQUIRED);
  }

  public static Optional<String> getSraTemplateFieldValue(JsonNode node, String fieldName) {
    return getTemplateFieldValue(node, fieldName, true, SRA_REQUIRED);
  }

  private static Optional<String> getTemplateFieldValue(JsonNode node, String fieldName,
                                                        boolean requiredField, String[] requiredFieldValues) {

    boolean requiredFieldValue = Arrays.asList(requiredFieldValues).contains(fieldName);
    return getTemplateFieldValue(node, fieldName, requiredField, requiredFieldValue);

  }

  public static Optional<String> getTemplateFieldValue(JsonNode node, String fieldName,
                                                       boolean requiredField, boolean requiredFieldValue) {

    if (node.has(fieldName)) {
      JsonNode fieldNode = node.get(fieldName);
      if (fieldNode.hasNonNull(VALUE_FIELD) && !fieldNode.get(VALUE_FIELD).asText().isEmpty()) {
        return Optional.of(fieldNode.get(VALUE_FIELD).asText());
      }
      else if (fieldNode.hasNonNull(ID_FIELD) && !fieldNode.get(ID_FIELD).asText().isEmpty()) {
        return Optional.of(fieldNode.get(ID_FIELD).asText());
      }
      else {
        if (requiredFieldValue) {
          throw new IllegalArgumentException("Missing required field value: " + fieldName);
        }
        else {
          return Optional.empty();
        }
      }
    }
    else {
      if (requiredField) {
        throw new IllegalArgumentException("Missing required field: " + fieldName);
      }
      else {
        return Optional.empty();
      }
    }
  }

  public static JsonNode getTemplateElementNode(JsonNode node, String elementName) {
    if (node.has(elementName)) {
      if (node.hasNonNull(elementName) && node.get(elementName).size() > 0) {
        return node.get(elementName);
      }
      else {
        throw new IllegalArgumentException("Template element is null or empty : " + elementName);
      }
    }
    else {
      throw new IllegalArgumentException("Missing required template element: " + elementName);
    }
  }

}
