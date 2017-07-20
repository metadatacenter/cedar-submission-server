package org.metadatacenter.submission.upload.flow;

import java.util.HashMap;
import java.util.Map;

public class SubmissionUploadStatus {

  private long totalFilesCount;
  private long uploadedFilesCount;
  // Map flowIdentifier -> FileUploadStatus
  private Map<String, FileUploadStatus> filesUploadStatus;

  public SubmissionUploadStatus(long totalFilesCount, long uploadedFilesCount, Map<String, FileUploadStatus> filesUploadStatus) {
    this.totalFilesCount = totalFilesCount;
    this.uploadedFilesCount = uploadedFilesCount;
    this.filesUploadStatus = filesUploadStatus;
  }

  public long getTotalFilesCount() {
    return totalFilesCount;
  }

  public long getUploadedFilesCount() {
    return uploadedFilesCount;
  }

  public Map<String, FileUploadStatus> getFilesUploadStatus() {
    return filesUploadStatus;
  }

  public void setTotalFilesCount(long totalFilesCount) {
    this.totalFilesCount = totalFilesCount;
  }

  public void setUploadedFilesCount(long uploadedFilesCount) {
    this.uploadedFilesCount = uploadedFilesCount;
  }

  public void setFilesUploadStatus(Map<String, FileUploadStatus> filesUploadStatus) {
    this.filesUploadStatus = filesUploadStatus;
  }
}
