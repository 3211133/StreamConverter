package com.streamconverter.command.impl.xml;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamconverter.test.StreamingTestUtils.TrackingInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("XMLバリデーションコマンドのテスト")
class ValidateTest {

  private String schemaPath;
  private String validXmlContent;

  @BeforeEach
  void setUp() throws IOException {
    // Use classpath identifier directly (no filesystem path resolution)
    schemaPath = "test-schema.xsd";

    // Load XML content for tests
    ClassLoader classLoader = getClass().getClassLoader();
    try (InputStream validXmlStream = classLoader.getResourceAsStream("valid-test.xml")) {
      if (validXmlStream == null) {
        throw new IOException("valid-test.xml not found in classpath");
      }
      validXmlContent = new String(validXmlStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  @DisplayName("コンストラクタのテスト")
  void testConstructor() {
    // コンストラクタのテスト
    ValidateCommand command = ValidateCommand.create(schemaPath);
    assertNotNull(command);
  }

  @Test
  @DisplayName("execute正常系：有効なXML")
  void testExecuteWithValidXml() throws IOException {
    // 正常系のexecuteメソッドテスト
    ValidateCommand command = ValidateCommand.create(schemaPath);

    try (InputStream inputStream =
            getClass().getClassLoader().getResourceAsStream("valid-test.xml");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      // 例外が発生しないことを確認
      assertDoesNotThrow(
          () -> {
            command.execute(inputStream, outputStream);
          });

      // 出力が入力と同じであることを確認
      String result = outputStream.toString(StandardCharsets.UTF_8);
      assertEquals(validXmlContent, result);
    }
  }

  @Test
  @DisplayName("execute異常系：無効なXML")
  void testExecuteWithInvalidXml() throws IOException {
    // 無効なXMLでのexecuteメソッドテスト
    ValidateCommand command = ValidateCommand.create(schemaPath);

    try (InputStream inputStream =
            getClass().getClassLoader().getResourceAsStream("invalid-test.xml");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      // StreamProcessingExceptionが発生することを期待
      assertThrows(
          com.streamconverter.StreamProcessingException.class,
          () -> {
            command.execute(inputStream, outputStream);
          });
    }
  }

  @Test
  @DisplayName("execute異常系：null入力ストリーム")
  void testExecuteWithNullInputStream() {
    // null入力ストリームでのexecuteメソッドテスト
    ValidateCommand command = ValidateCommand.create(schemaPath);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(
        NullPointerException.class,
        () -> {
          command.execute(null, outputStream);
        });
  }

  @Test
  @DisplayName("execute異常系：null出力ストリーム")
  void testExecuteWithNullOutputStream() throws IOException {
    // null出力ストリームでのexecuteメソッドテスト
    ValidateCommand command = ValidateCommand.create(schemaPath);

    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("valid-test.xml")) {
      assertThrows(
          NullPointerException.class,
          () -> {
            command.execute(inputStream, null);
          });
    }
  }

  @Test
  @DisplayName("execute異常系：存在しないスキーマファイル")
  void testExecuteWithNonExistentSchemaFile() throws IOException {
    // クラスパスに存在しないスキーマ指定でのコンストラクタテスト
    // StreamProcessingExceptionが発生することを期待
    assertThrows(
        com.streamconverter.StreamProcessingException.class,
        () -> {
          ValidateCommand.create("non-existent-schema.xsd");
        });
  }

  @Test
  @DisplayName("セキュリティ：パストラバーサルパターンは存在しないリソースとして扱われる")
  void testPathTraversalTreatedAsNonExistent() {
    // パストラバーサルパターン（..）を含むパスは、ClassLoaderが解決しないため
    // 単に「存在しないリソース」としてStreamProcessingExceptionが発生する
    // （ClasspathResourceValidatorのドキュメント参照）
    assertThrows(
        com.streamconverter.StreamProcessingException.class,
        () -> {
          ValidateCommand.create("../secret-schema.xsd");
        },
        "Path with .. should be treated as non-existent resource");

    assertThrows(
        com.streamconverter.StreamProcessingException.class,
        () -> {
          ValidateCommand.create("schemas/../../etc/passwd");
        },
        "Path with .. should be treated as non-existent resource");
  }

  @Test
  @DisplayName("execute：空の入力ストリーム")
  void testExecuteWithEmptyInputStream() throws IOException {
    // 空の入力ストリームでのexecuteメソッドテスト
    ValidateCommand command = ValidateCommand.create(schemaPath);

    try (InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      // StreamProcessingExceptionが発生することを期待（空のXMLは無効）
      assertThrows(
          com.streamconverter.StreamProcessingException.class,
          () -> {
            command.execute(inputStream, outputStream);
          });
    }
  }

  @Test
  @DisplayName("Streaming XML validation behavior verification")
  void testStreamingXmlValidationBehavior() throws IOException {
    ValidateCommand command = ValidateCommand.create(schemaPath);

    // Create multiple valid XML documents to observe streaming behavior
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlBuilder.append("<root id=\"streaming-test\">\n");

    for (int i = 0; i < 50; i++) {
      xmlBuilder.append(
          String.format(
              "  <element>Record %d: This is a test record %d for XML validation streaming behavior verification with value %d</element>%n",
              i, i, i * 100));
    }
    xmlBuilder.append("</root>");

    String xmlData = xmlBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute XML validation
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during XML validation");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify data was processed incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read during XML validation");

    // Verify the validation was successful and content was passed through
    String output = monitoringOutputStream.getContent();
    assertEquals(xmlData, output, "XML validation should pass through valid content unchanged");
  }

  @Test
  @DisplayName("Incremental XML validation processing verification")
  void testIncrementalXmlValidationProcessing() throws IOException {
    ValidateCommand command = ValidateCommand.create(schemaPath);

    // Create complex XML with nested structures to force incremental processing
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlBuilder.append("<root id=\"incremental-test\">\n");

    for (int i = 1; i <= 100; i++) {
      xmlBuilder.append(
          String.format(
              "  <element>XML Validation Record %d with value %d in Category %d - Detailed content for record %d with extensive XML validation streaming behavior testing data and complex nested structures (tags: tag%d, streaming, validation)</element>%n",
              i, i * 1000, i % 10, i, i));
    }
    xmlBuilder.append("</root>");

    String xmlData = xmlBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - perform incremental XML validation processing
    long processingStart = System.nanoTime();
    command.execute(trackingInputStream, monitoringOutputStream);
    long processingEnd = System.nanoTime();

    // Then - verify incremental processing characteristics
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "Output should be written during XML validation");
    assertTrue(trackingInputStream.isFullyRead(), "Input should be fully processed");

    // Verify substantial XML data was processed
    assertTrue(
        trackingInputStream.getBytesRead() > 15000,
        "Should have processed substantial amount of XML data");

    long processingTime = processingEnd - processingStart;
    assertTrue(processingTime > 0, "XML validation should take measurable time");

    // Verify output was generated correctly (XML validation should pass through valid content)
    String output = monitoringOutputStream.getContent();
    assertEquals(xmlData, output, "XML validation should pass through valid content unchanged");
  }
}
