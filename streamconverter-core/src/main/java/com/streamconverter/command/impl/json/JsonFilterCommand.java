package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.path.IPath;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * JSON Filter Command Class
 *
 * <p>This class implements pure data extraction from JSON using TreePath expressions. Unlike
 * JsonNavigateCommand which applies transformations, JsonFilterCommand only extracts/filters data
 * based on specified paths without any modifications.
 *
 * <p>Features: - Extract specific elements using TreePath expressions - Preserve exact data types
 * and structure of extracted elements - Memory-efficient processing for large JSON files - Support
 * for simple path expressions
 */
public class JsonFilterCommand extends AbstractStreamCommand {

  private static final int BUFFER_SIZE = 8192; // 8KB buffer for streaming
  private static final int MAX_MEMORY_BUFFER = 10 * 1024 * 1024; // 10MB max buffer

  private final IPath<List<String>> jsonPath;
  private final ObjectMapper objectMapper;

  /**
   * Constructor for JSON filtering with typed TreePath selector.
   *
   * @param jsonPath the typed TreePath to extract data
   * @throws IllegalArgumentException if jsonPath is null
   */
  public JsonFilterCommand(IPath<List<String>> jsonPath) {
    if (jsonPath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
    this.jsonPath = jsonPath;
    jsonPath.toString();
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      // Read JSON content efficiently
      String jsonContent = readJsonContent(reader);

      if (jsonContent.trim().isEmpty()) {
        writer.write("null");
        writer.flush();
        return;
      }

      try {
        // Apply simple TreePath-like extraction using lightweight parsing
        String result = extractJsonValue(jsonContent, jsonPath.toString());
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
   * @return JSON content as string, or empty string if no content
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

    return jsonBuilder.toString(); // Return empty string instead of null
  }

  /**
   * Extract JSON value using simple TreePath-like expressions
   *
   * @param jsonContent the JSON content string
   * @param path the TreePath expression (simplified)
   * @return extracted value as JSON string
   */
  private String extractJsonValue(String jsonContent, String path) {
    try {
      JsonNode rootNode = objectMapper.readTree(jsonContent);

      // Handle root path
      if ("$".equals(path)) {
        return objectMapper.writeValueAsString(rootNode);
      }

      // Handle special case of $[*].property (root array with wildcard)
      if (path.startsWith("$[*].") && path.length() > 5) {
        String propertyPath = path.substring(5); // Remove "$[*]."
        if (rootNode.isArray()) {
          StringBuilder resultBuilder = new StringBuilder("[");
          boolean first = true;
          for (JsonNode arrayElement : rootNode) {
            if (!first) resultBuilder.append(",");
            first = false;

            // Apply the property path to each array element
            String[] propertySegments = propertyPath.split("\\.");
            JsonNode extractedNode = arrayElement;
            for (String segment : propertySegments) {
              if (segment.isEmpty()) continue;
              extractedNode = extractedNode.get(segment);
              if (extractedNode == null) {
                extractedNode = objectMapper.getNodeFactory().nullNode();
                break;
              }
            }
            resultBuilder.append(objectMapper.writeValueAsString(extractedNode));
          }
          resultBuilder.append("]");
          return resultBuilder.toString();
        } else {
          return "null";
        }
      }

      // Remove the '$.' prefix if present
      String normalizedPath = path.startsWith("$.") ? path.substring(2) : path;

      // Navigate through the path
      JsonNode currentNode = rootNode;
      String[] pathSegments = normalizedPath.split("\\.");

      for (String segment : pathSegments) {
        if (segment.isEmpty()) continue;

        // Handle array indexing (e.g., "users[0]")
        if (segment.contains("[") && segment.endsWith("]")) {
          String fieldName = segment.substring(0, segment.indexOf("["));
          String indexStr = segment.substring(segment.indexOf("[") + 1, segment.indexOf("]"));

          if (!fieldName.isEmpty()) {
            currentNode = currentNode.get(fieldName);
            if (currentNode == null) return "null";
          }

          // Handle wildcard array access [*]
          if ("*".equals(indexStr)) {
            if (currentNode.isArray()) {
              // For wildcard, we need to handle subsequent path segments differently
              // This is a simplified implementation that extracts all matching elements
              StringBuilder resultBuilder = new StringBuilder("[");
              boolean first = true;
              for (JsonNode arrayElement : currentNode) {
                if (!first) resultBuilder.append(",");
                first = false;

                // If there are more path segments, apply them to each array element
                String remainingPath =
                    String.join(
                        ".",
                        Arrays.copyOfRange(
                            pathSegments,
                            Arrays.asList(pathSegments).indexOf(segment) + 1,
                            pathSegments.length));
                if (!remainingPath.isEmpty()) {
                  JsonNode extractedNode = arrayElement;
                  String[] remainingSegments = remainingPath.split("\\.");
                  for (String remainingSeg : remainingSegments) {
                    if (remainingSeg.isEmpty()) continue;
                    extractedNode = extractedNode.get(remainingSeg);
                    if (extractedNode == null) {
                      extractedNode = objectMapper.getNodeFactory().nullNode();
                      break;
                    }
                  }
                  resultBuilder.append(objectMapper.writeValueAsString(extractedNode));
                } else {
                  resultBuilder.append(objectMapper.writeValueAsString(arrayElement));
                }
              }
              resultBuilder.append("]");
              return resultBuilder.toString();
            } else {
              return "null";
            }
          } else {
            // Regular array indexing
            try {
              int index = Integer.parseInt(indexStr);
              if (currentNode.isArray() && index >= 0 && index < currentNode.size()) {
                currentNode = currentNode.get(index);
              } else {
                return "null";
              }
            } catch (NumberFormatException e) {
              return "null";
            }
          }
        } else {
          // Simple property access
          currentNode = currentNode.get(segment);
          if (currentNode == null) {
            return "null";
          }
        }
      }

      return objectMapper.writeValueAsString(currentNode);
    } catch (Exception e) {
      // If JSON parsing fails, fall back to simple string-based extraction
      return extractSimplePropertyFallback(jsonContent, path);
    }
  }

  /**
   * Fallback method for simple string-based extraction when JSON parsing fails
   *
   * @param jsonContent the JSON content string
   * @param path the TreePath expression
   * @return extracted value as JSON string or original content
   */
  private String extractSimplePropertyFallback(String jsonContent, String path) {
    // Handle root path
    if ("$".equals(path)) {
      return jsonContent.trim();
    }

    // Simple property extraction: $.property
    if (path.startsWith("$.") && !path.contains("[") && path.indexOf(".", 2) == -1) {
      String property = path.substring(2);
      return extractSimpleProperty(jsonContent, property);
    }

    // For other complex paths that couldn't be parsed, return null instead of original content
    return "null";
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
