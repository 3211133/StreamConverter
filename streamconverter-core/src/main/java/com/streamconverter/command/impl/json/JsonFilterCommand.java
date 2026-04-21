package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.path.IPath;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    List<PathSegment> segments = JsonPathParser.parse(jsonPath.toString());

    try (JsonParser parser = jsonFactory.createParser(inputStream);
        JsonGenerator generator = jsonFactory.createGenerator(outputStream)) {
      generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

      if (segments.isEmpty()) {
        JsonValueCopier.copyValue(parser, generator);
      } else {
        JsonPathExtractor.extractPath(parser, generator, segments, 0);
      }

      generator.flush();
    }
  }
}
