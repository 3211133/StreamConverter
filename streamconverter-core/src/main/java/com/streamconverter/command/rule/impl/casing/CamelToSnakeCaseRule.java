package com.streamconverter.command.rule.impl.casing;

import com.streamconverter.command.rule.IRule;

/**
 * Transforms camelCase strings to snake_case format.
 *
 * <p>This rule converts strings from camelCase notation to snake_case notation by inserting
 * underscores before uppercase letters and converting all letters to lowercase.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>{@code userName} → {@code user_name}
 *   <li>{@code firstName} → {@code first_name}
 *   <li>{@code userAccountID} → {@code user_account_id}
 *   <li>{@code XMLHttpRequest} → {@code xml_http_request}
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * IRule rule = CamelToSnakeCaseRule.builder().build();
 * IStreamCommand command = JsonNavigateCommand.create("$.userName", rule);
 * }</pre>
 *
 * @since 1.0
 */
public class CamelToSnakeCaseRule implements IRule {

  /** Common acronyms that should be split when found consecutively */
  private static final String[] COMMON_ACRONYMS = {
    "JSON", "XML", "API", "URL", "HTTP", "HTTPS", "FTP", "SQL", "HTML", "CSS", "JS", "REST", "SOAP"
  };

  /** Whether to preserve leading/trailing underscores */
  private final boolean preserveUnderscores;

  /** Whether to handle acronyms specially */
  private final boolean handleAcronyms;

  /** Private constructor for builder pattern */
  private CamelToSnakeCaseRule(boolean preserveUnderscores, boolean handleAcronyms) {
    this.preserveUnderscores = preserveUnderscores;
    this.handleAcronyms = handleAcronyms;
  }

  @Override
  public String apply(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    String result;

    if (handleAcronyms) {
      result = processWithAcronyms(input);
    } else {
      // Simple camelCase conversion - separate ALL uppercase letters
      result = processWithoutAcronyms(input);
    }

    // Convert to lowercase
    result = result.toLowerCase();

    // Clean up multiple underscores if not preserving
    if (!preserveUnderscores) {
      result = result.replaceAll("_{2,}", "_");
      // Remove leading/trailing underscores
      result = result.replaceAll("^_+|_+$", "");
    }

    return result;
  }

  /**
   * Processes input with proper acronym handling using character-by-character analysis.
   *
   * @param input the input string to process
   * @return processed string with underscores inserted at proper boundaries
   */
  private String processWithAcronyms(String input) {
    // Use a three-pass approach for better acronym handling
    String result = input;

    // Pass 1: Handle consecutive acronyms like JSONAPI -> JSON_API FIRST
    // This must happen before other transformations
    result = splitConsecutiveAcronyms(result);

    // Pass 2: Insert underscores between lowercase/digit/underscore and uppercase
    // Include underscore in the pattern if preserving underscores
    String lowercasePattern = preserveUnderscores ? "([a-z0-9_])([A-Z])" : "([a-z0-9])([A-Z])";
    result = result.replaceAll(lowercasePattern, "$1_$2");

    // Pass 3: Insert underscores before uppercase that's followed by lowercase (end of acronym)
    result = result.replaceAll("([A-Z])([A-Z][a-z])", "$1_$2");

    return result;
  }

  /**
   * Processes input without acronym handling - separates EVERY uppercase letter.
   *
   * @param input the input string to process
   * @return processed string with underscores between every case transition
   */
  private String processWithoutAcronyms(String input) {
    if (input.length() <= 1) {
      return input;
    }

    StringBuilder result = new StringBuilder();
    char[] chars = input.toCharArray();

    for (int i = 0; i < chars.length; i++) {
      char current = chars[i];
      char previous = i > 0 ? chars[i - 1] : '\0';

      // Add underscore before current character if:
      // 1. Previous is lowercase/digit and current is uppercase
      // 2. Previous is uppercase and current is uppercase (separate all uppercase)
      if (i > 0
          && Character.isUpperCase(current)
          && (Character.isLowerCase(previous)
              || Character.isDigit(previous)
              || Character.isUpperCase(previous))) {
        result.append('_');
      }

      result.append(current);
    }

    return result.toString();
  }

  /**
   * Splits consecutive acronyms based on configurable patterns. This method uses a dictionary
   * approach to identify and split consecutive acronyms like JSONAPI -> JSON_API
   */
  private String splitConsecutiveAcronyms(String input) {
    String result = input;

    // For each acronym, split if it is immediately followed by another uppercase sequence (length
    // >= 3)
    for (String acronym : COMMON_ACRONYMS) {
      // Use regex to match the acronym followed by another uppercase sequence of length >= 3
      // e.g., JSONAPI -> JSON_API, XMLHTTPS -> XML_HTTPS
      result = result.replaceAll(acronym + "([A-Z]{3,})", acronym + "_$1");
    }

    return result;
  }

  /**
   * Creates a builder for configuring the CamelToSnakeCaseRule.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a CamelToSnakeCaseRule with default settings.
   *
   * @return new rule instance with default configuration
   */
  public static CamelToSnakeCaseRule create() {
    return new Builder().build();
  }

  /** Builder class for CamelToSnakeCaseRule configuration. */
  public static class Builder {
    private boolean preserveUnderscoresFlag;
    private boolean handleAcronymsFlag;

    /** Creates a builder with the default configuration. */
    public Builder() {
      this.preserveUnderscoresFlag = false;
      this.handleAcronymsFlag = true;
    }

    /**
     * Sets whether to preserve existing underscores in the input.
     *
     * @param preserve true to preserve underscores, false to clean them up
     * @return this builder
     */
    public Builder preserveUnderscores(boolean preserve) {
      this.preserveUnderscoresFlag = preserve;
      return this;
    }

    /**
     * Sets whether to handle acronyms specially (e.g., XMLHttpRequest → xml_http_request).
     *
     * @param handle true to handle acronyms, false to treat them as regular uppercase
     * @return this builder
     */
    public Builder handleAcronyms(boolean handle) {
      this.handleAcronymsFlag = handle;
      return this;
    }

    /**
     * Builds the CamelToSnakeCaseRule with current configuration.
     *
     * @return configured rule instance
     */
    public CamelToSnakeCaseRule build() {
      return new CamelToSnakeCaseRule(preserveUnderscoresFlag, handleAcronymsFlag);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "CamelToSnakeCaseRule{preserveUnderscores=%s, handleAcronyms=%s}",
        preserveUnderscores, handleAcronyms);
  }
}
