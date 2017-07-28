package org.metadatacenter.submission.status;

public enum SubmissionType
{
  IMMPORT("Immport"), NCBI_AIRR("NCBI-AIRR");

  private final String value;

  SubmissionType(String value)
  {
    this.value = value;
  }

  public String getValue()
  {
    return value;
  }
}