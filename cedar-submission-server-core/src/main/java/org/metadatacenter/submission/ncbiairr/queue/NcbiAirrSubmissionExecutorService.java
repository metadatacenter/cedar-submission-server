package org.metadatacenter.submission.ncbiairr.queue;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.submission.Constants;
import org.metadatacenter.submission.ncbiairr.NcbiAirrSubmission;
import org.metadatacenter.submission.ncbiairr.upload.NcbiAirrFtpUploadService;
import org.metadatacenter.submission.upload.ftp.UploaderCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NcbiAirrSubmissionExecutorService {

  private CedarConfig cedarConfig;

  public NcbiAirrSubmissionExecutorService(CedarConfig cedarConfig) {
    this.cedarConfig = cedarConfig;
  }

  private static final Logger logger = LoggerFactory.getLogger(NcbiAirrSubmissionExecutorService.class);

  // Main entry point
  public void handleEvent(NcbiAirrSubmissionQueueEvent event) {
    submit(event.getSubmission());
  }

  private void submit(NcbiAirrSubmission submission) {
    try {
      // Read files
      List<File> filesToSubmit = new ArrayList<>();
      for (String filePath : submission.getLocalFilePaths()) {
        filesToSubmit.add(new File(filePath));
      }

      logger.info("Uploading to NCBI...");
      if (!Constants.NCBI_AIRR_SIMULATION_MODE) { // real submission
        // TODO: check response and send notification to the user
        NcbiAirrFtpUploadService.uploadToNcbi(submission.getSubmissionFolder(),
            filesToSubmit, cedarConfig.getSubmissionConfig().getNcbi().getSra().getFtp(), submission.getUploadSubmitReadyFile());
      }
      else { // simulation mode
        Thread.sleep(Constants.NCBI_AIRR_SIMULATION_MODE_TIMEOUT);
      }
      logger.info("Submission successful!!!!. Submission id: " + submission.getId() + "; No. files: " +
          submission.getLocalFilePaths().size());

    } catch (IOException e) {
      e.printStackTrace();
    } catch (UploaderCreationException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }
}
