package org.metadatacenter.submission.upload.flow;

public class FlowChunkUploadStatus {

  public long flowTotalChunks;
  public long flowUploadedChunks;

  public FlowChunkUploadStatus(long flowTotalChunks, long flowUploadedChunks) {
    this.flowTotalChunks = flowTotalChunks;
    this.flowUploadedChunks = flowUploadedChunks;
  }

  public boolean isUploadFinished() {
    return (this.flowUploadedChunks == this.flowTotalChunks);
  }

  /* Getters and Setters */

  public long getFlowTotalChunks() {
    return flowTotalChunks;
  }

  public void setFlowTotalChunks(long flowTotalChunks) {
    this.flowTotalChunks = flowTotalChunks;
  }

  public long getFlowUploadedChunks() {
    return flowUploadedChunks;
  }

  public void setFlowUploadedChunks(long flowUploadedChunks) {
    this.flowUploadedChunks = flowUploadedChunks;
  }

  @Override
  public String toString() {
    return "FlowChunkUploadStatus{" +
        "flowTotalChunks=" + flowTotalChunks +
        ", flowUploadedChunks=" + flowUploadedChunks +
        '}';
  }
}
