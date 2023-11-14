package org.metadatacenter.submission.status;

public record SubmissionStatusDescriptor(String submissionID, String userID, String statusURL, SubmissionStatus submissionStatus, SubmissionStatusTask submissionStatusTask) {
}
