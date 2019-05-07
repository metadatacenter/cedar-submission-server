package org.metadatacenter.submission.ncbi.pipelines.generic;

import org.metadatacenter.exception.CedarException;
import org.metadatacenter.submission.BioProjectForAIRRNCBI;
import org.metadatacenter.submission.BioSampleForAIRRNCBI;
import org.metadatacenter.submission.CAIRRTemplate;
import org.metadatacenter.submission.CEDARValidationResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NcbiGenericValidator
{
  public CEDARValidationResponse validate(CAIRRTemplate cairrInstance) throws CedarException
  {
    CEDARValidationResponse validationResponse = new CEDARValidationResponse();
    List<String> messages = new ArrayList<>();
    validationResponse.setMessages(messages);

    messages.addAll(validateBioProject(cairrInstance.getBioProjectForAIRRNCBI()));

    List<BioSampleForAIRRNCBI> bioSamples = cairrInstance.getBioSampleForAIRRNCBI();

    List<String> sampleIDs = bioSamples.stream().filter(e -> e.getSampleID() != null)
      .map(e -> e.getSampleID().getValue()).collect(Collectors.toList());

    for (BioSampleForAIRRNCBI bioSample : bioSamples) {

    }

    validationResponse.setIsValid(true);

    return validationResponse;
  }

  private List<String> validateBioProject(BioProjectForAIRRNCBI bioProject)
  {
    List<String> messages = new ArrayList<>();

    if (bioProject.getStudyTitle() == null || bioProject.getStudyTitle().getValue() == null || bioProject
      .getStudyTitle().getValue().isEmpty())
      messages.add("Study Title field must be supplied for BioProject");

    if (bioProject.getStudyType() == null || bioProject.getStudyType().getId() == null )
      messages.add("Study Type field must be supplied for BioProject");

    if (bioProject.getFundingAgency() == null || bioProject.getFundingAgency().getValue() == null || bioProject
      .getFundingAgency().getValue().isEmpty())
      messages.add("Funding Agency field must be supplied for BioProject");

    if (bioProject.getLabName() == null || bioProject.getLabName().getValue() == null || bioProject.getLabName()
      .getValue().isEmpty())
      messages.add("Lab Name field must be supplied for BioProject");

    if (bioProject.getContactInformationDataCollection() == null || bioProject.getContactInformationDataCollection().getValue() == null || bioProject.getContactInformationDataCollection().getValue()
      .isEmpty())
      messages.add("Email field must be supplied for BioProject");


    return messages;
  }

  private List<String> validateBioSample(BioSampleForAIRRNCBI bioSample)
  {
    List<String> messages = new ArrayList<>();

    return messages;
  }
}
