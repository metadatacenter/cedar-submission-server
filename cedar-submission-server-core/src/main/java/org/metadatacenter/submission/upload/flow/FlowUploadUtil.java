package org.metadatacenter.submission.upload.flow;

import org.apache.commons.fileupload.FileItem;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;

public class FlowUploadUtil {

  public static FlowChunkData getFlowChunkData(List<FileItem> fileItems) {

    String submissionId = null;
    int numberOfFiles = -1;
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
        if (item.getFieldName().equals("submissionId")) {
          submissionId = item.getString();
        } else if (item.getFieldName().equals("numberOfFiles")) {
          numberOfFiles = Integer.parseInt(item.getString());
        } else if (item.getFieldName().equals("flowChunkNumber")) {
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
      } else { // It is a file
        try {
          flowFileInputStream = item.getInputStream();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return new FlowChunkData(submissionId, numberOfFiles, flowChunkNumber, flowChunkSize, flowCurrentChunkSize,
        flowTotalSize, flowIdentifier, flowFilename, flowRelativePath, flowTotalChunks, flowFileInputStream);

  }

//  // returns the file name (without the extension)
//  public static String getFileNamePrefix(String fileName) {
//    if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
//      return fileName.substring(0, fileName.lastIndexOf("."));
//    else return fileName;
//  }
//
//  // returns the file extension
//  public static String getFileNameSuffix(String fileName) {
//    if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
//      return fileName.substring(fileName.lastIndexOf(".") + 1);
//    else return "";
//  }

  public static void writeToRandomAccessFile(RandomAccessFile raf, FlowChunkData data, long contentLength) throws IOException {
    // Seek to position
    raf.seek((data.flowChunkNumber - 1) * data.flowChunkSize);
    // Save to file
    InputStream is = data.getFlowFileInputStream();
    long read = 0;
    byte[] bytes = new byte[1024 * 100];
    while (read < contentLength) {
      int r = is.read(bytes);
      if (r < 0) {
        break;
      }
      raf.write(bytes, 0, r);
      read += r;
    }
    raf.close();
  }

  public static String getTempFolderName(String uploadType, String userId, String uploadIdentifier) {
    return System.getProperty("java.io.tmpdir") + uploadType + "/" + userId + "/" + uploadIdentifier;
  }

  public static String getDateBasedFolderName(DateTimeZone dateTimeZone) {
    return DateTime.now(dateTimeZone).toString().replace(":", "-");
  }

  public static String getLastFragmentOfUrl(String url) {
    return url.substring(url.lastIndexOf("/") + 1, url.length());
  }

}
