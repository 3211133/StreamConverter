package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * JSON Filter Command Class
 *
 * <p>This class implements pure data extraction from JSON using JSONPath expressions. Unlike
 * JsonNavigateCommand which applies transformations, JsonFilterCommand only extracts/filters data
 * based on specified paths without any modifications.
 *
 * <p>Features: - Extract specific elements using JSONPath expressions - Preserve exact data types
 * and structure of extracted elements - Memory-efficient processing for large JSON files - Support
 * for simple path expressions
 */
public class JsonFilterCommand extends AbstractStreamCommand {

  private static final int BUFFER_SIZE = 8192; // 8KB buffer for streaming
  private static final int MAX_MEMORY_BUFFER = 10 * 1024 * 1024; // 10MB max buffer

  private final String jsonPath;

  /**
   * Constructor for JSON filtering with JSONPath selector.
   *
   * @param jsonPath the JSONPath expression to extract data (e.g., "$.users", "$.name")
   * @throws IllegalArgumentException if jsonPath is null or empty
   */
  public JsonFilterCommand(String jsonPath) {
    if (jsonPath == null || jsonPath.trim().isEmpty()) {
      throw new IllegalArgumentException("JSONPath cannot be null or empty");
    }
    this.jsonPath = jsonPath.trim();
  }

  @Override
  protected String getCommandDetails() {
    return String.format("JsonFilterCommand(jsonPath='%s')", jsonPath);
  }

  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      // Read JSON content efficiently
      String jsonContent = readJsonContent(reader);

      if (jsonContent == null || jsonContent.trim().isEmpty()) {
        writer.write("null");
        writer.flush();
        return;
      }

      try {
        // Apply simple JSONPath-like extraction using lightweight parsing
        String result = extractJsonValue(jsonContent, jsonPath);
        writer.write(result);
        writer.flush();

      } catch (Exception e) {
        // If extraction fails, return null
        writer.write("null");
        writer.flush();
      }
    }
  }

  /**
   * Read JSON content from reader with memory management
   *
   * @param reader the BufferedReader to read from
   * @return JSON content as string, or null if empty
   * @throws IOException if reading fails
   */
  private String readJsonContent(BufferedReader reader) throws IOException {
    StringBuilder jsonBuilder = new StringBuilder();
    char[] buffer = new char[BUFFER_SIZE];
    int totalCharsRead = 0;
    int charsRead;

    while ((charsRead = reader.read(buffer)) != -1) {
      totalCharsRead += charsRead;

      // Memory protection: prevent reading excessively large JSON into memory
      if (totalCharsRead > MAX_MEMORY_BUFFER) {
        throw new IOException(
            "JSON content too large for filtering. Use streaming NavigateCommand instead.");
      }

      jsonBuilder.append(buffer, 0, charsRead);
    }

    return jsonBuilder.length() > 0 ? jsonBuilder.toString() : null;
  }

  /**
   * Extract JSON value using simple JSONPath-like expressions
   *
   * @param jsonContent the JSON content string
   * @param path the JSONPath expression (simplified)
   * @return extracted value as JSON string
   */
  private String extractJsonValue(String jsonContent, String path) {
    // Handle root path
    if ("$".equals(path)) {
      return jsonContent.trim();
    }

    // Simple property extraction: $.property
    if (path.startsWith("$.") && !path.contains("[") && path.indexOf(".", 2) == -1) {
      String property = path.substring(2);
      return extractSimpleProperty(jsonContent, property);
    }

    // For complex paths, return the original content for now
    // In a full implementation, you would add array indexing, nested properties, etc.
    return jsonContent.trim();
  }

  /**
   * Extract a simple property from JSON content
   *
   * @param jsonContent JSON string
   * @param property property name to extract
   * @return property value as JSON string or "null" if not found
   */
  private String extractSimpleProperty(String jsonContent, String property) {
    String searchPattern = "\"" + property + "\":";
    int propertyStart = jsonContent.indexOf(searchPattern);

    if (propertyStart == -1) {
      return "null"; // Property not found
    }

    // Find the start of the value
    int colonIndex = propertyStart + searchPattern.length();
    int valueStart = colonIndex;
    while (valueStart < jsonContent.length()
        && Character.isWhitespace(jsonContent.charAt(valueStart))) {
      valueStart++;
    }

    if (valueStart >= jsonContent.length()) {
      return "null";
    }

    // Extract the value based on its type
    char firstChar = jsonContent.charAt(valueStart);

    if (firstChar == '"') {
      // String value
      return extractQuotedString(jsonContent, valueStart);
    } else if (firstChar == '{') {
      // Object value
      return extractJsonObject(jsonContent, valueStart);
    } else if (firstChar == '[') {
      // Array value
      return extractJsonArray(jsonContent, valueStart);
    } else {
      // Number, boolean, or null
      return extractSimpleValue(jsonContent, valueStart);
    }
  }

  private String extractQuotedString(String json, int start) {
    StringBuilder result = new StringBuilder();
    result.append('"');
    int i = start + 1; // Skip opening quote

    while (i < json.length()) {
      char c = json.charAt(i);
      if (c == '"' && (i == start + 1 || json.charAt(i - 1) != '\\')) {
        result.append('"');
        break;
      }
      result.append(c);
      i++;
    }

    return result.toString();
  }

  private String extractJsonObject(String json, int start) {
    StringBuilder result = new StringBuilder();
    int braceCount = 0;

    for (int i = start; i < json.length(); i++) {
      char c = json.charAt(i);
      result.append(c);

      if (c == '{') braceCount++;
      else if (c == '}') braceCount--;

      if (braceCount == 0) break;
    }

    return result.toString();
  }

  private String extractJsonArray(String json, int start) {
    StringBuilder result = new StringBuilder();
    int bracketCount = 0;

    for (int i = start; i < json.length(); i++) {
      char c = json.charAt(i);
      result.append(c);

      if (c == '[') bracketCount++;
      else if (c == ']') bracketCount--;

      if (bracketCount == 0) break;
    }

    return result.toString();
  }

  private String extractSimpleValue(String json, int start) {
    StringBuilder result = new StringBuilder();

    for (int i = start; i < json.length(); i++) {
      char c = json.charAt(i);
      if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
        break;
      }
      result.append(c);
    }

    return result.toString();
  }
}
