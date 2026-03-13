package com.streamconverter.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

/**
 * パイプラインの入力ソースを表すインターフェース。
 *
 * <p>{@link #open()} でストリームを取得し、try-with-resources で安全にクローズすること。 ファイル・既存ストリームからのファクトリメソッドを提供する。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // ファイルからのソース
 * try (InputStream in = Source.ofFile(Path.of("input.csv")).open()) {
 *     converter.run(in, outputStream);
 * }
 *
 * // Pipeline DSL での使用
 * Pipeline.input(Source.ofFile(Path.of("input.csv")))
 *     .then(new CsvNavigateCommand(...))
 *     .to(Sink.toFile(Path.of("output.csv")));
 * }</pre>
 */
@FunctionalInterface
public interface Source {

  /**
   * 入力ストリームを開いて返す。
   *
   * @return 入力ストリーム
   * @throws IOException ストリームを開けない場合
   */
  InputStream open() throws IOException;

  /**
   * 既存の {@link InputStream} を Source としてラップする。
   *
   * <p>ストリームのクローズは呼び出し側が管理する。
   *
   * @param inputStream ラップする入力ストリーム
   * @return Source
   * @throws NullPointerException inputStream が null の場合
   */
  static Source of(InputStream inputStream) {
    Objects.requireNonNull(inputStream, "inputStream must not be null");
    return () -> inputStream;
  }

  /**
   * ファイルパスから Source を作成する。
   *
   * <p>{@link #open()} が呼ばれるたびに新しい {@link FileInputStream} を開く。
   *
   * @param path 入力ファイルのパス
   * @return Source
   * @throws NullPointerException path が null の場合
   */
  static Source ofFile(Path path) {
    Objects.requireNonNull(path, "path must not be null");
    return () -> new FileInputStream(path.toFile());
  }
}
