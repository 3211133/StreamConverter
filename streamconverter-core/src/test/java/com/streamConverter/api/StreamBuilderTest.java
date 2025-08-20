package com.streamConverter.api;

import static com.streamConverter.test.TestUtils.createTestData;
import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.CommandResult;
import com.streamConverter.StreamConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * StreamBuilder APIのテストクラス
 *
 * <p>流暢なAPIの各機能が正しく動作することを検証します。
 */
@DisplayName("StreamBuilder API Tests")
class StreamBuilderTest {

  private static final String SAMPLE_JSON =
      """
        {
            "user": {
                "name": "John Doe",
                "email": "john@example.com"
            },
            "status": "active"
        }
        """;

  private static final String SAMPLE_CSV =
      createTestData("id,name,department", "1,John,Engineering", "2,Jane,Marketing");

  private static final String SAMPLE_XML =
      """
        <?xml version="1.0"?>
        <config>
            <database>
                <host>localhost</host>
                <port>5432</port>
            </database>
        </config>
        """;

  @Test
  @DisplayName("基本的なビルダーパターンの動作確認")
  void testBasicBuilderPattern() throws IOException {
    // Given
    String input = "Hello, World!";

    // When
    StreamBuilder builder = StreamBuilder.create().fromString(input).process("test-processor");

    // Then
    assertFalse(builder.isEmpty());
    assertEquals(1, builder.getCommandCount());
    assertTrue(builder.getPipelineInfo().contains("SampleStreamCommand"));
  }

  @Test
  @DisplayName("JSON特化メソッドの動作確認")
  void testJsonSpecificMethods() throws IOException {
    // When
    StreamBuilder builder =
        StreamBuilder.create().fromString(SAMPLE_JSON).formatJson().extractJson("user");

    // Then
    assertEquals(2, builder.getCommandCount());
    String pipelineInfo = builder.getPipelineInfo();
    assertTrue(pipelineInfo.contains("JsonNavigateCommand"));
  }

  @Test
  @DisplayName("CSV特化メソッドの動作確認")
  void testCsvSpecificMethods() throws IOException {
    // When
    StreamBuilder builder = StreamBuilder.create().fromString(SAMPLE_CSV).extractCsv("name");

    // Then
    assertEquals(1, builder.getCommandCount());
    assertTrue(builder.getPipelineInfo().contains("CsvNavigateCommand"));
  }

  @Test
  @DisplayName("XML特化メソッドの動作確認")
  void testXmlSpecificMethods() throws IOException {
    // When
    StreamBuilder builder =
        StreamBuilder.create().fromString(SAMPLE_XML).extractXml("config/database");

    // Then
    assertEquals(1, builder.getCommandCount());
    assertTrue(builder.getPipelineInfo().contains("XmlNavigateCommand"));
  }

  @Test
  @DisplayName("文字エンコーディング変換の動作確認")
  void testEncodingConversion() throws IOException {
    // When
    StreamBuilder builder =
        StreamBuilder.create()
            .fromString("Test Data")
            .convertEncoding("UTF-8", "UTF-16")
            .convertEncoding("UTF-16", "UTF-8");

    // Then
    assertEquals(2, builder.getCommandCount());
    String pipelineInfo = builder.getPipelineInfo();
    assertTrue(pipelineInfo.contains("CharacterConvertCommand"));
  }

  @Test
  @DisplayName("条件付き処理の動作確认")
  void testConditionalProcessing() throws IOException {
    // Given
    boolean condition1 = true;
    boolean condition2 = false;

    // When
    StreamBuilder builder =
        StreamBuilder.create()
            .fromString("test")
            .when(condition1, b -> b.process("executed"))
            .when(condition2, b -> b.process("skipped"));

    // Then
    assertEquals(1, builder.getCommandCount()); // condition2はfalseなのでスキップ
    assertTrue(builder.getPipelineInfo().contains("executed"));
    assertFalse(builder.getPipelineInfo().contains("skipped"));
  }

  @Test
  @DisplayName("パイプライン実行とOutputStream出力")
  void testPipelineExecutionWithOutputStream() throws IOException {
    // Given
    String input = SAMPLE_JSON;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // When
    List<CommandResult> results =
        StreamBuilder.create().fromString(input).formatJson().toStream(outputStream);

    // Then
    assertNotNull(results);
    assertEquals(1, results.size());
    assertTrue(outputStream.size() > 0);
  }

  @Test
  @DisplayName("パイプライン実行と文字列結果取得")
  void testPipelineExecutionWithStringResult() throws IOException {
    // Given
    String input = SAMPLE_CSV;

    // When
    String result = StreamBuilder.create().fromString(input).extractCsv("name").toString();

    // Then
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  @DisplayName("StreamConverter構築と再利用")
  void testStreamConverterBuilding() throws IOException {
    // Given
    StreamBuilder builder =
        StreamBuilder.create().fromString("test data").process("processor1").process("processor2");

    // When
    StreamConverter converter = builder.build();

    // Then
    assertNotNull(converter);

    // 構築後もビルダーは再利用可能
    StreamConverter converter2 = builder.build();
    assertNotNull(converter2);
    assertNotSame(converter, converter2);
  }

  @Test
  @DisplayName("入力ソースが設定されていない場合のエラー")
  void testErrorWhenNoInputSource() {
    // Given
    StreamBuilder builder = StreamBuilder.create().process("test");

    // When & Then
    assertThrows(
        IllegalStateException.class,
        () -> {
          builder.asString();
        });
  }

  @Test
  @DisplayName("コマンドが設定されていない場合のエラー")
  void testErrorWhenNoCommands() {
    // Given
    StreamBuilder builder = StreamBuilder.create().fromString("test data");

    // When & Then
    assertThrows(
        IllegalStateException.class,
        () -> {
          builder.build();
        });
  }

  @Test
  @DisplayName("null値チェック")
  void testNullValueChecks() {
    StreamBuilder builder = StreamBuilder.create();

    // Input sources
    assertThrows(NullPointerException.class, () -> builder.fromString(null));
    assertThrows(NullPointerException.class, () -> builder.fromStream(null));
    assertThrows(NullPointerException.class, () -> builder.fromFile(null));

    // Commands
    assertThrows(NullPointerException.class, () -> builder.extractJson(null));
    assertThrows(NullPointerException.class, () -> builder.extractCsv(null));
    assertThrows(NullPointerException.class, () -> builder.extractXml(null));
    assertThrows(NullPointerException.class, () -> builder.process(null));
    assertThrows(NullPointerException.class, () -> builder.addCommand(null));
    assertThrows(NullPointerException.class, () -> builder.sendHttp(null));
    assertThrows(NullPointerException.class, () -> builder.convertEncoding(null, "UTF-8"));
    assertThrows(NullPointerException.class, () -> builder.convertEncoding("UTF-8", null));

    // Terminal operations
    assertThrows(NullPointerException.class, () -> builder.toStream(null));
    assertThrows(NullPointerException.class, () -> builder.toFile(null));

    // Conditional processing
    assertThrows(NullPointerException.class, () -> builder.when(true, null));
  }

  @Test
  @DisplayName("パイプライン情報の詳細確認")
  void testPipelineInformation() throws IOException {
    // Given & When
    StreamBuilder builder =
        StreamBuilder.create()
            .fromString("test")
            .extractJson("$.test")
            .process("processor")
            .convertEncoding("UTF-8", "UTF-16");

    String pipelineInfo = builder.getPipelineInfo();

    // Then
    assertTrue(pipelineInfo.contains("3 commands"));
    assertTrue(pipelineInfo.contains("1. JsonNavigateCommand"));
    assertTrue(pipelineInfo.contains("2. SampleStreamCommand"));
    assertTrue(pipelineInfo.contains("3. CharacterConvertCommand"));

    assertEquals(3, builder.getCommandCount());
    assertFalse(builder.isEmpty());
  }

  @Test
  @DisplayName("複雑なパイプライン構築と実行")
  void testComplexPipelineConstruction() throws IOException {
    // Given
    String jsonInput =
        """
            {
                "data": {
                    "records": [
                        {"id": 1, "name": "Item1"},
                        {"id": 2, "name": "Item2"}
                    ]
                }
            }
            """;

    // When
    String result =
        StreamBuilder.create()
            .fromString(jsonInput)
            .formatJson()
            .extractJson("data")
            .process("data-processor")
            .when(true, builder -> builder.process("conditional-processor"))
            .process("final-formatter")
            .toString();

    // Then
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }
}
