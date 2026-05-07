package com.streamconverter.examples;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.LineEndingNormalizeCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 例1: IStreamCommand と StreamConverter の仕組み
 *
 * <p>StreamConverter のパイプライン処理の基本を示す。
 *
 * <p><b>この例で学べること:</b>
 *
 * <ul>
 *   <li>{@link IStreamCommand} はラムダ式でも、名前付きクラスとして実装することもできる
 *   <li>{@link StreamConverter#create(IStreamCommand...)} に複数のコマンドを渡すと順番に接続されパイプラインになる
 *   <li>各コマンドは別の仮想スレッドで並列実行される（ログのスレッド名で確認できる）
 *   <li>前段コマンドの出力が後段コマンドの入力に自動的にバイト列として接続される
 * </ul>
 *
 * <p><b>パイプライン構成（3段）:</b>
 *
 * <pre>
 * [コマンド1: ラムダ実装]     各行の前後空白をトリム
 *          ↓
 * [コマンド2: クラス実装]     各行を大文字に変換（IStreamCommand 実装）
 *          ↓
 * [コマンド3: 組み込みコマンド] 行末コードを LF に統一（LineEndingNormalizeCommand）
 * </pre>
 */
public class PipelineBasicsExample {

  private static final Logger log = LoggerFactory.getLogger(PipelineBasicsExample.class);

  /**
   * @param args コマンドライン引数（未使用）
   * @throws IOException I/O エラー
   */
  public static void main(String[] args) throws IOException {
    log.info("=== 例1: IStreamCommand と StreamConverter の仕組み ===");

    String input = "  hello world  \r\n" + "  stream converter  \r\n" + "  pipeline demo  \r\n";

    log.info("入力データ（各行に前後スペース、行末 CRLF）:\n{}", input);

    // --- コマンド1: ラムダ実装 ---
    // IStreamCommand は @FunctionalInterface なのでラムダで実装できる。
    // ただしラムダはクラス名を持たないため、ログでは "IStreamCommand" と表示される。
    IStreamCommand trimCommand =
        (in, out) -> {
          String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
          StringBuilder sb = new StringBuilder();
          for (String line : text.split("\n")) {
            sb.append(line.stripTrailing().stripLeading()).append("\n");
          }
          out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        };

    // --- コマンド2: IStreamCommand 実装クラス ---
    // 名前付きクラスとして実装することで:
    //   - クラス名がログに表示される（UpperCaseCommand）
    IStreamCommand upperCaseCommand = new UpperCaseCommand();

    // --- コマンド3: 組み込みコマンド ---
    // LineEndingNormalizeCommand は IStreamCommand を実装した既製コマンド。
    // 行末コードを UNIX (LF) / WINDOWS (CRLF) / CLASSIC_MAC (CR) に統一する。
    IStreamCommand normalizeCommand =
        new LineEndingNormalizeCommand(LineEndingNormalizeCommand.LineEndingType.UNIX);

    // --- パイプライン実行 ---
    // StreamConverter.create() に3つのコマンドを渡すと、
    // 内部で各コマンドを仮想スレッドで並列起動し、AbortablePipedStream で接続する。
    StreamConverter converter =
        StreamConverter.create(trimCommand, upperCaseCommand, normalizeCommand);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (InputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) {
      converter.run(inputStream, output);
    }

    String expected = "HELLO WORLD\n" + "STREAM CONVERTER\n" + "PIPELINE DEMO\n";

    String result = output.toString(StandardCharsets.UTF_8);
    log.info("期待値（空白トリム→大文字変換→行末 LF 統一）:\n{}", expected);
    log.info("出力データ:\n{}", result);
  }

  /**
   * IStreamCommand を実装したクラス実装の例。
   *
   * <p>名前付きクラスとして実装することで:
   *
   * <ul>
   *   <li>クラス名（UpperCaseCommand）がログのコマンド名として自動的に使われる
   *   <li>{@code execute()} メソッドを実装するだけでよい
   * </ul>
   */
  static class UpperCaseCommand implements IStreamCommand {

    private static final Logger log = LoggerFactory.getLogger(UpperCaseCommand.class);

    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
      String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      log.debug("UpperCaseCommand: {} 文字を大文字に変換", text.length());
      outputStream.write(text.toUpperCase().getBytes(StandardCharsets.UTF_8));
    }
  }
}
