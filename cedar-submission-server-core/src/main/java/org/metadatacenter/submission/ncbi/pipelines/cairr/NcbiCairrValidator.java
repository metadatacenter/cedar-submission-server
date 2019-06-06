package org.metadatacenter.submission.ncbi.pipelines.cairr;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.submission.CEDARValidationResponse;
import org.metadatacenter.submission.ncbi.pipelines.NcbiPipelinesCommonUtil;

import java.util.*;

import static org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrConstants.*;

public class NcbiCairrValidator {

  public CEDARValidationResponse validate(JsonNode instance) {

    CEDARValidationResponse validationResponse = new CEDARValidationResponse();
    List<String> messages = new ArrayList<>();

    /** Validate top level fields **/
    NcbiCairrUtil.isValidField(instance, SUBMISSION_RELEASE_DATE_FIELD, true, false);

    /** Validate BioProject **/
    Optional<JsonNode> bioproject = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, BIOPROJECT_ELEMENT);
    if (bioproject.isEmpty()) {
      messages.add("Missing element: " + BIOPROJECT_ELEMENT);
    }
    else {
      messages.addAll(validateBioproject(bioproject.get()));
    }

    /** Validate BioSample **/
    Optional<JsonNode> biosamples = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, BIOSAMPLE_ELEMENT);
    if (biosamples.isEmpty()) {
      messages.add("Missing element: " + BIOSAMPLE_ELEMENT);
    }
    else {
      messages.addAll(validateBiosample(biosamples.get()));
    }

    /** Validate SRA **/
    Optional<JsonNode> sras = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, SRA_ELEMENT);
    if (sras.isEmpty()) {
      messages.add("Missing element: " + SRA_ELEMENT);
    }
    else {
      messages.addAll(validateSra(sras.get()));
    }

    /** Check the BioSample-SRA references **/
    messages.addAll(validateBiosampleSraRefs(biosamples.get(), sras.get()));

    /** Return validation messages **/
    validationResponse.setMessages(messages);

    if (messages.size() == 0) {
      validationResponse.setIsValid(true);
    } else {
      validationResponse.setIsValid(false);
    }

    return validationResponse;
  }

  private List<String> validateBioproject(JsonNode bioproject) {
    List<String> messages = new ArrayList<>();
    for (String fieldName : Arrays.asList(BIOPROJECT_FIELDS)) {
      if (!NcbiCairrUtil.isValidBioprojectField(bioproject, fieldName)) {
        messages.add(fieldName + " value must be supplied for BioProject");
      }
    }
    return messages;
  }

  private List<String> validateBiosample(JsonNode biosamples) {
    List<String> messages = new ArrayList<>();
    for (JsonNode biosample : biosamples) {
      for (String fieldName : Arrays.asList(BIOSAMPLE_FIELDS)) {
        if (!NcbiCairrUtil.isValidBiosampleField(biosample, fieldName)) {
          messages.add(fieldName + " value must be supplied for BioSample");
        }
      }
    }
    return messages;
  }

  private List<String> validateSra(JsonNode sras) {
    List<String> messages = new ArrayList<>();
    for (JsonNode sra : sras) {
      for (String fieldName : Arrays.asList(SRA_FIELDS)) {
        if (!NcbiCairrUtil.isValidSraField(sra, fieldName)) {
          messages.add(fieldName + " value must be supplied for SRA");
        }
      }
      // File type and file names
      Iterator<JsonNode> fileNamesIt = sra.get(SRA_FILE_NAME_FIELD).iterator();
      while (fileNamesIt.hasNext()) {
        String fileNameField = fileNamesIt.next().asText();
        Optional<String> fileName = NcbiCairrUtil.getTemplateFieldValue(sra, fileNameField);
        if (!fileName.isPresent()) {
          messages.add("File name field not present: " + fileNameField);
        }
      }
    }
    return messages;
  }

  private List<String> validateBiosampleSraRefs(JsonNode biosamples, JsonNode sras) {
    List<String> messages = new ArrayList<>();

    List<String> biosampleSampleNames = new ArrayList<>();
    for (JsonNode biosample : biosamples) {
      Optional<String> biosampleSampleName = NcbiCairrUtil.getTemplateFieldValue(biosample, BIOSAMPLE_SAMPLE_ID_FIELD);
      if (biosampleSampleName.isPresent()) {
        biosampleSampleNames.add(biosampleSampleName.get());
      }
    }

    List<String> sraSampleNames = new ArrayList<>();
    for (JsonNode sra : sras) {
      Optional<String> sraSampleName = NcbiCairrUtil.getTemplateFieldValue(sra, SRA_SAMPLE_ID_FIELD);
      if (sraSampleName.isPresent()) {
        sraSampleNames.add(sraSampleName.get());
      }
    }

    for (String biosampleSampleName : biosampleSampleNames) {
      if (!sraSampleNames.contains(biosampleSampleName)) {
        messages.add("Sample name in the BioSample section is not present in the SRA section: " + biosampleSampleName);
      }
    }

    for (String sraSampleName : sraSampleNames) {
      if (!biosampleSampleNames.contains(sraSampleName)) {
        messages.add("Sample name in the SRA section is not present in the BioSample section: " + sraSampleName);
      }
    }

    return messages;
  }

}
