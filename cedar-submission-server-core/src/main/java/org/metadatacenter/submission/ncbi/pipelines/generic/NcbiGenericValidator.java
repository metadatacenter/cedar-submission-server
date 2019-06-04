package org.metadatacenter.submission.ncbi.pipelines.generic;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.submission.BioProjectForAIRRNCBI;
import org.metadatacenter.submission.BioSampleForAIRRNCBI;
import org.metadatacenter.submission.CAIRRTemplate;
import org.metadatacenter.submission.CEDARValidationResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericConstants.*;

// TODO: do validation
public class NcbiGenericValidator
{
  public CEDARValidationResponse validate(JsonNode instance) throws CedarException
  {
    CEDARValidationResponse validationResponse = new CEDARValidationResponse();
    List<String> messages = new ArrayList<>();

    // Validate BioProject
    JsonNode bioProjectNode = NcbiGenericUtil.getTemplateElementNode(instance, BIOPROJECT_ELEMENT);
    messages.addAll(validateBioProject(bioProjectNode));

    // Validate BioSample
    JsonNode bioSampleNode = NcbiGenericUtil.getTemplateElementNode(instance, BIOSAMPLE_ELEMENT);
    messages.addAll(validateBioSample(bioProjectNode));

    // Validate SRA
    JsonNode sraNode = NcbiGenericUtil.getTemplateElementNode(instance, SRA_ELEMENT);
    messages.addAll(validateSra(sraNode));

    validationResponse.setMessages(messages);

    if (messages.size() == 0) {
      validationResponse.setIsValid(true);
    }
    else {
      validationResponse.setIsValid(false);
    }

    return validationResponse;
  }

  private List<String> validateBioProject(JsonNode bioProject)
  {
    List<String> messages = new ArrayList<>();

    for (String requiredField : Arrays.asList(BIOPROJECT_REQUIRED)) {
      if (!NcbiGenericUtil.getTemplateFieldValue(bioProject, requiredField).isPresent()) {
        messages.add(requiredField + " field must be supplied for BioProject");
      }
    }

    return messages;
  }

  private List<String> validateBioSample(JsonNode bioSample)
  {
    List<String> messages = new ArrayList<>();

    for (String requiredField : Arrays.asList(BIOSAMPLE_REQUIRED)) {
      if (!NcbiGenericUtil.getTemplateFieldValue(bioSample, requiredField).isPresent()) {
        messages.add(requiredField + " field must be supplied for BioProject");
      }
    }

    return messages;
  }

  private List<String> validateSra(JsonNode sra)
  {
    List<String> messages = new ArrayList<>();

    for (String requiredField : Arrays.asList(SRA_REQUIRED)) {
      if (!NcbiGenericUtil.getTemplateFieldValue(sra, requiredField).isPresent()) {
        messages.add(requiredField + " field must be supplied for BioProject");
      }
    }

    return messages;
  }
}
