package com.streamConverter.benchmark;

import java.time.Instant;

/**
 * リソース使用量の測定結果を表すクラス
 *
 * <p>メモリ使用量、処理時間、スループットなどのパフォーマンス指標を保持します。 5GBデータ/50MBメモリ目標の評価に使用されます。
 */
public class ResourceUsage {
  private final long durationMillis;
  private final long memoryUsedBytes;
  private final long peakMemoryBytes;
  private final long startMemoryBytes;
  private final long endMemoryBytes;
  private final long dataSizeBytes;
  private final double throughputMBps;
  private final Instant measurementTime;

  /**
   * リソース使用量結果を構築します
   *
   * @param durationMillis 処理時間（ミリ秒）
   * @param memoryUsedBytes 使用メモリ量（バイト）
   * @param peakMemoryBytes ピークメモリ使用量（バイト）
   * @param startMemoryBytes 開始時メモリ使用量（バイト）
   * @param endMemoryBytes 終了時メモリ使用量（バイト）
   * @param dataSizeBytes 処理したデータサイズ（バイト）
   * @param throughputMBps スループット（MB/秒）
   * @param measurementTime 測定時刻
   */
  public ResourceUsage(
      long durationMillis,
      long memoryUsedBytes,
      long peakMemoryBytes,
      long startMemoryBytes,
      long endMemoryBytes,
      long dataSizeBytes,
      double throughputMBps,
      Instant measurementTime) {
    this.durationMillis = durationMillis;
    this.memoryUsedBytes = memoryUsedBytes;
    this.peakMemoryBytes = peakMemoryBytes;
    this.startMemoryBytes = startMemoryBytes;
    this.endMemoryBytes = endMemoryBytes;
    this.dataSizeBytes = dataSizeBytes;
    this.throughputMBps = throughputMBps;
    this.measurementTime = measurementTime;
  }

  /**
   * 処理時間を取得（ミリ秒）
   *
   * @return 処理時間
   */
  public long getDurationMillis() {
    return durationMillis;
  }

  /**
   * 使用メモリ量を取得（バイト）
   *
   * @return 使用メモリ量
   */
  public long getMemoryUsedBytes() {
    return memoryUsedBytes;
  }

  /**
   * 使用メモリ量を取得（MB）
   *
   * @return 使用メモリ量（MB）
   */
  public double getMemoryUsedMB() {
    return memoryUsedBytes / 1024.0 / 1024.0;
  }

  /**
   * ピークメモリ使用量を取得（バイト）
   *
   * @return ピークメモリ使用量
   */
  public long getPeakMemoryBytes() {
    return peakMemoryBytes;
  }

  /**
   * ピークメモリ使用量を取得（MB）
   *
   * @return ピークメモリ使用量（MB）
   */
  public double getPeakMemoryMB() {
    return peakMemoryBytes / 1024.0 / 1024.0;
  }

  /**
   * 開始時メモリ使用量を取得（バイト）
   *
   * @return 開始時メモリ使用量
   */
  public long getStartMemoryBytes() {
    return startMemoryBytes;
  }

  /**
   * 終了時メモリ使用量を取得（バイト）
   *
   * @return 終了時メモリ使用量
   */
  public long getEndMemoryBytes() {
    return endMemoryBytes;
  }

  /**
   * 処理したデータサイズを取得（バイト）
   *
   * @return データサイズ
   */
  public long getDataSizeBytes() {
    return dataSizeBytes;
  }

  /**
   * 処理したデータサイズを取得（MB）
   *
   * @return データサイズ（MB）
   */
  public double getDataSizeMB() {
    return dataSizeBytes / 1024.0 / 1024.0;
  }

  /**
   * スループットを取得（MB/秒）
   *
   * @return スループット
   */
  public double getThroughputMBps() {
    return throughputMBps;
  }

  /**
   * 測定時刻を取得
   *
   * @return 測定時刻
   */
  public Instant getMeasurementTime() {
    return measurementTime;
  }

  /**
   * 5GBデータ/50MBメモリ目標を満たしているかを確認
   *
   * @return 目標を満たしている場合true
   */
  public boolean meets5GB50MBTarget() {
    return getMemoryUsedMB() <= 50.0 && getThroughputMBps() >= 100.0;
  }

  /**
   * メモリ効率性（データサイズに対するメモリ使用率）を計算
   *
   * @return メモリ効率性（0.0-1.0、低いほど効率的）
   */
  public double getMemoryEfficiency() {
    if (dataSizeBytes == 0) return 0.0;
    return (double) memoryUsedBytes / dataSizeBytes;
  }

  @Override
  public String toString() {
    return String.format(
        "ResourceUsage{duration=%dms, memoryUsed=%.2fMB, peakMemory=%.2fMB, "
            + "dataSize=%.2fMB, throughput=%.2fMB/s, efficiency=%.4f, meets5GB50MB=%s}",
        durationMillis,
        getMemoryUsedMB(),
        getPeakMemoryMB(),
        getDataSizeMB(),
        throughputMBps,
        getMemoryEfficiency(),
        meets5GB50MBTarget());
  }
}
