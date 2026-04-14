package com.streamconverter.command.rule.impl.casing;

import com.streamconverter.command.rule.IRule;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms snake_case strings to camelCase format.
 *
 * <p>This rule converts strings from snake_case notation to camelCase notation by removing
 * underscores and capitalizing the first letter of each word after the first.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>{@code user_name} → {@code userName}
 *   <li>{@code first_name} → {@code firstName}
 *   <li>{@code user_account_id} → {@code userAccountId}
 *   <li>{@code xml_http_request} → {@code xmlHttpRequest}
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * IRule rule = SnakeToCamelCaseRule.builder().build();
 * IStreamCommand command = JsonNavigateCommand.create("$.user_name", rule);
 * }</pre>
 *
 * @since 1.0
 */
public class SnakeToCamelCaseRule implements IRule {

  /** Pattern to match snake_case transitions (underscore followed by letter) */
  private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("_([a-z])");

  /** Pattern to match multiple consecutive underscores */
  private static final Pattern MULTIPLE_UNDERSCORES_PATTERN = Pattern.compile("_{2,}");

  /** Whether to capitalize the first letter (PascalCase instead of camelCase) */
  private final boolean capitalizeFirst;

  /** Whether to preserve leading/trailing underscores */
  private final boolean preserveUnderscores;

  /** Private constructor for builder pattern */
  private SnakeToCamelCaseRule(boolean capitalizeFirst, boolean preserveUnderscores) {
    this.capitalizeFirst = capitalizeFirst;
    this.preserveUnderscores = preserveUnderscores;
  }

  @Override
  public String apply(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }

    String result = input;

    // Clean up multiple underscores if not preserving
    if (!preserveUnderscores) {
      result = MULTIPLE_UNDERSCORES_PATTERN.matcher(result).replaceAll("_");
    }

    // Convert snake_case to camelCase by capitalizing letters after underscores
    Matcher matcher = SNAKE_CASE_PATTERN.matcher(result);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String replacement = matcher.group(1).toUpperCase();
      matcher.appendReplacement(sb, replacement);
    }
    matcher.appendTail(sb);
    result = sb.toString();

    // Handle leading/trailing underscores
    if (!preserveUnderscores) {
      result = result.replaceAll("^_+|_+$", "");
    }

    // Capitalize first letter if PascalCase is requested
    if (capitalizeFirst && !result.isEmpty()) {
      result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
    } else if (!capitalizeFirst && !result.isEmpty()) {
      // Ensure first letter is lowercase for camelCase
      result = Character.toLowerCase(result.charAt(0)) + result.substring(1);
    }

    return result;
  }

  /**
   * Creates a builder for configuring the SnakeToCamelCaseRule.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a SnakeToCamelCaseRule with default settings (camelCase, not PascalCase).
   *
   * @return new rule instance with default configuration
   */
  public static SnakeToCamelCaseRule create() {
    return new Builder().build();
  }

  /**
   * Creates a SnakeToCamelCaseRule that produces PascalCase output.
   *
   * @return new rule instance configured for PascalCase
   */
  public static SnakeToCamelCaseRule createPascalCase() {
    return new Builder().capitalizeFirst(true).build();
  }

  /** Builder class for SnakeToCamelCaseRule configuration. */
  public static class Builder {
    private boolean capitalizeFirstFlag;
    private boolean preserveUnderscoresFlag;

    /** Creates a builder with the default configuration. */
    public Builder() {
      this.capitalizeFirstFlag = false;
      this.preserveUnderscoresFlag = false;
    }

    /**
     * Sets whether to capitalize the first letter (PascalCase instead of camelCase).
     *
     * @param capitalize true for PascalCase, false for camelCase
     * @return this builder
     */
    public Builder capitalizeFirst(boolean capitalize) {
      this.capitalizeFirstFlag = capitalize;
      return this;
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
     * Builds the SnakeToCamelCaseRule with current configuration.
     *
     * @return configured rule instance
     */
    public SnakeToCamelCaseRule build() {
      return new SnakeToCamelCaseRule(capitalizeFirstFlag, preserveUnderscoresFlag);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "SnakeToCamelCaseRule{capitalizeFirst=%s, preserveUnderscores=%s}",
        capitalizeFirst, preserveUnderscores);
  }
}
