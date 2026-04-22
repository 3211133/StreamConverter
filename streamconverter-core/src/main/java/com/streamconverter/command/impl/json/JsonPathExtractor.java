package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.util.List;

/**
 * Extracts values from a streaming JSON document according to a parsed path.
 *
 * <p>Uses Jackson's streaming API ({@link JsonParser}/{@link JsonGenerator}) to navigate to
 * matching path segments and write the corresponding values without loading the full document into
 * memory. Value copying and skipping are delegated to {@link JsonValueCopier}.
 */
final class JsonPathExtractor {

  private JsonPathExtractor() {}

  /**
   * Advance the parser to the next value and navigate {@code segments[segIdx..]} to write the
   * matching value(s) to {@code generator}.
   */
  static void extractPath(
      JsonParser parser, JsonGenerator generator, List<PathSegment> segments, int segIdx)
      throws IOException {
    if (segIdx >= segments.size()) {
      JsonValueCopier.copyValue(parser, generator);
      return;
    }

    PathSegment seg = segments.get(segIdx);
    JsonToken token = parser.nextToken();

    if (token == null) {
      generator.writeNull();
      return;
    }

    extractFromToken(parser, generator, segments, segIdx, token, seg);
  }

  /**
   * Same as {@link #extractPath} but the parser's current token has already been consumed and is
   * supplied as {@code currentToken}.
   */
  static void extractFromToken(
      JsonParser parser,
      JsonGenerator generator,
      List<PathSegment> segments,
      int segIdx,
      JsonToken currentToken)
      throws IOException {
    if (segIdx >= segments.size()) {
      JsonValueCopier.copyValue(parser, generator, currentToken);
      return;
    }
    extractFromToken(parser, generator, segments, segIdx, currentToken, segments.get(segIdx));
  }

  private static void extractFromToken(
      JsonParser parser,
      JsonGenerator generator,
      List<PathSegment> segments,
      int segIdx,
      JsonToken token,
      PathSegment seg)
      throws IOException {
    if (seg.isField()) {
      extractField(parser, generator, segments, segIdx, token, seg);
    } else if (seg.isWildcard) {
      extractWildcard(parser, generator, segments, segIdx, token);
    } else {
      extractIndex(parser, generator, segments, segIdx, token, seg);
    }
  }

  private static void extractField(
      JsonParser parser,
      JsonGenerator generator,
      List<PathSegment> segments,
      int segIdx,
      JsonToken token,
      PathSegment seg)
      throws IOException {
    if (token != JsonToken.START_OBJECT) {
      JsonValueCopier.skipValue(parser, token);
      generator.writeNull();
      return;
    }
    boolean found = false;
    JsonToken t;
    while ((t = parser.nextToken()) != null && t != JsonToken.END_OBJECT) {
      String name = parser.currentName();
      if (seg.fieldName.equals(name)) {
        extractPath(parser, generator, segments, segIdx + 1);
        found = true;
      } else {
        parser.nextToken();
        JsonValueCopier.skipValue(parser, parser.currentToken());
      }
    }
    if (!found) {
      generator.writeNull();
    }
  }

  private static void extractWildcard(
      JsonParser parser,
      JsonGenerator generator,
      List<PathSegment> segments,
      int segIdx,
      JsonToken token)
      throws IOException {
    if (token != JsonToken.START_ARRAY) {
      JsonValueCopier.skipValue(parser, token);
      generator.writeNull();
      return;
    }
    generator.writeStartArray();
    JsonToken elemToken;
    while ((elemToken = parser.nextToken()) != null && elemToken != JsonToken.END_ARRAY) {
      if (segIdx + 1 >= segments.size()) {
        JsonValueCopier.copyValue(parser, generator, elemToken);
      } else {
        extractFromToken(parser, generator, segments, segIdx + 1, elemToken);
      }
    }
    generator.writeEndArray();
  }

  private static void extractIndex(
      JsonParser parser,
      JsonGenerator generator,
      List<PathSegment> segments,
      int segIdx,
      JsonToken token,
      PathSegment seg)
      throws IOException {
    if (token != JsonToken.START_ARRAY) {
      JsonValueCopier.skipValue(parser, token);
      generator.writeNull();
      return;
    }
    int currentIdx = 0;
    boolean found = false;
    JsonToken arrToken;
    while ((arrToken = parser.nextToken()) != null && arrToken != JsonToken.END_ARRAY) {
      if (currentIdx == seg.arrayIndex) {
        extractPath(parser, generator, segments, segIdx + 1);
        found = true;
        JsonToken skipToken;
        while ((skipToken = parser.nextToken()) != null && skipToken != JsonToken.END_ARRAY) {
          JsonValueCopier.skipValue(parser, skipToken);
        }
        break;
      } else {
        JsonValueCopier.skipValue(parser, arrToken);
      }
      currentIdx++;
    }
    if (!found) {
      generator.writeNull();
    }
  }
}
