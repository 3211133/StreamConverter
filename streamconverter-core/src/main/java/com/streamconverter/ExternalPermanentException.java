package com.streamconverter;

import java.io.IOException;

/**
 * Indicates a permanent external system failure (notification class A in the exception policy):
 * failures where the remedy requires administrator intervention.
 *
 * <p>This exception is used for failures from external systems that require administrator attention
 * to resolve. Examples include:
 *
 * <ul>
 *   <li>External API permanent shutdown or removal
 *   <li>Authentication failure to external system
 *   <li>Configuration error preventing communication with external system
 *   <li>External system returning an error that will not resolve on its own
 * </ul>
 *
 * <p>The default user-facing message advises the user to contact an administrator:
 *
 * <pre>{@code
 * "システムエラーが発生しました。管理者にお問い合わせください。"
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
 * @see ExternalTransientException
 */
public final class ExternalPermanentException extends ExternalSystemException {
  private static final long serialVersionUID = 1L;

  /**
   * Default user-facing message for permanent external failures. Advises users to contact an
   * administrator.
   */
  public static final String DEFAULT_USER_MESSAGE = "システムエラーが発生しました。管理者にお問い合わせください。";

  /**
   * Constructs a new ExternalPermanentException with the specified detail message.
   *
   * @param message the detail message (developer-facing, for logging)
   */
  public ExternalPermanentException(String message) {
    super(message);
  }

  /**
   * Constructs a new ExternalPermanentException with the specified detail message and cause.
   *
   * @param message the detail message (developer-facing, for logging)
   * @param cause the underlying cause
   */
  public ExternalPermanentException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new ExternalPermanentException with the specified cause.
   *
   * @param cause the underlying cause
   */
  public ExternalPermanentException(Throwable cause) {
    super(cause);
  }

  /**
   * Returns the default user-facing message for permanent external failures.
   *
   * <p>This message is suitable for display to end users, advising them to contact an
   * administrator.
   *
   * @return the default permanent failure message
   */
  @Override
  public String getUserMessage() {
    return DEFAULT_USER_MESSAGE;
  }
}
