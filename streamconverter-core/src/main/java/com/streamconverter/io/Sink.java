package com.streamconverter.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;

/**
 * パイプラインの出力シンクを表すインターフェース。
 *
 * <p>{@link #open()} でストリームを取得し、try-with-resources で安全にクローズすること。 ファイル・既存ストリームへのファクトリメソッドを提供する。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // ファイルへのシンク
 * try (OutputStream out = Sink.toFile(Path.of("output.csv")).open()) {
 *     converter.run(inputStream, out);
 * }
 *
 * // Pipeline DSL での使用
 * Pipeline.input(Source.ofFile(Path.of("input.csv")))
 *     .then(new CsvNavigateCommand(...))
 *     .to(Sink.toFile(Path.of("output.csv")));
 * }</pre>
 */
@FunctionalInterface
public interface Sink {

  /**
   * 出力ストリームを開いて返す。
   *
   * @return 出力ストリーム
   * @throws IOException ストリームを開けない場合
   */
  OutputStream open() throws IOException;

  /**
   * 既存の {@link OutputStream} を Sink としてラップする。
   *
   * <p>ストリームのクローズは呼び出し側が管理する。
   *
   * @param outputStream ラップする出力ストリーム
   * @return Sink
   * @throws NullPointerException outputStream が null の場合
   */
  static Sink of(OutputStream outputStream) {
    Objects.requireNonNull(outputStream, "outputStream must not be null");
    return () -> outputStream;
  }

  /**
   * ファイルパスへの Sink を作成する。
   *
   * <p>{@link #open()} が呼ばれるたびに新しい {@link FileOutputStream} を開く（上書きモード）。
   *
   * @param path 出力ファイルのパス
   * @return Sink
   * @throws NullPointerException path が null の場合
   */
  static Sink toFile(Path path) {
    Objects.requireNonNull(path, "path must not be null");
    return () -> new FileOutputStream(path.toFile());
  }
}
