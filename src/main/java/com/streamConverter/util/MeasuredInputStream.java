package com.streamConverter.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * 入力ストリームのデータサイズを測定するラッパークラス
 *
 * <p>このクラスは、InputStreamをラップして読み取ったバイト数を自動的に計測します。 自動ログ出力機能でパフォーマンス測定に使用されます。
 */
public class MeasuredInputStream extends InputStream {
  private final InputStream delegate;
  private long bytesRead = 0;
  private boolean closed = false;

  /**
   * コンストラクタ
   *
   * @param delegate ラップ対象のInputStream
   */
  public MeasuredInputStream(InputStream delegate) {
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public int read() throws IOException {
    if (closed) {
      return -1;
    }

    int result = delegate.read();
    if (result != -1) {
      bytesRead++;
    }
    return result;
  }

  @Override
  public int read(byte[] b) throws IOException {
    if (closed) {
      return -1;
    }

    int result = delegate.read(b);
    if (result > 0) {
      bytesRead += result;
    }
    return result;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (closed) {
      return -1;
    }

    int result = delegate.read(b, off, len);
    if (result > 0) {
      bytesRead += result;
    }
    return result;
  }

  @Override
  public long skip(long n) throws IOException {
    if (closed) {
      return 0;
    }

    long result = delegate.skip(n);
    if (result > 0) {
      bytesRead += result;
    }
    return result;
  }

  @Override
  public int available() throws IOException {
    if (closed) {
      return 0;
    }
    return delegate.available();
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      delegate.close();
    }
  }

  @Override
  public synchronized void mark(int readlimit) {
    if (!closed) {
      delegate.mark(readlimit);
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
