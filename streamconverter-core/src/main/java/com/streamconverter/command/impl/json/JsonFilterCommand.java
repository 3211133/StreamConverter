package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
 * and structure of extracted elements - Streaming processing via Jackson Streaming API
 * (JsonParser/JsonGenerator); the document is never fully loaded into memory - Support for simple
 * path expressions including wildcards ($[*].field, $.array[*].nested.field)
 */
public class JsonFilterCommand extends AbstractStreamCommand {

  private final IPath<List<String>> jsonPath;
  private final JsonFactory jsonFactory;

  /**
   * Constructor for JSON filtering with typed TreePath selector.
   *
   * @param jsonPath the typed TreePath to extract data
   * @throws IllegalArgumentException if jsonPath is null
   */
  private JsonFilterCommand(IPath<List<String>> jsonPath) {
    super();
    this.jsonPath = jsonPath;
    this.jsonFactory = new JsonFactory();
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
    String path = jsonPath.toString();
    List<PathSegment> segments = parsePath(path);

    try (JsonParser parser = jsonFactory.createParser(inputStream);
        JsonGenerator generator = jsonFactory.createGenerator(outputStream)) {
      generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

      if (segments.isEmpty()) {
        // Root path "$": copy the entire document
        copyValue(parser, generator);
      } else {
        extractPath(parser, generator, segments, 0);
      }

      generator.flush();
    }
  }

  // -------------------------------------------------------------------------
  // Path parsing
  // -------------------------------------------------------------------------

  /** A single segment in a parsed path. */
  private static class PathSegment {

    final String fieldName; // non-null for field access
    final boolean isWildcard; // true for [*]
    final int arrayIndex; // >= 0 for numeric index, -1 otherwise

    static PathSegment field(String name) {
      return new PathSegment(name, false, -1);
    }

    static PathSegment wildcard() {
      return new PathSegment(null, true, -1);
    }

    static PathSegment index(int i) {
      return new PathSegment(null, false, i);
    }

    private PathSegment(String fieldName, boolean isWildcard, int arrayIndex) {
      this.fieldName = fieldName;
      this.isWildcard = isWildcard;
      this.arrayIndex = arrayIndex;
    }

    boolean isField() {
      return fieldName != null;
    }
  }

  /**
   * Parse a path string into an ordered list of {@link PathSegment}s.
   *
   * <p>Supported syntax:
   *
   * <ul>
   *   <li>{@code $} – root (empty list)
   *   <li>{@code $.name} – top-level field
   *   <li>{@code $[*].name} – root-array wildcard then field
   *   <li>{@code $.users[*].profile.department} – field, wildcard, nested fields
   * </ul>
   *
   * @param path the path expression
   * @return ordered list of path segments (empty = root)
   */
  private static List<PathSegment> parsePath(String path) {
    List<PathSegment> result = new ArrayList<>();
    if ("$".equals(path)) {
      return result;
    }

    // Strip leading "$" then process the remaining characters
    int start = path.startsWith("$") ? 1 : 0;
    String rest = path.substring(start);

    int i = 0;
    while (i < rest.length()) {
      char c = rest.charAt(i);
      if (c == '.') {
        i++; // skip dot separator
      } else if (c == '[') {
        // Array access: [*] or [N]
        int close = rest.indexOf(']', i);
        if (close == -1) {
          break; // malformed – stop here
        }
        String inner = rest.substring(i + 1, close);
        if ("*".equals(inner)) {
          result.add(PathSegment.wildcard());
        } else {
          try {
            result.add(PathSegment.index(Integer.parseInt(inner)));
          } catch (NumberFormatException e) {
            result.add(PathSegment.wildcard()); // treat unknown as wildcard
          }
        }
        i = close + 1;
      } else {
        // Field name: read until '.', '[', or end
        int end = i;
        while (end < rest.length() && rest.charAt(end) != '.' && rest.charAt(end) != '[') {
          end++;
        }
        String fieldName = rest.substring(i, end);
        if (!fieldName.isEmpty()) {
          result.add(PathSegment.field(fieldName));
        }
        i = end;
      }
    }
    return result;
  }

  // -------------------------------------------------------------------------
  // Streaming extraction
  // -------------------------------------------------------------------------

  /**
   * Advance the parser to the next value and recursively navigate {@code segments[segIdx..]} to
   * write the matching value(s) to {@code generator}.
   *
   * @param parser the JSON parser (not yet advanced to the target value)
   * @param generator the JSON generator
   * @param segments the full path segment list
   * @param segIdx current position in {@code segments}
   */
  private void extractPath(
      JsonParser parser, JsonGenerator generator, List<PathSegment> segments, int segIdx)
      throws IOException {

    if (segIdx >= segments.size()) {
      copyValue(parser, generator);
      return;
    }

    PathSegment seg = segments.get(segIdx);
    JsonToken token = parser.nextToken();

    if (token == null) {
      generator.writeNull();
      return;
    }

    if (seg.isField()) {
      // Expect an object; find the named field
      if (token != JsonToken.START_OBJECT) {
        skipValue(parser, token);
        generator.writeNull();
        return;
      }
      boolean found = false;
      while (true) {
        JsonToken t = parser.nextToken();
        if (t == null || t == JsonToken.END_OBJECT) {
          break;
        }
        String name = parser.currentName();
        if (seg.fieldName.equals(name)) {
          extractPath(parser, generator, segments, segIdx + 1);
          found = true;
        } else {
          parser.nextToken();
          skipValue(parser, parser.currentToken());
        }
      }
      if (!found) {
        generator.writeNull();
      }
    } else if (seg.isWildcard) {
      // Expect an array; iterate elements and extract from each
      if (token != JsonToken.START_ARRAY) {
        skipValue(parser, token);
        generator.writeNull();
        return;
      }
      generator.writeStartArray();
      while (true) {
        JsonToken elemToken = parser.nextToken();
        if (elemToken == null || elemToken == JsonToken.END_ARRAY) {
          break;
        }
        if (segIdx + 1 >= segments.size()) {
          copyValue(parser, generator, elemToken);
        } else {
          extractFromToken(parser, generator, segments, segIdx + 1, elemToken);
        }
      }
      generator.writeEndArray();
    } else {
      // Numeric index access
      if (token != JsonToken.START_ARRAY) {
        skipValue(parser, token);
        generator.writeNull();
        return;
      }
      int currentIdx = 0;
      boolean found = false;
      while (true) {
        JsonToken arrToken = parser.nextToken();
        if (arrToken == null || arrToken == JsonToken.END_ARRAY) {
          break;
        }
        if (currentIdx == seg.arrayIndex) {
          extractPath(parser, generator, segments, segIdx + 1);
          found = true;
          while (true) {
            JsonToken skipToken = parser.nextToken();
            if (skipToken == null || skipToken == JsonToken.END_ARRAY) {
              break;
            }
            skipValue(parser, skipToken);
          }
          break;
        } else {
          skipValue(parser, arrToken);
        }
        currentIdx++;
      }
      if (!found) {
        generator.writeNull();
      }
    }
  }

  /**
   * Same as {@link #extractPath} but the parser's current token has already been consumed and is
   * supplied as {@code currentToken}. Used when iterating array elements where {@code nextToken()}
   * was already called to detect {@code END_ARRAY}.
   */
  private void extractFromToken(
      JsonParser parser,
      JsonGenerator generator,
      List<PathSegment> segments,
      int segIdx,
      JsonToken currentToken)
      throws IOException {

    if (segIdx >= segments.size()) {
      copyValue(parser, generator, currentToken);
      return;
    }

    PathSegment seg = segments.get(segIdx);

    if (seg.isField()) {
      if (currentToken != JsonToken.START_OBJECT) {
        skipValue(parser, currentToken);
        generator.writeNull();
        return;
      }
      boolean found = false;
      while (true) {
        JsonToken t2 = parser.nextToken();
        if (t2 == null || t2 == JsonToken.END_OBJECT) {
          break;
        }
        String name = parser.currentName();
        if (seg.fieldName.equals(name)) {
          extractPath(parser, generator, segments, segIdx + 1);
          found = true;
        } else {
          parser.nextToken();
          skipValue(parser, parser.currentToken());
        }
      }
      if (!found) {
        generator.writeNull();
      }
    } else if (seg.isWildcard) {
      if (currentToken != JsonToken.START_ARRAY) {
        skipValue(parser, currentToken);
        generator.writeNull();
        return;
      }
      generator.writeStartArray();
      while (true) {
        JsonToken elemToken2 = parser.nextToken();
        if (elemToken2 == null || elemToken2 == JsonToken.END_ARRAY) {
          break;
        }
        if (segIdx + 1 >= segments.size()) {
          copyValue(parser, generator, elemToken2);
        } else {
          extractFromToken(parser, generator, segments, segIdx + 1, elemToken2);
        }
      }
      generator.writeEndArray();
    } else {
      if (currentToken != JsonToken.START_ARRAY) {
        skipValue(parser, currentToken);
        generator.writeNull();
        return;
      }
      int currentIdx = 0;
      boolean found = false;
      while (true) {
        JsonToken arrToken2 = parser.nextToken();
        if (arrToken2 == null || arrToken2 == JsonToken.END_ARRAY) {
          break;
        }
        if (currentIdx == seg.arrayIndex) {
          extractPath(parser, generator, segments, segIdx + 1);
          found = true;
          while (true) {
            JsonToken skipToken2 = parser.nextToken();
            if (skipToken2 == null || skipToken2 == JsonToken.END_ARRAY) {
              break;
            }
            skipValue(parser, skipToken2);
          }
          break;
        } else {
          skipValue(parser, arrToken2);
        }
        currentIdx++;
      }
      if (!found) {
        generator.writeNull();
      }
    }
  }

  // -------------------------------------------------------------------------
  // Copy / skip helpers
  // -------------------------------------------------------------------------

  /**
   * Copy the next complete value from {@code parser} to {@code generator}. Advances the parser by
   * one token internally.
   */
  private void copyValue(JsonParser parser, JsonGenerator generator) throws IOException {
    JsonToken token = parser.nextToken();
    if (token == null) {
      generator.writeNull();
      return;
    }
    copyValue(parser, generator, token);
  }

  /**
   * Copy a complete value starting at {@code token} (already read) from {@code parser} to {@code
   * generator}.
   */
  private void copyValue(JsonParser parser, JsonGenerator generator, JsonToken token)
      throws IOException {
    switch (token) {
      case START_OBJECT:
        generator.writeStartObject();
        while (true) {
          JsonToken objToken = parser.nextToken();
          if (objToken == null || objToken == JsonToken.END_OBJECT) {
            break;
          }
          generator.writeFieldName(parser.currentName());
          copyValue(parser, generator);
        }
        generator.writeEndObject();
        break;

      case START_ARRAY:
        generator.writeStartArray();
        while (true) {
          JsonToken t = parser.nextToken();
          if (t == JsonToken.END_ARRAY) {
            break;
          }
          copyValue(parser, generator, t);
        }
        generator.writeEndArray();
        break;

      case VALUE_STRING:
        generator.writeString(parser.getText());
        break;

      case VALUE_NUMBER_INT:
        generator.writeNumber(parser.getLongValue());
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

      default:
        generator.writeNull();
        break;
    }
  }

  /**
   * Skip over a complete value starting at {@code token} (already read). Does not write anything.
   */
  private void skipValue(JsonParser parser, JsonToken token) throws IOException {
    if (token == null) {
      return;
    }
    switch (token) {
      case START_OBJECT:
        int objDepth = 1;
        while (objDepth > 0) {
          JsonToken t = parser.nextToken();
          if (t == JsonToken.START_OBJECT) {
            objDepth++;
          } else if (t == JsonToken.END_OBJECT) {
            objDepth--;
          }
        }
        break;

      case START_ARRAY:
        int arrDepth = 1;
        while (arrDepth > 0) {
          JsonToken t = parser.nextToken();
          if (t == JsonToken.START_ARRAY) {
            arrDepth++;
          } else if (t == JsonToken.END_ARRAY) {
            arrDepth--;
          }
        }
        break;

      default:
        // Scalar values are self-contained – nothing more to skip
        break;
    }
  }
}
