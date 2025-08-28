package com.streamConverter.path;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * 完全統合されたPathインターフェース データ表現と処理機能を単一のオブジェクトに統合
 *
 * <p>このインターフェースは、StreamConverterで使用される様々な種類のパスセレクタ （JSONPath、XPath、CSVパス）の統一的な契約を提供します。
 *
 * <p>Path実装は構築時に構文を検証し、不正なパスに対して意味のある エラーメッセージを提供する必要があります。
 *
 * @param <T> コンテキスト型（JSONPath=JsonNode, XPath=List&lt;String&gt;, CSVPath=Integer）
 */
public interface IPath<T> {

  // === 基本データアクセス ===

  /**
   * パス構文の妥当性検証
   *
   * @throws IllegalArgumentException パス構文が不正な場合
   */
  void validate();

  /**
   * パス文字列を取得
   *
   * @return 正規化されたパス文字列
   */
  String getPath();

  /**
   * パス種別を取得
   *
   * @return パス種別（例: "JSONPath", "XPath", "CSVPath"）
   */
  String getType();

  /**
   * 他のパスとの等価性判定 パス文字列と型が同じかチェック
   *
   * @param other 比較対象のパス
   * @return パスが等価な場合true
   */
  boolean isEquivalentTo(IPath<?> other);

  // === 処理機能（PathHandlerの統合）===

  /**
   * 指定されたコンテキストがこのパスにマッチするか判定 旧PathHandler.isTarget()相当
   *
   * @param context 判定対象のコンテキスト
   * @return マッチする場合true
   */
  boolean matches(T context);

  /**
   * データから単一値を抽出
   *
   * @param data 抽出元データ
   * @param resultType 期待する結果型
   * @param <R> 結果型
   * @return 抽出された値（見つからない場合はEmpty）
   */
  <R> Optional<R> extract(Object data, Class<R> resultType);

  /**
   * データから複数値をストリームで抽出 配列やリスト構造に対応
   *
   * @param data 抽出元データ
   * @param resultType 期待する結果型
   * @param <R> 結果型
   * @return 抽出された値のストリーム
   */
  <R> Stream<R> extractAll(Object data, Class<R> resultType);

  // === メタ情報とパフォーマンス ===

  /**
   * 実行統計情報の取得 マッチ成功率、抽出成功率などを監視
   *
   * @return パフォーマンス統計
   */
  PathStatistics getStatistics();
}
