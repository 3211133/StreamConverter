package com.streamConverter.command.rule.impl.string;

import com.streamConverter.command.rule.IRule;
import java.util.Locale;

/**
 * Converts strings to lowercase.
 *
 * <p>This rule converts all uppercase letters in the input string to lowercase using the specified
 * locale or the default locale if none is specified.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>{@code "HELLO"} → {@code "hello"}
 *   <li>{@code "Hello World"} → {@code "hello world"}
 *   <li>{@code "XMLHttpRequest"} → {@code "xmlhttprequest"}
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * IRule rule = new LowerCaseRule();
 * IRule localeRule = new LowerCaseRule(Locale.ENGLISH);
 * IStreamCommand command = JsonNavigateCommand.create("$.name", rule);
 * }</pre>
 *
 * @since 1.0
 */
public class LowerCaseRule implements IRule {

  /** The locale to use for case conversion */
  private final Locale locale;

  /** Creates a LowerCaseRule using the default locale. */
  public LowerCaseRule() {
    this.locale = Locale.getDefault();
  }

  /**
   * Creates a LowerCaseRule using the specified locale.
   *
   * @param locale the locale to use for case conversion
   * @throws IllegalArgumentException if locale is null
   */
  public LowerCaseRule(Locale locale) {
    if (locale == null) {
      throw new IllegalArgumentException("Locale cannot be null");
    }
    this.locale = locale;
  }

  @Override
  public String apply(String input) {
    return input != null ? input.toLowerCase(locale) : null;
  }

  /**
   * Gets the locale used for case conversion.
   *
   * @return the locale
   */
  public Locale getLocale() {
    return locale;
  }

  @Override
  public String toString() {
    return String.format("LowerCaseRule{locale=%s}", locale);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    LowerCaseRule that = (LowerCaseRule) obj;
    return locale.equals(that.locale);
  }

  @Override
  public int hashCode() {
    return locale.hashCode();
  }
}
