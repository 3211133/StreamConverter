package com.streamconverter;

import java.io.IOException;

/**
 * Base exception for stream processing failures, part of the exception policy notification class
 * hierarchy.
 *
 * <p>All failures in StreamConverter pipeline execution are classified into one of three
 * notification classes:
 *
 * <ul>
 *   <li>{@link UserInputException} (U) — user can fix by correcting input
 *   <li>{@link ExternalTransientException} (T) — external system temporarily unavailable; user
 *       should retry
 *   <li>{@link ExternalPermanentException} or {@link InternalSystemException} (A) — requires
 *       administrator intervention
 * </ul>
 *
 * <p><strong>Base exception contract:</strong> {@code StreamProcessingException} is the parent of
 * all notification-class failures, and all concrete notification classes extend this type (either
 * directly or through {@link ExternalSystemException}).
 *
 * <p><strong>Extends IOException:</strong> Callers of {@link
 * com.streamconverter.command.IStreamCommand#execute} can handle all stream-level failures with a
 * single {@code catch (IOException)} block, consistent with the method's checked-exception
 * contract.
 *
 * <p><strong>Message mechanism:</strong> All exceptions provide a user-friendly message via {@link
 * #getUserMessage()}. Concrete types override this to return type-specific messages: {@link
 * UserInputException} returns a dynamic message provided at construction; other types return a
 * type-specific {@code DEFAULT_USER_MESSAGE} constant suitable for display to end users.
 *
 * <p>See docs/reference/EXCEPTION_POLICY.md for the full exception classification policy.
 *
 * @see UserInputException
 * @see ExternalSystemException
 * @see ExternalTransientException
 * @see ExternalPermanentException
 * @see InternalSystemException
 * @see AggregatedStreamProcessingException
 */
public class StreamProcessingException extends IOException {
  private static final long serialVersionUID = 1L;

  /**
   * Default user-facing message for generic stream processing failures. Advises users to contact an
   * administrator.
   *
   * <p>Concrete notification-class exceptions (U/T/A) override {@link #getUserMessage()} to return
   * a type-specific message. This default is used as a fallback for unclassified failures.
   */
  public static final String DEFAULT_USER_MESSAGE = "システムエラーが発生しました。管理者にお問い合わせください。";

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

  /**
   * Returns a user-friendly message suitable for display to end users.
   *
   * <p>This method is designed to be called by main-layer code to provide consistent end-user
   * notifications without needing instanceof checks or conditional branching. The message is chosen
   * based on the notification class:
   *
   * <ul>
   *   <li>{@link UserInputException}: Dynamic message provided at construction time
   *   <li>{@link ExternalTransientException}: Generic "please retry" message
   *   <li>{@link ExternalPermanentException}: Generic "contact administrator" message
   *   <li>{@link InternalSystemException}: Generic "contact administrator" message
   *   <li>Other subtypes: Returns the base {@link #DEFAULT_USER_MESSAGE}
   * </ul>
   *
   * <p>Concrete subtypes override this method to return type-specific messages.
   *
   * @return sanitized user-facing message (never contains internal details like stack traces, file
   *     paths, or class names)
   */
  public String getUserMessage() {
    return DEFAULT_USER_MESSAGE;
  }
}
