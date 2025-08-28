package com.streamConverter.path;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 全Path実装の共通基底クラス テンプレートメソッドパターンで統一された振る舞いを提供
 *
 * <p>このクラスは、すべてのPath実装に共通する機能を提供します： - パフォーマンス統計の自動収集 - 統一されたエラーハンドリング - テンプレートメソッドパターンによる実装の標準化
 *
 * @param <T> コンテキスト型
 */
public abstract class AbstractPath<T> implements IPath<T> {

  protected final String path;
  protected final String pathType;
  protected final PathStatistics statistics;

  /**
   * AbstractPathのコンストラクタ
   *
   * @param path パス文字列
   * @param pathType パス種別
   */
  protected AbstractPath(String path, String pathType) {
    this.path = validateAndNormalize(path);
    this.pathType = pathType;
    this.statistics = new PathStatistics();
    validate();
  }

  // === 共通実装 ===

  @Override
  public final String getPath() {
    return path;
  }

  @Override
  public final String getType() {
    return pathType;
  }

  @Override
  public final PathStatistics getStatistics() {
    return statistics;
  }

  @Override
  public final boolean isEquivalentTo(IPath<?> other) {
    return Objects.equals(this.path, other.getPath())
        && Objects.equals(this.pathType, other.getType());
  }

  // === サブクラス実装必須（Template Method Pattern）===

  /**
   * パス文字列の検証と正規化
   *
   * @param rawPath 生のパス文字列
   * @return 正規化されたパス文字列
   * @throws IllegalArgumentException 不正なパスの場合
   */
  protected abstract String validateAndNormalize(String rawPath);

  /**
   * コンテキストマッチングの実装
   *
   * @param context 判定対象のコンテキスト
   * @return マッチする場合true
   */
  protected abstract boolean doMatches(T context);

  /**
   * 単一値抽出の実装
   *
   * @param data 抽出元データ
   * @param resultType 期待する結果型
   * @param <R> 結果型
   * @return 抽出された値（見つからない場合はEmpty）
   */
  protected abstract <R> Optional<R> doExtract(Object data, Class<R> resultType);

  /**
   * 複数値抽出の実装
   *
   * @param data 抽出元データ
   * @param resultType 期待する結果型
   * @param <R> 結果型
   * @return 抽出された値のストリーム
   */
  protected abstract <R> Stream<R> doExtractAll(Object data, Class<R> resultType);

  // === テンプレートメソッド（統計付き）===

  @Override
  public final boolean matches(T context) {
    statistics.incrementMatchAttempts();
    try {
      boolean result = doMatches(context);
      if (result) {
        statistics.incrementMatchSuccesses();
      }
      return result;
    } catch (Exception e) {
      // マッチエラーは失敗として記録
      return false;
    }
  }

  @Override
  public final <R> Optional<R> extract(Object data, Class<R> resultType) {
    statistics.incrementExtractAttempts();
    try {
      Optional<R> result = doExtract(data, resultType);
      if (result.isPresent()) {
        statistics.incrementExtractSuccesses();
      }
      return result;
    } catch (Exception e) {
      // 抽出エラーはEmptyとして返す
      return Optional.empty();
    }
  }

  @Override
  public final <R> Stream<R> extractAll(Object data, Class<R> resultType) {
    statistics.incrementExtractAttempts();
    try {
      Stream<R> result = doExtractAll(data, resultType);
      // Note: Stream評価は遅延なので統計更新は困難
      // 成功かどうかはStream消費時にしかわからない
      return result;
    } catch (Exception e) {
      // 抽出エラーは空のStreamとして返す
      return Stream.empty();
    }
  }

  // === 共通ユーティリティメソッド ===

  /**
   * 文字列がnullまたは空かチェック
   *
   * @param str チェック対象文字列
   * @return nullまたは空の場合true
   */
  protected static boolean isNullOrEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }

  /**
   * 結果型の妥当性チェック
   *
   * @param resultType 結果型
   * @throws IllegalArgumentException 不正な結果型の場合
   */
  protected static void validateResultType(Class<?> resultType) {
    if (resultType == null) {
      throw new IllegalArgumentException("Result type cannot be null");
    }
  }

  @Override
  public String toString() {
    return String.format("%s('%s')", getType(), getPath());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    AbstractPath<?> that = (AbstractPath<?>) obj;
    return Objects.equals(path, that.path) && Objects.equals(pathType, that.pathType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, pathType);
  }
}
