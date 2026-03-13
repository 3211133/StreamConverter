package com.streamconverter.execution;

/**
 * パイプライン実行時のメモリ予算設定。
 *
 * <p>バックプレッシャーとメモリ上限を宣言的に設定するための Builder を提供する。 設定値は {@code StreamConverter.create(MemoryBudget,
 * IStreamCommand...)} に渡すことで パイプラインに適用される。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * MemoryBudget budget = MemoryBudget.builder()
 *     .bufferPolicy(BufferPolicy.fixed(128 * 1024))  // 128KB バッファ
 *     .build();
 *
 * StreamConverter converter = StreamConverter.create(budget, command1, command2);
 * }</pre>
 */
public final class MemoryBudget {

  private final BufferPolicy bufferPolicy;

  private MemoryBudget(Builder builder) {
    this.bufferPolicy = builder.bufferPolicy;
  }

  /**
   * バッファポリシーを返す。
   *
   * @return BufferPolicy
   */
  public BufferPolicy getBufferPolicy() {
    return bufferPolicy;
  }

  /**
   * デフォルト設定の MemoryBudget を返す。
   *
   * @return デフォルト MemoryBudget
   */
  public static MemoryBudget defaultBudget() {
    return builder().build();
  }

  /**
   * MemoryBudget の Builder を返す。
   *
   * @return Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** MemoryBudget の Builder。 */
  public static final class Builder {

    private BufferPolicy bufferPolicy = BufferPolicy.defaultPolicy();

    private Builder() {}

    /**
     * バッファポリシーを設定する。
     *
     * @param bufferPolicy バッファポリシー
     * @return this Builder
     * @throws NullPointerException bufferPolicy が null の場合
     */
    public Builder bufferPolicy(BufferPolicy bufferPolicy) {
      if (bufferPolicy == null) {
        throw new NullPointerException("bufferPolicy must not be null");
      }
      this.bufferPolicy = bufferPolicy;
      return this;
    }

    /**
     * MemoryBudget を構築して返す。
     *
     * @return 構築された MemoryBudget
     */
    public MemoryBudget build() {
      return new MemoryBudget(this);
    }
  }

  @Override
  public String toString() {
    return "MemoryBudget{bufferPolicy=" + bufferPolicy + "}";
  }
}
