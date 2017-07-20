package org.metadatacenter.submission.upload.flow;

import sun.jvm.hotspot.oops.Instance;

import javax.management.InstanceNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * Map submissionIdentifier -> Map of flowIdentifier -> FlowUploadStatus
 */
public class SubmissionUploadManager {


  private Map<String, SubmissionUploadStatus> submissionsUploadStatus = new HashMap<>();

  // Single instance
  private SubmissionUploadManager() {
  }

  private static SubmissionUploadManager singleInstance;

  public static synchronized SubmissionUploadManager getInstance() {
    if (singleInstance == null) {
      singleInstance = new SubmissionUploadManager();
    }
    return singleInstance;
  }

  // Updates the upload status with the latest file chunk that has been uploaded
  public synchronized void updateStatus(FlowData data) {

    String submissionId = data.getSubmissionId();
    String fileId = data.getFlowIdentifier();
    long totalFilesCount = data.getTotalFilesCount();
    long fileTotalChunks = data.getFlowTotalChunks();

    // If the submission does not exist in the map, create it
    if (!submissionsUploadStatus.containsKey(submissionId)) {
      Map<String, FileUploadStatus> filesUploadStatus = new HashMap<>();
      SubmissionUploadStatus submissionUploadStatus = new SubmissionUploadStatus(totalFilesCount, 0, filesUploadStatus);
      submissionsUploadStatus.put(submissionId, submissionUploadStatus);
    }
    SubmissionUploadStatus submissionUploadStatus = submissionsUploadStatus.get(submissionId);

    // If the file does not exist in the submission, create it
    if (!submissionUploadStatus.getFilesUploadStatus().containsKey(fileId)) {
      FileUploadStatus fileUploadStatus = new FileUploadStatus(fileTotalChunks, 0);
      submissionUploadStatus.getFilesUploadStatus().put(fileId, fileUploadStatus);
    }

    FileUploadStatus fileUploadStatus = submissionUploadStatus.getFilesUploadStatus().get(fileId);

    // Increase the number of file chunks uploaded
    long uploadedChunks = fileUploadStatus.getFileUploadedChunks();
    fileUploadStatus.setFileUploadedChunks(uploadedChunks + 1);

    // Increase the number of files uploaded, if the chunk was the last one for a file
    if (isFileUploadComplete(fileUploadStatus)) {
      long uploadedFiles = submissionUploadStatus.getUploadedFilesCount();
      submissionUploadStatus.setUploadedFilesCount(uploadedFiles + 1);
    }
  }

  private boolean isFileUploadComplete(FileUploadStatus fileUploadStatus)  {
    if (fileUploadStatus.fileUploadedChunks == fileUploadStatus.fileTotalChunks) {
      return true;
    }
    else if (fileUploadStatus.fileUploadedChunks > fileUploadStatus.fileTotalChunks) {
      throw new InternalError("Uploaded file chunks is higher than total file chunks");
    }
    else {
      return false;
    }
  }

  public boolean isSubmissionUploadComplete(String submissionId) throws InstanceNotFoundException {
    if (!submissionsUploadStatus.containsKey(submissionId)) {
      throw new InstanceNotFoundException("Submission not found (submissionId = " + submissionId);
    }
    SubmissionUploadStatus submissionUploadStatus = submissionsUploadStatus.get(submissionId);

    if (submissionUploadStatus.getUploadedFilesCount() == submissionUploadStatus.getTotalFilesCount()) {
      return true;
    }
    else if (submissionUploadStatus.getUploadedFilesCount() > submissionUploadStatus.getTotalFilesCount()) {
      throw new InternalError("Number of uploaded files is higher than the total number of files (submissionId = " + submissionId);
    }
    else {
      return false;
    }

  }

  public void removeSubmissionStatus(String submissionId) {
    submissionsUploadStatus.remove(submissionId);
  }

}
