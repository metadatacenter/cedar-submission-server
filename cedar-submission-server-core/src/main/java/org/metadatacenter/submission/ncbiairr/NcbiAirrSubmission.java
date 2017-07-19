package org.metadatacenter.submission.ncbiairr;

import java.util.Collection;

public class NcbiAirrSubmission {

  private String id;
  private String cedarUserId;
  private Collection<String> localFilePaths;
  private String submissionFolder;

  public NcbiAirrSubmission() {};

  public NcbiAirrSubmission(String id, String cedarUserId, Collection<String> localFilePaths, String submissionFolder) {
    this.id = id;
    this.cedarUserId = cedarUserId;
    this.localFilePaths = localFilePaths;
    this.submissionFolder = submissionFolder;
  }

  public String getId() {
    return id;
  }

  public String getCedarUserId() {
    return cedarUserId;
  }

  public Collection<String> getLocalFilePaths() {
    return localFilePaths;
  }

  public String getSubmissionFolder() {
    return submissionFolder;
  }

  @Override
  public String toString() {
    return "NcbiAirrSubmission{" +
        "id='" + id + '\'' +
        ", cedarUserId='" + cedarUserId + '\'' +
        ", localFilePaths=" + localFilePaths +
        ", submissionFolder='" + submissionFolder + '\'' +
        '}';
  }
}
