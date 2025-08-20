package com.streamConverter.command.rule;

/**
 * Pass-through rule implementation
 *
 * <p>This rule implementation returns the input string unchanged. Useful as a default rule or for
 * testing purposes when no transformation is needed.
 */
public class PassThroughRule implements IRule {

  /** Default constructor. */
  public PassThroughRule() {}

  /**
   * Apply the pass-through rule - returns input unchanged.
   *
   * @param input the input string to process
   * @return the same input string without any modifications
   * @throws IllegalArgumentException if input is null
   */
  @Override
  public String apply(String input) {
    if (input == null) {
      throw new IllegalArgumentException("Input cannot be null");
    }
    return input;
  }
}
