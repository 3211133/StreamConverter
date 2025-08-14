package com.streamConverter.test;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * プラットフォーム適応型テストユーティリティ
 *
 * <p>異なるOS環境での性能差異を考慮したテスト実行をサポートします。 CI環境とローカル環境の違い、OS固有の性能特性を自動調整します。
 */
public class PlatformAdaptiveTestUtils {

  private static final Logger logger = LoggerFactory.getLogger(PlatformAdaptiveTestUtils.class);

  /** プラットフォーム性能係数 CI環境での実測値に基づく調整値 */
  private static final double WINDOWS_PERFORMANCE_FACTOR = 0.7; // 30%性能低下を考慮

  private static final double MACOS_PERFORMANCE_FACTOR = 0.8; // 20%性能低下を考慮
  private static final double LINUX_PERFORMANCE_FACTOR = 1.0; // ベースライン

  /** CI環境検出とリソース制約考慮 */
  private static final boolean IS_CI =
      System.getenv("CI") != null
          || System.getenv("GITHUB_ACTIONS") != null
          || System.getenv("JENKINS_URL") != null;

  /**
   * プラットフォーム適応型タイムアウト計算
   *
   * @param baseTimeoutSeconds ベースタイムアウト時間（秒）
   * @return プラットフォーム調整済みタイムアウト時間（秒）
   */
  public static long getAdaptiveTimeout(long baseTimeoutSeconds) {
    double platformFactor = getPlatformPerformanceFactor();
    double ciFactor = IS_CI ? 1.5 : 1.0; // CI環境では50%余裕を持たせる

    long adaptiveTimeout = Math.round(baseTimeoutSeconds / platformFactor * ciFactor);

    logger.debug(
        "Adaptive timeout: base={}s, platform={}, ci={}, result={}s",
        baseTimeoutSeconds,
        platformFactor,
        ciFactor,
        adaptiveTimeout);

    return Math.max(adaptiveTimeout, baseTimeoutSeconds); // 最小でもベース時間は保証
  }

  /**
   * プラットフォーム適応型メモリしきい値計算
   *
   * @param baseMemoryMB ベースメモリ使用量（MB）
   * @return プラットフォーム調整済みメモリしきい値（MB）
   */
  public static long getAdaptiveMemoryThreshold(long baseMemoryMB) {
    double platformFactor = getPlatformPerformanceFactor();
    double gcFactor = getGCFactor(); // GC特性による調整

    long adaptiveThreshold = Math.round(baseMemoryMB / platformFactor * gcFactor);

    logger.debug(
        "Adaptive memory threshold: base={}MB, platform={}, gc={}, result={}MB",
        baseMemoryMB,
        platformFactor,
        gcFactor,
        adaptiveThreshold);

    return Math.max(adaptiveThreshold, baseMemoryMB);
  }

  /**
   * プラットフォーム適応型スループット期待値計算
   *
   * @param baseThroughputMBps ベーススループット（MB/s）
   * @return プラットフォーム調整済み最小期待スループット（MB/s）
   */
  public static double getAdaptiveThroughput(double baseThroughputMBps) {
    double platformFactor = getPlatformPerformanceFactor();
    double ioFactor = getIOFactor(); // I/O性能特性による調整

    double adaptiveThroughput = baseThroughputMBps * platformFactor * ioFactor;

    logger.debug(
        "Adaptive throughput: base={}MB/s, platform={}, io={}, result={}MB/s",
        baseThroughputMBps,
        platformFactor,
        ioFactor,
        adaptiveThroughput);

    return Math.max(adaptiveThroughput, baseThroughputMBps * 0.5); // 最低50%は期待
  }

  /**
   * プラットフォーム固有リソース検出
   *
   * @return 十分なリソースがあるかどうか
   */
  public static boolean hasAdequateResources() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    int availableProcessors = runtime.availableProcessors();

    // プラットフォーム別最小リソース要件
    long minMemoryMB = IS_CI ? 1024L * 1024 * 1024 : 512L * 1024 * 1024; // CI: 1GB, Local: 512MB
    int minProcessors = IS_CI ? 2 : 1;

    boolean hasMemory = maxMemory >= minMemoryMB;
    boolean hasProcessors = availableProcessors >= minProcessors;

    if (!hasMemory || !hasProcessors) {
      logger.warn(
          "Insufficient resources: memory={}MB (min={}MB), processors={} (min={})",
          maxMemory / 1024 / 1024,
          minMemoryMB / 1024 / 1024,
          availableProcessors,
          minProcessors);
    }

    return hasMemory && hasProcessors;
  }

  /**
   * プラットフォーム別テストデータサイズ調整
   *
   * @param baseSize ベースデータサイズ（バイト）
   * @return プラットフォーム調整済みデータサイズ（バイト）
   */
  public static long getAdaptiveDataSize(long baseSize) {
    double platformFactor = getPlatformPerformanceFactor();
    double resourceFactor = getResourceFactor();

    long adaptiveSize = Math.round(baseSize * platformFactor * resourceFactor);

    logger.debug(
        "Adaptive data size: base={}MB, platform={}, resource={}, result={}MB",
        baseSize / 1024 / 1024,
        platformFactor,
        resourceFactor,
        adaptiveSize / 1024 / 1024);

    return Math.max(adaptiveSize, baseSize / 4); // 最小でも25%は実行
  }

  /** 現在のプラットフォーム情報をログ出力 */
  public static void logPlatformInfo() {
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    Runtime runtime = Runtime.getRuntime();

    logger.info("=== Platform Information ===");
    logger.info(
        "OS: {} {} {}",
        System.getProperty("os.name"),
        System.getProperty("os.version"),
        System.getProperty("os.arch"));
    logger.info(
        "Java: {} {} ({})",
        System.getProperty("java.version"),
        System.getProperty("java.vendor"),
        System.getProperty("java.vm.name"));
    logger.info(
        "Memory: Max={}MB, Available Processors={}",
        runtime.maxMemory() / 1024 / 1024,
        osBean.getAvailableProcessors());
    logger.info("CI Environment: {}", IS_CI);
    logger.info("Performance Factor: {}", getPlatformPerformanceFactor());
  }

  // Private helper methods

  private static double getPlatformPerformanceFactor() {
    if (OS.WINDOWS.isCurrentOs()) {
      return WINDOWS_PERFORMANCE_FACTOR;
    } else if (OS.MAC.isCurrentOs()) {
      return MACOS_PERFORMANCE_FACTOR;
    } else {
      return LINUX_PERFORMANCE_FACTOR;
    }
  }

  private static double getGCFactor() {
    String gcName =
        ManagementFactory.getGarbageCollectorMXBeans().stream()
            .findFirst()
            .map(gc -> gc.getName())
            .orElse("Unknown");

    // GCアルゴリズム別調整（簡易版）
    if (gcName.contains("G1") || gcName.contains("ZGC") || gcName.contains("Shenandoah")) {
      return 1.2; // 低レイテンシGCは若干メモリ余裕
    } else if (gcName.contains("Parallel")) {
      return 1.1; // 並列GC
    } else {
      return 1.0; // デフォルト
    }
  }

  private static double getIOFactor() {
    // OS固有のI/O性能特性
    if (OS.WINDOWS.isCurrentOs()) {
      return IS_CI ? 0.6 : 0.8; // Windowsは特にCI環境でI/O性能が低い
    } else if (OS.MAC.isCurrentOs()) {
      return IS_CI ? 0.7 : 0.9; // macOSはローカルでは高性能
    } else {
      return IS_CI ? 0.8 : 1.0; // LinuxはCI環境でも比較的安定
    }
  }

  private static double getResourceFactor() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();

    // メモリ量に基づくデータサイズ調整
    if (maxMemory >= 4L * 1024 * 1024 * 1024) { // 4GB+
      return 1.0;
    } else if (maxMemory >= 2L * 1024 * 1024 * 1024) { // 2GB+
      return 0.8;
    } else if (maxMemory >= 1L * 1024 * 1024 * 1024) { // 1GB+
      return 0.6;
    } else {
      return 0.4; // 1GB未満
    }
  }
}
