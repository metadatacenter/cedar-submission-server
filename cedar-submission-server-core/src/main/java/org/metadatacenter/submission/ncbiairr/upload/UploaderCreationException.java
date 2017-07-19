package org.metadatacenter.submission.ncbiairr.upload;

public class UploaderCreationException extends Exception {

  public UploaderCreationException(String message) {
    super(message);
  }

  public UploaderCreationException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
