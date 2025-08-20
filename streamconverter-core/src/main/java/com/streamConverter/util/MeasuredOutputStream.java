package com.streamConverter.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 出力ストリームのデータサイズを測定するラッパークラス
 *
 * <p>このクラスは、OutputStreamをラップして書き込んだバイト数を自動的に計測します。 自動ログ出力機能でパフォーマンス測定に使用されます。
 */
public class MeasuredOutputStream extends OutputStream {
  private final OutputStream delegate;
  private long bytesWritten;
  private boolean closed;

  /**
   * コンストラクタ
   *
   * @param delegateStream ラップ対象のOutputStream
   */
  public MeasuredOutputStream(final OutputStream delegateStream) {
    super();
    this.delegate =
        java.util.Objects.requireNonNull(
            delegateStream, "delegate OutputStream cannot be null");
  }

  @Override
  public void write(final int value) throws IOException {
    if (closed) {
      throw new IOException("Stream is closed");
    }

    delegate.write(value);
    bytesWritten++;
  }

  @Override
  public void write(final byte[] buffer) throws IOException {
    if (closed) {
      throw new IOException("Stream is closed");
    }

    delegate.write(buffer);
    bytesWritten += buffer.length;
  }

  @Override
  public void write(final byte[] buffer, final int offset, final int length)
      throws IOException {
    if (closed) {
      throw new IOException("Stream is closed");
    }

    delegate.write(buffer, offset, length);
    bytesWritten += length;
  }

  @Override
  public void flush() throws IOException {
    if (!closed) {
      delegate.flush();
    }
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      delegate.close();
    }
  }

  /**
   * 現在までに書き込んだバイト数を取得
   *
   * @return 書き込んだバイト数
   */
  public long getBytesWritten() {
    return bytesWritten;
  }

  /**
   * ストリームがクローズされているかどうかを確認
   *
   * @return クローズされている場合true
   */
  public boolean isClosed() {
    return closed;
  }
}
