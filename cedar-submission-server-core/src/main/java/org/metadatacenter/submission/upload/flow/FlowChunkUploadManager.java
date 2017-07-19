package org.metadatacenter.submission.upload.flow;

import javax.management.InstanceNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Map submissionIdentifier -> Map of flowIdentifier -> FlowChunkUploadStatus
 */
public class FlowChunkUploadManager {


  private HashMap<String, HashMap<String, FlowChunkUploadStatus>> submissionMap = new HashMap<>();
  // Map flowIdentifier -> FlowChunkUploadStatus
  private HashMap<String, FlowChunkUploadStatus> statusMap = new HashMap<>();

  // Single instance
  private FlowChunkUploadManager() {
  }

  private static FlowChunkUploadManager singleInstance;

  public static synchronized FlowChunkUploadManager getInstance() {
    if (singleInstance == null) {
      singleInstance = new FlowChunkUploadManager();
    }
    return singleInstance;
  }

  private synchronized void addFlowStatus(String flowIdentifier, long totalChunks) {
    FlowChunkUploadStatus status = new FlowChunkUploadStatus(totalChunks, 1);
    statusMap.put(flowIdentifier, status);
  }

  public synchronized void removeFlowStatus(String flowIdentifier) {
    statusMap.remove(flowIdentifier);
  }

  public synchronized void increaseUploadedChunksCount(String flowIdentifier, long totalChunks) throws InstanceNotFoundException {
    if (statusMap.containsKey(flowIdentifier)) {
      FlowChunkUploadStatus status = statusMap.get(flowIdentifier);
      statusMap.replace(flowIdentifier, status,
          new FlowChunkUploadStatus(status.getFlowTotalChunks(), status.getFlowUploadedChunks() + 1));
    }
    else {
      addFlowStatus(flowIdentifier, totalChunks);
    }
  }

  public boolean isUploadFinished(String flowIdentifier) {
    if (statusMap.containsKey(flowIdentifier)) {
      FlowChunkUploadStatus status = statusMap.get(flowIdentifier);
      return status.isUploadFinished();
    }
    return false;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    Iterator<Map.Entry<String, FlowChunkUploadStatus>> iter = statusMap.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, FlowChunkUploadStatus> entry = iter.next();
      sb.append(entry.getKey());
      sb.append('=').append('"');
      sb.append(entry.getValue());
      sb.append('"');
      if (iter.hasNext()) {
        sb.append(',').append(' ');
      }
    }
    return sb.toString();
  }

}
