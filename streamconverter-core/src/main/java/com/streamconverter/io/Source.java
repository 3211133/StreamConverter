package com.streamconverter.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * パイプラインの入力ソースを表すインターフェース。
 *
 * <p>{@link #open()} でストリームを取得する。{@link com.streamconverter.StreamConverter#run(Source, Sink)}
 * に渡した場合、ストリームの close は {@code run} メソッドが責任を持って行う。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // ファイルからのソース（StreamConverter.run が close を管理）
 * converter.run(Source.ofFile(Path.of("input.csv")), sink);
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
   * <p>呼び出し側（または {@link com.streamconverter.StreamConverter#run(Source, Sink)}）が 返されたストリームを close
   * する責任を持つ。
   *
   * @return 入力ストリーム
   * @throws IOException ストリームを開けない場合
   */
  InputStream open() throws IOException;

  /**
   * 既存の {@link InputStream} を Source としてラップする。
   *
   * <p><strong>所有権の注意:</strong> {@link com.streamconverter.StreamConverter#run(Source, Sink)}
   * に渡すと、このストリームは {@code run} の完了時に close される。close 後にストリームを使用してはならない。 close
   * されることを避けたい場合は、InputStream を直接 {@link com.streamconverter.StreamConverter#run(InputStream,
   * java.io.OutputStream)} に渡すこと。
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
   * <p>{@link #open()} が呼ばれるたびに新しい {@link InputStream} を開く。
   *
   * @param path 入力ファイルのパス
   * @return Source
   * @throws NullPointerException path が null の場合
   */
  static Source ofFile(Path path) {
    Objects.requireNonNull(path, "path must not be null");
    return () -> Files.newInputStream(path);
  }
}
