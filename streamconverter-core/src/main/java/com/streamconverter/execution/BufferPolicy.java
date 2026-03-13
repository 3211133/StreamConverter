package com.streamconverter.execution;

/**
 * パイプライン内の PipedStream バッファサイズポリシー。
 *
 * <p>バッファサイズはスループットとメモリ使用量のトレードオフに影響する。 大きなバッファはスループットを向上させるが、メモリ消費が増加する。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // 固定バッファ（64KB）
 * MemoryBudget budget = MemoryBudget.builder()
 *     .bufferPolicy(BufferPolicy.fixed(64 * 1024))
 *     .build();
 *
 * // デフォルトバッファ（64KB）
 * BufferPolicy policy = BufferPolicy.defaultPolicy();
 * }</pre>
 */
public final class BufferPolicy {

  /** デフォルトバッファサイズ: 64KB */
  public static final int DEFAULT_BUFFER_SIZE_BYTES = 64 * 1024;

  private final int bufferSizeBytes;

  private BufferPolicy(int bufferSizeBytes) {
    if (bufferSizeBytes <= 0) {
      throw new IllegalArgumentException(
          "bufferSizeBytes must be positive, but was: " + bufferSizeBytes);
    }
    this.bufferSizeBytes = bufferSizeBytes;
  }

  /**
   * デフォルトバッファポリシー（64KB）を返す。
   *
   * @return デフォルトの BufferPolicy
   */
  public static BufferPolicy defaultPolicy() {
    return new BufferPolicy(DEFAULT_BUFFER_SIZE_BYTES);
  }

  /**
   * 固定サイズバッファポリシーを作成する。
   *
   * @param bytes バッファサイズ（バイト）。正の値であること
   * @return 指定サイズの BufferPolicy
   * @throws IllegalArgumentException bytes が 0 以下の場合
   */
  public static BufferPolicy fixed(int bytes) {
    return new BufferPolicy(bytes);
  }

  /**
   * バッファサイズ（バイト）を返す。
   *
   * @return バッファサイズ
   */
  public int getBufferSizeBytes() {
    return bufferSizeBytes;
  }

  @Override
  public String toString() {
    return "BufferPolicy{bufferSizeBytes=" + bufferSizeBytes + "}";
  }
}
