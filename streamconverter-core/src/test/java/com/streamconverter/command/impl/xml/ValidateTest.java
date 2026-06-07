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
import org.junit.jupiter.api.Tag;
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
  @Tag("known-bug") // #729
  @DisplayName(
      "ValidateCommand は XMLバリデーション失敗時に IOException をスローする（StreamProcessingException を外に出さない）")
  void bug_validateCommand_xmlValidationFailureThrowsStreamProcessingExceptionNotIOException()
      throws IOException {
    ValidateCommand command = ValidateCommand.create(schemaPath);

    try (InputStream inputStream =
            getClass().getClassLoader().getResourceAsStream("invalid-test.xml");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      // IStreamCommand.execute() の契約は throws IOException のみ。
      // バグ: SAXException → StreamProcessingException（RuntimeException）がキャッチされずに伝播。
      // 期待: IOException がスローされるべき
      assertThrows(
          IOException.class,
          () -> command.execute(inputStream, outputStream),
          "ValidateCommand.execute() は IOException をスローするべきだが、StreamProcessingException（RuntimeException）が伝播する");
    }
  }

  @Test
  @DisplayName("execute：コンテンツをそのまま通過させ、入力ストリームを完全に消費する")
  void testXmlValidationPassesThroughAndConsumesInputStream() throws IOException {
    ValidateCommand command = ValidateCommand.create(schemaPath);

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

    command.execute(trackingInputStream, monitoringOutputStream);

    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during XML validation");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");
    assertTrue(
        trackingInputStream.getBytesRead() > 3000,
        "Should have processed substantial amount of XML data");

    String output = monitoringOutputStream.getContent();
    assertEquals(xmlData, output, "XML validation should pass through valid content unchanged");
  }
}
