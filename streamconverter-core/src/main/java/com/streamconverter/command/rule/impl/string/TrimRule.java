package com.streamconverter.command.rule.impl.string;

import com.streamconverter.command.rule.IRule;

/**
 * Trims leading and trailing whitespace from strings.
 *
 * <p>This rule removes whitespace characters from the beginning and end of the input string.
 * Whitespace includes spaces, tabs, newlines, and other Unicode whitespace characters.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>{@code " hello "} → {@code "hello"}
 *   <li>{@code "\t\nworld\r\n"} → {@code "world"}
 *   <li>{@code "already trimmed"} → {@code "already trimmed"}
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * IRule rule = new TrimRule();
 * IStreamCommand command = JsonNavigateCommand.create("$.name", rule);
 * }</pre>
 *
 * @since 1.0
 */
public class TrimRule implements IRule {

  @Override
  public String apply(String input) {
    return input != null ? input.trim() : null;
  }

  @Override
  public String toString() {
    return "TrimRule{}";
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof TrimRule;
  }

  @Override
  public int hashCode() {
    return TrimRule.class.hashCode();
  }
}
