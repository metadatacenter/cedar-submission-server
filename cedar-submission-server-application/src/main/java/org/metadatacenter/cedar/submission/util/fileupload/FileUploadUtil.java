package org.metadatacenter.cedar.submission.util.fileupload;

import org.apache.commons.fileupload.FileItem;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FileUploadUtil {

  public static FlowChunkData getFlowChunkData(List<FileItem> fileItems) {

    long flowChunkNumber = -1;
    long flowChunkSize = -1;
    long flowCurrentChunkSize = -1;
    long flowTotalSize = -1;
    String flowIdentifier = null;
    String flowFilename = null;
    String flowRelativePath = null;
    long flowTotalChunks = -1;
    InputStream flowFileInputStream = null;

    for (FileItem item : fileItems) {
      if (item.isFormField()) {
        if (item.getFieldName().equals("flowChunkNumber")) {
          flowChunkNumber = Long.parseLong(item.getString());
        } else if (item.getFieldName().equals("flowChunkSize")) {
          flowChunkSize = Long.parseLong(item.getString());
        } else if (item.getFieldName().equals("flowCurrentChunkSize")) {
          flowCurrentChunkSize = Long.parseLong(item.getString());
        } else if (item.getFieldName().equals("flowTotalSize")) {
          flowTotalSize = Long.parseLong(item.getString());
        } else if (item.getFieldName().equals("flowIdentifier")) {
          flowIdentifier = item.getString();
        } else if (item.getFieldName().equals("flowFilename")) {
          flowFilename = item.getString();
        } else if (item.getFieldName().equals("flowRelativePath")) {
          flowRelativePath = item.getString();
        } else if (item.getFieldName().equals("flowTotalChunks")) {
          flowTotalChunks = Long.parseLong(item.getString());
        }
      }
      else { // It is a file
        try {
          flowFileInputStream = item.getInputStream();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return new FlowChunkData(flowChunkNumber, flowChunkSize, flowCurrentChunkSize,
        flowTotalSize, flowIdentifier, flowFilename, flowRelativePath, flowTotalChunks, flowFileInputStream);

  }
}
