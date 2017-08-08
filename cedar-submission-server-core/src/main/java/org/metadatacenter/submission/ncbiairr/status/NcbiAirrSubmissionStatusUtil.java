package org.metadatacenter.submission.ncbiairr.status;

import org.metadatacenter.submission.Constants;
import org.metadatacenter.submission.ncbiairr.status.report.NcbiAirrSubmissionState;
import org.metadatacenter.submission.ncbiairr.status.report.NcbiAirrSubmissionStatusReport;
import org.metadatacenter.submission.status.SubmissionState;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusUtil;
import org.w3c.dom.Document;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringWriter;

public class NcbiAirrSubmissionStatusUtil {

  public static SubmissionStatus toSubmissionStatus(String submissionId, NcbiAirrSubmissionStatusReport report) {
    SubmissionState submissionState = null;
    if (report.getState().equals(NcbiAirrSubmissionState.SUBMITTED)) {
      submissionState = SubmissionState.PROCESSING;
    } else if (report.getState().equals(NcbiAirrSubmissionState.PROCESSING)) {
      submissionState = SubmissionState.PROCESSING;
    } else if (report.getState().equals(NcbiAirrSubmissionState.PROCESSED_ERROR)) {
      submissionState = SubmissionState.ERROR;
    } else if (report.getState().equals(NcbiAirrSubmissionState.FAILED)) {
      submissionState = SubmissionState.REJECTED;
    }
    // TODO: complete with all NCBI states
    else {
      submissionState = SubmissionState.SUCCEEDED;
    }

    String message = SubmissionStatusUtil.getShortStatusMessage(submissionId, submissionState) +
        "\nNCBI STATUS REPORT\n==================" + report.getTextReport();

    return new SubmissionStatus(submissionId, submissionState, message);
  }

  public static String generatePlainTextReport(Document xmlReport) throws TransformerException {
    TransformerFactory factory = TransformerFactory.newInstance();
    InputStream inputStream =
        NcbiAirrSubmissionStatusUtil.class.getClassLoader().getResourceAsStream(Constants.NCBI_AIRR_XSLT_PATH);
    Source xslSource = new StreamSource(inputStream);
    Transformer transformer = factory.newTransformer(xslSource);
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(xmlReport), new StreamResult(writer));
    String output = writer.getBuffer().toString();
    return output;
  }

}
