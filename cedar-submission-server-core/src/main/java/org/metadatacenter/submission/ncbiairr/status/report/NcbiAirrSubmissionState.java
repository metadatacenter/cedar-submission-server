package org.metadatacenter.submission.ncbiairr.status.report;

public enum NcbiAirrSubmissionState {
  SUBMITTED("submitted"), PROCESSING("processing"), PROCESSED_ERROR("processed-error"), FAILED("failed");

  private final String value;

  NcbiAirrSubmissionState(String value) {
    this.value = value;
  }

  public static NcbiAirrSubmissionState fromString(String state) {
    for (NcbiAirrSubmissionState s : NcbiAirrSubmissionState.values()) {
      if (s.getValue().equals(state)) {
        return s;
      }
    }
    return null;
  }

  public String getValue() {
    return value;
  }

}