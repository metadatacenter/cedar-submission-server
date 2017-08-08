package org.metadatacenter.submission.ncbiairr;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.FileUtils;
import org.metadatacenter.submission.AIRRTemplate;
import org.metadatacenter.submission.AIRRTemplate2SRAConverter;
import org.metadatacenter.submission.Constants;
import org.metadatacenter.submission.upload.flow.FileUploadStatus;
import org.metadatacenter.submission.upload.flow.FlowUploadUtil;
import org.metadatacenter.submission.upload.flow.SubmissionUploadManager;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NcbiAirrSubmissionUtil {

  final static Logger logger = LoggerFactory.getLogger(NcbiAirrSubmissionUtil.class);

  public static NcbiAirrSubmission generateSubmission(String submissionId, String userId, String ncbiFolderName)
      throws IOException, JAXBException, DatatypeConfigurationException {

    List<String> submissionFilePaths = new ArrayList<>();
    Map<String, FileUploadStatus> filesUploadStatus = SubmissionUploadManager.getInstance()
        .getSubmissionsUploadStatus(submissionId).getFilesUploadStatus();

    // Iterate over the submission files. Use the AIRR instance to generate the submission.xml file, keep the xml file
    for (Map.Entry<String, FileUploadStatus> entry : filesUploadStatus.entrySet()) {
      FileUploadStatus fileUploadStatus = entry.getValue();
      // If it is a data file, we keep the same path
      if (!fileUploadStatus.isMetadataFile()) {
        submissionFilePaths.add(fileUploadStatus.getFileLocalPath());
      }
      // If it is a metadata file (instance), generate the submission.xml file and save its path instead
      else {
        // XML generation
        File airrInstance = new File(fileUploadStatus.getFileLocalPath());
        String submissionLocalFolderPath =
            FlowUploadUtil.getSubmissionLocalFolderPath(Constants.NCBI_AIRR_LOCAL_FOLDER_NAME, userId, submissionId);
        File submissionXmlFile = generateSubmissionXmlFile(airrInstance, submissionLocalFolderPath);
        submissionFilePaths.add(submissionXmlFile.getAbsolutePath());
      }
    }

    // Generate the submission object
    NcbiAirrSubmission submission =
        new NcbiAirrSubmission(submissionId, userId, submissionFilePaths,
            ncbiFolderName, Constants.NCBI_AIRR_UPLOAD_SUBMIT_READY_FILE);

    return submission;
  }

  /**
   * Generates a submission.xml file from an AIRR instance (metadata) file
   */
  private static File generateSubmissionXmlFile(File airrInstanceFile, String submissionLocalFolderPath) throws
      IOException, JAXBException, DatatypeConfigurationException {
    AIRRTemplate2SRAConverter converter = new AIRRTemplate2SRAConverter();

    AIRRTemplate airrInstance = null;
    try {
      airrInstance = JsonMapper.MAPPER.readValue(airrInstanceFile, AIRRTemplate.class);
    } catch (JsonMappingException e) {
      throw new IOException("The instance uploaded is not compatible with the AIRR template", e);
    }
    String submissionXml = converter.generateSRASubmissionXMLFromAIRRTemplateInstance(airrInstance);
    File submissionXmlFile = new File(submissionLocalFolderPath + "/" + Constants.SUBMISSION_XML_FILE_NAME);
    FileUtils.writeStringToFile(submissionXmlFile, submissionXml);
    return submissionXmlFile;
  }

}
