package com.streamConverter.command.impl.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.command.rule.IRule;
import com.streamConverter.path.TreePath;
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

  private final TreePath treePath;
  private final IRule rule;
  private final ObjectMapper objectMapper;

  /**
   * Constructor for JSON navigation with TreePath selector and transformation rule.
   *
   * @param treePath the TreePath to select data
   * @param rule the transformation rule to apply to selected elements
   * @throws IllegalArgumentException if treePath or rule is null
   */
  public JsonNavigateCommand(TreePath treePath, IRule rule) {
    if (treePath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    this.treePath = treePath;
    this.rule = rule;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Factory method for creating a JSON navigation command with TreePath and rule.
   *
   * @param treePath the TreePath to select data
   * @param rule the transformation rule to apply to selected elements
   * @return a JsonNavigateCommand that transforms the specified path with the given rule
   * @throws IllegalArgumentException if rule is null
   */
  public static JsonNavigateCommand create(TreePath treePath, IRule rule) {
    return new JsonNavigateCommand(treePath, rule);
  }

  @Override
  protected String getCommandDetails() {
    return String.format(
        "JsonNavigateCommand(treePath='%s', rule='%s')",
        treePath.toString(), rule.getClass().getSimpleName());
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

      // Process with JSONPath filtering - transform matching elements
      processJsonStreamWithPath(parser, generator);

      generator.flush();
    } catch (com.fasterxml.jackson.core.JsonParseException e) {
      handleJsonParseException(outputStream, e);
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
          String fieldName = parser.currentName();

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
    return treePath.matches(currentPath);
  }

  /** Handle JSON parsing exceptions */
  private void handleJsonParseException(OutputStream outputStream, Exception e) throws IOException {
    String errorMessage = String.format("JSON parsing error: %s", e.getMessage());
    outputStream.write(errorMessage.getBytes());
    outputStream.flush();
  }
}
