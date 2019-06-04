package org.metadatacenter.submission.ncbi.pipelines.generic;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.submission.BioProjectForAIRRNCBI;
import org.metadatacenter.submission.BioSampleForAIRRNCBI;
import org.metadatacenter.submission.CAIRRTemplate;
import org.metadatacenter.submission.CEDARValidationResponse;

import java.util.ArrayList;
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
    // TODO: validate biosample


    // TODO: validate SRA?

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

    if (!NcbiGenericUtil.getTemplateFieldValue(bioProject, BIOPROJECT_STUDY_ID_FIELD).isPresent()) {
      messages.add("Study ID field must be supplied for BioProject");
    }

    if (!NcbiGenericUtil.getTemplateFieldValue(bioProject, BIOPROJECT_CONTACT_INFO_FIELD).isPresent()) {
      messages.add("Contact Information field must be supplied for BioProject");
    }

    if (!NcbiGenericUtil.getTemplateFieldValue(bioProject, BIOPROJECT_CONTACT_EMAIL_FIELD).isPresent()) {
      messages.add("Contact Email field must be supplied for BioProject");
    }

    return messages;
  }

  private List<String> validateBioSample(JsonNode bioSample)
  {
    List<String> messages = new ArrayList<>();

    return messages;
  }
}
