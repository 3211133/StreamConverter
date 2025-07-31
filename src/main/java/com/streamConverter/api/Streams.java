package com.streamConverter.api;

import com.streamConverter.CommandResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 高レベルなストリーム処理APIを提供するユーティリティクラス
 *
 * <p>このクラスは最も一般的なストリーム処理パターンを簡潔に記述できる 静的メソッドを提供します。Java 8のStreams APIにインスパイアされた設計です。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // JSON処理の例
 * String result = Streams.json("{'name':'John','age':30}")
 *     .validate("user.schema.json")
 *     .extract("$.name")
 *     .toString();
 *
 * // CSV処理の例
 * List<CommandResult> results = Streams.csv("id,name\n1,John\n2,Jane")
 *     .extract("name")
 *     .process("uppercase")
 *     .toStream(outputStream);
 *
 * // XMLからJSONへの変換例
 * String json = Streams.xml("<user><name>John</name></user>")
 *     .validate("user.xsd")
 *     .extract("//name")
 *     .convertEncoding("UTF-8", "UTF-16")
 *     .toString();
 * }</pre>
 */
public final class Streams {

  /** ユーティリティクラスのため、インスタンス化を禁止 */
  private Streams() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  // === Data Format Entry Points ===

  /**
   * JSON文字列からストリーム処理を開始
   *
   * @param jsonString JSON文字列
   * @return JSONに特化したStreamBuilder
   */
  public static JsonStreamBuilder json(String jsonString) {
    return new JsonStreamBuilder(jsonString);
  }

  /**
   * CSV文字列からストリーム処理を開始
   *
   * @param csvString CSV文字列
   * @return CSVに特化したStreamBuilder
   */
  public static CsvStreamBuilder csv(String csvString) {
    return new CsvStreamBuilder(csvString);
  }

  /**
   * XML文字列からストリーム処理を開始
   *
   * @param xmlString XML文字列
   * @return XMLに特化したStreamBuilder
   */
  public static XmlStreamBuilder xml(String xmlString) {
    return new XmlStreamBuilder(xmlString);
  }

  /**
   * 汎用文字列からストリーム処理を開始
   *
   * @param content 処理対象の文字列
   * @return 汎用StreamBuilder
   */
  public static StreamBuilder from(String content) {
    return StreamBuilder.create().fromString(content);
  }

  /**
   * InputStreamからストリーム処理を開始
   *
   * @param inputStream 入力ストリーム
   * @return 汎用StreamBuilder
   */
  public static StreamBuilder from(InputStream inputStream) {
    return StreamBuilder.create().fromStream(inputStream);
  }

  /**
   * ファイルからストリーム処理を開始
   *
   * @param filePath ファイルパス
   * @return 汎用StreamBuilder
   * @throws IOException ファイル読み込みエラー
   */
  public static StreamBuilder fromFile(String filePath) throws IOException {
    return StreamBuilder.create().fromFile(filePath);
  }

  // === Specialized Stream Builders ===

  /** JSON専用のStreamBuilder */
  public static class JsonStreamBuilder {
    private final StreamBuilder builder;

    private JsonStreamBuilder(String jsonString) {
      this.builder = StreamBuilder.create().fromString(jsonString);
    }

    /**
     * JSONスキーマでバリデーション
     *
     * @param schemaPath JSONスキーマファイルのパス
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public JsonStreamBuilder validate(String schemaPath) {
      builder.validateJson(schemaPath);
      return this;
    }

    /**
     * JSONPathで値を抽出
     *
     * @param jsonPath JSONPath式（例: "$.user.name"）
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public JsonStreamBuilder extract(String jsonPath) {
      builder.extractJson(jsonPath);
      return this;
    }

    /**
     * JSONをフォーマット
     *
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public JsonStreamBuilder format() {
      builder.formatJson();
      return this;
    }

    /**
     * カスタム処理を追加
     *
     * @param commandName コマンド名（ログ用）
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public JsonStreamBuilder process(String commandName) {
      builder.process(commandName);
      return this;
    }

    /**
     * 文字エンコーディング変換
     *
     * @param from 変換元エンコーディング
     * @param to 変換先エンコーディング
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public JsonStreamBuilder convertEncoding(String from, String to) {
      builder.convertEncoding(from, to);
      return this;
    }

    /**
     * HTTP送信
     *
     * @param url 送信先URL
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public JsonStreamBuilder sendHttp(String url) {
      builder.sendHttp(url);
      return this;
    }

    /**
     * 汎用StreamBuilderに変換
     *
     * @return 汎用StreamBuilderインスタンス
     */
    public StreamBuilder asGeneric() {
      return builder;
    }

    /**
     * 文字列として結果を取得
     *
     * @return 処理結果の文字列
     * @throws IOException 入出力エラーが発生した場合
     */
    public String asString() throws IOException {
      return builder.asString();
    }

    /**
     * ストリームに出力
     *
     * @param outputStream 出力先ストリーム
     * @return コマンド実行結果のリスト
     * @throws IOException 入出力エラーが発生した場合
     */
    public List<CommandResult> toStream(OutputStream outputStream) throws IOException {
      return builder.toStream(outputStream);
    }

    /**
     * ファイルに出力
     *
     * @param filePath 出力先ファイルパス
     * @return コマンド実行結果のリスト
     * @throws IOException 入出力エラーが発生した場合
     */
    public List<CommandResult> toFile(String filePath) throws IOException {
      return builder.toFile(filePath);
    }
  }

  /** CSV専用のStreamBuilder */
  public static class CsvStreamBuilder {
    private final StreamBuilder builder;

    private CsvStreamBuilder(String csvString) {
      this.builder = StreamBuilder.create().fromString(csvString);
    }

    /**
     * CSVスキーマでバリデーション
     *
     * @param requiredColumns 必須カラム名の配列
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public CsvStreamBuilder validate(String[] requiredColumns) {
      builder.validateCsv(requiredColumns);
      return this;
    }

    /**
     * 列名で値を抽出
     *
     * @param columnName 抽出対象の列名
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public CsvStreamBuilder extract(String columnName) {
      builder.extractCsv(columnName);
      return this;
    }

    /**
     * カスタム処理を追加
     *
     * @param commandName コマンド名（ログ用）
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public CsvStreamBuilder process(String commandName) {
      builder.process(commandName);
      return this;
    }

    /**
     * 文字エンコーディング変換
     *
     * @param from 変換元エンコーディング
     * @param to 変換先エンコーディング
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public CsvStreamBuilder convertEncoding(String from, String to) {
      builder.convertEncoding(from, to);
      return this;
    }

    /**
     * HTTP送信
     *
     * @param url 送信先URL
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public CsvStreamBuilder sendHttp(String url) {
      builder.sendHttp(url);
      return this;
    }

    /**
     * 汎用StreamBuilderに変換
     *
     * @return 汎用StreamBuilderインスタンス
     */
    public StreamBuilder asGeneric() {
      return builder;
    }

    /**
     * 文字列として結果を取得
     *
     * @return 処理結果の文字列
     * @throws IOException 入出力エラーが発生した場合
     */
    public String asString() throws IOException {
      return builder.asString();
    }

    /**
     * ストリームに出力
     *
     * @param outputStream 出力先ストリーム
     * @return コマンド実行結果のリスト
     * @throws IOException 入出力エラーが発生した場合
     */
    public List<CommandResult> toStream(OutputStream outputStream) throws IOException {
      return builder.toStream(outputStream);
    }

    /**
     * ファイルに出力
     *
     * @param filePath 出力先ファイルパス
     * @return コマンド実行結果のリスト
     * @throws IOException 入出力エラーが発生した場合
     */
    public List<CommandResult> toFile(String filePath) throws IOException {
      return builder.toFile(filePath);
    }
  }

  /** XML専用のStreamBuilder */
  public static class XmlStreamBuilder {
    private final StreamBuilder builder;

    private XmlStreamBuilder(String xmlString) {
      this.builder = StreamBuilder.create().fromString(xmlString);
    }

    /**
     * XMLスキーマでバリデーション
     *
     * @param schemaPath XMLスキーマファイルのパス
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public XmlStreamBuilder validate(String schemaPath) {
      builder.validateXml(schemaPath);
      return this;
    }

    /**
     * XPathで値を抽出
     *
     * @param xpath XPath式（例: "//user/name"）
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public XmlStreamBuilder extract(String xpath) {
      builder.extractXml(xpath);
      return this;
    }

    /**
     * カスタム処理を追加
     *
     * @param commandName コマンド名（ログ用）
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public XmlStreamBuilder process(String commandName) {
      builder.process(commandName);
      return this;
    }

    /**
     * 文字エンコーディング変換
     *
     * @param from 変換元エンコーディング
     * @param to 変換先エンコーディング
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public XmlStreamBuilder convertEncoding(String from, String to) {
      builder.convertEncoding(from, to);
      return this;
    }

    /**
     * HTTP送信
     *
     * @param url 送信先URL
     * @return このビルダーインスタンス（メソッドチェーン用）
     */
    public XmlStreamBuilder sendHttp(String url) {
      builder.sendHttp(url);
      return this;
    }

    /**
     * 汎用StreamBuilderに変換
     *
     * @return 汎用StreamBuilderインスタンス
     */
    public StreamBuilder asGeneric() {
      return builder;
    }

    /**
     * 文字列として結果を取得
     *
     * @return 処理結果の文字列
     * @throws IOException 入出力エラーが発生した場合
     */
    public String asString() throws IOException {
      return builder.asString();
    }

    /**
     * ストリームに出力
     *
     * @param outputStream 出力先ストリーム
     * @return コマンド実行結果のリスト
     * @throws IOException 入出力エラーが発生した場合
     */
    public List<CommandResult> toStream(OutputStream outputStream) throws IOException {
      return builder.toStream(outputStream);
    }

    /**
     * ファイルに出力
     *
     * @param filePath 出力先ファイルパス
     * @return コマンド実行結果のリスト
     * @throws IOException 入出力エラーが発生した場合
     */
    public List<CommandResult> toFile(String filePath) throws IOException {
      return builder.toFile(filePath);
    }
  }
}
