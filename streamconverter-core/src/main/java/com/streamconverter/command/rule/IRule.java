package com.streamconverter.command.rule;

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
 * // 単純な変換ルール
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
   * @param input 変換対象の文字列
   * @return String output 変換結果を格納する文字列
   */
  String apply(String input);
}
