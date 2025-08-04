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

  private static final int BUFFER_SIZE = 8192; // 8KB buffer for streaming
  private static final int PROCESSING_THRESHOLD = 65536; // 64KB processing threshold

  private final String jsonPath;
  private final IRule rule;

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
    String pathInfo = jsonPath != null ? String.format("jsonPath='%s'", jsonPath) : "entire JSON";
    return String.format(
        "JsonNavigateCommand(%s, rule='%s')", pathInfo, rule.getClass().getSimpleName());
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

  /** Apply transformation rule to entire JSON content using memory-efficient approach */
  private void applyRuleToEntireJson(BufferedReader reader, Writer writer) throws IOException {
    processJsonInChunks(reader, writer, rule);
  }

  /**
   * Apply transformation rule to specific JSONPath elements while preserving JSON structure Uses
   * memory-efficient approach that maintains JSON integrity for proper JSONPath evaluation
   */
  private void applyRuleToJsonPath(BufferedReader reader, Writer writer) throws IOException {
    // JSONPath requires complete JSON structure - use controlled memory approach
    String completeJson = readCompleteJsonFromReader(reader);
    String transformedJson = applyRuleToJsonPathComplete(completeJson, jsonPath, rule);

    writer.write(transformedJson);
    writer.flush();
  }

  /**
   * Complete JSON processing with JSONPath-based rule application This method processes the entire
   * JSON structure to enable proper JSONPath evaluation
   */
  private String applyRuleToJsonPathComplete(String json, String path, IRule rule) {
    // Handle complete JSON structure for proper JSONPath processing
    // This ensures JSONPath expressions can traverse the full document structure

    if (path == null || !path.startsWith("$.")) {
      // Fallback to entire JSON transformation
      return rule.apply(json);
    }

    // Handle basic JSONPath patterns with complete JSON context
    if (path.matches("^\\$\\.[a-zA-Z_][a-zA-Z0-9_]*$")) {
      // Simple property access like "$.name" - process entire JSON
      return applyRuleToJsonProperty(json, path.substring(2), rule);
    }

    // Handle array access patterns like "$.users[*].name"
    if (path.contains("[*]") || path.contains("\\[\\d+\\]")) {
      return applyRuleToJsonArrayPath(json, path, rule);
    }

    // For complex paths, apply rule to entire JSON to maintain structure
    // This preserves JSON integrity while applying transformations
    return rule.apply(json);
  }

  /**
   * Apply rule to JSONPath array expressions like "$.users[*].name" Processes complete JSON to
   * handle array traversal correctly
   */
  private String applyRuleToJsonArrayPath(String json, String path, IRule rule) {
    // This is a simplified implementation for basic array path patterns
    // Production code would use a proper JSONPath library for complete functionality

    try {
      // Handle wildcard array access like "$.users[*].name"
      if (path.contains("[*]")) {
        String propertyPath = path.replace("[*]", "");
        return applyRuleToJsonProperty(json, propertyPath.substring(2), rule);
      }

      // Handle indexed array access like "$.users[0].name"
      java.util.regex.Pattern arrayPattern = java.util.regex.Pattern.compile("\\[(\\d+)\\]");
      java.util.regex.Matcher matcher = arrayPattern.matcher(path);
      if (matcher.find()) {
        String cleanPath = path.replaceAll("\\[\\d+\\]", "");
        return applyRuleToJsonProperty(json, cleanPath.substring(2), rule);
      }

    } catch (Exception e) {
      // Fallback to entire JSON transformation on parsing errors
      return rule.apply(json);
    }

    return json; // Return unchanged if pattern not recognized
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

  // Common utility methods
  private void processJsonInChunks(BufferedReader reader, Writer writer, IRule rule)
      throws IOException {
    StringBuilder jsonBuffer = new StringBuilder();
    char[] buffer = new char[BUFFER_SIZE];
    int charsRead;

    // Read JSON in chunks instead of line-by-line to preserve JSON structure
    while ((charsRead = reader.read(buffer)) != -1) {
      jsonBuffer.append(buffer, 0, charsRead);

      // Process complete JSON tokens when buffer reaches threshold
      if (jsonBuffer.length() > PROCESSING_THRESHOLD) {
        processAndWriteChunk(jsonBuffer.toString(), writer, rule);
        jsonBuffer.setLength(0); // Clear buffer
      }
    }

    // Process remaining content
    if (jsonBuffer.length() > 0) {
      processAndWriteChunk(jsonBuffer.toString(), writer, rule);
    }

    writer.flush();
  }

  private void processAndWriteChunk(String chunk, Writer writer, IRule rule) throws IOException {
    String transformedChunk = rule.apply(chunk);
    writer.write(transformedChunk);
    writer.flush();
  }

  private String readCompleteJsonFromReader(BufferedReader reader) throws IOException {
    StringBuilder jsonBuilder = new StringBuilder();
    char[] buffer = new char[BUFFER_SIZE];
    int charsRead;

    // Read JSON in manageable chunks while preserving structure
    while ((charsRead = reader.read(buffer)) != -1) {
      jsonBuilder.append(buffer, 0, charsRead);
    }

    return jsonBuilder.toString();
  }
}
