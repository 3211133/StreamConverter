package com.streamconverter;

import java.io.IOException;

/**
 * Indicates user input error (notification class U in the exception policy): failures where the
 * remedy is for the end user to correct their input.
 *
 * <p>This exception carries a user-friendly message set by the caller. The message must be
 * sanitized (no internal implementation details, file paths, stack traces, or exception class
 * names). Sanitization is the responsibility of the code that constructs this exception.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>Malformed CSV row that fails parsing
 *   <li>JSON data that doesn't match the expected schema
 *   <li>XML that fails validation
 *   <li>User input that violates a documented constraint
 * </ul>
 *
 * <p>Extends {@link StreamProcessingException}, and therefore {@link IOException}, so that callers
 * of {@link com.streamconverter.command.IStreamCommand#execute} can handle all stream-level
 * failures with a single {@code catch (IOException)} block.
 *
 * <p>See docs/reference/EXCEPTION_POLICY.md for the full exception classification policy.
 *
 * @see StreamProcessingException
 * @see ExternalTransientException
 * @see ExternalPermanentException
 * @see InternalSystemException
 */
public final class UserInputException extends StreamProcessingException {
  private static final long serialVersionUID = 1L;

  /**
   * Default user message for when input classification is unknown. Callers should provide a more
   * specific message at construction time.
   */
  public static final String DEFAULT_USER_MESSAGE = "入力データが正しくありません。ご確認の上、再度お試しください。";

  private final String userMessage;

  /**
   * Constructs a new UserInputException with the specified user-friendly message.
   *
   * <p>The message must be sanitized — it should not contain internal implementation details, file
   * paths, stack traces, or exception class names.
   *
   * @param userMessage a sanitized, user-friendly message describing what is wrong with the input
   *     and how to fix it
   */
  public UserInputException(String userMessage) {
    super(userMessage);
    this.userMessage = userMessage;
  }

  /**
   * Constructs a new UserInputException with the specified user-friendly message and cause.
   *
   * <p>The message must be sanitized — it should not contain internal implementation details, file
   * paths, stack traces, or exception class names.
   *
   * @param userMessage a sanitized, user-friendly message describing what is wrong with the input
   * @param cause the cause of the exception
   */
  public UserInputException(String userMessage, Throwable cause) {
    super(userMessage, cause);
    this.userMessage = userMessage;
  }

  /**
   * Returns the user-friendly message set at construction time.
   *
   * <p>This is a dynamic message specific to the input failure. Callers use this to display error
   * information to end users.
   *
   * @return the sanitized user message
   */
  @Override
  public String getUserMessage() {
    return userMessage;
  }
}
