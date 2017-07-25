package org.metadatacenter.submission.ncbiairr.queue;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.submission.Constants;
import org.metadatacenter.submission.ncbiairr.NcbiAirrSubmission;
import org.metadatacenter.submission.ncbiairr.status.NcbiAirrSubmissionStatusTask;
import org.metadatacenter.submission.ncbiairr.upload.NcbiAirrFtpUploadService;
import org.metadatacenter.submission.status.SubmissionStatusManager;
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

      // Track the submission status
      NcbiAirrSubmissionStatusTask submissionStatusTask = new NcbiAirrSubmissionStatusTask(submission.getId(),
          submission.getCedarUserId(), null, cedarConfig.getSubmissionConfig().getNcbi().getSra().getFtp(),
          submission.getSubmissionFolder());
      SubmissionStatusManager.getInstance().addSubmission(submissionStatusTask);

      if (Constants.NCBI_AIRR_SUBMIT) { // real submission
        NcbiAirrFtpUploadService.uploadToNcbi(submission.getSubmissionFolder(),
            filesToSubmit, cedarConfig.getSubmissionConfig().getNcbi().getSra().getFtp(), submission.getUploadSubmitReadyFile());
      }
      else { // simulated submission
        Thread.sleep(Constants.NCBI_AIRR_SIMULATION_MODE_TIMEOUT);
      }

      logger.info("Submission to the NCBI completed! Submission id: " + submission.getId() + "; No. files: " +
          submission.getLocalFilePaths().size());
      logger.info("Deleting the submission local folder: " + submission.getSubmissionFolder());
      // Delete the submission local folder
      //FileUtils.deleteDirectory(new File(submission.getSubmissionFolder()));

    } catch (IOException e) {
      logger.error("Error submitting the data to the NCBI.");
      logger.error(e.getMessage());
    } catch (UploaderCreationException e) {
      logger.error("Error submitting the data to the NCBI.");
      logger.error(e.getMessage());
    } catch (InterruptedException e) {
      logger.error("Error submitting the data to the NCBI.");
      logger.error(e.getMessage());
    }
  }
}
