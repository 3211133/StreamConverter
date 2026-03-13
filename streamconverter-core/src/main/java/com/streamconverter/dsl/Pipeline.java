package com.streamconverter.dsl;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.io.Sink;
import com.streamconverter.io.Source;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * タイプセーフなパイプライン構築用 Fluent Builder。
 *
 * <p>{@link Source} → {@link IStreamCommand} チェーン → {@link Sink} の流れで パイプラインを宣言的に構築・実行する。内部では
 * {@link StreamConverter} を使用する。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * Pipeline.input(Source.ofFile(Path.of("data.csv")))
 *     .then(new CsvNavigateCommand(new CSVPath("name"), new PassThroughRule()))
 *     .then(new CharacterConvertCommand("UTF-8", "UTF-16"))
 *     .to(Sink.toFile(Path.of("output.txt")));
 * }</pre>
 *
 * @see Source
 * @see Sink
 * @see StreamConverter
 */
public final class Pipeline {

  private final Source source;
  private final List<IStreamCommand> commands;

  private Pipeline(Source source, List<IStreamCommand> commands) {
    this.source = source;
    this.commands = commands;
  }

  /**
   * 指定した {@link Source} からパイプラインを開始する。
   *
   * @param source 入力ソース
   * @return このパイプラインの Builder
   * @throws NullPointerException source が null の場合
   */
  public static Pipeline input(Source source) {
    Objects.requireNonNull(source, "source must not be null");
    return new Pipeline(source, new ArrayList<>());
  }

  /**
   * パイプラインにコマンドを追加する。
   *
   * <p>コマンドは追加された順序で実行される。
   *
   * @param command パイプラインに追加するコマンド
   * @return このパイプラインの Builder（メソッドチェーン用）
   * @throws NullPointerException command が null の場合
   */
  public Pipeline then(IStreamCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    List<IStreamCommand> newCommands = new ArrayList<>(this.commands);
    newCommands.add(command);
    return new Pipeline(this.source, newCommands);
  }

  /**
   * パイプラインを実行して結果を指定の {@link Sink} に書き込む。
   *
   * <p>少なくとも 1 つのコマンドが追加されていなければならない。 Source と Sink のストリームはこのメソッド内で自動的に管理される。
   *
   * @param sink 出力シンク
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   * @throws NullPointerException sink が null の場合
   * @throws IllegalStateException コマンドが 1 つも追加されていない場合
   */
  public void to(Sink sink) throws IOException {
    Objects.requireNonNull(sink, "sink must not be null");
    if (commands.isEmpty()) {
      throw new IllegalStateException(
          "Pipeline has no commands. Add at least one command with .then()");
    }
    StreamConverter converter = StreamConverter.create(commands);
    converter.run(source, sink);
  }
}
