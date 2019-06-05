package org.metadatacenter.submission.ncbi.pipelines.generic;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.Optional;

import static org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericConstants.*;

public class NcbiGenericUtil {

  public static Optional<String> getTemplateFieldValue(JsonNode node, String fieldName) {

    if (node.has(fieldName)) {
      JsonNode fieldNode = node.get(fieldName);
      if (fieldNode.hasNonNull(VALUE_FIELD) && !fieldNode.get(VALUE_FIELD).asText().isEmpty()) {
        return Optional.of(fieldNode.get(VALUE_FIELD).asText());
      } else if (fieldNode.hasNonNull(ID_FIELD) && !fieldNode.get(ID_FIELD).asText().isEmpty()) {
        return Optional.of(fieldNode.get(ID_FIELD).asText());
      } else {
        return Optional.empty();
      }
    } else {

      return Optional.empty();

    }
  }

  public static JsonNode getTemplateElementNode(JsonNode node, String elementName) {
    if (node.has(elementName)) {
      if (node.hasNonNull(elementName) && node.get(elementName).size() > 0) {
        return node.get(elementName);
      } else {
        throw new IllegalArgumentException("Template element is null or empty : " + elementName);
      }
    } else {
      throw new IllegalArgumentException("Missing required template element: " + elementName);
    }
  }

  public static boolean isValidField(JsonNode node, String fieldName, boolean requiredField, boolean requiredFieldValue) {
    if (node.has(fieldName)) {
      JsonNode fieldNode = node.get(fieldName);
      if (!fieldNode.isArray()) { // it is not an array node
        if (fieldNode.hasNonNull(VALUE_FIELD) && !fieldNode.get(VALUE_FIELD).asText().isEmpty()) {
          return true;
        } else if (fieldNode.hasNonNull(ID_FIELD) && !fieldNode.get(ID_FIELD).asText().isEmpty()) {
          return true;
        } else {
          if (requiredFieldValue) {
            return false;
          } else {
            return true;
          }
        }
      }
      else { // it is an array node
        if (fieldNode.size() > 0) {
          return true;
        }
        else {
          return false;
        }
      }
    } // if the field is not there
    else {
      if (requiredField) {
        return false;
      } else {
        return true;
      }
    }
  }

  public static boolean isValidBioprojectField(JsonNode bioproject, String fieldName) {
    boolean requiredFieldValue = false;
    if (Arrays.asList(BIOPROJECT_REQUIRED_FIELD_VALUES).contains(fieldName)) {
      requiredFieldValue = true;
    }
    return isValidField(bioproject, fieldName, true, requiredFieldValue);
  }

  public static boolean isValidBiosampleField(JsonNode biosample, String fieldName) {
    boolean requiredFieldValue = false;
    if (Arrays.asList(BIOSAMPLE_REQUIRED_FIELD_VALUES).contains(fieldName)) {
      requiredFieldValue = true;
    }
    return isValidField(biosample, fieldName, true, requiredFieldValue);
  }

  public static boolean isValidSraField(JsonNode sra, String fieldName) {
    boolean requiredFieldValue = false;
    if (Arrays.asList(SRA_REQUIRED_FIELD_VALUES).contains(fieldName)) {
      requiredFieldValue = true;
    }
    return isValidField(sra, fieldName, true, requiredFieldValue);
  }

}
