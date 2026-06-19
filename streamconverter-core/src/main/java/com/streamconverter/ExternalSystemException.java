package com.streamconverter;

import java.io.IOException;

/**
 * Abstract parent type for external system failures (notification classes T and A in the exception
 * policy).
 *
 * <p>This is an internal abstraction for the exception hierarchy. Callers must not directly throw
 * this type; use one of its concrete subtypes instead:
 *
 * <ul>
 *   <li>{@link ExternalTransientException} for temporary failures that may resolve with retry
 *   <li>{@link ExternalPermanentException} for failures that require administrator intervention
 * </ul>
 *
 * <p>Callers who need to handle both transient and permanent external failures can use this type as
 * a catch target:
 *
 * <pre>{@code
 * try {
 *   converter.execute();
 * } catch (ExternalSystemException e) {
 *   // Handle both transient and permanent external failures
 *   log.error("External system failure", e);
 * }
 * }</pre>
 *
 * <p>Extends {@link StreamProcessingException}, and therefore {@link IOException}, so that callers
 * of {@link com.streamconverter.command.IStreamCommand#execute} can handle all stream-level
 * failures with a single {@code catch (IOException)} block.
 *
 * <p>See docs/reference/EXCEPTION_POLICY.md for the full exception classification policy.
 *
 * @see ExternalTransientException
 * @see ExternalPermanentException
 */
public abstract class ExternalSystemException extends StreamProcessingException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new ExternalSystemException with the specified detail message.
   *
   * @param message the detail message
   */
  protected ExternalSystemException(String message) {
    super(message);
  }

  /**
   * Constructs a new ExternalSystemException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the underlying cause
   */
  protected ExternalSystemException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new ExternalSystemException with the specified cause.
   *
   * @param cause the underlying cause
   */
  protected ExternalSystemException(Throwable cause) {
    super(cause);
  }
}
