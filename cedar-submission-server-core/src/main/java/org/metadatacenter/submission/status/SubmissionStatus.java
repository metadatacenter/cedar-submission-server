package org.metadatacenter.submission.status;

public record SubmissionStatus(String submissionID, SubmissionState submissionState, String statusMessage) {

  @Override
  public String toString() {
    return "SubmissionStatus{" +
        "submissionID='" + submissionID + '\'' +
        ", submissionState=" + submissionState +
        ", statusMessage='" + statusMessage + '\'' +
        '}';
  }

  public String getSummary() {
    return "SubmissionStatus{submissionID='" + submissionID + " , submissionState=" + submissionState + '}';
  }
}
