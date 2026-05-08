package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;

/**
 * Copies or skips complete JSON values using Jackson's streaming API.
 *
 * <p>Used by {@link JsonExtractCommand} for streaming value copy and skip operations.
 */
final class JsonValueCopier {

  private JsonValueCopier() {}

  /** Copy the next complete value from {@code parser} to {@code generator}. */
  static void copyValue(JsonParser parser, JsonGenerator generator) throws IOException {
    JsonToken token = parser.nextToken();
    if (token == null) {
      generator.writeNull();
      return;
    }
    copyValue(parser, generator, token);
  }

  /** Copy a complete value starting at {@code token} (already read). */
  static void copyValue(JsonParser parser, JsonGenerator generator, JsonToken token)
      throws IOException {
    switch (token) {
      case START_OBJECT -> copyObject(parser, generator);
      case START_ARRAY -> copyArray(parser, generator);
      case VALUE_STRING -> generator.writeString(parser.getText());
      case VALUE_NUMBER_INT -> generator.writeNumber(parser.getLongValue());
      case VALUE_NUMBER_FLOAT -> generator.writeNumber(parser.getDoubleValue());
      case VALUE_TRUE -> generator.writeBoolean(true);
      case VALUE_FALSE -> generator.writeBoolean(false);
      default -> generator.writeNull();
    }
  }

  private static void copyObject(JsonParser parser, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    JsonToken t;
    while ((t = parser.nextToken()) != null && t != JsonToken.END_OBJECT) {
      generator.writeFieldName(parser.currentName());
      copyValue(parser, generator);
    }
    generator.writeEndObject();
  }

  private static void copyArray(JsonParser parser, JsonGenerator generator) throws IOException {
    generator.writeStartArray();
    JsonToken t;
    while ((t = parser.nextToken()) != null && t != JsonToken.END_ARRAY) {
      copyValue(parser, generator, t);
    }
    generator.writeEndArray();
  }

  /** Skip over a complete value starting at {@code token} (already read). */
  static void skipValue(JsonParser parser, JsonToken token) throws IOException {
    if (token == null) {
      return;
    }
    switch (token) {
      case START_OBJECT -> skipNested(parser, JsonToken.START_OBJECT, JsonToken.END_OBJECT);
      case START_ARRAY -> skipNested(parser, JsonToken.START_ARRAY, JsonToken.END_ARRAY);
      default -> {
        /* Scalar values are self-contained */
      }
    }
  }

  private static void skipNested(JsonParser parser, JsonToken open, JsonToken close)
      throws IOException {
    int depth = 1;
    while (depth > 0) {
      JsonToken t = parser.nextToken();
      if (t == open) {
        depth++;
      } else if (t == close) {
        depth--;
      }
    }
  }
}
