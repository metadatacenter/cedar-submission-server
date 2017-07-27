package org.metadatacenter.submission.ncbiairr.status;

import org.metadatacenter.submission.Constants;
import org.metadatacenter.submission.ncbiairr.status.report.NcbiAirrSubmissionState;
import org.metadatacenter.submission.ncbiairr.status.report.NcbiAirrSubmissionStatusReport;
import org.metadatacenter.submission.status.SubmissionState;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

public class NcbiAirrSubmissionStatusUtil {

  public static SubmissionStatus toSubmissionStatus(String submissionId, NcbiAirrSubmissionStatusReport report) {
    SubmissionState submissionState = null;
    if (report.getState().equals(NcbiAirrSubmissionState.SUBMITTED)) {
      submissionState = SubmissionState.STARTED;
    }
    else if (report.getState().equals(NcbiAirrSubmissionState.PROCESSED_ERROR)) {
      submissionState = SubmissionState.ERROR;
    }
    else if (report.getState().equals(NcbiAirrSubmissionState.FAILED)) {
      submissionState = SubmissionState.REJECTED;
    }
    // TODO: complete with all NCBI states
    else {
      submissionState = SubmissionState.COMPLETED;
    }

    String message = report.getTextReport();

    return new SubmissionStatus(submissionId, submissionState, message);
  }

  public static String generatePlainTextReport(Document xmlReport) throws TransformerException {
    TransformerFactory factory = TransformerFactory.newInstance();
    File xsltFilePath =
        new File(NcbiAirrSubmissionStatusUtil.class.getClassLoader().getResource(Constants.NCBI_AIRR_XSLT_PATH).getPath());
    Source xslt = new StreamSource(xsltFilePath);
    Transformer transformer = factory.newTransformer(xslt);
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(xmlReport), new StreamResult(writer));
    String output = writer.getBuffer().toString();
    return output;
  }

}
