package com.streamconverter;

import java.io.IOException;

/**
 * Custom exception for stream processing errors.
 *
 * <p>This exception is thrown when errors occur during stream processing operations, providing
 * meaningful context about the failure while preserving the original exception information.
 *
 * <p>Extends {@link java.io.IOException} so that callers of {@link
 * com.streamconverter.command.IStreamCommand#execute} can handle all stream-level failures with a
 * single {@code catch (IOException)} block, consistent with the method's checked-exception
 * contract.
 */
public class StreamProcessingException extends IOException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new StreamProcessingException with the specified detail message.
   *
   * @param message the detail message
   */
  public StreamProcessingException(String message) {
    super(message);
  }

  /**
   * Constructs a new StreamProcessingException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public StreamProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new StreamProcessingException with the specified cause.
   *
   * @param cause the cause of the exception
   */
  public StreamProcessingException(Throwable cause) {
    super(cause);
  }
}
