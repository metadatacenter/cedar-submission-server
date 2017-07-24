package org.metadatacenter.submission.ncbiairr.status;

import org.metadatacenter.config.FTPConfig;
import org.metadatacenter.submission.status.SubmissionState;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusDescriptor;
import org.metadatacenter.submission.status.SubmissionStatusManager;
import org.metadatacenter.submission.status.ftp.FtpStatusChecker;
import org.metadatacenter.submission.upload.ftp.UploaderCreationException;

import java.util.Optional;

public class NcbiAirrSubmissionStatusUtil {

  static public SubmissionStatus getNcbiAirrSubmissionStatus(String submissionID) throws UploaderCreationException {

    SubmissionStatusDescriptor submissionStatusDescriptor =
        SubmissionStatusManager.getInstance().getCurrentSubmissions().get(submissionID);

    NcbiAirrSubmissionStatusTask submissionStatusTask =
        (NcbiAirrSubmissionStatusTask) submissionStatusDescriptor.getSubmissionStatusTask();

    FTPConfig ftpConfig = submissionStatusTask.getFtpConfig();

    FtpStatusChecker ftpStatusChecker = FtpStatusChecker.getStatusChecker(ftpConfig.getHost(), ftpConfig.getUser(),
        ftpConfig.getPassword(), Optional.of(ftpConfig.getSubmissionDirectory()));

    


//    CloseableHttpResponse response = null;
//    CloseableHttpClient client = null;
//
//    Optional<String> token = getImmPortBearerToken();
//    if (!token.isPresent()) {
//      logger.warn("Could not get an ImmPort token");
//      return new SubmissionStatus(submissionID, SubmissionState.ERROR, "Could not get an ImmPort token");
//    }
//
//    try {
//      String immPortStatusURL = IMMPORT_STATUS_URL_BASE + submissionID + "/status";
//      HttpGet get = new HttpGet(immPortStatusURL);
//      get.setHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_BEARER_PREFIX + token.get());
//      get.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
//      client = HttpClientBuilder.create().build();
//      response = client.execute(get);
//
//      if (response.getStatusLine().getStatusCode() == 200) {
//        HttpEntity entity = response.getEntity();
//        return immPortSubmissionResponseBody2SubmissionStatus(submissionID, entity);
//      } else {
//        String errorMessage =
//            "Unexpected status code calling " + immPortStatusURL + "; status=" + response.getStatusLine()
// .getStatusCode();
//        logger.warn(errorMessage);
//        return new SubmissionStatus(submissionID, SubmissionState.ERROR, errorMessage);
//      }
//    } catch (IOException e) {
//      String errorMessage = "IO exception calling import status endpoint: " + e.getMessage();
//      logger.warn(errorMessage);
//      return new SubmissionStatus(submissionID, SubmissionState.ERROR, errorMessage);
//    } finally {
//      HttpClientUtils.closeQuietly(response);
//      HttpClientUtils.closeQuietly(client);
//    }

    return new SubmissionStatus(submissionID, SubmissionState.COMPLETED, "submission completed");
  }


}
