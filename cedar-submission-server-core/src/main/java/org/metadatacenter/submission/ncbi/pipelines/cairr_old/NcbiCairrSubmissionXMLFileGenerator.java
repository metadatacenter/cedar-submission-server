package org.metadatacenter.submission.ncbi.pipelines.cairr_old;

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
import java.text.ParseException;

public class NcbiCairrSubmissionXMLFileGenerator implements NcbiSubmissionXMLFileGenerator {

  final static Logger logger = LoggerFactory.getLogger(NcbiCairrSubmissionXMLFileGenerator.class);

  public File generateSubmissionXmlFile(File instanceFile, String submissionLocalFolderPath) throws
      IOException, JAXBException, DatatypeConfigurationException {
    CAIRRTemplateInstance2SRAXMLConverter converter = new CAIRRTemplateInstance2SRAXMLConverter();

    CAIRRTemplate cairrInstance;
    String submissionXml = null;
    try {
      cairrInstance = JsonMapper.MAPPER.readValue(instanceFile, CAIRRTemplate.class);
      submissionXml = converter.convertTemplateInstanceToXML(cairrInstance);
    } catch (JsonMappingException e) {
      throw new IOException("The instance uploaded is not compatible with the CAIRR template", e);
    } catch (ParseException e) {
      logger.error(e.getMessage());
    }

    logger.info("XML: " + submissionXml);

    File submissionXmlFile = new File(submissionLocalFolderPath + "/" + NcbiConstants.SUBMISSION_XML_FILE_NAME);
    FileUtils.writeStringToFile(submissionXmlFile, submissionXml);
    return submissionXmlFile;
  }
}
