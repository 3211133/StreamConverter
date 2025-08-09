package com.streamConverter.command.impl.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("XMLバリデーションコマンドのテスト")
class ValidateTest {

  private String schemaPath;
  private String validXmlPath;
  private String invalidXmlPath;
  private String validXmlContent;

  @BeforeEach
  void setUp() throws IOException {
    // テストリソースのパスを設定
    schemaPath = new File("src/test/resources/test-schema.xsd").getAbsolutePath();
    validXmlPath = new File("src/test/resources/valid-test.xml").getAbsolutePath();
    invalidXmlPath = new File("src/test/resources/invalid-test.xml").getAbsolutePath();

    // XMLコンテンツを読み込み
    validXmlContent = Files.readString(Paths.get(validXmlPath));
    Files.readString(Paths.get(invalidXmlPath));
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

    try (InputStream inputStream = new FileInputStream(validXmlPath);
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

    try (InputStream inputStream = new FileInputStream(invalidXmlPath);
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

    try (InputStream inputStream = new FileInputStream(validXmlPath)) {
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
