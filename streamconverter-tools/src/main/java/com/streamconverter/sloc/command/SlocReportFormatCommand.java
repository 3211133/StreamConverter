package com.streamconverter.sloc.command;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.sloc.ModuleSloc;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * {@link ModuleSloc} オブジェクトのストリームを人が読みやすいレポート形式に整形するコマンド。
 *
 * <p>入力: {@link SlocAggregateCommand} が出力する {@link java.io.ObjectOutputStream} ストリーム （各モジュールの
 * {@link ModuleSloc} + name="Total" の合計行）
 *
 * <p><b>セキュリティ前提:</b> 入力ストリームは同一 JVM 内の前段コマンド（{@link SlocAggregateCommand}）が
 * 書き出したものであり、外部入力を直接受け取らない。予期しない型が含まれる場合は {@link ClassNotFoundException} / {@link
 * ClassCastException} を {@link java.io.IOException} に変換して伝播する。
 *
 * <p>出力例:
 *
 * <pre>
 * === SLOC Report ===
 *
 *   streamconverter-core       1,869
 *   streamconverter-db           349
 *   streamconverter-http          92
 *   streamconverter-tools      1,038
 *   streamconverter-web           79
 *   ───────────────────────────────
 *   Total                      3,427
 * </pre>
 */
public class SlocReportFormatCommand implements IStreamCommand {

  private static final int MODULE_COL_WIDTH = 25;
  private static final int LINES_COL_WIDTH = 6;

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    String separator = "  " + "─".repeat(MODULE_COL_WIDTH + LINES_COL_WIDTH + 1);
    NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

    try (ObjectInputStream ois = new ObjectInputStream(input);
        PrintWriter writer =
            new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
      writer.println("=== SLOC Report ===");
      writer.println();
      while (true) {
        try {
          ModuleSloc sloc = (ModuleSloc) ois.readObject();
          if (ModuleSloc.TOTAL_NAME.equals(sloc.name())) {
            writer.println(separator);
          }
          writer.printf(
              "  %-" + MODULE_COL_WIDTH + "s %" + LINES_COL_WIDTH + "s%n",
              sloc.name(),
              numberFormat.format(sloc.lines()));
        } catch (EOFException e) {
          break;
        } catch (ClassNotFoundException | ClassCastException e) {
          throw new IOException("Unexpected object type in stream: expected ModuleSloc", e);
        }
      }
    }
  }
}
