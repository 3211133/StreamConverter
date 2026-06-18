package com.streamconverter.command.rule;

import com.streamconverter.StreamProcessingException;

/**
 * ルールインターフェース
 *
 * <p>このインターフェースは、ストリーム変換のルールを定義するためのものです。 具体的なルールはこのインターフェースを実装するクラスで定義されます。
 * ルールは、ストリーム変換の際に適用される条件や処理を定義します。
 *
 * <p>このインターフェースは関数型インターフェースです。ラムダ式やメソッド参照で実装できます。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // 単純な変換ルール（例外を投げない場合はラムダのまま使用可）
 * IRule upperCaseRule = input -> input.toUpperCase();
 * IRule trimRule = String::trim;
 *
 * // 複合ルール（トリムして大文字に変換）
 * IRule trimAndUpperCase = input -> input.trim().toUpperCase();
 *
 * // StreamConverterコマンドでの使用例
 * IRule dataCleaningRule = input -> input.trim().replaceAll("\\s+", " ");
 * JsonWalker command = JsonWalker.create(
 *     TreePath.fromJson("$.user.name"),
 *     dataCleaningRule
 * );
 * }</pre>
 */
@FunctionalInterface
public interface IRule {

  /**
   * ルールの適用を実行します。
   *
   * <p>このメソッドは、ストリーム変換の際にルールを適用するために使用されます。 具体的なルールの実装は、このメソッドをオーバーライドして定義します。
   * 変換対象とする箇所を特定したあとにこのメソッドを呼び出すことを想定しています。
   *
   * <p>実装がU/T/A分類済みの例外をスローする場合は {@link StreamProcessingException} のサブタイプを
   * 直接throwすること。walker層がこれをIOExceptionとして伝播し、converter層が集約する。
   *
   * @param input 変換対象の文字列
   * @return String output 変換結果を格納する文字列
   * @throws StreamProcessingException 入力エラー（U）・外部障害（T/A）・内部障害（A）の場合
   */
  String apply(String input) throws StreamProcessingException;
}
