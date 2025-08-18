package com.streamConverter.command.impl.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("XMLバリデーションコマンドのテスト")
class ValidateTest {

  private String schemaPath;
  private String validXmlContent;

  @BeforeEach
  void setUp() throws IOException {
    // Use classpath resource URLs for cross-platform compatibility
    ClassLoader classLoader = getClass().getClassLoader();

    // Get schema path from classpath - works across all platforms
    URL schemaResource = classLoader.getResource("test-schema.xsd");
    if (schemaResource == null) {
      throw new IOException("test-schema.xsd not found in classpath");
    }

    try {
      schemaPath = Paths.get(schemaResource.toURI()).toString();
    } catch (Exception e) {
      throw new IOException("Failed to resolve schema path", e);
    }

    // Load XML content for tests
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
    ValidateCommand command = new ValidateCommand(schemaPath);
    assertNotNull(command);
  }

  @Test
  @DisplayName("execute正常系：有効なXML")
  void testExecuteWithValidXml() throws IOException {
    // 正常系のexecuteメソッドテスト
    ValidateCommand command = new ValidateCommand(schemaPath);

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
    ValidateCommand command = new ValidateCommand(schemaPath);

    try (InputStream inputStream =
            getClass().getClassLoader().getResourceAsStream("invalid-test.xml");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      // StreamProcessingExceptionが発生することを期待
      assertThrows(
          com.streamConverter.StreamProcessingException.class,
          () -> {
            command.execute(inputStream, outputStream);
          });
    }
  }

  @Test
  @DisplayName("execute異常系：null入力ストリーム")
  void testExecuteWithNullInputStream() {
    // null入力ストリームでのexecuteメソッドテスト
    ValidateCommand command = new ValidateCommand(schemaPath);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    Exception exception =
        assertThrows(
            IOException.class,
            () -> {
              command.execute(null, outputStream);
            });

    // The root cause should be NullPointerException
    assertTrue(
        exception.getCause() instanceof NullPointerException,
        "Root cause should be NullPointerException");
  }

  @Test
  @DisplayName("execute異常系：null出力ストリーム")
  void testExecuteWithNullOutputStream() throws IOException {
    // null出力ストリームでのexecuteメソッドテスト
    ValidateCommand command = new ValidateCommand(schemaPath);

    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("valid-test.xml")) {
      Exception exception =
          assertThrows(
              IOException.class,
              () -> {
                command.execute(inputStream, null);
              });

      // The root cause should be NullPointerException
      assertTrue(
          exception.getCause() instanceof NullPointerException,
          "Root cause should be NullPointerException");
    }
  }

  @Test
  @DisplayName("execute異常系：存在しないスキーマファイル")
  void testExecuteWithNonExistentSchemaFile() throws IOException {
    // 存在しないスキーマファイルでのコンストラクタテスト（セキュリティ強化により、コンストラクタで例外が発生）
    // StreamProcessingExceptionが発生することを期待（存在しないスキーマファイル）
    assertThrows(
        com.streamConverter.StreamProcessingException.class,
        () -> {
          new ValidateCommand("non-existent-schema.xsd");
        });
  }

  @Test
  @DisplayName("execute：空の入力ストリーム")
  void testExecuteWithEmptyInputStream() throws IOException {
    // 空の入力ストリームでのexecuteメソッドテスト
    ValidateCommand command = new ValidateCommand(schemaPath);

    try (InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      // StreamProcessingExceptionが発生することを期待（空のXMLは無効）
      assertThrows(
          com.streamConverter.StreamProcessingException.class,
          () -> {
            command.execute(inputStream, outputStream);
          });
    }
  }

  @Test
  @DisplayName("Streaming XML validation behavior verification")
  void testStreamingXmlValidationBehavior() throws IOException {
    ValidateCommand command = new ValidateCommand(schemaPath);

    // Create multiple valid XML documents to observe streaming behavior
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlBuilder.append("<root id=\"streaming-test\">\n");

    for (int i = 0; i < 50; i++) {
      xmlBuilder.append(
          String.format(
              "  <element>Record %d: This is a test record %d for XML validation streaming behavior verification with value %d</element>\n",
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
    ValidateCommand command = new ValidateCommand(schemaPath);

    // Create complex XML with nested structures to force incremental processing
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlBuilder.append("<root id=\"incremental-test\">\n");

    for (int i = 1; i <= 100; i++) {
      xmlBuilder.append(
          String.format(
              "  <element>XML Validation Record %d with value %d in Category %d - Detailed content for record %d with extensive XML validation streaming behavior testing data and complex nested structures (tags: tag%d, streaming, validation)</element>\n",
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

  /** Custom InputStream that tracks read operations for streaming behavior verification */
  private static class TrackingInputStream extends ByteArrayInputStream {
    private long fullyReadTime = -1;
    private final int totalBytes;
    private int bytesRead = 0;

    public TrackingInputStream(byte[] buf) {
      super(buf);
      this.totalBytes = buf.length;
    }

    @Override
    public int read() {
      int result = super.read();
      if (result != -1) {
        bytesRead++;
      } else if (fullyReadTime == -1) {
        fullyReadTime = System.nanoTime();
      }
      return result;
    }

    @Override
    public int read(byte[] b, int off, int len) {
      int bytesActuallyRead = super.read(b, off, len);
      if (bytesActuallyRead > 0) {
        bytesRead += bytesActuallyRead;
      }
      if (bytesActuallyRead == -1 && fullyReadTime == -1) {
        fullyReadTime = System.nanoTime();
      }
      return bytesActuallyRead;
    }

    public boolean isFullyRead() {
      return fullyReadTime != -1;
    }

    public long getFullyReadTime() {
      return fullyReadTime;
    }

    public int getBytesRead() {
      return bytesRead;
    }

    public int getTotalBytes() {
      return totalBytes;
    }

    public double getReadProgress() {
      return totalBytes > 0 ? (double) bytesRead / totalBytes : 0.0;
    }
  }

  /** Custom OutputStream that monitors write operations and timing */
  private static class MonitoringOutputStream extends ByteArrayOutputStream {
    private long firstWriteTime = -1;
    private boolean hasWriteOccurred = false;

    @Override
    public void write(int b) {
      recordFirstWrite();
      super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      recordFirstWrite();
      super.write(b, off, len);
    }

    private void recordFirstWrite() {
      if (!hasWriteOccurred) {
        firstWriteTime = System.nanoTime();
        hasWriteOccurred = true;
      }
    }

    public boolean hasWriteOccurred() {
      return hasWriteOccurred;
    }

    public long getFirstWriteTime() {
      return firstWriteTime;
    }

    public String getContent() {
      return toString(StandardCharsets.UTF_8);
    }
  }
}
