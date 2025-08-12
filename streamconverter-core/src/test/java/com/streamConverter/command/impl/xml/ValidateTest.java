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
    // テストリソースをクラスパスから取得（クロスプラットフォーム対応）
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      // クロスプラットフォーム対応: URLからパスを安全に取得
      URL schemaResource = classLoader.getResource("test-schema.xsd");
      if (schemaResource == null) {
        throw new IOException("test-schema.xsd not found in classpath");
      }

      // URI経由でパスを取得し、プラットフォームに依存しない形式にする
      schemaPath = Paths.get(schemaResource.toURI()).toString();

      // XMLコンテンツを直接クラスパスから読み込み
      try (InputStream validXmlStream = classLoader.getResourceAsStream("valid-test.xml")) {
        if (validXmlStream == null) {
          throw new IOException("valid-test.xml not found in classpath");
        }
        validXmlContent = new String(validXmlStream.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (Exception e) {
      throw new IOException("Failed to load test resources", e);
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
}
