package org.metadatacenter.submission.ncbi.airr;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.FileUtils;
import org.metadatacenter.submission.AIRRTemplate;
import org.metadatacenter.submission.ncbi.NcbiConstants;
import org.metadatacenter.submission.ncbi.NcbiSubmissionXMLFileGenerator;
import org.metadatacenter.util.json.JsonMapper;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.File;
import java.io.IOException;

public class NcbiAirrSubmissionXMLFileGenerator implements NcbiSubmissionXMLFileGenerator {

  public File generateSubmissionXmlFile(File instanceFile, String submissionLocalFolderPath) throws
      IOException, JAXBException, DatatypeConfigurationException {
    AIRRTemplateInstance2SRAXMLConverter converter = new AIRRTemplateInstance2SRAXMLConverter();

    AIRRTemplate airrInstance;
    try {
      airrInstance = JsonMapper.MAPPER.readValue(instanceFile, AIRRTemplate.class);
    } catch (JsonMappingException e) {
      throw new IOException("The instance uploaded is not compatible with the AIRR template", e);
    }
    String submissionXml = converter.convertTemplateInstanceToXML(airrInstance);
    File submissionXmlFile = new File(submissionLocalFolderPath + "/" + NcbiConstants.SUBMISSION_XML_FILE_NAME);
    FileUtils.writeStringToFile(submissionXmlFile, submissionXml);
    return submissionXmlFile;
  }
}
