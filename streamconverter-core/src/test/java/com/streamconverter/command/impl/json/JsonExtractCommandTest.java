package com.streamconverter.command.impl.json;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonExtractCommandTest {

  private static ByteArrayInputStream utf8(String s) {
    return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
  }

  private static String execute(JsonExtractCommand cmd, String json) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    cmd.execute(utf8(json), out);
    return out.toString(StandardCharsets.UTF_8);
  }

  // ---- 複数マッチ ----

  @Test
  @DisplayName("$[*].name: 配列内の複数フィールドを抽出したとき各値がスペース区切りで出力される")
  void testMultipleMatches_rootArrayWildcard() throws IOException {
    String json = "[{\"name\":\"Alice\"},{\"name\":\"Bob\"},{\"name\":\"Carol\"}]";
    JsonExtractCommand cmd = JsonExtractCommand.create(TreePath.fromJson("$[*].name"));
    String result = execute(cmd, json);
    assertEquals("\"Alice\" \"Bob\" \"Carol\"", result);
  }

  @Test
  @DisplayName("$.users[*].profile.department: ネスト配列内の複数フィールドを正確に抽出する")
  void testMultipleMatches_nestedArrayWildcard() throws IOException {
    String json =
        "{\"users\":["
            + "{\"name\":\"A\",\"profile\":{\"department\":\"Engineering\"}},"
            + "{\"name\":\"B\",\"profile\":{\"department\":\"Marketing\"}}"
            + "]}";
    JsonExtractCommand cmd =
        JsonExtractCommand.create(TreePath.fromJson("$.users[*].profile.department"));
    String result = execute(cmd, json);
    assertEquals("\"Engineering\" \"Marketing\"", result);
  }

  // ---- 切り詰めストリーム ----

  @Test
  @DisplayName("オブジェクト途中で切り詰められた入力は IOException を投げる")
  void testTruncatedStream_insideObject_throwsIOException() {
    // {"name": の直後で切り詰め（値が欠落）
    String truncated = "{\"name\":";
    JsonExtractCommand cmd = JsonExtractCommand.create(TreePath.fromJson("$.name"));
    assertThrows(
        IOException.class, () -> execute(cmd, truncated), "切り詰めストリームで IOException が投げられるべき");
  }

  @Test
  @DisplayName("配列途中で切り詰められた入力は IOException を投げる")
  void testTruncatedStream_insideArray_throwsIOException() {
    // 配列が閉じられていない
    String truncated = "[{\"name\":\"Alice\"},{\"name\":\"Bob\"";
    JsonExtractCommand cmd = JsonExtractCommand.create(TreePath.fromJson("$[*].name"));
    assertThrows(
        IOException.class, () -> execute(cmd, truncated), "切り詰めストリームで IOException が投げられるべき");
  }

  @Test
  @DisplayName("フィールド名の直後でストリームが終端した場合は IOException を投げる")
  void testTruncatedStream_afterFieldName_throwsIOException() {
    // フィールド名はあるが値がない（完全にストリームが終端）
    String truncated = "{\"name\"";
    JsonExtractCommand cmd = JsonExtractCommand.create(TreePath.fromJson("$.name"));
    assertThrows(
        IOException.class, () -> execute(cmd, truncated), "フィールド値欠落で IOException が投げられるべき");
  }

  // ---- currentPath 整合性 ----

  @Test
  @DisplayName("例外後も同一インスタンスで再実行したとき currentPath が汚染されていない")
  void testCurrentPathNotCorruptedAfterException() throws IOException {
    JsonExtractCommand cmd = JsonExtractCommand.create(TreePath.fromJson("$.name"));

    // 1回目: 切り詰めで例外
    assertThrows(IOException.class, () -> execute(cmd, "{\"name\":"));

    // 2回目: 正常入力で正しく動作する（currentPath が汚染されていれば失敗する）
    String result = execute(cmd, "{\"name\":\"Alice\",\"age\":30}");
    assertEquals("\"Alice\"", result);
  }

  // ---- 正常系（既存テストの補完）----

  @Test
  @DisplayName("$[*].id: 数値フィールドを正確に抽出する")
  void testRootArrayWildcard_numericField() throws IOException {
    String json = "[{\"id\":1,\"name\":\"A\"},{\"id\":2,\"name\":\"B\"}]";
    JsonExtractCommand cmd = JsonExtractCommand.create(TreePath.fromJson("$[*].id"));
    String result = execute(cmd, json);
    assertEquals("1 2", result);
  }

  @Test
  @DisplayName("空オブジェクトへのパスは null を出力する")
  void testEmptyObject_returnsNull() throws IOException {
    JsonExtractCommand cmd = JsonExtractCommand.create(TreePath.fromJson("$.name"));
    String result = execute(cmd, "{}");
    assertEquals("null", result);
  }

  @Test
  @DisplayName("空配列への $[*].name は null を出力する")
  void testEmptyArray_returnsNull() throws IOException {
    JsonExtractCommand cmd = JsonExtractCommand.create(TreePath.fromJson("$[*].name"));
    String result = execute(cmd, "[]");
    assertEquals("null", result);
  }

  @Test
  @DisplayName("ネストオブジェクト値をそのまま抽出できる")
  void testExtractNestedObject() throws IOException {
    String json = "{\"user\":{\"name\":\"Alice\",\"age\":30}}";
    JsonExtractCommand cmd = JsonExtractCommand.create(TreePath.fromJson("$.user"));
    String result = execute(cmd, json);
    assertEquals("{\"name\":\"Alice\",\"age\":30}", result);
  }

  @Test
  @DisplayName("配列値をそのまま抽出できる")
  void testExtractArrayValue() throws IOException {
    String json = "{\"tags\":[\"java\",\"json\"]}";
    JsonExtractCommand cmd = JsonExtractCommand.create(TreePath.fromJson("$.tags"));
    String result = execute(cmd, json);
    assertEquals("[\"java\",\"json\"]", result);
  }

  // ---- TreePath ワイルドカードセグメント解析 ----

  @Test
  @DisplayName("TreePath.fromJson: $[*].name は segments=[\"name\"] としてパースされる")
  void testTreePath_rootArrayWildcard_segments() {
    TreePath path = TreePath.fromJson("$[*].name");
    assertTrue(path.matches(java.util.List.of("name")));
    assertFalse(path.matches(java.util.List.of("*", "name")));
  }

  @Test
  @DisplayName(
      "TreePath.fromJson: $.users[*].profile.department は segments=[\"users\",\"profile\",\"department\"] としてパースされる")
  void testTreePath_nestedArrayWildcard_segments() {
    TreePath path = TreePath.fromJson("$.users[*].profile.department");
    assertTrue(path.matches(java.util.List.of("users", "profile", "department")));
    assertFalse(path.matches(java.util.List.of("users", "*", "profile", "department")));
  }
}
