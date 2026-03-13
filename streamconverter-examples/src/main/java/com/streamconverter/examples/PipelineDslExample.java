package com.streamconverter.examples;

import com.streamconverter.command.impl.charcode.CharacterConvertCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.dsl.Pipeline;
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
 * <p>Pipeline DSL を使用すると、{@code Source} → コマンドチェーン → {@code Sink} の流れで パイプラインを宣言的に構築・実行できる。
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
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // Pipeline DSL でパイプラインを構築・実行
    Pipeline.input(Source.of(inputStream))
        .then(CsvNavigateCommand.create(CSVPath.of("name"), new PassThroughRule()))
        .then(CharacterConvertCommand.create("UTF-8", "UTF-8")) // 同一エンコーディング（例示用）
        .to(Sink.of(outputStream));

    System.out.println("Pipeline completed. Output size: " + outputStream.size() + " bytes");
    System.out.println("Output: " + outputStream.toString(StandardCharsets.UTF_8));
  }
}
