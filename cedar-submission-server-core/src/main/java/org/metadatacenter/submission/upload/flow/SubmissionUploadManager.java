package org.metadatacenter.submission.upload.flow;

import jakarta.ws.rs.BadRequestException;
import org.metadatacenter.submission.exception.SubmissionInstanceNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmissionUploadManager {

  private static SubmissionUploadManager singleInstance;
  // Keyed by (ownerUserId, submissionId), not by the client-supplied submissionId alone. Otherwise two
  // users that pick the same submissionId would share one entry: their chunk counts would merge and the
  // file-path lookups would return one user's files to the other, who would then submit them under their
  // own credentials. Composing the owner into the key makes another user's entry unaddressable.
  private Map<String, SubmissionUploadStatus> submissionsUploadStatus = new HashMap<>(); // key(ownerUserId, submissionId) -> status

  // Single instance
  private SubmissionUploadManager() {
  }

  public static synchronized SubmissionUploadManager getInstance() {
    if (singleInstance == null) {
      singleInstance = new SubmissionUploadManager();
    }
    return singleInstance;
  }

  // A NUL byte cannot appear in a CEDAR user id or in a flow.js submissionId, so it is an unambiguous
  // separator: no (ownerUserId, submissionId) pair can collide with a different pair.
  private static String key(String ownerUserId, String submissionId) {
    return ownerUserId + "\u0000" + submissionId;
  }

  // Updates the upload status with the latest file chunk that has been uploaded
  public synchronized void updateStatus(FlowData data, String ownerUserId, String submissionFolderPath) {

    String key = key(ownerUserId, data.getSubmissionId());
    String fileId = data.getFlowIdentifier();
    long totalFilesCount = data.getTotalFilesCount();
    long fileTotalChunks = data.getFlowTotalChunks();

    // If the submission does not exist in the map, create it
    if (!submissionsUploadStatus.containsKey(key)) {
      Map<String, FileUploadStatus> filesUploadStatus = new HashMap<>();
      SubmissionUploadStatus submissionUploadStatus =
          new SubmissionUploadStatus(totalFilesCount, 0, filesUploadStatus, submissionFolderPath);
      submissionsUploadStatus.put(key, submissionUploadStatus);
    }
    SubmissionUploadStatus submissionUploadStatus = submissionsUploadStatus.get(key);

    // If the file does not exist in the submission, create it
    if (!submissionUploadStatus.getFilesUploadStatus().containsKey(fileId)) {
      String fileLocalPath = FlowUploadUtil.getFileLocalFolderPath(submissionFolderPath, data.flowFilename);
      // Check if the file is a metadata file
      boolean isMetadataFile = FlowUploadUtil.isMetadataFile(data);
      FileUploadStatus fileUploadStatus =
          new FileUploadStatus(fileTotalChunks, 0, fileLocalPath, isMetadataFile);
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

  private boolean isFileUploadComplete(FileUploadStatus fileUploadStatus) {
    if (fileUploadStatus.getFileUploadedChunks() == fileUploadStatus.getFileTotalChunks()) {
      return true;
    } else if (fileUploadStatus.getFileUploadedChunks() > fileUploadStatus.getFileTotalChunks()) {
      throw new BadRequestException("Uploaded file chunks is higher than total file chunks");
    } else {
      return false;
    }
  }

  public boolean isSubmissionUploadComplete(String ownerUserId, String submissionId) throws SubmissionInstanceNotFoundException {
    String key = key(ownerUserId, submissionId);
    if (!submissionsUploadStatus.containsKey(key)) {
      throw new SubmissionInstanceNotFoundException("Submission not found (submissionId = " + submissionId);
    }
    SubmissionUploadStatus submissionUploadStatus = submissionsUploadStatus.get(key);

    if (submissionUploadStatus.getUploadedFilesCount() == submissionUploadStatus.getTotalFilesCount()) {
      return true;
    } else if (submissionUploadStatus.getUploadedFilesCount() > submissionUploadStatus.getTotalFilesCount()) {
      throw new BadRequestException("Number of uploaded files is higher than the total number of files (submissionId = " +
          submissionId);
    } else {
      return false;
    }
  }

  public void removeSubmissionStatus(String ownerUserId, String submissionId) {
    submissionsUploadStatus.remove(key(ownerUserId, submissionId));
  }

  // Returns local file paths
  public List<String> getSubmissionFilePaths(String ownerUserId, String submissionId) throws SubmissionInstanceNotFoundException {
    String key = key(ownerUserId, submissionId);
    List<String> filePaths = new ArrayList<>();
    if (!submissionsUploadStatus.containsKey(key)) {
      throw new SubmissionInstanceNotFoundException("Submission not found (submissionId = " + submissionId);
    }
    if (!isSubmissionUploadComplete(ownerUserId, submissionId)) {
      throw new BadRequestException("The submission upload is not complete (submissionId = " + submissionId);
    }
    SubmissionUploadStatus submissionUploadStatus = submissionsUploadStatus.get(key);
    for (Map.Entry<String, FileUploadStatus> entry : submissionUploadStatus.getFilesUploadStatus().entrySet()) {
      filePaths.add(entry.getValue().getFileLocalPath());
    }
    return filePaths;
  }

  public SubmissionUploadStatus getSubmissionsUploadStatus(String ownerUserId, String submissionId) {
    return submissionsUploadStatus.get(key(ownerUserId, submissionId));
  }
}
