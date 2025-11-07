package com.streamconverter.path;

/**
 * 最小限のPathインターフェース
 *
 * <p>このインターフェースは、StreamConverterで使用される様々な種類のパスセレクタ （TreePath、TreePath、CSVパス）の統一的な契約を提供します。
 *
 * <p>Don't Ask, Tell原則に従い、パス一致判定のみに特化した設計です。
 *
 * <p>このインターフェースは関数型インターフェースです。ラムダ式やメソッド参照で実装できます。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * IPath<JsonNode> rootPath = node -> node.isRoot();
 * IPath<Integer> evenColumnPath = col -> col % 2 == 0;
 * }</pre>
 *
 * @param <T> コンテキスト型（TreePath=JsonNode, TreePath=List&lt;String&gt;, CSVPath=Integer）
 */
@FunctionalInterface
public interface IPath<T> {

  /**
   * 指定されたコンテキストがこのパスにマッチするか判定
   *
   * @param context 判定対象のコンテキスト
   * @return マッチする場合true
   */
  boolean matches(T context);
}
