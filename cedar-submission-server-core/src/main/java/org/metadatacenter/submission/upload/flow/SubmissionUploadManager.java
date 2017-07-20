package org.metadatacenter.submission.upload.flow;

import javax.management.InstanceNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  public synchronized void updateStatus(FlowData data, String submissionFolderPath) {

    String submissionId = data.getSubmissionId();
    String fileId = data.getFlowIdentifier();
    long totalFilesCount = data.getTotalFilesCount();
    long fileTotalChunks = data.getFlowTotalChunks();

    // If the submission does not exist in the map, create it
    if (!submissionsUploadStatus.containsKey(submissionId)) {
      Map<String, FileUploadStatus> filesUploadStatus = new HashMap<>();
      SubmissionUploadStatus submissionUploadStatus =
          new SubmissionUploadStatus(totalFilesCount, 0, filesUploadStatus, submissionFolderPath);
      submissionsUploadStatus.put(submissionId, submissionUploadStatus);
    }
    SubmissionUploadStatus submissionUploadStatus = submissionsUploadStatus.get(submissionId);

    // If the file does not exist in the submission, create it
    if (!submissionUploadStatus.getFilesUploadStatus().containsKey(fileId)) {
      String fileLocalPath = FlowUploadUtil.getFileTmpFolderPath(submissionFolderPath, data.flowFilename);
      FileUploadStatus fileUploadStatus = new FileUploadStatus(fileTotalChunks, 0, fileLocalPath);
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
    if (fileUploadStatus.getFileUploadedChunks() == fileUploadStatus.getFileTotalChunks()) {
      return true;
    }
    else if (fileUploadStatus.getFileUploadedChunks() > fileUploadStatus.getFileTotalChunks()) {
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

  // Returns local file paths
  public List<String> getSubmissionFilePaths(String submissionId) throws InstanceNotFoundException {
    List<String> filePaths = new ArrayList<>();
    if (!submissionsUploadStatus.containsKey(submissionId)) {
      throw new InstanceNotFoundException("Submission not found (submissionId = " + submissionId);
    }
    if (!isSubmissionUploadComplete(submissionId)) {
      throw new InternalError("The submission upload is not complete (submissionId = " + submissionId);
    }
    SubmissionUploadStatus submissionUploadStatus = submissionsUploadStatus.get(submissionId);
    for (Map.Entry<String, FileUploadStatus> entry : submissionUploadStatus.getFilesUploadStatus().entrySet()) {
      filePaths.add(entry.getValue().getFileLocalPath());
    }
    return filePaths;
  }
}
