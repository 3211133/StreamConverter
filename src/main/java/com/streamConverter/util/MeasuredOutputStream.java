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
  private long bytesWritten = 0;
  private boolean closed = false;

  /**
   * コンストラクタ
   *
   * @param delegate ラップ対象のOutputStream
   */
  public MeasuredOutputStream(OutputStream delegate) {
    this.delegate =
        java.util.Objects.requireNonNull(delegate, "delegate OutputStream cannot be null");
  }

  @Override
  public void write(int b) throws IOException {
    if (closed) {
      throw new IOException("Stream is closed");
    }

    delegate.write(b);
    bytesWritten++;
  }

  @Override
  public void write(byte[] b) throws IOException {
    if (closed) {
      throw new IOException("Stream is closed");
    }

    delegate.write(b);
    bytesWritten += b.length;
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (closed) {
      throw new IOException("Stream is closed");
    }

    delegate.write(b, off, len);
    bytesWritten += len;
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
