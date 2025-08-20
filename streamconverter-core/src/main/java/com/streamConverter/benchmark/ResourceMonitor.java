package com.streamConverter.benchmark;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * リソース使用量を監視するユーティリティクラス
 *
 * <p>メモリ使用量とパフォーマンス指標を測定し、大容量データ処理の効率性を検証します。 5GBデータ/50MBメモリ目標の達成を評価するために使用されます。
 */
public class ResourceMonitor {
  private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
  private final AtomicLong peakMemory = new AtomicLong(0);
  private final AtomicLong currentMemory = new AtomicLong(0);

  private long startTime;
  private long startMemory;
  private long dataSize;
  private ScheduledExecutorService executor;
  private volatile boolean monitoring = false;

  /** Creates a new {@code ResourceMonitor}. */
  public ResourceMonitor() {}

  /**
   * モニタリングを開始します
   *
   * @param expectedDataSize 処理予定のデータサイズ（バイト）
   */
  public void start(long expectedDataSize) {
    this.dataSize = expectedDataSize;
    this.startTime = System.currentTimeMillis();

    // 初期メモリ状態を記録
    forceGC();
    this.startMemory = getCurrentMemoryUsage();
    this.peakMemory.set(startMemory);
    this.currentMemory.set(startMemory);

    // メモリ監視スレッドを開始
    this.executor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "ResourceMonitor-Thread");
              t.setDaemon(true);
              return t;
            });

    this.monitoring = true;
    this.executor.scheduleAtFixedRate(this::updateMemoryUsage, 50, 50, TimeUnit.MILLISECONDS);
  }

  /**
   * モニタリングを停止し、結果を返します
   *
   * @return リソース使用量の測定結果
   */
  public ResourceUsage stop() {
    if (!monitoring) {
      throw new IllegalStateException("Monitor not started");
    }

    monitoring = false;
    if (executor != null) {
      executor.shutdown();
      try {
        executor.awaitTermination(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    // 最終測定
    forceGC();
    long endTime = System.currentTimeMillis();
    long endMemory = getCurrentMemoryUsage();

    long duration = endTime - startTime;
    long memoryUsed = peakMemory.get() - startMemory;
    double throughputMBps =
        dataSize > 0 && duration > 0 ? (dataSize / 1024.0 / 1024.0) / (duration / 1000.0) : 0.0;

    return new ResourceUsage(
        duration,
        memoryUsed,
        peakMemory.get(),
        startMemory,
        endMemory,
        dataSize,
        throughputMBps,
        Instant.now());
  }

  /** 現在のメモリ使用量を更新 */
  private void updateMemoryUsage() {
    if (!monitoring) return;

    long current = getCurrentMemoryUsage();
    currentMemory.set(current);

    // ピーク値を更新
    long peak = peakMemory.get();
    while (current > peak && !peakMemory.compareAndSet(peak, current)) {
      peak = peakMemory.get();
    }
  }

  /**
   * 現在のヒープメモリ使用量を取得
   *
   * @return ヒープメモリ使用量（バイト）
   */
  private long getCurrentMemoryUsage() {
    MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    return heapUsage.getUsed();
  }

  /** ガベージコレクションを強制実行 */
  private void forceGC() {
    System.gc();
    System.gc(); // 2回実行してより確実にクリーンアップ
    try {
      Thread.sleep(100); // GC完了待機
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * 現在のメモリ使用量をMB単位で取得（デバッグ用）
   *
   * @return メモリ使用量（MB）
   */
  public double getCurrentMemoryMB() {
    return currentMemory.get() / 1024.0 / 1024.0;
  }

  /**
   * ピークメモリ使用量をMB単位で取得（デバッグ用）
   *
   * @return ピークメモリ使用量（MB）
   */
  public double getPeakMemoryMB() {
    return peakMemory.get() / 1024.0 / 1024.0;
  }
}
