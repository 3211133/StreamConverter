package com.streamconverter.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * パイプラインの出力シンクを表すインターフェース。
 *
 * <p>{@link #open()} でストリームを取得する。{@link com.streamconverter.StreamConverter#run(Source, Sink)}
 * に渡した場合、ストリームの close は {@code run} メソッドが責任を持って行う。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // ファイルへのシンク（StreamConverter.run が close を管理）
 * converter.run(source, Sink.toFile(Path.of("output.csv")));
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
   * <p>呼び出し側（または {@link com.streamconverter.StreamConverter#run(Source, Sink)}）が 返されたストリームを close
   * する責任を持つ。
   *
   * @return 出力ストリーム
   * @throws IOException ストリームを開けない場合
   */
  OutputStream open() throws IOException;

  /**
   * 既存の {@link OutputStream} を Sink としてラップする。
   *
   * <p><strong>所有権の注意:</strong> {@link com.streamconverter.StreamConverter#run(Source, Sink)}
   * に渡すと、このストリームは {@code run} の完了時に close される。close 後にストリームを使用してはならない。 close
   * されることを避けたい場合は、OutputStream を直接 {@link
   * com.streamconverter.StreamConverter#run(java.io.InputStream, OutputStream)} に渡すこと。
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
   * ファイルパスへの Sink を作成する（上書きモード）。
   *
   * <p>{@link #open()} が呼ばれるたびに新しい {@link OutputStream} を開く。既存ファイルは上書きされる。
   *
   * @param path 出力ファイルのパス
   * @return Sink
   * @throws NullPointerException path が null の場合
   */
  static Sink toFile(Path path) {
    Objects.requireNonNull(path, "path must not be null");
    return () ->
        Files.newOutputStream(
            path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }
}
