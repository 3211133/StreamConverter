package com.streamconverter.examples;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.MdcPropagatingRule;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.context.PipelineContext;
import com.streamconverter.path.CSVPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 例3: PipelineContext によるスレッド間の値共有と MDC 伝搬
 *
 * <p>StreamConverter では各コマンドが別の仮想スレッドで並列実行される。 スレッドをまたいで値を共有するための仕組みが {@link PipelineContext} である。
 *
 * <p><b>この例で学べること:</b>
 *
 * <ul>
 *   <li>複数コマンドは別スレッドで動くため、通常の変数で値を渡すことができない
 *   <li>{@link PipelineContext#putShared(String, String)} でパイプライン内の全コマンドが参照できる値を登録できる
 *   <li>{@link MdcPropagatingRule} は抽出した値を {@link PipelineContext} 経由で MDC に自動反映する
 *   <li>後段コマンドのログにも前段で抽出した値が自動的に含まれる（TurboFilter による同期）
 * </ul>
 *
 * <p><b>パイプライン構成（3段）:</b>
 *
 * <pre>
 * [コマンド1: ラムダ]
 *   最初の1行から注文IDを読み取り PipelineContext.putShared("orderId", ...) に格納
 *          ↓
 * [コマンド2: CsvNavigateCommand + MdcPropagatingRule]
 *   productName 列を抽出し、値を "productName" キーで MDC に自動伝搬
 *   → このコマンドのログに orderId と productName が含まれる
 *          ↓
 * [コマンド3: CsvNavigateCommand + TrimRule]
 *   address 列の前後空白をトリム
 *   → このコマンドのログにも orderId が含まれる（PipelineContext 経由）
 * </pre>
 */
public class PipelineContextExample {

  private static final Logger log = LoggerFactory.getLogger(PipelineContextExample.class);

  /**
   * @param args コマンドライン引数（未使用）
   * @throws IOException I/O エラー
   */
  public static void main(String[] args) throws IOException {
    log.info("=== 例3: PipelineContext によるスレッド間値共有と MDC 伝搬 ===");

    String csv =
        "orderId,productName,address\n"
            + "ORD-2026-0001,Laptop Computer,  Tokyo Japan  \n"
            + "ORD-2026-0002,Wireless Mouse,  Osaka Japan  \n";

    log.info("入力 CSV:\n{}", csv);

    // --- コマンド1: ラムダで PipelineContext に注文IDを格納 ---
    // 各コマンドは別スレッドで動くため、ローカル変数や static フィールドでは値を安全に共有できない。
    // PipelineContext.putShared() を使うと、同じパイプライン内の全コマンドスレッドで値が共有される。
    IStreamCommand extractOrderId =
        (in, out) -> {
          byte[] data = in.readAllBytes();
          String content = new String(data, StandardCharsets.UTF_8);

          // ヘッダーをスキップして最初のデータ行から orderId を抽出
          String[] lines = content.split("\n");
          if (lines.length > 1) {
            String firstDataLine = lines[1];
            String orderId = firstDataLine.split(",")[0].trim();
            // putShared: 全コマンドスレッドで参照可能になり、呼び出しスレッドの MDC にも即反映
            PipelineContext.putShared("orderId", orderId);
            log.info("orderId を PipelineContext に格納: {}", orderId);
          }

          out.write(data);
        };

    // --- コマンド2: MdcPropagatingRule で productName を MDC に伝搬 ---
    // MdcPropagatingRule は値をパススルーしながら PipelineContext.putShared() を呼び出す Rule。
    // これにより、後続の全コマンドのログに productName が自動的に含まれる。
    IStreamCommand propagateProductName =
        CsvNavigateCommand.create(
            CSVPath.of("productName"), MdcPropagatingRule.create("productName"));

    // --- コマンド3: TrimRule で address をトリム ---
    // このコマンドのログには orderId が含まれる。
    // PipelineContextTurboFilter がログ出力直前に PipelineContext の共有値を MDC に同期するため。
    IStreamCommand trimAddress = CsvNavigateCommand.create(CSVPath.of("address"), new TrimRule());

    // --- パイプライン実行 ---
    StreamConverter converter =
        StreamConverter.create(extractOrderId, propagateProductName, trimAddress);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))) {
      converter.run(inputStream, output);
    }

    String result = output.toString(StandardCharsets.UTF_8);
    String expected =
        "orderId,productName,address\r\n"
            + "ORD-2026-0001,Laptop Computer,Tokyo Japan\r\n"
            + "ORD-2026-0002,Wireless Mouse,Osaka Japan\r\n";
    log.info("期待値:\n{}", expected);
    log.info("出力 CSV:\n{}", result);
    log.info("ログの各行に orderId / productName が含まれていることを確認してください");
  }
}
