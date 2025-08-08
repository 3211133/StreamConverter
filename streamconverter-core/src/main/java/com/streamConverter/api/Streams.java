package com.streamConverter.api;

import java.io.InputStream;

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
 *     .asString();
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
 *     .asString();
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
   * @return JSON形式に設定されたStreamBuilder
   */
  public static StreamBuilder json(String jsonString) {
    return StreamBuilder.create().fromString(jsonString).asJson();
  }

  /**
   * CSV文字列からストリーム処理を開始
   *
   * @param csvString CSV文字列
   * @return CSV形式に設定されたStreamBuilder
   */
  public static StreamBuilder csv(String csvString) {
    return StreamBuilder.create().fromString(csvString).asCsv();
  }

  /**
   * XML文字列からストリーム処理を開始
   *
   * @param xmlString XML文字列
   * @return XML形式に設定されたStreamBuilder
   */
  public static StreamBuilder xml(String xmlString) {
    return StreamBuilder.create().fromString(xmlString).asXml();
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
}
