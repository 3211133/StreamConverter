package com.streamconverter;

import java.io.IOException;

/**
 * 対向コマンドの異常終了により、パイプへの read/write が継続不可能になったことを示す例外。
 *
 * <p>{@link AbortablePipedStream} が、対向コマンドの異常終了を検知した際に read/write 呼び出し元へ送出する。
 *
 * <p>コマンド実装者はこの例外を通常の {@link IOException} として扱ってよい。 {@link StreamConverter}
 * はこの例外を「パイプ経由の副次的失敗」とみなし、 対向コマンドの根本例外を優先して呼び出し元に伝える。
 *
 * @see AbortablePipedStream
 */
public class PipeAbortedException extends IOException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a {@link PipeAbortedException} with no detail message.
   *
   * <p>This exception indicates that a paired command aborted and pipe IO can no longer continue.
   */
  public PipeAbortedException() {
    super("Pipe aborted");
  }
}
