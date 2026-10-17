package org.metadatacenter.submission.resources;

/**
 * Descriptions shared by the submission server's OpenAPI annotations.
 *
 * <p>Every route here authenticates its caller, and the refusals that follow from that read the same
 * on each of them, so their text is written once here and referenced from every operation. Annotation
 * values must be compile-time constants, which is why these are strings rather than a shared
 * annotation.
 */
final class SubmissionApiDocs {

  static final String UNAUTHORIZED = "The request carries no valid credentials";

  static final String NOT_ACCEPTABLE = "The request does not accept a JSON response";

  static final String NOT_JSON = "The body is not declared as JSON";

  static final String NOT_MULTIPART = "The body is not declared as multipart/form-data";

  static final String RATE_LIMITED = "The deployment limits each user's request rate, and the caller exceeded it";

  static final String INTERNAL_ERROR = "Internal server error";

  static final String UNAVAILABLE = "The graph database, or another service the request depends on, is "
      + "unavailable";

  static final String VALIDATION_BODY_REFUSED = "The body is not JSON, or is not an object holding instance "
      + "and, optionally, userFileNames, and no other field. The message names the problem.";

  private SubmissionApiDocs() {
  }
}
