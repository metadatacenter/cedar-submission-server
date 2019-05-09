package org.metadatacenter.submission.ncbi.pipelines.generic;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

import static org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericConstants.ID_FIELD;
import static org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericConstants.VALUE_FIELD;

public class NcbiGenericUtil {

  public static Optional<String> getTemplateFieldValue(JsonNode node, String fieldName) {
    // by default, the field is required, the field value is optional
    return getTemplateFieldValue(node, fieldName, true, false);
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
