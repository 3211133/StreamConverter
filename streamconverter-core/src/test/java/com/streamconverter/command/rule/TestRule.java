package com.streamconverter.command.rule;

import com.streamconverter.StreamProcessingException;
import java.util.Objects;

/**
 * Test implementation of IRule for unit testing commands.
 *
 * <p>This rule performs simple string replacement operations for testing purposes.
 */
public class TestRule implements IRule {

  private final String searchPattern;
  private final String replacement;

  /**
   * Creates a test rule that replaces searchPattern with replacement.
   *
   * @param searchPattern the pattern to search for
   * @param replacement the replacement string
   */
  public TestRule(String searchPattern, String replacement) {
    this.searchPattern = Objects.requireNonNull(searchPattern, "searchPattern cannot be null");
    this.replacement = Objects.requireNonNull(replacement, "replacement cannot be null");
  }

  @Override
  public String apply(String input) throws StreamProcessingException {
    Objects.requireNonNull(input, "input cannot be null");
    return input.replace(searchPattern, replacement);
  }

  /** Factory method for common test scenarios. */
  public static TestRule upperCaseRule() {
    return new TestRule("test", "TEST");
  }

  /** Factory method for XML tag replacement. */
  public static TestRule xmlTagRule() {
    return new TestRule("<value>", "<newValue>");
  }

  /** Factory method for content transformation. */
  public static TestRule contentTransformRule() {
    return new TestRule("original", "transformed");
  }
}
