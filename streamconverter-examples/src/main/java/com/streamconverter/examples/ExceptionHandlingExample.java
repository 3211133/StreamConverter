package com.streamconverter.examples;

import com.streamconverter.AggregatedStreamProcessingException;
import com.streamconverter.ExternalPermanentException;
import com.streamconverter.ExternalTransientException;
import com.streamconverter.InternalSystemException;
import com.streamconverter.StreamConverter;
import com.streamconverter.StreamProcessingException;
import com.streamconverter.UserInputException;
import com.streamconverter.command.impl.csv.CsvValidateCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 例6: main 層での AggregatedStreamProcessingException 処理パターン
 *
 * <p>StreamConverter が投げる {@link AggregatedStreamProcessingException} を main 層で受け取り、
 * 通知分類（U/T/A）に応じて処理を分岐するパターンを示す。
 *
 * <p><b>この例で学べること:</b>
 *
 * <ul>
 *   <li>{@link AggregatedStreamProcessingException} は {@link StreamProcessingException} の
 *       サブクラスなので、{@code catch (StreamProcessingException)} で一括捕捉できる
 *   <li>{@link AggregatedStreamProcessingException#getAllFailures()} で全独立失敗を取得できる
 *   <li>{@link StreamProcessingException#getUserMessage()} は通知分類ごとにサニタイズ済みメッセージを返す
 *   <li>U（ユーザー起因） / T（外部一時障害） / A（管理者対応必要）で表示内容を変える
 * </ul>
 *
 * <p><b>main 層の責務（EXCEPTION_POLICY.md §main）:</b>
 *
 * <pre>
 * - ライブラリ規定対象外（main 層は自由に解釈する）
 * - 受け取るのは必ず1つの AggregatedStreamProcessingException（単独失敗も含む）
 * - getAllFailures() で全独立失敗を取得して用途別に処理
 * </pre>
 */
public class ExceptionHandlingExample {

  private static final Logger log = LoggerFactory.getLogger(ExceptionHandlingExample.class);

  /**
   * @param args コマンドライン引数（未使用）
   * @throws IOException I/O エラー
   */
  public static void main(String[] args) throws IOException {
    log.info("=== 例6: main 層での AggregatedStreamProcessingException 処理パターン ===");

    // カラム不足の不正 CSV（UserInputException が発生するシナリオ）
    String invalidCsv = "id,name\n1,Alice\n2,Bob\n";

    log.info("--- シナリオ1: UserInputException（U 分類） ---");
    runScenario(invalidCsv, CsvValidateCommand.create("id", "name", "email"));

    log.info("--- シナリオ2: 複数失敗の handleFailures() パターン ---");
    showHandlePattern();
  }

  // ---------------------------------------------------------------------------
  // シナリオ1: 実際にパイプラインを実行して失敗を受け取る
  // ---------------------------------------------------------------------------

  private static void runScenario(String csv, CsvValidateCommand validator) throws IOException {
    StreamConverter converter = StreamConverter.create(validator);

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      converter.run(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), out);
      log.info("成功（出力 {} バイト）", out.size());

    } catch (StreamProcessingException e) {
      handleFailures(e);
    }
  }

  // ---------------------------------------------------------------------------
  // main 層での AggregatedStreamProcessingException 処理パターン
  // ---------------------------------------------------------------------------

  /**
   * AggregatedStreamProcessingException（または単独の StreamProcessingException）を処理する。
   *
   * <p>ライブラリ契約: 届くのは必ず1つの {@link AggregatedStreamProcessingException} か その基底型（{@link
   * StreamProcessingException}）。{@link AggregatedStreamProcessingException#getAllFailures()} が返す
   * リストはU/T/A分類済み型のみを含む（{@link com.streamconverter.PipelineFailureHandler} 参照）。
   */
  static void handleFailures(StreamProcessingException e) {
    // AggregatedStreamProcessingException かどうかで全失敗リストを取得
    java.util.List<StreamProcessingException> failures =
        (e instanceof AggregatedStreamProcessingException agg)
            ? agg.getAllFailures()
            : java.util.List.of(e);

    // ユーザー向けメッセージは最初の U 分類 → なければ先頭の失敗を採用
    String userFacingMessage =
        failures.stream()
            .filter(f -> f instanceof UserInputException)
            .findFirst()
            .map(StreamProcessingException::getUserMessage)
            .orElseGet(() -> failures.get(0).getUserMessage());

    log.info("[ユーザー向けメッセージ] {}", userFacingMessage);

    // 全失敗を通知分類ごとに処理
    for (StreamProcessingException failure : failures) {
      if (failure instanceof UserInputException) {
        // U: ユーザーが入力を修正すれば解消
        log.warn("[U: 入力エラー] {}", failure.getUserMessage());

      } else if (failure instanceof ExternalTransientException) {
        // T: 一時的な外部障害—リトライを推奨
        log.warn("[T: 一時障害] {} — リトライをお試しください", failure.getUserMessage());

      } else if (failure instanceof ExternalPermanentException) {
        // A（外部永続障害）: 管理者対応
        log.error("[A: 外部永続障害] {} (管理者に連絡)", failure.getUserMessage(), failure);

      } else if (failure instanceof InternalSystemException) {
        // A（内部システム障害）: 管理者対応
        log.error("[A: システムエラー] {} (詳細はオペレーターログを確認)", failure.getUserMessage(), failure);

      } else {
        // ライブラリ契約違反: 未知の例外型が main 層に到達した—バグ
        log.error(
            "[UNCLASSIFIED BUG] 未知の例外型 {} が main 層に到達しました: {}",
            failure.getClass().getName(),
            failure.getUserMessage(),
            failure);
      }
    }
  }

  // ---------------------------------------------------------------------------
  // シナリオ2: 複数失敗シナリオの handleFailures() 呼び出しを直接示す
  // ---------------------------------------------------------------------------

  private static void showHandlePattern() {
    // 実際のパイプライン失敗と同じ形式で直接組み立てて挙動を示す（U/T/A 全分類）
    java.util.List<StreamProcessingException> multipleFailures =
        java.util.List.of(
            new UserInputException("3行目: 必須フィールド 'email' が空です"),
            new ExternalTransientException("データベース接続タイムアウト—リトライ可能"),
            new ExternalPermanentException("参照テーブル 'categories' が存在しません"),
            new InternalSystemException("一時ファイルの書き込みに失敗しました"));

    AggregatedStreamProcessingException agg =
        new AggregatedStreamProcessingException(multipleFailures);

    log.info("複数失敗（U + T + A×2）の場合の handleFailures() 出力:");
    handleFailures(agg);
  }
}
