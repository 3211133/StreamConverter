package com.streamconverter;

import java.io.IOException;
import java.util.Objects;

/**
 * Wraps an {@link IOException} as an unchecked exception so it can be carried through functional
 * interfaces, such as {@link com.streamconverter.command.rule.IRule#apply}, that do not declare
 * checked exceptions.
 *
 * <p>This type is {@code public} because rule implementations live in other packages and modules
 * (for example, the database rules in {@code streamconverter-db}) and need to throw it. Its role is
 * still strictly that of a carrier: it must not escape public command boundaries. Code at a command
 * boundary (for example, a {@code CommandStageRunner}) must catch this exception, unwrap it via
 * {@link #getCause()}, and rethrow the underlying {@link IOException} (or a {@link
 * StreamProcessingException} wrapping it) so that callers continue to observe the checked exception
 * contract.
 *
 * <p>This class follows the same design as {@link java.io.UncheckedIOException}.
 */
public final class UncheckedStreamException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new UncheckedStreamException with the specified cause.
   *
   * @param cause the {@link IOException} to wrap
   * @throws NullPointerException if {@code cause} is {@code null}
   */
  public UncheckedStreamException(IOException cause) {
    super(Objects.requireNonNull(cause));
  }

  /**
   * Returns the cause of this exception.
   *
   * @return the {@link IOException} that was passed to the constructor
   */
  @Override
  public IOException getCause() {
    return (IOException) super.getCause();
  }
}
