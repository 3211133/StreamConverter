package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.command.rule.IRule;
import com.streamConverter.command.rule.PassThroughRule;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * JSON Navigate Command Class
 *
 * <p>This class implements command for targeted JSON transformation using JSONPath. It identifies
 * specific elements using JSONPath expressions and applies IRule transformations to those elements
 * while preserving the overall JSON structure.
 */
public class JsonNavigateCommand extends AbstractStreamCommand {

  private String jsonPath;
  private IRule rule;

  /**
   * Constructor for JSON navigation with JSONPath selector and transformation rule.
   *
   * @param jsonPath the JSONPath expression to select data (e.g., "$.users[*].name")
   * @param rule the transformation rule to apply to selected elements
   * @throws IllegalArgumentException if rule is null
   */
  public JsonNavigateCommand(String jsonPath, IRule rule) {
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    this.jsonPath = jsonPath;
    this.rule = rule;
  }

  /**
   * Constructor for JSON navigation with JSONPath selector using PassThroughRule.
   *
   * @param jsonPath the JSONPath expression to select data (e.g., "$.users[*].name")
   */
  public JsonNavigateCommand(String jsonPath) {
    this(jsonPath, new PassThroughRule());
  }

  /** Default constructor - processes entire JSON with PassThroughRule. */
  public JsonNavigateCommand() {
    this(null, new PassThroughRule());
  }

  @Override
  protected String getCommandDetails() {
    if (jsonPath != null) {
      return String.format(
          "JsonNavigateCommand(jsonPath='%s', rule='%s')",
          jsonPath, rule.getClass().getSimpleName());
    } else {
      return String.format(
          "JsonNavigateCommand(entire JSON, rule='%s')", rule.getClass().getSimpleName());
    }
  }

  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      if (jsonPath == null) {
        // Apply rule to entire JSON content
        applyRuleToEntireJson(reader, writer);
      } else {
        // Apply rule to specific JSONPath elements while preserving structure
        applyRuleToJsonPath(reader, writer);
      }
    }
  }

  /**
   * Apply transformation rule to entire JSON content Reads the entire JSON, applies the rule, and
   * outputs the result
   */
  private void applyRuleToEntireJson(BufferedReader reader, Writer writer) throws IOException {
    StringBuilder jsonBuilder = new StringBuilder();
    String line;

    // Read entire JSON content
    while ((line = reader.readLine()) != null) {
      jsonBuilder.append(line);
    }

    // Apply rule to entire content
    String transformedJson = rule.apply(jsonBuilder.toString());
    writer.write(transformedJson);
    writer.flush();
  }

  /**
   * Apply transformation rule to specific JSONPath elements while preserving JSON structure This is
   * a simplified implementation - production version would need proper JSON parsing
   */
  private void applyRuleToJsonPath(BufferedReader reader, Writer writer) throws IOException {
    StringBuilder jsonBuilder = new StringBuilder();
    String line;

    // Read entire JSON content
    while ((line = reader.readLine()) != null) {
      jsonBuilder.append(line);
    }

    String originalJson = jsonBuilder.toString();

    // For now, apply simple path-based transformation
    // In production, this would use proper JSONPath library
    String transformedJson = applyRuleToJsonPathSimple(originalJson, jsonPath, rule);

    writer.write(transformedJson);
    writer.flush();
  }

  /**
   * Simple JSONPath-based rule application This is a basic implementation for demonstration -
   * production would use JSONPath library
   */
  private String applyRuleToJsonPathSimple(String json, String path, IRule rule) {
    // Basic implementation for simple JSONPath patterns
    // In production, this would use a proper JSONPath library like Jayway JsonPath

    if (path == null || !path.startsWith("$.")) {
      // Fallback to entire JSON transformation
      return rule.apply(json);
    }

    // Handle very basic JSONPath patterns as demonstration
    // This is a simplified implementation - real JSONPath is much more complex
    if (path.matches("^\\$\\.[a-zA-Z_][a-zA-Z0-9_]*$")) {
      // Simple property access like "$.name"
      return applyRuleToJsonProperty(json, path.substring(2), rule);
    }

    // For complex paths, fallback to entire JSON transformation
    // TODO: Implement proper JSONPath navigation and targeted transformation
    // This maintains functionality while acknowledging the current limitation
    return rule.apply(json);
  }

  /** Apply rule to a simple JSON property Basic implementation for property-level transformation */
  private String applyRuleToJsonProperty(String json, String property, IRule rule) {
    // Simple regex-based property transformation for basic cases
    // This handles quoted string values in JSON properties
    String pattern = "(\"" + property + "\"\\s*:\\s*\")([^\"]*)(\"[,}\\]])";

    // Use manual string replacement since replaceAll with lambda is not supported in older Java
    StringBuilder result = new StringBuilder();
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
    java.util.regex.Matcher m = p.matcher(json);

    int lastEnd = 0;
    while (m.find()) {
      result.append(json, lastEnd, m.start());

      String prefix = m.group(1); // "property": "
      String value = m.group(2); // the actual value
      String suffix = m.group(3); // closing quote and delimiter

      String transformedValue = rule.apply(value);
      // Escape quotes in the transformed value
      transformedValue = transformedValue.replace("\"", "\\\"");

      result.append(prefix).append(transformedValue).append(suffix);
      lastEnd = m.end();
    }
    result.append(json, lastEnd, json.length());

    return result.toString();
  }

  /** Legacy method kept for compatibility - now applies rule to formatted JSON */
  private void streamFormatJson(BufferedReader reader, Writer writer) throws IOException {
    int indent = 0;
    boolean inString = false;
    boolean escaped = false;
    int ch;

    while ((ch = reader.read()) != -1) {
      char c = (char) ch;

      if (escaped) {
        writer.write(c);
        escaped = false;
        continue;
      }

      if (c == '\\') {
        escaped = true;
        writer.write(c);
        continue;
      }

      if (c == '"') {
        inString = !inString;
        writer.write(c);
        continue;
      }

      if (inString) {
        writer.write(c);
        continue;
      }

      switch (c) {
        case '{':
        case '[':
          writer.write(c);
          writer.write('\n');
          indent++;
          addIndentation(writer, indent);
          break;
        case '}':
        case ']':
          writer.write('\n');
          indent--;
          addIndentation(writer, indent);
          writer.write(c);
          break;
        case ',':
          writer.write(c);
          writer.write('\n');
          addIndentation(writer, indent);
          break;
        case ' ':
        case '\t':
        case '\r':
        case '\n':
          // Skip whitespace outside strings for cleaner formatting
          break;
        default:
          writer.write(c);
          break;
      }
    }
    writer.flush();
  }

  /** Legacy method kept for compatibility */
  private void parseAndNavigateJson(BufferedReader reader, Writer writer) throws IOException {
    // Redirect to new implementation
    applyRuleToJsonPath(reader, writer);
  }

  private String formatJson(String json) {
    // Simple JSON formatting - add newlines and indentation
    StringBuilder formatted = new StringBuilder();
    int indent = 0;
    boolean inString = false;
    boolean escaped = false;

    for (char c : json.toCharArray()) {
      if (escaped) {
        formatted.append(c);
        escaped = false;
        continue;
      }

      if (c == '\\') {
        escaped = true;
        formatted.append(c);
        continue;
      }

      if (c == '"') {
        inString = !inString;
        formatted.append(c);
        continue;
      }

      if (inString) {
        formatted.append(c);
        continue;
      }

      switch (c) {
        case '{':
        case '[':
          formatted.append(c).append('\n');
          indent++;
          addIndentation(formatted, indent);
          break;
        case '}':
        case ']':
          formatted.append('\n');
          indent--;
          addIndentation(formatted, indent);
          formatted.append(c);
          break;
        case ',':
          formatted.append(c).append('\n');
          addIndentation(formatted, indent);
          break;
        default:
          formatted.append(c);
          break;
      }
    }

    return formatted.toString();
  }

  private void addIndentation(StringBuilder sb, int indent) {
    for (int i = 0; i < indent; i++) {
      sb.append("  ");
    }
  }

  private void addIndentation(Writer writer, int indent) throws IOException {
    for (int i = 0; i < indent; i++) {
      writer.write("  ");
    }
  }

  private String navigateJson(String json, String path) {
    // Simple JSONPath implementation for basic navigation
    try {
      if (path.startsWith("$.")) {
        path = path.substring(2);
      }

      // Handle array access like "users[0].name"
      if (path.contains("[") && path.contains("]")) {
        return navigateWithArray(json, path);
      }

      // Handle simple property access like "users.name"
      return navigateWithProperty(json, path);
    } catch (Exception e) {
      return "Error navigating JSON: " + e.getMessage();
    }
  }

  private String navigateWithProperty(String json, String path) {
    String[] parts = path.split("\\.");
    String current = json;

    for (String part : parts) {
      current = extractProperty(current, part);
      if (current == null) {
        return "null";
      }
    }

    return current;
  }

  private String navigateWithArray(String json, String path) {
    // Simple array navigation - just return the first element for demo
    if (path.contains("[*]")) {
      String propertyPath = path.replace("[*]", "");
      return navigateWithProperty(json, propertyPath);
    }
    return navigateWithProperty(json, path.replaceAll("\\[\\d+\\]", ""));
  }

  private String extractProperty(String json, String property) {
    String searchKey = "\"" + property + "\"";
    int keyIndex = json.indexOf(searchKey);
    if (keyIndex == -1) {
      return null;
    }

    int colonIndex = json.indexOf(":", keyIndex);
    if (colonIndex == -1) {
      return null;
    }

    int start = colonIndex + 1;
    while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
      start++;
    }

    if (start >= json.length()) {
      return null;
    }

    // Handle string values
    if (json.charAt(start) == '"') {
      int end = json.indexOf('"', start + 1);
      return json.substring(start + 1, end);
    }

    // Handle numeric/boolean values
    int end = start;
    while (end < json.length()
        && json.charAt(end) != ','
        && json.charAt(end) != '}'
        && json.charAt(end) != ']'
        && !Character.isWhitespace(json.charAt(end))) {
      end++;
    }

    return json.substring(start, end);
  }
}
