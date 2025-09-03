package com.streamConverter.path;

/**
 * 最小限のPathインターフェース
 *
 * <p>このインターフェースは、StreamConverterで使用される様々な種類のパスセレクタ （TreePath、TreePath、CSVパス）の統一的な契約を提供します。
 *
 * <p>Don't Ask, Tell原則に従い、パス一致判定のみに特化した設計です。
 *
 * @param <T> コンテキスト型（TreePath=JsonNode, TreePath=List&lt;String&gt;, CSVPath=Integer）
 */
public interface IPath<T> {

  /**
   * 指定されたコンテキストがこのパスにマッチするか判定
   *
   * @param context 判定対象のコンテキスト
   * @return マッチする場合true
   */
  boolean matches(T context);
}
