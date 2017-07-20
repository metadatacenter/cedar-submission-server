package org.metadatacenter.cedar.submission.core;

public enum SubmissionState {
  UNSUBMITTED(0), IN_PROGRESS(1), COMPLETED(2), ERROR(3);

  private final int value;

  SubmissionState(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}