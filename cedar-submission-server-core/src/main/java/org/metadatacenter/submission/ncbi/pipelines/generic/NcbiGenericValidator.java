package org.metadatacenter.submission.ncbi.pipelines.generic;

import com.fasterxml.jackson.databind.JsonNode;
import generated.Submission;
import org.metadatacenter.submission.CEDARValidationResponse;
import org.metadatacenter.submission.ncbi.pipelines.NcbiPipelinesCommonUtil;

import java.util.*;

import static org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericConstants.*;

public class NcbiGenericValidator {

  public CEDARValidationResponse validate(JsonNode instance) {

    CEDARValidationResponse validationResponse = new CEDARValidationResponse();
    List<String> messages = new ArrayList<>();

    /** Validate top level fields **/
    NcbiGenericUtil.isValidField(instance, SUBMISSION_RELEASE_DATE_FIELD, true, false);

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
      if (!NcbiGenericUtil.isValidBioprojectField(bioproject, fieldName)) {
        messages.add(fieldName + " value must be supplied for BioProject");
      }
    }
    return messages;
  }

  private List<String> validateBiosample(JsonNode biosamples) {
    List<String> messages = new ArrayList<>();
    for (JsonNode biosample : biosamples) {
      for (String fieldName : Arrays.asList(BIOSAMPLE_FIELDS)) {
        if (!NcbiGenericUtil.isValidBiosampleField(biosample, fieldName)) {
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
        if (!NcbiGenericUtil.isValidSraField(sra, fieldName)) {
          messages.add(fieldName + " value must be supplied for SRA");
        }
      }
      // File type and file names
      Iterator<JsonNode> fileNamesIt = sra.get(SRA_FILE_NAME_FIELD).iterator();
      while (fileNamesIt.hasNext()) {
        String fileNameField = fileNamesIt.next().asText();
        Optional<String> fileName = NcbiGenericUtil.getTemplateFieldValue(sra, fileNameField);
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
      Optional<String> biosampleSampleName = NcbiGenericUtil.getTemplateFieldValue(biosample, BIOSAMPLE_SAMPLE_NAME_FIELD);
      if (biosampleSampleName.isPresent()) {
        biosampleSampleNames.add(biosampleSampleName.get());
      }
    }

    List<String> sraSampleNames = new ArrayList<>();
    for (JsonNode sra : sras) {
      Optional<String> sraSampleName = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_SAMPLE_NAME_FIELD);
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

  /**
   * Checks the consistency between the file names selected to be uploaded and the file names included in the instance (SRA section)
   * @return
   */
  public CEDARValidationResponse validateFilenames(JsonNode instance, List<String> userFileNames) {

    CEDARValidationResponse validationResponse = new CEDARValidationResponse();
    List<String> messages = new ArrayList<>();

    // Extract file names from the sra section
    List<String> sraFilenames = extractSraFileNames(instance);

    for (String sraFilename : sraFilenames) {
      if (!userFileNames.contains(sraFilename)) {
        messages.add("File name in the SRA section is not present in the files selected by the user: '" + sraFilename + "'");
      }
    }

    for (String userFileName : userFileNames) {
      if (!sraFilenames.contains(userFileName)) {
        messages.add("File name selected by the user is not present in the SRA section: '" + userFileName + "'");
      }
    }

    /** Return validation messages **/
    validationResponse.setMessages(messages);

    if (messages.size() == 0) {
      validationResponse.setIsValid(true);
    } else {
      validationResponse.setIsValid(false);
    }

    return validationResponse;
  }

  public List<String> extractSraFileNames(JsonNode instance) {

    JsonNode sras = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, SRA_ELEMENT).get();
    List<String> sraFileNames = new ArrayList();

    for (JsonNode sra : sras) {

      if (sra.hasNonNull(SRA_FILE_NAME_FIELD) && sra.get(SRA_FILE_NAME_FIELD).size() > 0) {
        Iterator<JsonNode> fileNameFieldsIt = sra.get(SRA_FILE_NAME_FIELD).iterator();

        while (fileNameFieldsIt.hasNext()) {
          String fileNameField = fileNameFieldsIt.next().asText();
          Optional<String> fileName = NcbiGenericUtil.getTemplateFieldValue(sra, fileNameField);
          sraFileNames.add(fileName.get());
        }

      } else {
        return new ArrayList<>();
      }
    }
    return sraFileNames;
  }



}
