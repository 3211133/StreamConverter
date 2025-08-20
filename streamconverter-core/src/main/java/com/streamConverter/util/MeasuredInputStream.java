package com.streamConverter.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * 入力ストリームのデータサイズを測定するラッパークラス
 *
 * <p>このクラスは、InputStreamをラップして読み取ったバイト数を自動的に計測します。 自動ログ出力機能でパフォーマンス測定に使用されます。
 */
public class MeasuredInputStream extends InputStream {
  private final InputStream delegate;
  private long bytesRead;
  private boolean closed;

  /**
   * コンストラクタ
   *
   * @param delegateStream ラップ対象のInputStream
   */
  public MeasuredInputStream(final InputStream delegateStream) {
    super();
    this.delegate =
        java.util.Objects.requireNonNull(
            delegateStream, "delegate InputStream cannot be null");
  }

  @Override
  public int read() throws IOException {
    int result = -1;
    if (!closed) {
      result = delegate.read();
      if (result != -1) {
        bytesRead++;
      }
    }
    return result;
  }

  @Override
  public int read(final byte[] buffer) throws IOException {
    int result = -1;
    if (!closed) {
      result = delegate.read(buffer);
      if (result > 0) {
        bytesRead += result;
      }
    }
    return result;
  }

  @Override
  public int read(final byte[] buffer, final int offset, final int length) throws IOException {
    int result = -1;
    if (!closed) {
      result = delegate.read(buffer, offset, length);
      if (result > 0) {
        bytesRead += result;
      }
    }
    return result;
  }

  @Override
  public long skip(final long numBytes) throws IOException {
    long result = 0;
    if (!closed) {
      result = delegate.skip(numBytes);
      if (result > 0) {
        bytesRead += result;
      }
    }
    return result;
  }

  @Override
  public int available() throws IOException {
    int result = 0;
    if (!closed) {
      result = delegate.available();
    }
    return result;
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      delegate.close();
    }
  }

  @Override
  public synchronized void mark(final int readLimit) {
    if (!closed) {
      delegate.mark(readLimit);
    }
  }

  @Override
  public synchronized void reset() throws IOException {
    if (!closed) {
      delegate.reset();
    }
  }

  @Override
  public boolean markSupported() {
    return !closed && delegate.markSupported();
  }

  /**
   * 現在までに読み取ったバイト数を取得
   *
   * @return 読み取ったバイト数
   */
  public long getBytesRead() {
    return bytesRead;
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
