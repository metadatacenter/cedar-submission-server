package org.metadatacenter.submission.ncbi.status.report;

/**
 * This class represents the submission status extracted from a report.[number].xml file
 *
 * @param textReport generated from the xml report
 */
public record NcbiSubmissionStatusReport(NcbiSubmissionState state, String xmlReport, String textReport) {

}
