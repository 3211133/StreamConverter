package com.streamconverter;

import java.io.IOException;

/**
 * Indicates a temporary external system failure (notification class T in the exception policy):
 * failures where the remedy is to wait and retry.
 *
 * <p>This exception is used for transient failures from external systems that are expected to
 * resolve with time or retry. Examples include:
 *
 * <ul>
 *   <li>External API temporary outage or rate limiting
 *   <li>Database connection pool exhaustion
 *   <li>Network timeout (if deemed transient by the protocol handler)
 * </ul>
 *
 * <p>The default user-facing message advises the user to wait and retry:
 *
 * <pre>{@code
 * "サービスが混雑しています。時間をおいて再試行してください。"
 * }</pre>
 *
 * <p>Extends {@link ExternalSystemException}, and therefore {@link StreamProcessingException} and
 * {@link IOException}, so that callers of {@link
 * com.streamconverter.command.IStreamCommand#execute} can handle all stream-level failures with a
 * single {@code catch (IOException)} block.
 *
 * <p>See docs/reference/EXCEPTION_POLICY.md for the full exception classification policy.
 *
 * @see ExternalSystemException
 * @see ExternalPermanentException
 */
public final class ExternalTransientException extends ExternalSystemException {
  private static final long serialVersionUID = 1L;

  /**
   * Default user-facing message for transient external failures. Advises users to wait and retry.
   */
  public static final String DEFAULT_USER_MESSAGE = "サービスが混雑しています。時間をおいて再試行してください。";

  /**
   * Constructs a new ExternalTransientException with the specified detail message.
   *
   * @param message the detail message (developer-facing, for logging)
   */
  public ExternalTransientException(String message) {
    super(message);
  }

  /**
   * Constructs a new ExternalTransientException with the specified detail message and cause.
   *
   * @param message the detail message (developer-facing, for logging)
   * @param cause the underlying cause
   */
  public ExternalTransientException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new ExternalTransientException with the specified cause.
   *
   * @param cause the underlying cause
   */
  public ExternalTransientException(Throwable cause) {
    super(cause);
  }

  /**
   * Returns the default user-facing message for transient external failures.
   *
   * <p>This message is suitable for display to end users, advising them to wait and retry.
   *
   * @return the default transient failure message
   */
  @Override
  public String getUserMessage() {
    return DEFAULT_USER_MESSAGE;
  }
}
