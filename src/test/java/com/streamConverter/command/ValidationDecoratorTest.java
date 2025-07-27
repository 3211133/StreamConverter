package com.streamConverter.command;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.StreamProcessingException;
import com.streamConverter.command.ValidationDecorator.ValidationType;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.validation.ValidationResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** ValidationDecoratorクラスのテスト */
public class ValidationDecoratorTest {

  @TempDir Path tempDir;

  private SampleStreamCommand baseCommand;
  private ByteArrayOutputStream outputStream;

  @BeforeEach
  void setUp() {
    baseCommand = new SampleStreamCommand("test");
    outputStream = new ByteArrayOutputStream();
  }

  @Test
  @DisplayName("JSON validation success")
  void testJsonValidationSuccess() throws IOException {
    // 有効なJSONスキーマファイルを作成
    String jsonSchema =
        """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "name": {"type": "string"},
            "age": {"type": "number"}
          },
          "required": ["name", "age"]
        }
        """;

    Path schemaFile = tempDir.resolve("test-schema.json");
    Files.write(schemaFile, jsonSchema.getBytes(StandardCharsets.UTF_8));

    // 有効なJSONデータ
    String validJson =
        """
        {
          "name": "John Doe",
          "age": 30
        }
        """;

    ValidationDecorator decorator =
        new ValidationDecorator(baseCommand, ValidationType.JSON, schemaFile.toString());

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(validJson.getBytes(StandardCharsets.UTF_8));

    // 実行（例外が発生しないことを確認）
    assertDoesNotThrow(() -> decorator.execute(inputStream, outputStream));

    // ValidationResultの確認
    ValidationResult result = decorator.getLastValidationResult();
    assertNotNull(result);
    assertTrue(result.isValid());
    assertEquals("JSON", result.getValidationType());
    assertEquals(0, result.getErrorCount());
  }

  @Test
  @DisplayName("JSON validation failure")
  void testJsonValidationFailure() throws IOException {
    // JSONスキーマファイルを作成
    String jsonSchema =
        """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "name": {"type": "string"},
            "age": {"type": "number"}
          },
          "required": ["name", "age"]
        }
        """;

    Path schemaFile = tempDir.resolve("test-schema.json");
    Files.write(schemaFile, jsonSchema.getBytes(StandardCharsets.UTF_8));

    // 無効なJSONデータ（必須フィールドage不足）
    String invalidJson =
        """
        {
          "name": "John Doe"
        }
        """;

    ValidationDecorator decorator =
        new ValidationDecorator(baseCommand, ValidationType.JSON, schemaFile.toString());

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

    // バリデーション失敗の例外が発生することを確認
    assertThrows(
        StreamProcessingException.class, () -> decorator.execute(inputStream, outputStream));

    // ValidationResultの確認
    ValidationResult result = decorator.getLastValidationResult();
    assertNotNull(result);
    assertFalse(result.isValid());
    assertEquals("JSON", result.getValidationType());
    assertTrue(result.getErrorCount() > 0);
  }

  @Test
  @DisplayName("XML validation success")
  void testXmlValidationSuccess() throws IOException {
    // 有効なXMLスキーマファイルを作成
    String xmlSchema =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
          <xs:element name="person">
            <xs:complexType>
              <xs:sequence>
                <xs:element name="name" type="xs:string"/>
                <xs:element name="age" type="xs:int"/>
              </xs:sequence>
            </xs:complexType>
          </xs:element>
        </xs:schema>
        """;

    Path schemaFile = tempDir.resolve("test-schema.xsd");
    Files.write(schemaFile, xmlSchema.getBytes(StandardCharsets.UTF_8));

    // 有効なXMLデータ
    String validXml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <person>
          <name>John Doe</name>
          <age>30</age>
        </person>
        """;

    ValidationDecorator decorator =
        new ValidationDecorator(baseCommand, ValidationType.XML, schemaFile.toString());

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(validXml.getBytes(StandardCharsets.UTF_8));

    // 実行（例外が発生しないことを確認）
    assertDoesNotThrow(() -> decorator.execute(inputStream, outputStream));

    // ValidationResultの確認
    ValidationResult result = decorator.getLastValidationResult();
    assertNotNull(result);
    assertTrue(result.isValid());
    assertEquals("XML", result.getValidationType());
  }

  @Test
  @DisplayName("CSV validation success")
  void testCsvValidationSuccess() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};

    ValidationDecorator decorator = new ValidationDecorator(baseCommand, requiredColumns);

    // 有効なCSVデータ
    String validCsv =
        """
        id,name,email
        1,John Doe,john@example.com
        2,Jane Smith,jane@example.com
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(validCsv.getBytes(StandardCharsets.UTF_8));

    // 実行（例外が発生しないことを確認）
    assertDoesNotThrow(() -> decorator.execute(inputStream, outputStream));

    // ValidationResultの確認
    ValidationResult result = decorator.getLastValidationResult();
    assertNotNull(result);
    assertTrue(result.isValid());
    assertEquals("CSV", result.getValidationType());
  }

  @Test
  @DisplayName("CSV validation failure - missing required column")
  void testCsvValidationFailure() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};

    ValidationDecorator decorator = new ValidationDecorator(baseCommand, requiredColumns);

    // 無効なCSVデータ（emailカラム不足）
    String invalidCsv =
        """
        id,name
        1,John Doe
        2,Jane Smith
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(invalidCsv.getBytes(StandardCharsets.UTF_8));

    // バリデーション失敗の例外が発生することを確認
    assertThrows(
        StreamProcessingException.class, () -> decorator.execute(inputStream, outputStream));

    // ValidationResultの確認
    ValidationResult result = decorator.getLastValidationResult();
    assertNotNull(result);
    assertFalse(result.isValid());
    assertEquals("CSV", result.getValidationType());
    assertTrue(result.getErrorCount() > 0);
  }

  @Test
  @DisplayName("Constructor validation")
  void testConstructorValidation() {
    // null delegate
    assertThrows(
        NullPointerException.class,
        () -> new ValidationDecorator(null, ValidationType.JSON, "schema.json"));

    // null validation type
    assertThrows(
        NullPointerException.class,
        () -> new ValidationDecorator(baseCommand, null, "schema.json"));

    // null schema path
    assertThrows(
        NullPointerException.class,
        () -> new ValidationDecorator(baseCommand, ValidationType.JSON, null));

    // empty schema path
    assertThrows(
        IllegalArgumentException.class,
        () -> new ValidationDecorator(baseCommand, ValidationType.JSON, ""));
  }

  @Test
  @DisplayName("Getter methods")
  void testGetterMethods() {
    ValidationDecorator decorator =
        new ValidationDecorator(baseCommand, ValidationType.JSON, "test-schema.json");

    assertEquals(baseCommand, decorator.getDelegate());
    assertEquals(ValidationType.JSON, decorator.getValidationType());
    assertEquals("test-schema.json", decorator.getSchemaPath());
    assertEquals("SampleStreamCommand", decorator.getCommandName());
    assertNull(decorator.getLastValidationResult()); // 未実行時はnull
  }

  @Test
  @DisplayName("CSV constructor with required columns")
  void testCsvConstructorWithRequiredColumns() {
    String[] requiredColumns = {"id", "name"};
    ValidationDecorator decorator = new ValidationDecorator(baseCommand, requiredColumns);

    assertEquals(ValidationType.CSV, decorator.getValidationType());
    assertTrue(decorator.getSchemaPath().contains("CSV_REQUIRED_COLUMNS"));
    assertTrue(decorator.getSchemaPath().contains("id,name"));
  }

  @Test
  @DisplayName("CSV constructor with no required columns")
  void testCsvConstructorWithNoRequiredColumns() {
    ValidationDecorator decorator = new ValidationDecorator(baseCommand, new String[0]);

    assertEquals(ValidationType.CSV, decorator.getValidationType());
    assertEquals("CSV_NO_REQUIRED_COLUMNS", decorator.getSchemaPath());
  }
}
