package com.streamconverter.dsl;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.execution.ErrorPolicy;
import com.streamconverter.execution.ExecutionStrategy;
import com.streamconverter.execution.MemoryBudget;
import com.streamconverter.execution.ParallelExecutionStrategy;
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
 * <p>条件付きコマンド追加・エラーポリシー設定が {@link StreamConverter} 直接使用より簡潔に記述できる:
 *
 * <pre>{@code
 * boolean needsCharConvert = !targetEncoding.equals("UTF-8");
 * Pipeline.input(Source.ofFile(inputPath))
 *     .then(new CsvNavigateCommand(new CSVPath("name"), new PassThroughRule()))
 *     .thenIf(needsCharConvert, new CharacterConvertCommand("UTF-8", targetEncoding))
 *     .withErrorPolicy(ErrorPolicy.retry(3, 100))
 *     .withExecutionStrategy(new SequentialExecutionStrategy())
 *     .to(Sink.toFile(outputPath));
 * }</pre>
 *
 * @see Source
 * @see Sink
 * @see StreamConverter
 */
public final class Pipeline {

  private final Source source;
  private final List<IStreamCommand> commands;
  private final ExecutionStrategy strategy;
  private final ErrorPolicy errorPolicy;
  private final MemoryBudget memoryBudget;

  private Pipeline(
      Source source,
      List<IStreamCommand> commands,
      ExecutionStrategy strategy,
      ErrorPolicy errorPolicy,
      MemoryBudget memoryBudget) {
    this.source = source;
    this.commands = commands;
    this.strategy = strategy;
    this.errorPolicy = errorPolicy;
    this.memoryBudget = memoryBudget;
  }

  /**
   * 指定した {@link Source} からパイプラインを開始する。
   *
   * <p>デフォルト設定: {@link ParallelExecutionStrategy}、{@link ErrorPolicy#failFast()}、{@link
   * MemoryBudget#defaultBudget()}。
   *
   * @param source 入力ソース
   * @return このパイプラインの Builder
   * @throws NullPointerException source が null の場合
   */
  public static Pipeline input(Source source) {
    Objects.requireNonNull(source, "source must not be null");
    return new Pipeline(
        source,
        new ArrayList<>(),
        new ParallelExecutionStrategy(),
        ErrorPolicy.failFast(),
        MemoryBudget.defaultBudget());
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
    return new Pipeline(
        this.source, newCommands, this.strategy, this.errorPolicy, this.memoryBudget);
  }

  /**
   * 条件が真の場合のみコマンドを追加する。
   *
   * <p>条件が偽の場合は同じ {@link Pipeline} インスタンスを返す（コマンドは追加されない）。 実行時の条件分岐を if 文なしに記述できる:
   *
   * <pre>{@code
   * pipeline.thenIf(needsConversion, new CharacterConvertCommand("UTF-8", targetEncoding))
   * }</pre>
   *
   * @param condition コマンドを追加するかどうかの条件
   * @param command 条件が真の場合に追加するコマンド
   * @return condition が真の場合はコマンドを追加した新しい Pipeline、偽の場合は同じ Pipeline
   * @throws NullPointerException command が null の場合
   */
  public Pipeline thenIf(boolean condition, IStreamCommand command) {
    return condition ? then(command) : this;
  }

  /**
   * 指定した {@link ErrorPolicy} を設定した新しい Pipeline を返す。
   *
   * @param errorPolicy 設定するエラーポリシー
   * @return errorPolicy を設定した新しい Pipeline
   * @throws NullPointerException errorPolicy が null の場合
   */
  public Pipeline withErrorPolicy(ErrorPolicy errorPolicy) {
    Objects.requireNonNull(errorPolicy, "errorPolicy must not be null");
    return new Pipeline(this.source, this.commands, this.strategy, errorPolicy, this.memoryBudget);
  }

  /**
   * 指定した {@link ExecutionStrategy} を設定した新しい Pipeline を返す。
   *
   * @param strategy 設定する実行戦略
   * @return strategy を設定した新しい Pipeline
   * @throws NullPointerException strategy が null の場合
   */
  public Pipeline withExecutionStrategy(ExecutionStrategy strategy) {
    Objects.requireNonNull(strategy, "strategy must not be null");
    return new Pipeline(this.source, this.commands, strategy, this.errorPolicy, this.memoryBudget);
  }

  /**
   * 指定した {@link MemoryBudget} を設定した新しい Pipeline を返す。
   *
   * @param memoryBudget 設定するメモリバジェット
   * @return memoryBudget を設定した新しい Pipeline
   * @throws NullPointerException memoryBudget が null の場合
   */
  public Pipeline withMemoryBudget(MemoryBudget memoryBudget) {
    Objects.requireNonNull(memoryBudget, "memoryBudget must not be null");
    return new Pipeline(this.source, this.commands, this.strategy, this.errorPolicy, memoryBudget);
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
    StreamConverter converter =
        StreamConverter.create(strategy, memoryBudget, errorPolicy, commands);
    converter.run(source, sink);
  }
}
