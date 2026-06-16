package com.streamconverter;

import java.io.IOException;

/**
 * Indicates an internal system failure (notification class A in the exception policy): failures
 * that require administrator intervention and are caused by bugs or configuration errors in the
 * application itself.
 *
 * <p>This exception is used for unexpected failures within the StreamConverter library or the
 * application using it. Examples include:
 *
 * <ul>
 *   <li>Unexpected runtime exceptions during stream processing
 *   <li>Resource (stream, connection) failure to close
 *   <li>Pipeline orchestration failure
 *   <li>Configuration error in the application's setup
 * </ul>
 *
 * <p>The default user-facing message advises the user to contact an administrator:
 *
 * <pre>{@code
 * "システムエラーが発生しました。管理者にお問い合わせください。"
 * }</pre>
 *
 * <p>Extends {@link StreamProcessingException}, and therefore {@link IOException}, so that callers
 * of {@link com.streamconverter.command.IStreamCommand#execute} can handle all stream-level
 * failures with a single {@code catch (IOException)} block.
 *
 * <p>See docs/reference/EXCEPTION_POLICY.md for the full exception classification policy.
 *
 * @see StreamProcessingException
 * @see UserInputException
 * @see ExternalTransientException
 * @see ExternalPermanentException
 */
public final class InternalSystemException extends StreamProcessingException {
  private static final long serialVersionUID = 1L;

  /**
   * Default user-facing message for internal system failures. Advises users to contact an
   * administrator.
   */
  public static final String DEFAULT_USER_MESSAGE = "システムエラーが発生しました。管理者にお問い合わせください。";

  /**
   * Constructs a new InternalSystemException with the specified detail message.
   *
   * @param message the detail message (developer-facing, for logging)
   */
  public InternalSystemException(String message) {
    super(message);
  }

  /**
   * Constructs a new InternalSystemException with the specified detail message and cause.
   *
   * @param message the detail message (developer-facing, for logging)
   * @param cause the underlying cause
   */
  public InternalSystemException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new InternalSystemException with the specified cause.
   *
   * @param cause the underlying cause
   */
  public InternalSystemException(Throwable cause) {
    super(cause);
  }

  /**
   * Returns the default user-facing message for internal system failures.
   *
   * <p>This message is suitable for display to end users, advising them to contact an
   * administrator.
   *
   * @return the default internal system failure message
   */
  @Override
  public String getUserMessage() {
    return DEFAULT_USER_MESSAGE;
  }
}
