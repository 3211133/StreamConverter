package com.streamconverter.command.impl.json;

import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("JsonStreamingValidateCommand Tests")
public class JsonStreamingValidateCommandTest {

  @TempDir Path tempDir;

  private Path schemaFile;

  @BeforeEach
  void setUp() throws IOException {
    String schemaContent =
        """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "id": {
              "type": "integer"
            },
            "name": {
              "type": "string"
            }
          },
          "required": ["id", "name"],
          "additionalProperties": false
        }
        """;

    schemaFile = tempDir.resolve("streaming-schema.json");
    Files.writeString(schemaFile, schemaContent, StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Factory method accepts valid schema path")
  void testFactoryWithValidSchemaPath() {
    assertDoesNotThrow(
        () -> {
          JsonStreamingValidateCommand command =
              JsonStreamingValidateCommand.create(schemaFile.toString());
          assertNotNull(command);
        });
  }

  @Test
  @DisplayName("Factory method rejects null schema path")
  void testFactoryWithNullSchemaPath() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JsonStreamingValidateCommand.create(null));
    assertEquals("Schema path cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("Factory method rejects empty schema path")
  void testFactoryWithEmptySchemaPath() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JsonStreamingValidateCommand.create(""));
    assertEquals("Schema path cannot be empty", exception.getMessage());
  }

  @Test
  @DisplayName("Factory method rejects whitespace-only schema path")
  void testFactoryWithWhitespaceSchemaPath() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> JsonStreamingValidateCommand.create("   "));
    assertEquals("Schema path cannot be empty", exception.getMessage());
  }

  @Test
  @DisplayName("Factory method rejects null schema registry")
  void testFactoryWithNullSchemaRegistry() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> JsonStreamingValidateCommand.create(schemaFile.toString(), null));
    assertEquals("Schema registry cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("Factory method accepts custom schema registry")
  void testFactoryWithCustomSchemaRegistry() {
    SchemaRegistry customRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7);

    JsonStreamingValidateCommand command =
        JsonStreamingValidateCommand.create(schemaFile.toString(), customRegistry);

    assertNotNull(command);
  }
}

