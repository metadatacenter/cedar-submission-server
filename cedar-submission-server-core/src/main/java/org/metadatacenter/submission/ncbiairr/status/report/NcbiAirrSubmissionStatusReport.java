package org.metadatacenter.submission.ncbiairr.status.report;

/**
 * This class represents the submission status extracted from a report.[number].xml file
 */
public class NcbiAirrSubmissionStatusReport {

  private final NcbiAirrSubmissionState state;
  private final String xmlReport;
  private final String textReport; // generated from the xml report

  public NcbiAirrSubmissionStatusReport(NcbiAirrSubmissionState state, String xmlReport, String textReport) {
    this.state = state;
    this.xmlReport = xmlReport;
    this.textReport = textReport;
  }

  public NcbiAirrSubmissionState getState() {
    return state;
  }

  public String getXmlReport() {
    return xmlReport;
  }

  public String getTextReport() {
    return textReport;
  }
}
