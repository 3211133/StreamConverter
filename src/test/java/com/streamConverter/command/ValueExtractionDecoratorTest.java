package com.streamConverter.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.context.ExecutionContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

/** ValueExtractionDecoratorのテストクラス */
class ValueExtractionDecoratorTest {

  @Mock private IStreamCommand mockCommand;

  private AutoCloseable mockCloseable;

  @BeforeEach
  void setUp() {
    mockCloseable = MockitoAnnotations.openMocks(this);
    MDC.clear();
  }

  @AfterEach
  void tearDown() throws Exception {
    MDC.clear();
    if (mockCloseable != null) {
      mockCloseable.close();
    }
  }

  @Test
  @DisplayName("JSON形式からの値抽出が正常に動作する")
  void testJsonValueExtraction() throws IOException {
    // Given
    String jsonData = "{\"user\":{\"id\":\"12345\",\"name\":\"John\"}}";
    InputStream inputStream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(mockCommand, "JSON", "$.user.id", "userId");

    // When
    decorator.execute(inputStream, outputStream);

    // Then
    assertEquals("12345", MDC.get("userId"));
    verify(mockCommand, times(1)).execute(any(InputStream.class), any(OutputStream.class));
  }

  @Test
  @DisplayName("JSON配列からの値抽出が正常に動作する")
  void testJsonArrayValueExtraction() throws IOException {
    // Given
    String jsonData = "{\"items\":[{\"name\":\"item1\"},{\"name\":\"item2\"}]}";
    InputStream inputStream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(mockCommand, "JSON", "$.items[0].name", "itemName");

    // When
    decorator.execute(inputStream, outputStream);

    // Then
    assertEquals("item1", MDC.get("itemName"));
    verify(mockCommand, times(1)).execute(any(InputStream.class), any(OutputStream.class));
  }

  @Test
  @DisplayName("XML形式からの値抽出が正常に動作する")
  void testXmlValueExtraction() throws IOException {
    // Given
    String xmlData = "<root><user id=\"12345\"><name>John</name></user></root>";
    InputStream inputStream = new ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(mockCommand, "XML", "/root/user/@id", "userId");

    // When
    decorator.execute(inputStream, outputStream);

    // Then
    assertEquals("12345", MDC.get("userId"));
    verify(mockCommand, times(1)).execute(any(InputStream.class), any(OutputStream.class));
  }

  @Test
  @DisplayName("XMLテキストノードからの値抽出が正常に動作する")
  void testXmlTextNodeExtraction() throws IOException {
    // Given
    String xmlData = "<root><user><name>John</name></user></root>";
    InputStream inputStream = new ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(mockCommand, "XML", "/root/user/name/text()", "userName");

    // When
    decorator.execute(inputStream, outputStream);

    // Then
    assertEquals("John", MDC.get("userName"));
    verify(mockCommand, times(1)).execute(any(InputStream.class), any(OutputStream.class));
  }

  @Test
  @DisplayName("CSV形式からの値抽出が正常に動作する")
  void testCsvValueExtraction() throws IOException {
    // Given
    String csvData = "user_id,name,age\n12345,John,30\n67890,Jane,25";
    InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(mockCommand, "CSV", "user_id", "userId");

    // When
    decorator.execute(inputStream, outputStream);

    // Then
    assertEquals("12345", MDC.get("userId"));
    verify(mockCommand, times(1)).execute(any(InputStream.class), any(OutputStream.class));
  }

  @Test
  @DisplayName("ExecutionContextを使用した値抽出が正常に動作する")
  void testValueExtractionWithContext() throws IOException {
    // Given
    String jsonData = "{\"user\":{\"id\":\"12345\"}}";
    InputStream inputStream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();
    ExecutionContext context = ExecutionContext.create();

    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(new SampleStreamCommand(), "JSON", "$.user.id", "userId");

    // When
    decorator.execute(inputStream, outputStream, context);

    // Then
    assertEquals("12345", MDC.get("userId"));
    assertEquals("12345", context.getUserContext("userId"));
    assertNotNull(MDC.get("executionId"));
  }

  @Test
  @DisplayName("存在しないパスの場合、警告ログが出力され処理が継続される")
  void testNonExistentPath() throws IOException {
    // Given
    String jsonData = "{\"user\":{\"name\":\"John\"}}";
    InputStream inputStream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(mockCommand, "JSON", "$.user.id", "userId");

    // When
    decorator.execute(inputStream, outputStream);

    // Then
    assertNull(MDC.get("userId"));
    verify(mockCommand, times(1)).execute(any(InputStream.class), any(OutputStream.class));
  }

  @Test
  @DisplayName("空の入力ストリームの場合、処理が継続される")
  void testEmptyInputStream() throws IOException {
    // Given
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    OutputStream outputStream = new ByteArrayOutputStream();

    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(mockCommand, "JSON", "$.user.id", "userId");

    // When
    decorator.execute(inputStream, outputStream);

    // Then
    assertNull(MDC.get("userId"));
    verify(mockCommand, times(1)).execute(any(InputStream.class), any(OutputStream.class));
  }

  @Test
  @DisplayName("無効なデータ形式の場合、IllegalArgumentExceptionが発生する")
  void testInvalidDataFormat() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new ValueExtractionDecorator(mockCommand, "INVALID", "$.path", "key");
        });
  }

  @Test
  @DisplayName("null引数の場合、適切な例外が発生する")
  void testNullArguments() {
    // When & Then
    assertThrows(
        NullPointerException.class,
        () -> {
          new ValueExtractionDecorator(null, "JSON", "$.path", "key");
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new ValueExtractionDecorator(mockCommand, null, "$.path", "key");
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new ValueExtractionDecorator(mockCommand, "JSON", null, "key");
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new ValueExtractionDecorator(mockCommand, "JSON", "$.path", null);
        });
  }

  @Test
  @DisplayName("空文字列引数の場合、適切な例外が発生する")
  void testEmptyStringArguments() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new ValueExtractionDecorator(mockCommand, "", "$.path", "key");
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new ValueExtractionDecorator(mockCommand, "JSON", "", "key");
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new ValueExtractionDecorator(mockCommand, "JSON", "$.path", "");
        });
  }

  @Test
  @DisplayName("無効なJSON形式の場合、IOExceptionが発生する")
  void testInvalidJsonFormat() {
    // Given
    String invalidJson = "{invalid json}";
    InputStream inputStream =
        new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(mockCommand, "JSON", "$.user.id", "userId");

    // When & Then
    assertThrows(
        IOException.class,
        () -> {
          decorator.execute(inputStream, outputStream);
        });
  }

  @Test
  @DisplayName("無効なXML形式の場合、IOExceptionが発生する")
  void testInvalidXmlFormat() {
    // Given
    String invalidXml = "<invalid><xml>";
    InputStream inputStream = new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(mockCommand, "XML", "//user/@id", "userId");

    // When & Then
    assertThrows(
        IOException.class,
        () -> {
          decorator.execute(inputStream, outputStream);
        });
  }

  @Test
  @DisplayName("gettersが正常に動作する")
  void testGetters() {
    // Given
    ValueExtractionDecorator decorator =
        new ValueExtractionDecorator(mockCommand, "JSON", "$.user.id", "userId");

    // When & Then
    assertEquals("JSON", decorator.getDataFormat());
    assertEquals("$.user.id", decorator.getExtractionPath());
    assertEquals("userId", decorator.getMdcKey());
  }
}
