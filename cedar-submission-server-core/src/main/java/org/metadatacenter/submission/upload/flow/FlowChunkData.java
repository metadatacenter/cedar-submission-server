package org.metadatacenter.submission.upload.flow;

import java.io.InputStream;

/**
 * Object that represents a chunk of data uploaded to the server using the Flow.js library
 * See: https://github.com/flowjs/flow.js
 */
public class FlowChunkData {

  public String submissionId;
  public int numberOfFiles;
  public long flowChunkNumber;
  public long flowChunkSize;
  public long flowCurrentChunkSize;
  public long flowTotalSize;
  public String flowIdentifier;
  public String flowFilename;
  public String flowRelativePath;
  public long flowTotalChunks;
  public InputStream flowFileInputStream;

  public FlowChunkData(String submissionId, int numberOfFiles, long flowChunkNumber, long flowChunkSize, long
      flowCurrentChunkSize, long flowTotalSize, String flowIdentifier, String flowFilename, String flowRelativePath,
                       long flowTotalChunks, InputStream flowFileInputStream) {
    this.submissionId = submissionId;
    this.numberOfFiles = numberOfFiles;
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

  public String getSubmissionId() {
    return submissionId;
  }

  public int getNumberOfFiles() {
    return numberOfFiles;
  }

  public long getFlowChunkNumber() {
    return flowChunkNumber;
  }

  public long getFlowChunkSize() {
    return flowChunkSize;
  }

  public long getFlowCurrentChunkSize() {
    return flowCurrentChunkSize;
  }

  public long getFlowTotalSize() {
    return flowTotalSize;
  }

  public String getFlowIdentifier() {
    return flowIdentifier;
  }

  public String getFlowFilename() {
    return flowFilename;
  }

  public String getFlowRelativePath() {
    return flowRelativePath;
  }

  public long getFlowTotalChunks() {
    return flowTotalChunks;
  }

  public InputStream getFlowFileInputStream() {
    return flowFileInputStream;
  }
}
