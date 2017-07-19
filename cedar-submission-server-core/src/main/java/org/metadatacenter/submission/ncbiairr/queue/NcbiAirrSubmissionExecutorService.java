package org.metadatacenter.submission.ncbiairr.queue;

import org.metadatacenter.submission.ncbiairr.NcbiAirrSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NcbiAirrSubmissionExecutorService {

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
      Thread.sleep(60000);

      // TODO: check response and send notification to the user
//      UploadService.uploadToNcbi(submission.getSubmissionFolder(),
//          filesToSubmit, cedarConfig.getSubmissionConfig().getNcbi().getSra().getFtp());

      logger.info("Submission successful!!!!. Submission id: " + submission.getId() + "; No. files: " +
          submission.getLocalFilePaths().size());

//    } catch (IOException e) {
//      e.printStackTrace();
//    } catch (UploaderCreationException e) {
//      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }
}
