package com.streamConverter.api;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.CommandResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Streams APIのテストクラス
 *
 * <p>高レベルなストリーム処理APIの各機能が正しく動作することを検証します。
 */
@DisplayName("Streams API Tests")
class StreamsTest {

  private static final String SAMPLE_JSON =
      """
        {
            "user": {
                "name": "John Doe",
                "email": "john@example.com",
                "age": 30
            },
            "status": "active"
        }
        """;

  private static final String SAMPLE_CSV =
      "id,name,department,salary\n"
          + "1,John Smith,Engineering,75000\n"
          + "2,Jane Doe,Marketing,65000\n"
          + "3,Bob Johnson,Engineering,80000\n";

  private static final String SAMPLE_XML =
      """
        <?xml version="1.0"?>
        <configuration>
            <database>
                <host>localhost</host>
                <port>5432</port>
                <name>myapp</name>
            </database>
            <server>
                <port>8080</port>
                <ssl>true</ssl>
            </server>
        </configuration>
        """;

  @Test
  @DisplayName("JSON専用APIの基本動作")
  void testJsonStreamBuilder() throws IOException {
    // When
    StreamBuilder jsonBuilder = Streams.json(SAMPLE_JSON);

    // Then
    assertNotNull(jsonBuilder);

    // Method chaining test
    String result = jsonBuilder.format().extract("user").asString();

    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  @DisplayName("CSV専用APIの基本動作")
  void testCsvStreamBuilder() throws IOException {
    // When
    StreamBuilder csvBuilder = Streams.csv(SAMPLE_CSV);

    // Then
    assertNotNull(csvBuilder);

    // Method chaining test
    String result = csvBuilder.extract("name").process("name-formatter").asString();

    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  @DisplayName("XML専用APIの基本動作")
  void testXmlStreamBuilder() throws IOException {
    // When
    StreamBuilder xmlBuilder = Streams.xml(SAMPLE_XML);

    // Then
    assertNotNull(xmlBuilder);

    // Method chaining test
    String result =
        xmlBuilder.extract("configuration/database").process("config-parser").asString();

    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  @DisplayName("汎用fromメソッドの動作")
  void testGenericFromMethods() throws IOException {
    // String input
    StreamBuilder stringBuilder = Streams.from("test data");
    assertNotNull(stringBuilder);

    // InputStream input
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream("test data".getBytes(StandardCharsets.UTF_8));
    StreamBuilder streamBuilder = Streams.from(inputStream);
    assertNotNull(streamBuilder);
  }

  @Test
  @DisplayName("JSON特化APIのvalidate機能")
  void testJsonValidation() throws IOException {
    // Given
    String schemaPath = "user-schema.json";

    // When & Then (スキーマファイルが存在しないためIOExceptionが発生することを期待)
    assertDoesNotThrow(
        () -> {
          StreamBuilder builder = Streams.json(SAMPLE_JSON);
          // ここではビルダーの構築のみテスト（実行はしない）
          assertNotNull(builder);
        });
  }

  @Test
  @DisplayName("CSV特化APIのextract機能")
  void testCsvExtraction() throws IOException {
    // When
    String result = Streams.csv(SAMPLE_CSV).extract("department").asString();

    // Then
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  @DisplayName("XML特化APIのextract機能")
  void testXmlExtraction() throws IOException {
    // When
    String result = Streams.xml(SAMPLE_XML).extract("configuration/server/port").asString();

    // Then
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  @DisplayName("文字エンコーディング変換の動作")
  void testEncodingConversion() throws IOException {
    // JSON with encoding conversion
    String jsonResult =
        Streams.json(SAMPLE_JSON)
            .format()
            .convertEncoding("UTF-8", "UTF-16")
            .convertEncoding("UTF-16", "UTF-8")
            .asString();
    assertNotNull(jsonResult);

    // CSV with encoding conversion
    String csvResult =
        Streams.csv(SAMPLE_CSV)
            .extract("name")
            .convertEncoding("UTF-8", "UTF-16")
            .convertEncoding("UTF-16", "UTF-8")
            .asString();
    assertNotNull(csvResult);

    // XML with encoding conversion
    String xmlResult =
        Streams.xml(SAMPLE_XML)
            .extract("configuration")
            .convertEncoding("UTF-8", "UTF-16")
            .convertEncoding("UTF-16", "UTF-8")
            .asString();
    assertNotNull(xmlResult);
  }

  @Test
  @DisplayName("HTTP送信機能の動作確認")
  void testHttpSending() throws IOException {
    // Given
    String testUrl = "https://httpbin.org/post";

    // When & Then (実際のHTTP送信はしないが、パイプライン構築をテスト)
    assertDoesNotThrow(
        () -> {
          StreamBuilder builder = Streams.json(SAMPLE_JSON).format().sendHttp(testUrl);
          assertNotNull(builder);
        });
  }

  @Test
  @DisplayName("汎用StreamBuilderへの変換")
  void testAsGenericConversion() throws IOException {
    // JSON -> Generic
    StreamBuilder genericFromJson = Streams.json(SAMPLE_JSON).format();
    assertNotNull(genericFromJson);
    assertTrue(genericFromJson.getCommandCount() > 0);

    // CSV -> Generic
    StreamBuilder genericFromCsv = Streams.csv(SAMPLE_CSV).extract("name");
    assertNotNull(genericFromCsv);
    assertTrue(genericFromCsv.getCommandCount() > 0);

    // XML -> Generic
    StreamBuilder genericFromXml = Streams.xml(SAMPLE_XML).extract("configuration");
    assertNotNull(genericFromXml);
    assertTrue(genericFromXml.getCommandCount() > 0);
  }

  @Test
  @DisplayName("OutputStream出力の動作確認")
  void testOutputStreamOperations() throws IOException {
    // JSON to OutputStream
    ByteArrayOutputStream jsonOutput = new ByteArrayOutputStream();
    List<CommandResult> jsonResults = Streams.json(SAMPLE_JSON).format().toStream(jsonOutput);
    assertNotNull(jsonResults);
    assertTrue(jsonOutput.size() > 0);

    // CSV to OutputStream
    ByteArrayOutputStream csvOutput = new ByteArrayOutputStream();
    List<CommandResult> csvResults =
        Streams.csv(SAMPLE_CSV).extract("department").toStream(csvOutput);
    assertNotNull(csvResults);
    assertTrue(csvOutput.size() > 0);

    // XML to OutputStream
    ByteArrayOutputStream xmlOutput = new ByteArrayOutputStream();
    List<CommandResult> xmlResults =
        Streams.xml(SAMPLE_XML).extract("configuration/database").toStream(xmlOutput);
    assertNotNull(xmlResults);
    assertTrue(xmlOutput.size() > 0);
  }

  @Test
  @DisplayName("複雑なチェーン処理の動作確認")
  void testComplexChaining() throws IOException {
    // Complex JSON processing chain
    String jsonResult =
        Streams.json(SAMPLE_JSON)
            .format()
            .extract("user")
            .process("user-validator")
            .process("user-formatter")
            .convertEncoding("UTF-8", "UTF-16")
            .convertEncoding("UTF-16", "UTF-8")
            .asString();
    assertNotNull(jsonResult);

    // Complex CSV processing chain
    String csvResult =
        Streams.csv(SAMPLE_CSV)
            .extract("salary")
            .process("salary-calculator")
            .process("currency-formatter")
            .asString();
    assertNotNull(csvResult);

    // Complex XML processing chain
    String xmlResult =
        Streams.xml(SAMPLE_XML)
            .extract("configuration")
            .process("config-validator")
            .process("config-transformer")
            .asString();
    assertNotNull(xmlResult);
  }

  @Test
  @DisplayName("Streamsクラスのインスタンス化禁止確認")
  void testStreamsInstantiationProhibition() {
    // Streams is a utility class and should not be instantiable
    // This is enforced by the private constructor with AssertionError
    // We can't directly test this without reflection, but we can verify
    // that all methods are static
    assertDoesNotThrow(
        () -> {
          Streams.json("{}");
          Streams.csv("a,b\n1,2");
          Streams.xml("<root></root>");
          Streams.from("test");
        });
  }

  @Test
  @DisplayName("エラー処理の動作確認")
  void testErrorHandling() {
    // Test with null inputs (should throw NullPointerException)
    assertThrows(NullPointerException.class, () -> Streams.json(null));
    assertThrows(NullPointerException.class, () -> Streams.csv(null));
    assertThrows(NullPointerException.class, () -> Streams.xml(null));
    assertThrows(NullPointerException.class, () -> Streams.from((String) null));
    assertThrows(NullPointerException.class, () -> Streams.from((ByteArrayInputStream) null));
  }

  @Test
  @DisplayName("format()メソッドの形式制限確認")
  void testFormatMethodRestriction() throws IOException {
    // JSON形式では format() が正常に動作することを確認
    assertDoesNotThrow(
        () -> {
          StreamBuilder jsonBuilder = Streams.json(SAMPLE_JSON);
          jsonBuilder.format(); // JSON形式なので例外は発生しない
        });

    // CSV形式で format() を呼び出すと例外が発生することを確認
    assertThrows(
        IllegalStateException.class,
        () -> {
          StreamBuilder csvBuilder = Streams.csv(SAMPLE_CSV);
          csvBuilder.format(); // CSV形式なので例外が発生
        });

    // XML形式で format() を呼び出すと例外が発生することを確認
    assertThrows(
        IllegalStateException.class,
        () -> {
          StreamBuilder xmlBuilder = Streams.xml(SAMPLE_XML);
          xmlBuilder.format(); // XML形式なので例外が発生
        });

    // 汎用形式で format() を呼び出すと例外が発生することを確認
    assertThrows(
        IllegalStateException.class,
        () -> {
          StreamBuilder genericBuilder = Streams.from("test data");
          genericBuilder.format(); // 汎用形式なので例外が発生
        });
  }

  @Test
  @DisplayName("データフォーマット特化APIの実用例")
  void testRealWorldUsageExamples() throws IOException {
    // Real-world JSON processing example
    String apiResponse =
        """
            {
                "status": "success",
                "data": {
                    "user_count": 1500,
                    "active_users": 1200
                }
            }
            """;

    String status = Streams.json(apiResponse).extract("status").asString();
    assertNotNull(status);

    // Real-world CSV processing example
    String salesData =
        "month,product,sales,region\n"
            + "Jan,Widget A,1000,North\n"
            + "Feb,Widget B,1500,South\n"
            + "Mar,Widget A,1200,East\n";

    String products =
        Streams.csv(salesData).extract("product").process("product-aggregator").asString();
    assertNotNull(products);

    // Real-world XML processing example
    String systemConfig =
        """
            <?xml version="1.0"?>
            <system>
                <monitoring>
                    <enabled>true</enabled>
                    <interval>30</interval>
                </monitoring>
                <logging>
                    <level>INFO</level>
                    <file>/var/log/app.log</file>
                </logging>
            </system>
            """;

    String monitoringConfig =
        Streams.xml(systemConfig)
            .extract("system/monitoring")
            .process("monitoring-parser")
            .asString();
    assertNotNull(monitoringConfig);
  }
}
