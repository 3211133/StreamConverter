package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.command.rule.IRule;
import com.streamconverter.path.TreePath;
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
  private final JsonFactory jsonFactory;

  /**
   * Constructor for JSON navigation with TreePath selector and transformation rule.
   *
   * @param treePath the TreePath to select data
   * @param rule the transformation rule to apply to selected elements
   * @throws IllegalArgumentException if treePath or rule is null
   */
  private JsonNavigateCommand(TreePath treePath, IRule rule) {
    this.treePath = treePath;
    this.rule = rule;
    this.jsonFactory = new JsonFactory();
  }

  /**
   * Factory method for creating a JSON navigation command with TreePath and rule.
   *
   * @param treePath the TreePath to select data
   * @param rule the transformation rule to apply to selected elements
   * @return a JsonNavigateCommand that transforms the specified path with the given rule
   * @throws IllegalArgumentException if treePath or rule is null
   */
  public static JsonNavigateCommand create(TreePath treePath, IRule rule) {
    if (treePath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    return new JsonNavigateCommand(treePath, rule);
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    processJsonWithStreaming(inputStream, outputStream);
  }

  /** Stream JSON processing with structure preservation */
  private void processJsonWithStreaming(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    try (JsonParser parser = jsonFactory.createParser(inputStream);
        JsonGenerator generator = jsonFactory.createGenerator(outputStream)) {

      // Process with JSONPath filtering - transform matching elements
      processJsonStreamWithPath(parser, generator);

      generator.flush();
    }
  }

  /** Process JSON with specific JSONPath targeting */
  private void processJsonStreamWithPath(JsonParser parser, JsonGenerator generator)
      throws IOException {
    // depth tracks how many objects deep we are (0 = before root object)
    int depth = 0;
    // currentPath[i] holds the field name active at object-depth i+1
    List<String> currentPath = new ArrayList<>();
    JsonToken token;

    while ((token = parser.nextToken()) != null) {
      switch (token) {
        case FIELD_NAME:
          String fieldName = parser.currentName();
          // Ensure currentPath has a slot for depth (1-based object depth)
          while (currentPath.size() < depth) {
            currentPath.add(null);
          }
          while (currentPath.size() > depth) {
            currentPath.remove(currentPath.size() - 1);
          }
          if (depth > 0) {
            currentPath.set(depth - 1, fieldName);
          }
          generator.writeFieldName(fieldName);
          break;

        case VALUE_STRING:
          String originalValue = parser.getText();
          if (isMatchingPath(currentPath)) {
            String transformed;
            try {
              transformed = rule.apply(originalValue);
            } catch (RuntimeException ruleEx) {
              throw new IOException("Rule application failed at path " + currentPath, ruleEx);
            }
            generator.writeString(transformed);
          } else {
            generator.writeString(originalValue);
          }
          break;

        case START_OBJECT:
          depth++;
          generator.writeStartObject();
          break;

        case END_OBJECT:
          depth--;
          // Trim path to current depth
          while (currentPath.size() > depth) {
            currentPath.remove(currentPath.size() - 1);
          }
          generator.writeEndObject();
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
    // Use matchesIgnoringArraySyntax so that paths like $.orders[*].product_code
    // correctly match the streaming currentPath ["orders", "product_code"].
    return treePath.matchesIgnoringArraySyntax(currentPath);
  }
}
