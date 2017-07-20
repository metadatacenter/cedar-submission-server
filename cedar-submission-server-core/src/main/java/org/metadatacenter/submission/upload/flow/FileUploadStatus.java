package org.metadatacenter.submission.upload.flow;

public class FileUploadStatus {

  public long fileTotalChunks;
  public long fileUploadedChunks;

  public FileUploadStatus(long flowTotalChunks, long flowUploadedChunks) {
    this.fileTotalChunks = flowTotalChunks;
    this.fileUploadedChunks = flowUploadedChunks;
  }

  public long getFileTotalChunks() {
    return fileTotalChunks;
  }

  public void setFileTotalChunks(long fileTotalChunks) {
    this.fileTotalChunks = fileTotalChunks;
  }

  public long getFileUploadedChunks() {
    return fileUploadedChunks;
  }

  public void setFileUploadedChunks(long fileUploadedChunks) {
    this.fileUploadedChunks = fileUploadedChunks;
  }
}
