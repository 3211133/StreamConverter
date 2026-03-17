package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.path.IPath;
import java.io.IOException;
import java.io.InputStream;
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
 * and structure of extracted elements - Loads full JSON tree into memory; use a streaming approach
 * for very large files - Support for simple path expressions
 */
public class JsonFilterCommand extends AbstractStreamCommand {

  private final IPath<List<String>> jsonPath;
  private final ObjectMapper objectMapper;

  /**
   * Constructor for JSON filtering with typed TreePath selector.
   *
   * @param jsonPath the typed TreePath to extract data
   * @throws IllegalArgumentException if jsonPath is null
   */
  private JsonFilterCommand(IPath<List<String>> jsonPath) {
    this.jsonPath = jsonPath;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Factory method for JSON filtering with typed path selector.
   *
   * @param jsonPath the typed path to extract data
   * @return a JsonFilterCommand instance
   * @throws IllegalArgumentException if jsonPath is null
   */
  public static JsonFilterCommand create(IPath<List<String>> jsonPath) {
    if (jsonPath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
    return new JsonFilterCommand(jsonPath);
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      JsonNode rootNode = objectMapper.readTree(inputStream);
      // Drain any remaining bytes to fully consume the input stream
      inputStream.transferTo(OutputStream.nullOutputStream());

      if (rootNode == null || rootNode.isNull()) {
        writer.write("null");
        writer.flush();
        return;
      }

      try {
        String result = extractJsonValue(rootNode, jsonPath.toString());
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
   * Extract JSON value using simple TreePath-like expressions
   *
   * @param rootNode the parsed JSON root node
   * @param path the TreePath expression (simplified)
   * @return extracted value as JSON string
   */
  private String extractJsonValue(JsonNode rootNode, String path) {
    try {

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
      return "null";
    }
  }
}
