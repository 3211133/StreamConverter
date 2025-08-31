package com.streamConverter.command.impl.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.command.rule.IRule;
import com.streamConverter.path.JSONPath;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON Navigation Command for applying transformations to JSON data
 *
 * <p>This command navigates through JSON structures and applies transformations using rules while
 * preserving the overall JSON structure. It focuses purely on navigation and transformation, not
 * extraction.
 *
 * <p>Responsibilities: - Navigate to specified JSON paths - Apply transformation rules to matching
 * elements - Preserve JSON structure during transformation - Stream processing for memory
 * efficiency
 */
public class JsonNavigateCommand extends AbstractStreamCommand {

  private final JSONPath jsonPath;
  private final IRule rule;
  private final ObjectMapper objectMapper;

  /**
   * Constructor for JSON navigation with typed JSONPath selector and transformation rule.
   *
   * @param jsonPath the typed JSONPath to select data (null for entire JSON processing)
   * @param rule the transformation rule to apply to selected elements
   * @throws IllegalArgumentException if rule is null
   */
  public JsonNavigateCommand(JSONPath jsonPath, IRule rule) {
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    this.jsonPath = jsonPath;
    this.rule = rule;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Factory method for creating a JSON navigation command with typed JSONPath and rule.
   *
   * @param jsonPath the typed JSONPath to select data
   * @param rule the transformation rule to apply to selected elements
   * @return a JsonNavigateCommand that transforms the specified path with the given rule
   * @throws IllegalArgumentException if rule is null
   */
  public static JsonNavigateCommand create(JSONPath jsonPath, IRule rule) {
    return new JsonNavigateCommand(jsonPath, rule);
  }

  /**
   * Factory method for creating a JSON navigation command that processes entire JSON with the given
   * rule.
   *
   * @param rule the transformation rule to apply to all string values
   * @return a JsonNavigateCommand that processes entire JSON with the given rule
   * @throws IllegalArgumentException if rule is null
   */
  public static JsonNavigateCommand createForAll(IRule rule) {
    return new JsonNavigateCommand(null, rule);
  }

  @Override
  protected String getCommandDetails() {
    String pathInfo =
        jsonPath != null ? String.format("jsonPath='%s'", jsonPath.toString()) : "entire JSON";
    return String.format(
        "JsonNavigateCommand(%s, rule='%s')", pathInfo, rule.getClass().getSimpleName());
  }

  @Override
  protected void executeInternal(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    processJsonWithStreaming(inputStream, outputStream);
  }

  /** Stream JSON processing with structure preservation */
  private void processJsonWithStreaming(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    JsonFactory jsonFactory = objectMapper.getFactory();

    try (JsonParser parser = jsonFactory.createParser(inputStream);
        JsonGenerator generator = jsonFactory.createGenerator(outputStream)) {

      if (jsonPath == null) {
        // Process entire JSON stream - transform all string values
        processEntireJsonStream(parser, generator);
      } else {
        // Process with JSONPath filtering - transform matching elements
        processJsonStreamWithPath(parser, generator);
      }

      generator.flush();
    } catch (com.fasterxml.jackson.core.JsonParseException e) {
      handleJsonParseException(outputStream, e);
    }
  }

  /** Process entire JSON and apply rule to all string values */
  private void processEntireJsonStream(JsonParser parser, JsonGenerator generator)
      throws IOException {
    JsonToken token;
    while ((token = parser.nextToken()) != null) {
      switch (token) {
        case VALUE_STRING:
          String transformedValue = rule.apply(parser.getText());
          generator.writeString(transformedValue);
          break;
        case FIELD_NAME:
          generator.writeFieldName(parser.getCurrentName());
          break;
        case START_OBJECT:
          generator.writeStartObject();
          break;
        case END_OBJECT:
          generator.writeEndObject();
          break;
        case START_ARRAY:
          generator.writeStartArray();
          break;
        case END_ARRAY:
          generator.writeEndArray();
          break;
        case VALUE_NUMBER_INT:
          generator.writeNumber(parser.getIntValue());
          break;
        case VALUE_NUMBER_FLOAT:
          generator.writeNumber(parser.getDoubleValue());
          break;
        case VALUE_TRUE:
          generator.writeBoolean(true);
          break;
        case VALUE_FALSE:
          generator.writeBoolean(false);
          break;
        case VALUE_NULL:
          generator.writeNull();
          break;
        default:
          // Copy other tokens as-is
          generator.copyCurrentEvent(parser);
          break;
      }
    }
  }

  /** Process JSON with specific JSONPath targeting */
  private void processJsonStreamWithPath(JsonParser parser, JsonGenerator generator)
      throws IOException {
    List<String> currentPath = new ArrayList<>();
    JsonToken token;

    while ((token = parser.nextToken()) != null) {
      switch (token) {
        case FIELD_NAME:
          String fieldName = parser.getCurrentName();

          // Reset path for new field at current level
          if (!currentPath.isEmpty() && currentPath.size() > 1) {
            // Remove previous sibling field from path
            currentPath.set(currentPath.size() - 1, fieldName);
          } else {
            // Clear and add current field
            currentPath.clear();
            currentPath.add(fieldName);
          }

          generator.writeFieldName(fieldName);
          break;

        case VALUE_STRING:
          String originalValue = parser.getText();
          boolean inTargetPath = isMatchingPath(currentPath);
          if (inTargetPath) {
            String transformedValue = rule.apply(originalValue);
            generator.writeString(transformedValue);
          } else {
            generator.writeString(originalValue);
          }
          break;

        case START_OBJECT:
          generator.writeStartObject();
          // Don't modify path on object start
          break;

        case END_OBJECT:
          generator.writeEndObject();
          // Remove one level from path
          if (currentPath.size() > 1) {
            currentPath.remove(currentPath.size() - 1);
          }
          break;

        case START_ARRAY:
          generator.writeStartArray();
          break;

        case END_ARRAY:
          generator.writeEndArray();
          break;

        default:
          // Copy all other tokens as-is (numbers, booleans, null)
          generator.copyCurrentEvent(parser);
          break;
      }
    }
  }

  /** Simple path matching for streaming JSON processing */
  private boolean isMatchingPath(List<String> currentPath) {
    if (jsonPath == null || currentPath.isEmpty()) {
      return false;
    }

    // For simple paths like "$.propertyName", match last element
    if (currentPath.size() == 1) {
      com.fasterxml.jackson.databind.node.ObjectNode testNode = objectMapper.createObjectNode();
      testNode.put(currentPath.get(0), "test");
      boolean matches = jsonPath.matches(testNode);
      return matches;
    }

    return false;
  }

  /** Handle JSON parsing exceptions */
  private void handleJsonParseException(OutputStream outputStream, Exception e) throws IOException {
    String errorMessage = String.format("JSON parsing error: %s", e.getMessage());
    outputStream.write(errorMessage.getBytes());
    outputStream.flush();
  }
}
