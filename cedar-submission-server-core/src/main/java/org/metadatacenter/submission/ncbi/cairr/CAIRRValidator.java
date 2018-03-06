package org.metadatacenter.submission.ncbi.cairr;

import org.metadatacenter.exception.CedarException;
import org.metadatacenter.submission.BioProject;
import org.metadatacenter.submission.CAIRRTemplate;
import org.metadatacenter.submission.CEDARValidationResponse;

import java.util.ArrayList;
import java.util.List;

public class CAIRRValidator
{
  public CEDARValidationResponse validate(CAIRRTemplate cairrInstance) throws CedarException
  {
    CEDARValidationResponse validationResponse = new CEDARValidationResponse();
    List<String> messages = new ArrayList<>();
    validationResponse.setMessages(messages);

    messages.addAll(validateBioProject(cairrInstance.getBioProject()));

    return validationResponse;
  }

  private List<String> validateBioProject(BioProject bioProjectInstance)
  {
    List<String> messages = new ArrayList<>();

    return messages;
  }
}
