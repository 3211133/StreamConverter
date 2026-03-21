package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.path.IPath;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON Filter Command Class
 *
 * <p>This class implements pure data extraction from JSON using TreePath expressions. Unlike
 * JsonNavigateCommand which applies transformations, JsonFilterCommand only extracts/filters data
 * based on specified paths without any modifications.
 *
 * <p>Features: - Extract specific elements using TreePath expressions - Preserve exact data types
 * and structure of extracted elements - Stream processing for memory efficiency using Jackson
 * Streaming API - Support for simple path expressions
 */
public class JsonFilterCommand extends AbstractStreamCommand {

  private final IPath<List<String>> jsonPath;
  private final ObjectMapper objectMapper;

  // Path segment types for streaming JSON navigation
  private sealed interface PathSegment
      permits PropertySegment, ArrayWildcardSegment, ArrayIndexSegment {}

  private record PropertySegment(String name) implements PathSegment {}

  private record ArrayWildcardSegment() implements PathSegment {}

  private record ArrayIndexSegment(int index) implements PathSegment {}

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
    JsonFactory jsonFactory = objectMapper.getFactory();
    try (JsonParser parser = jsonFactory.createParser(inputStream);
        JsonGenerator generator = jsonFactory.createGenerator(outputStream)) {

      List<PathSegment> segments = parsePath(jsonPath.toString());
      JsonToken firstToken = parser.nextToken();
      if (firstToken == null) {
        generator.writeNull();
      } else {
        extractValue(parser, generator, segments, 0);
        // Consume any remaining tokens to ensure the InputStream is fully drained
        while (parser.nextToken() != null) {
          // discard
        }
      }
      generator.flush();
    }
  }

  /**
   * Parse a JSONPath string into a list of structured path segments.
   *
   * @param path the JSONPath string (e.g. "$.users[*].name")
   * @return list of path segments describing how to navigate the JSON
   */
  private List<PathSegment> parsePath(String path) {
    List<PathSegment> segments = new ArrayList<>();
    if ("$".equals(path)) {
      return segments;
    }

    // Remove the leading '$'
    String rest = path.startsWith("$") ? path.substring(1) : path;
    int i = 0;
    while (i < rest.length()) {
      char c = rest.charAt(i);
      if (c == '.') {
        // Property segment: read name until the next '.' or '['
        int start = i + 1;
        int end = start;
        while (end < rest.length() && rest.charAt(end) != '.' && rest.charAt(end) != '[') {
          end++;
        }
        if (end > start) {
          segments.add(new PropertySegment(rest.substring(start, end)));
        }
        i = end;
      } else if (c == '[') {
        // Array segment: [*] for wildcard or [n] for a specific index
        int end = rest.indexOf(']', i);
        if (end == -1) break;
        String indexStr = rest.substring(i + 1, end);
        if ("*".equals(indexStr)) {
          segments.add(new ArrayWildcardSegment());
        } else {
          try {
            segments.add(new ArrayIndexSegment(Integer.parseInt(indexStr)));
          } catch (NumberFormatException e) {
            // Skip unrecognized index syntax
          }
        }
        i = end + 1;
      } else {
        i++;
      }
    }
    return segments;
  }

  /**
   * Recursively extract the value at the given path from the current parser position.
   *
   * <p>The parser must be positioned at the first token of the current value on entry.
   * {@link JsonGenerator#copyCurrentStructure} advances the parser through the entire structure, so
   * on return the parser is positioned at the last token of the consumed value (END_OBJECT,
   * END_ARRAY, or the primitive value token).
   *
   * @param parser the JSON parser positioned at the first token of the current value
   * @param generator the JSON generator to write the extracted value to
   * @param segments the complete list of path segments
   * @param segIndex the index of the segment currently being evaluated
   * @throws IOException if a read or write error occurs
   */
  private void extractValue(
      JsonParser parser, JsonGenerator generator, List<PathSegment> segments, int segIndex)
      throws IOException {
    if (segIndex == segments.size()) {
      // Reached the target: copy this value as-is to the output
      generator.copyCurrentStructure(parser);
      return;
    }

    PathSegment seg = segments.get(segIndex);
    JsonToken token = parser.currentToken();

    if (seg instanceof PropertySegment ps) {
      if (token != JsonToken.START_OBJECT) {
        skipCurrentValue(parser);
        generator.writeNull();
        return;
      }
      boolean found = false;
      while (parser.nextToken() != JsonToken.END_OBJECT) {
        String fieldName = parser.currentName();
        parser.nextToken(); // advance to the field value
        if (!found && ps.name().equals(fieldName)) {
          found = true;
          extractValue(parser, generator, segments, segIndex + 1);
        } else {
          skipCurrentValue(parser);
        }
      }
      if (!found) {
        generator.writeNull();
      }

    } else if (seg instanceof ArrayWildcardSegment) {
      if (token != JsonToken.START_ARRAY) {
        skipCurrentValue(parser);
        generator.writeNull();
        return;
      }
      generator.writeStartArray();
      while (parser.nextToken() != JsonToken.END_ARRAY) {
        extractValue(parser, generator, segments, segIndex + 1);
      }
      generator.writeEndArray();

    } else if (seg instanceof ArrayIndexSegment ais) {
      if (token != JsonToken.START_ARRAY) {
        skipCurrentValue(parser);
        generator.writeNull();
        return;
      }
      int currentIndex = 0;
      boolean found = false;
      while (parser.nextToken() != JsonToken.END_ARRAY) {
        if (currentIndex == ais.index()) {
          found = true;
          extractValue(parser, generator, segments, segIndex + 1);
          // Skip remaining elements and advance to END_ARRAY
          while (parser.nextToken() != JsonToken.END_ARRAY) {
            skipCurrentValue(parser);
          }
          break;
        } else {
          skipCurrentValue(parser);
        }
        currentIndex++;
      }
      if (!found) {
        generator.writeNull();
      }
    }
  }

  /**
   * Skip the current value in the parser, consuming all tokens for objects and arrays.
   *
   * <p>On return the parser is positioned at the last token of the skipped value (END_OBJECT,
   * END_ARRAY, or the primitive value token).
   *
   * @param parser the JSON parser positioned at the first token of the value to skip
   * @throws IOException if a read error occurs
   */
  private void skipCurrentValue(JsonParser parser) throws IOException {
    JsonToken token = parser.currentToken();
    if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
      parser.skipChildren();
    }
    // Primitive values require no action; the cursor stays at the value token
  }
}
