package com.streamconverter;

/**
 * Custom exception for stream processing errors.
 *
 * <p>This exception is thrown when errors occur during stream processing operations, providing
 * meaningful context about the failure while preserving the original exception information.
 */
public class StreamProcessingException extends RuntimeException {
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
