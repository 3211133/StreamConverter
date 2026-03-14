package com.streamconverter.examples;

import com.streamconverter.command.impl.charcode.CharacterConvertCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.dsl.Pipeline;
import com.streamconverter.execution.ErrorPolicy;
import com.streamconverter.execution.SequentialExecutionStrategy;
import com.streamconverter.io.Sink;
import com.streamconverter.io.Source;
import com.streamconverter.path.CSVPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * {@link Pipeline} DSL の使用例。
 *
 * <p>Pipeline DSL を使用すると、{@code Source} → コマンドチェーン → {@code Sink} の流れで パイプラインを宣言的に構築・実行できる。 {@link
 * Pipeline#thenIf(boolean, com.streamconverter.command.IStreamCommand)} や {@link
 * Pipeline#withErrorPolicy(ErrorPolicy)} を使うことで、 条件分岐やエラーポリシー設定を流暢に記述できる。
 */
public class PipelineDslExample {

  /**
   * Pipeline DSL を使用した基本的なパイプライン例。
   *
   * @param args コマンドライン引数（未使用）
   * @throws IOException I/Oエラー
   */
  public static void main(String[] args) throws IOException {
    // サンプル CSV データ
    String csvData = "name,email\nAlice,alice@example.com\nBob,bob@example.com\n";
    String targetEncoding = "UTF-8";

    // thenIf を使った条件付きコマンド追加の例
    // targetEncoding が UTF-8 でなければ CharacterConvertCommand を追加する
    boolean needsCharConvert = !targetEncoding.equals("UTF-8");

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // Pipeline DSL でパイプラインを構築・実行
    // thenIf: needsCharConvert が false なので CharacterConvertCommand は追加されない
    // withErrorPolicy: リトライポリシーを設定
    // withExecutionStrategy: 逐次実行戦略を設定
    Pipeline.input(Source.of(inputStream))
        .then(CsvNavigateCommand.create(CSVPath.of("name"), new PassThroughRule()))
        .thenIf(needsCharConvert, CharacterConvertCommand.create("UTF-8", targetEncoding))
        .withErrorPolicy(ErrorPolicy.retry(3, 100))
        .withExecutionStrategy(new SequentialExecutionStrategy())
        .to(Sink.of(outputStream));

    System.out.println("Pipeline completed. Output size: " + outputStream.size() + " bytes");
    System.out.println("Output: " + outputStream.toString(StandardCharsets.UTF_8));
  }
}
