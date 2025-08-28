package com.streamConverter.path;

/**
 * Path統計情報（パフォーマンス監視用）
 *
 * <p>各Pathオブジェクトが自動的に収集する性能データを管理します。 マッチ処理と抽出処理の成功率を監視し、パフォーマンス分析に活用できます。
 */
public class PathStatistics {
  private long matchAttempts = 0;
  private long matchSuccesses = 0;
  private long extractAttempts = 0;
  private long extractSuccesses = 0;

  /** マッチ試行回数をインクリメント */
  public void incrementMatchAttempts() {
    matchAttempts++;
  }

  /** マッチ成功回数をインクリメント */
  public void incrementMatchSuccesses() {
    matchSuccesses++;
  }

  /** 抽出試行回数をインクリメント */
  public void incrementExtractAttempts() {
    extractAttempts++;
  }

  /** 抽出成功回数をインクリメント */
  public void incrementExtractSuccesses() {
    extractSuccesses++;
  }

  /**
   * マッチ成功率を取得
   *
   * @return 成功率（0.0-1.0）
   */
  public double getMatchSuccessRate() {
    return matchAttempts > 0 ? (double) matchSuccesses / matchAttempts : 0.0;
  }

  /**
   * 抽出成功率を取得
   *
   * @return 成功率（0.0-1.0）
   */
  public double getExtractSuccessRate() {
    return extractAttempts > 0 ? (double) extractSuccesses / extractAttempts : 0.0;
  }

  /**
   * 総試行回数を取得
   *
   * @return マッチと抽出の合計試行回数
   */
  public long getTotalAttempts() {
    return matchAttempts + extractAttempts;
  }

  /**
   * 総成功回数を取得
   *
   * @return マッチと抽出の合計成功回数
   */
  public long getTotalSuccesses() {
    return matchSuccesses + extractSuccesses;
  }

  /**
   * マッチ試行回数を取得
   *
   * @return マッチ試行回数
   */
  public long getMatchAttempts() {
    return matchAttempts;
  }

  /**
   * マッチ成功回数を取得
   *
   * @return マッチ成功回数
   */
  public long getMatchSuccesses() {
    return matchSuccesses;
  }

  /**
   * 抽出試行回数を取得
   *
   * @return 抽出試行回数
   */
  public long getExtractAttempts() {
    return extractAttempts;
  }

  /**
   * 抽出成功回数を取得
   *
   * @return 抽出成功回数
   */
  public long getExtractSuccesses() {
    return extractSuccesses;
  }

  /** 統計情報をリセット */
  public void reset() {
    matchAttempts = 0;
    matchSuccesses = 0;
    extractAttempts = 0;
    extractSuccesses = 0;
  }

  @Override
  public String toString() {
    return String.format(
        "PathStatistics{matchRate=%.2f%% (%d/%d), extractRate=%.2f%% (%d/%d)}",
        getMatchSuccessRate() * 100,
        matchSuccesses,
        matchAttempts,
        getExtractSuccessRate() * 100,
        extractSuccesses,
        extractAttempts);
  }
}
