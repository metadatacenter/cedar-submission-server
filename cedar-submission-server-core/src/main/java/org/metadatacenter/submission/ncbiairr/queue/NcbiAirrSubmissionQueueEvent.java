package org.metadatacenter.submission.ncbiairr.queue;

import org.metadatacenter.constant.CedarConstants;
import org.metadatacenter.submission.ncbiairr.NcbiAirrSubmission;

import java.time.Instant;

public class NcbiAirrSubmissionQueueEvent {

  private String createdAt;
  private long createdAtTS;
  private NcbiAirrSubmission submission;

  public NcbiAirrSubmissionQueueEvent() {}

  public NcbiAirrSubmissionQueueEvent(NcbiAirrSubmission submission) {
    Instant now = Instant.now();
    this.createdAt = CedarConstants.xsdDateTimeFormatter.format(now);
    this.createdAtTS = now.getEpochSecond();
    this.submission = submission;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public long getCreatedAtTS() {
    return createdAtTS;
  }

  public NcbiAirrSubmission getSubmission() {
    return submission;
  }

}
