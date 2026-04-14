package com.streamconverter.command.rule;

/**
 * Pass-through rule implementation
 *
 * <p>This rule implementation returns the input string unchanged. Useful as a default rule or for
 * testing purposes when no transformation is needed.
 */
public class PassThroughRule implements IRule {

  private final boolean requireNonNullInput;

  /** Default constructor. */
  public PassThroughRule() {
    this.requireNonNullInput = true;
  }

  /**
   * Apply the pass-through rule - returns input unchanged.
   *
   * @param input the input string to process
   * @return the same input string without any modifications
   * @throws IllegalArgumentException if input is null
   */
  @Override
  public String apply(String input) {
    if (requireNonNullInput && input == null) {
      throw new IllegalArgumentException("Input cannot be null");
    }
    return input;
  }
}
