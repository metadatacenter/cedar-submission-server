package org.metadatacenter.submission.immport;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.metadatacenter.submission.biosample.ImmPortGetTokenResponse;
import org.metadatacenter.submission.status.SubmissionState;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.metadatacenter.constant.HttpConstants.CONTENT_TYPE_APPLICATION_JSON;
import static org.metadatacenter.constant.HttpConstants.HTTP_AUTH_HEADER_BEARER_PREFIX;
import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_ACCEPT;
import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_AUTHORIZATION;
import static org.metadatacenter.util.json.JsonMapper.MAPPER;

public class ImmPortUtil
{
  final static Logger logger = LoggerFactory.getLogger(ImmPortUtil.class);

  // TODO Get from environment variables
  public static final String IMMPORT_CEDAR_USER_NAME = "cedaruser";
  public static final String IMMPORT_CEDAR_USER_PASSWORD = "GoCedar2017#";

  public static final String IMMPORT_TOKEN_URL = "https://auth.dev.immport.org/auth/token";
  public static final String IMMPORT_STATUS_URL_BASE = "https://api.dev.immport.org/data/upload/registration/";
  public static final String IMMPORT_WORKSPACES_URL_BASE = "https://api.dev.immport.org/users/";
  public static final String IMMPORT_WORKSPACES_URL =
    IMMPORT_WORKSPACES_URL_BASE + IMMPORT_CEDAR_USER_NAME + "/workspaces";
  public static final String IMMPORT_SUBMISSION_URL = "https://api.dev.immport.org/data/upload";

  public static String IMMPORT_LOCAL_FOLDER_NAME = "immport-upload";

  static public SubmissionStatus getImmPortSubmissionStatus(String submissionID)
  {
    CloseableHttpResponse response = null;
    CloseableHttpClient client = null;

    Optional<String> token = getImmPortBearerToken();
    if (!token.isPresent()) {
      logger.warn("Could not get an ImmPort token");
      return new SubmissionStatus(submissionID, SubmissionState.ERROR, "Could not get an ImmPort token");
    }

    try {
      HttpGet get = new HttpGet(IMMPORT_STATUS_URL_BASE + submissionID + "/status");
      get.setHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_BEARER_PREFIX + token.get());
      get.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
      client = HttpClientBuilder.create().build();
      response = client.execute(get);

      if (response.getStatusLine().getStatusCode() == 200) {
        HttpEntity entity = response.getEntity();
        return immPortSubmissionResponseBody2SubmissionStatus(submissionID, entity);
      } else {
        String errorMessage =
          "Unexpected status code calling " + IMMPORT_WORKSPACES_URL + "; status=" + response.getStatusLine()
            .getStatusCode();
        logger.warn(errorMessage);
        return new SubmissionStatus(submissionID, SubmissionState.ERROR, errorMessage);
      }
    } catch (IOException e) {
      String errorMessage = "IO exception connecting to host " + IMMPORT_WORKSPACES_URL + ": " + e.getMessage();
      logger.warn(errorMessage);
      return new SubmissionStatus(submissionID, SubmissionState.ERROR, errorMessage);
    } finally {
      HttpClientUtils.closeQuietly(response);
      HttpClientUtils.closeQuietly(client);
    }
  }

  static public Optional<String> getImmPortBearerToken()
  {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpPost post = new HttpPost(IMMPORT_TOKEN_URL);
    CloseableHttpResponse response = null;

    try {
      List<NameValuePair> parameters = new ArrayList<>(2);
      parameters.add(new BasicNameValuePair("username", IMMPORT_CEDAR_USER_NAME));
      parameters.add(new BasicNameValuePair("password", IMMPORT_CEDAR_USER_PASSWORD));
      post.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
      post.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);

      response = client.execute(post);

      if (response.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode()) {
        HttpEntity entity = response.getEntity();
        // Get ImmPortGetTokenResponse from stream
        ImmPortGetTokenResponse immportGetTokenResponse = MAPPER
          .readValue(entity.getContent(), ImmPortGetTokenResponse.class);

        if (immportGetTokenResponse.getStatus().intValue() == 200)
          return Optional.of(immportGetTokenResponse.getToken());
        else {
          logger.warn("Failed to get token from host " + IMMPORT_SUBMISSION_URL + "; ImmPort status code="
            + immportGetTokenResponse.getStatus().intValue() + ", error=" + immportGetTokenResponse.getError());
          return Optional.empty();
        }
      } else {
        logger.warn(
          "Failed to get token from host " + IMMPORT_SUBMISSION_URL + "; status code=" + response.getStatusLine()
            .getStatusCode());
        return Optional.empty();
      }
    } catch (IOException e) {
      logger.warn("IO error getting token from host " + IMMPORT_SUBMISSION_URL + "; error=" + e.getMessage());
      return Optional.empty();
    } finally {
      HttpClientUtils.closeQuietly(response);
      HttpClientUtils.closeQuietly(client);
    }
  }

  static private SubmissionStatus immPortSubmissionResponseBody2SubmissionStatus(String submissionID,
    HttpEntity responseEntity) throws IOException
  {
    if (responseEntity != null) {
      String responseBody = EntityUtils.toString(responseEntity);
      JsonNode immPortSubmissionResponseBody = MAPPER.readTree(responseBody);

      System.err.println("ImmPort response JSON " + immPortSubmissionResponseBody);

      if (immPortSubmissionResponseBody.has("error"))
        return new SubmissionStatus(submissionID, SubmissionState.ERROR,
          immPortSubmissionResponseBody.get("error").textValue());
      else {
        if (!immPortSubmissionResponseBody.has("status"))
          return new SubmissionStatus(submissionID, SubmissionState.ERROR,
            "No status field in ImmPort submit status response");

        String immPortSubmissionStatus = immPortSubmissionResponseBody.get("status").asText();

        if ("Completed".equals(immPortSubmissionStatus)) {
          if (!immPortSubmissionResponseBody.has("uploadTicketStatusUiUrl"))
            return new SubmissionStatus(submissionID, SubmissionState.COMPLETED,
              "Submission " + submissionID + " completed; no status URL found");
          else {
            String immPortStatusURL = immPortSubmissionResponseBody.get("uploadTicketStatusUiUrl").asText();
            return new SubmissionStatus(submissionID, SubmissionState.COMPLETED,
              "Submission " + submissionID + " completed; status URL = " + immPortStatusURL);
          }
          //          if (!immPortSubmissionResponseBody.has("uploadTicketNumber"))
          //            return new SubmissionStatus(submissionID, SubmissionState.ERROR,
          //              "No uploadTicketNumber field in ImmPort submit response");
        } else {

          Optional<SubmissionState> submissionState = immPortSubmissionStatus2SubmissionState(submissionID,
            immPortSubmissionStatus);

          if (submissionState.isPresent())
            return new SubmissionStatus(submissionID, submissionState.get(), "");
          else
            return new SubmissionStatus(submissionID, SubmissionState.ERROR,
              "Unknown ImmPort submission status " + immPortSubmissionStatus);
        }
      }
    } else
      return new SubmissionStatus(submissionID, SubmissionState.ERROR, "No JSON in ImmPort status response");
  }

  static private Optional<SubmissionState> immPortSubmissionStatus2SubmissionState(String submissionID,
    String immPortSubmisionStatus)
  {
    if ("Pending".equals(immPortSubmisionStatus))
      return Optional.of(SubmissionState.SUBMITTED);
    else if ("Started".equals(immPortSubmisionStatus))
      return Optional.of(SubmissionState.STARTED);
    else if ("Completed".equals(immPortSubmisionStatus))
      return Optional.of(SubmissionState.COMPLETED);
    else if ("Rejected".equals(immPortSubmisionStatus))
      return Optional.of(SubmissionState.REJECTED);
    else if ("Error".equals(immPortSubmisionStatus))
      return Optional.of(SubmissionState.ERROR);
    else {
      logger.warn("Unexpected ImmPort status " + immPortSubmisionStatus + " returned for submission " + submissionID);
      return Optional.empty();
    }
  }

}
