package org.metadatacenter.submission.ncbi.cairr;

import org.metadatacenter.exception.CedarException;
import org.metadatacenter.submission.BioProject;
import org.metadatacenter.submission.BioSample;
import org.metadatacenter.submission.CAIRRTemplate;
import org.metadatacenter.submission.CEDARValidationResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CAIRRValidator
{
  public CEDARValidationResponse validate(CAIRRTemplate cairrInstance) throws CedarException
  {
    CEDARValidationResponse validationResponse = new CEDARValidationResponse();
    List<String> messages = new ArrayList<>();
    validationResponse.setMessages(messages);

    messages.addAll(validateBioProject(cairrInstance.getBioProject()));

    List<BioSample> bioSamples = cairrInstance.getBioSample();

    List<String> sampleIDs = bioSamples.stream().filter(e -> e.getSampleName() != null)
      .map(e -> e.getSampleName().getValue()).collect(Collectors.toList());

    for (BioSample bioSample : bioSamples) {

    }

    return validationResponse;
  }

  private List<String> validateBioProject(BioProject bioProject)
  {
    List<String> messages = new ArrayList<>();

    if (bioProject.getStudyTitle() == null || bioProject.getStudyTitle().getValue() == null || bioProject
      .getStudyTitle().getValue().isEmpty())
      messages.add("Study Title field must be supplied for BioProject");

    if (bioProject.getStudyType() == null || bioProject.getStudyType().getValue() == null || bioProject.getStudyType()
      .getValue().isEmpty())
      messages.add("Study Type field must be supplied for BioProject");

    if (bioProject.getFundingAgency() == null || bioProject.getFundingAgency().getValue() == null || bioProject
      .getFundingAgency().getValue().isEmpty())
      messages.add("Funding Agency field must be supplied for BioProject");

    if (bioProject.getLabName() == null || bioProject.getLabName().getValue() == null || bioProject.getLabName()
      .getValue().isEmpty())
      messages.add("Lab Name field must be supplied for BioProject");

    if (bioProject.getFirstGivenName() == null || bioProject.getFirstGivenName().getValue() == null || bioProject
      .getFirstGivenName().getValue().isEmpty())
      messages.add("First (given) Name field must be supplied for BioProject");

    if (bioProject.getLastFamilyName() == null || bioProject.getLastFamilyName().getValue() == null || bioProject
      .getLastFamilyName().getValue().isEmpty())
      messages.add("Last (family) Name field must be supplied for BioProject");

    if (bioProject.getEMail() == null || bioProject.getEMail().getValue() == null || bioProject.getEMail().getValue()
      .isEmpty())
      messages.add("Email field must be supplied for BioProject");

    if (bioProject.getSubmittingOrganization() == null || bioProject.getSubmittingOrganization().getValue() == null
      || bioProject.getSubmittingOrganization().getValue().isEmpty())
      messages.add("Submitting Organization field must be supplied for BioProject");

    if (bioProject.getContactInformationCorrespondingAuthorEMail() == null
      || bioProject.getContactInformationCorrespondingAuthorEMail().getValue() != null || bioProject
      .getContactInformationCorrespondingAuthorEMail().getValue().isEmpty())
      messages.add("Corresponding Author Email field must be supplied for BioProject");

    if (bioProject.getDepartment() == null || bioProject.getDepartment().getValue() == null || bioProject
      .getDepartment().getValue().isEmpty())
      messages.add("Department field must be supplied for BioProject");

    return messages;
  }

  private List<String> validateBioSample(BioSample bioSample)
  {
    List<String> messages = new ArrayList<>();

    return messages;
  }
}
