package com.streamconverter;

/**
 * Indicates invalid input data (classification U in the exception policy): failures that are
 * resolved by fixing the input, such as parse errors and validation failures.
 *
 * <p>This exception is intended for failures caused by the content of the data being processed (for
 * example, malformed CSV rows, values that fail validation rules, or unparsable fields). The remedy
 * for such failures is to correct the input data, not the runtime environment.
 *
 * <p>Environmental I/O failures must NOT be wrapped in this type; propagate the original {@link
 * java.io.IOException} instead. Mixing environmental failures into this classification would
 * obscure the distinction the exception policy relies on between "fix the input" and "fix the
 * environment" failures.
 *
 * <p>See docs/reference/EXCEPTION_POLICY.md for the full exception classification policy.
 *
 * <p>Extends {@link StreamProcessingException}, and therefore {@link java.io.IOException}, so that
 * callers of {@link com.streamconverter.command.IStreamCommand#execute} can continue to handle all
 * stream-level failures with a single {@code catch (IOException)} block.
 */
public class InvalidInputDataException extends StreamProcessingException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new InvalidInputDataException with the specified detail message.
   *
   * @param message the detail message
   */
  public InvalidInputDataException(String message) {
    super(message);
  }

  /**
   * Constructs a new InvalidInputDataException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public InvalidInputDataException(String message, Throwable cause) {
    super(message, cause);
  }
}
