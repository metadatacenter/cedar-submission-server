package org.metadatacenter.submission.ncbi.cairr;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.FileUtils;
import org.metadatacenter.submission.CAIRRTemplate;
import org.metadatacenter.submission.ncbi.NcbiConstants;
import org.metadatacenter.submission.ncbi.NcbiSubmissionXMLFileGenerator;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.File;
import java.io.IOException;

public class NcbiCairrSubmissionXMLFileGenerator implements NcbiSubmissionXMLFileGenerator {

  final static Logger logger = LoggerFactory.getLogger(NcbiCairrSubmissionXMLFileGenerator.class);

  public File generateSubmissionXmlFile(File instanceFile, String submissionLocalFolderPath) throws
      IOException, JAXBException, DatatypeConfigurationException {
    CAIRRTemplateInstance2SRAXMLConverter converter = new CAIRRTemplateInstance2SRAXMLConverter();

    CAIRRTemplate cairrInstance;
    try {
      cairrInstance = JsonMapper.MAPPER.readValue(instanceFile, CAIRRTemplate.class);
    } catch (JsonMappingException e) {
      throw new IOException("The instance uploaded is not compatible with the CAIRR template", e);
    }
    String submissionXml = converter.convertTemplateInstanceToXML(cairrInstance);

    logger.info("XML: " + submissionXml);

    File submissionXmlFile = new File(submissionLocalFolderPath + "/" + NcbiConstants.SUBMISSION_XML_FILE_NAME);
    FileUtils.writeStringToFile(submissionXmlFile, submissionXml);
    return submissionXmlFile;
  }
}
