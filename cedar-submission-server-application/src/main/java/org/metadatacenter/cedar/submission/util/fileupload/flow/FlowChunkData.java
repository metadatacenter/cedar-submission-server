package org.metadatacenter.cedar.submission.util.fileupload.flow;

import java.io.InputStream;

/**
 * Object that represents a chunk of data uploaded to the server using the Flow.js library
 * See: https://github.com/flowjs/flow.js
 */
public class FlowChunkData {

  public long flowChunkNumber;
  public long flowChunkSize;
  public long flowCurrentChunkSize;
  public long flowTotalSize;
  public String flowIdentifier;
  public String flowFilename;
  public String flowRelativePath;
  public long flowTotalChunks;
  public InputStream flowFileInputStream;

  public FlowChunkData(long flowChunkNumber, long flowChunkSize, long flowCurrentChunkSize, long flowTotalSize,
                       String flowIdentifier, String flowFilename, String flowRelativePath, long flowTotalChunks,
                       InputStream flowFileInputStream) {
    this.flowChunkNumber = flowChunkNumber;
    this.flowChunkSize = flowChunkSize;
    this.flowCurrentChunkSize = flowCurrentChunkSize;
    this.flowTotalSize = flowTotalSize;
    this.flowIdentifier = flowIdentifier;
    this.flowFilename = flowFilename;
    this.flowRelativePath = flowRelativePath;
    this.flowTotalChunks = flowTotalChunks;
    this.flowFileInputStream = flowFileInputStream;
  }

  public long getFlowChunkNumber() {
    return flowChunkNumber;
  }

  public void setFlowChunkNumber(long flowChunkNumber) {
    this.flowChunkNumber = flowChunkNumber;
  }

  public long getFlowChunkSize() {
    return flowChunkSize;
  }

  public void setFlowChunkSize(long flowChunkSize) {
    this.flowChunkSize = flowChunkSize;
  }

  public long getFlowCurrentChunkSize() {
    return flowCurrentChunkSize;
  }

  public void setFlowCurrentChunkSize(long flowCurrentChunkSize) {
    this.flowCurrentChunkSize = flowCurrentChunkSize;
  }

  public long getFlowTotalSize() {
    return flowTotalSize;
  }

  public void setFlowTotalSize(long flowTotalSize) {
    this.flowTotalSize = flowTotalSize;
  }

  public String getFlowIdentifier() {
    return flowIdentifier;
  }

  public void setFlowIdentifier(String flowIdentifier) {
    this.flowIdentifier = flowIdentifier;
  }

  public String getFlowFilename() {
    return flowFilename;
  }

  public void setFlowFilename(String flowFilename) {
    this.flowFilename = flowFilename;
  }

  public String getFlowRelativePath() {
    return flowRelativePath;
  }

  public void setFlowRelativePath(String flowRelativePath) {
    this.flowRelativePath = flowRelativePath;
  }

  public long getFlowTotalChunks() {
    return flowTotalChunks;
  }

  public void setFlowTotalChunks(long flowTotalChunks) {
    this.flowTotalChunks = flowTotalChunks;
  }

  public InputStream getFlowFileInputStream() {
    return flowFileInputStream;
  }

  public void setFlowFileInputStream(InputStream flowFileInputStream) {
    this.flowFileInputStream = flowFileInputStream;
  }

  @Override
  public String toString() {
    return "FlowChunkInfo{" +
        "flowChunkNumber=" + flowChunkNumber +
        ", flowChunkSize=" + flowChunkSize +
        ", flowCurrentChunkSize=" + flowCurrentChunkSize +
        ", flowTotalSize=" + flowTotalSize +
        ", flowIdentifier='" + flowIdentifier + '\'' +
        ", flowFilename='" + flowFilename + '\'' +
        ", flowRelativePath='" + flowRelativePath + '\'' +
        ", flowTotalChunks=" + flowTotalChunks +
        ", flowFileInputStream=" + flowFileInputStream +
        '}';
  }
}
