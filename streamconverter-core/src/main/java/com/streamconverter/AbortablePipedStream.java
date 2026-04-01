package com.streamconverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * コマンド間の異常終了を伝播するためのPipedStreamペアラッパー。
 *
 * <p>いずれかのコマンドが異常終了した際に {@link #abort()} を呼ぶことで、 対向の read()/write() に {@link PipeAbortedException}
 * を送出させる。 これにより、対向コマンドは「パイプが閉じた」という IO エラーではなく、 「対向コマンドが異常終了した」という型付きの例外を受け取って終了できる。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * AbortablePipedStream pipe = new AbortablePipedStream(bufferSize);
 * // 前段コマンドには pipe.outputStream() を渡す
 * // 後段コマンドには pipe.inputStream() を渡す
 *
 * // 前段コマンドが異常終了した場合:
 * pipe.abort();
 * // → 後段コマンドの read() が PipeAbortedException を投げる
 * }</pre>
 *
 * @see PipeAbortedException
 */
public final class AbortablePipedStream implements AutoCloseable {

  private final PipedOutputStream out;
  private final PipedInputStream in;
  private final AtomicBoolean aborted = new AtomicBoolean(false);

  /**
   * 指定バッファサイズで PipedStream ペアを生成する。
   *
   * @param bufferSize パイプバッファのサイズ（バイト）。1 以上であること
   * @throws IllegalArgumentException bufferSize が 1 未満の場合
   * @throws IOException PipedStream の生成に失敗した場合
   */
  public AbortablePipedStream(int bufferSize) throws IOException {
    if (bufferSize < 1) {
      throw new IllegalArgumentException("bufferSize must be >= 1, got: " + bufferSize);
    }
    this.out = new PipedOutputStream();
    this.in = new PipedInputStream(out, bufferSize);
  }

  /**
   * 書き込み側のラッパーを返す（前段コマンドに渡す）。
   *
   * @return 前段コマンド用の OutputStream
   */
  public OutputStream outputStream() {
    return new AbortableOutputStream();
  }

  /**
   * 読み込み側のラッパーを返す（後段コマンドに渡す）。
   *
   * @return 後段コマンド用の InputStream
   */
  public InputStream inputStream() {
    return new AbortableInputStream();
  }

  /**
   * このパイプを abort 済みとしてマークする。
   *
   * <p>以降の read()/write()/flush() 呼び出しが {@link PipeAbortedException} を投げるようになる。 パイプのクローズは {@link
   * StreamConverter} の {@code closeResources()} が担うため、 このメソッドはフラグのセットのみを行う。
   *
   * <p>{@link #close()} の前後どちらで呼んでも安全。ただし {@link #close()} 後は read()/write() が呼ばれる機会がないため、abort
   * フラグは実質的に効果を持たない。
   */
  public void abort() {
    aborted.set(true);
  }

  /**
   * パイプを正常にクローズする。
   *
   * <p>{@link AutoCloseable} の実装。abort フラグはセットしない。 {@link #abort()} の前後どちらで呼んでも安全。
   */
  @Override
  public void close() throws IOException {
    try {
      out.close();
    } catch (IOException ignored) {
      // 既に閉じている場合は無視する
    }
    try {
      in.close();
    } catch (IOException ignored) {
      // 既に閉じている場合は無視する
    }
  }

  private void checkAborted() throws IOException {
    if (aborted.get()) {
      throw new PipeAbortedException();
    }
  }

  private class AbortableOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
      checkAborted();
      out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      checkAborted();
      out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      checkAborted();
      out.flush();
    }

    @Override
    public void close() throws IOException {
      out.close();
    }
  }

  private class AbortableInputStream extends InputStream {

    @Override
    public int read() throws IOException {
      checkAborted();
      return in.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      checkAborted();
      return in.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
      in.close();
    }
  }
}
