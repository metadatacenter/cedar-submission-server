package org.metadatacenter.submission.status;

public enum SubmissionState
{
  IN_PROGRESS(0), COMPLETED(1), ERROR(2);

  private final int value;

  SubmissionState(int value)
  {
    this.value = value;
  }

  public int getValue()
  {
    return value;
  }
}