package com.streamconverter.examples;

import com.streamconverter.StreamConverter;
import com.streamconverter.StreamProcessingException;
import com.streamconverter.command.impl.FileBufferCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.csv.CsvValidateCommand;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.CSVPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 例5: ConsumerCommand（検証） × FileBufferCommand による安全なパイプライン
 *
 * <p>並列パイプラインにおける検証コマンドの落とし穴と、その解決策を示す。
 *
 * <p><b>この例で学べること:</b>
 *
 * <ul>
 *   <li>{@link CsvValidateCommand} は {@link com.streamconverter.command.ConsumerCommand} のサブクラスで、
 *       入力を読みながら出力にも同時に流す（TeeInputStream）
 *   <li>各コマンドは並列実行されるため、検証が失敗する前に後段へデータが流れ始める
 *   <li>{@link FileBufferCommand} を挿入すると前段の完全完了を待ってから後段が開始する（逐次化）
 *   <li>{@link FileBufferCommand#createEncrypted()} で一時ファイルを AES-256-GCM 暗号化できる
 * </ul>
 *
 * <p><b>3つのパターンを比較する:</b>
 *
 * <pre>
 * パターンA（問題あり）:
 *   CsvValidateCommand → CsvNavigateCommand
 *   検証失敗でも後段に部分データが届く可能性がある
 *
 * パターンB（安全）:
 *   CsvValidateCommand → FileBufferCommand → CsvNavigateCommand
 *   FileBufferCommand が前段の完了を待ち、後段は確実に検証済みデータのみを受け取る
 *
 * パターンC（機密データ）:
 *   CsvValidateCommand → FileBufferCommand.createEncrypted() → CsvNavigateCommand
 *   一時ファイルを AES-256-GCM で暗号化するため、機密データでも安全
 * </pre>
 */
public class ValidationPipelineExample {

  private static final Logger log = LoggerFactory.getLogger(ValidationPipelineExample.class);

  /**
   * @param args コマンドライン引数（未使用）
   * @throws IOException I/O エラー
   */
  public static void main(String[] args) throws IOException {
    log.info("=== 例5: ConsumerCommand × FileBufferCommand による安全なパイプライン ===");

    String validCsv =
        "id,name,email\n" + "1,  Alice  ,alice@example.com\n" + "2,  Bob  ,bob@example.com\n";

    String invalidCsv =
        "id,name\n" // email カラムが欠けている
            + "1,Alice\n"
            + "2,Bob\n";

    log.info("正常データ（入力）:\n{}", validCsv);
    log.info("不正データ（入力）:\n{}", invalidCsv);

    log.info("--- パターンA: FileBufferCommand なし（問題あり） ---");
    patternA(validCsv, invalidCsv);

    log.info("--- パターンB: FileBufferCommand あり（安全） ---");
    patternB(validCsv, invalidCsv);

    log.info("--- パターンC: FileBufferCommand.createEncrypted()（機密データ向け） ---");
    patternC(validCsv);
  }

  // ---------------------------------------------------------------------------
  // パターンA: FileBufferCommand なし
  // ---------------------------------------------------------------------------

  private static void patternA(String validCsv, String invalidCsv) throws IOException {
    // CsvValidateCommand: ConsumerCommand のサブクラス
    // TeeInputStream を使って入力を読みながら出力にも同時に流す。
    // そのため検証が失敗した時点で後段にはすでに部分データが流れている可能性がある。
    CsvValidateCommand validator = CsvValidateCommand.create("id", "name", "email");
    CsvNavigateCommand trimName = CsvNavigateCommand.create(CSVPath.of("name"), new TrimRule());

    StreamConverter converter = StreamConverter.create(validator, trimName);

    String validExpected =
        "id,name,email\r\n" + "1,Alice,alice@example.com\r\n" + "2,Bob,bob@example.com\r\n";
    log.info("パターンA + 正常データ:\n期待値:\n{}", validExpected);
    try {
      ByteArrayOutputStream out1 = new ByteArrayOutputStream();
      converter.run(new ByteArrayInputStream(validCsv.getBytes(StandardCharsets.UTF_8)), out1);
      log.info("出力:\n{}", out1.toString(StandardCharsets.UTF_8));
    } catch (StreamProcessingException e) {
      log.error("失敗: {}", e.getMessage());
    }

    // 同じ StreamConverter を使いまわすことはできないため再生成
    validator = CsvValidateCommand.create("id", "name", "email");
    trimName = CsvNavigateCommand.create(CSVPath.of("name"), new TrimRule());
    converter = StreamConverter.create(validator, trimName);

    log.info(
        "パターンA + 不正データ（email カラムなし）:\n期待値: StreamProcessingException が発生するが、後段に部分データが届く可能性がある");
    try {
      ByteArrayOutputStream out2 = new ByteArrayOutputStream();
      converter.run(new ByteArrayInputStream(invalidCsv.getBytes(StandardCharsets.UTF_8)), out2);
      log.warn("成功してしまった（後段にデータが流れた可能性）:\n{}", out2.toString(StandardCharsets.UTF_8));
    } catch (StreamProcessingException e) {
      log.info("出力（検証エラー、期待通り）: {}", e.getMessage());
    }
  }

  // ---------------------------------------------------------------------------
  // パターンB: FileBufferCommand あり（逐次化）
  // ---------------------------------------------------------------------------

  private static void patternB(String validCsv, String invalidCsv) throws IOException {
    // FileBufferCommand を検証コマンドの後に挿入することで逐次化できる。
    // FileBufferCommand は:
    //   1. 前段（CsvValidateCommand）の出力を一時ファイルに書き込む（前段が完全に完了するまで待機）
    //   2. 前段完了後に一時ファイルを後段（CsvNavigateCommand）への入力として流す
    // これにより「検証が成功した場合のみ後段が動く」ことが保証される。
    String validExpected =
        "id,name,email\r\n" + "1,Alice,alice@example.com\r\n" + "2,Bob,bob@example.com\r\n";
    log.info("パターンB + 正常データ:\n期待値（パターンA と同一、FileBufferCommand は出力に影響しない）:\n{}", validExpected);
    try {
      ByteArrayOutputStream out3 = new ByteArrayOutputStream();
      StreamConverter.create(
              CsvValidateCommand.create("id", "name", "email"),
              FileBufferCommand.create(),
              CsvNavigateCommand.create(CSVPath.of("name"), new TrimRule()))
          .run(new ByteArrayInputStream(validCsv.getBytes(StandardCharsets.UTF_8)), out3);
      log.info("出力:\n{}", out3.toString(StandardCharsets.UTF_8));
    } catch (StreamProcessingException e) {
      log.error("失敗: {}", e.getMessage());
    }

    log.info(
        "パターンB + 不正データ（email カラムなし）:\n期待値: StreamProcessingException が発生し、後段は一切実行されない（out4 は空）");
    try {
      ByteArrayOutputStream out4 = new ByteArrayOutputStream();
      StreamConverter.create(
              CsvValidateCommand.create("id", "name", "email"),
              FileBufferCommand.create(),
              CsvNavigateCommand.create(CSVPath.of("name"), new TrimRule()))
          .run(new ByteArrayInputStream(invalidCsv.getBytes(StandardCharsets.UTF_8)), out4);
      log.warn("後段が実行された（問題あり）:\n{}", out4.toString(StandardCharsets.UTF_8));
    } catch (StreamProcessingException e) {
      log.info("出力（後段は実行されていない）: {}", e.getMessage());
    }
  }

  // ---------------------------------------------------------------------------
  // パターンC: FileBufferCommand.createEncrypted()（機密データ向け）
  // ---------------------------------------------------------------------------

  private static void patternC(String validCsv) throws IOException {
    // createEncrypted() は一時ファイルを AES-256-GCM で暗号化する。
    // 鍵と IV は実行ごとに生成され、永続化されない。
    // 機密情報を含む CSV を処理する場合に使用する。
    String validExpected =
        "id,name,email\r\n" + "1,Alice,alice@example.com\r\n" + "2,Bob,bob@example.com\r\n";
    log.info("パターンC + 正常データ:\n期待値（パターンB と同一、暗号化は透過的）:\n{}", validExpected);
    try {
      ByteArrayOutputStream out5 = new ByteArrayOutputStream();
      StreamConverter.create(
              CsvValidateCommand.create("id", "name", "email"),
              FileBufferCommand.createEncrypted(),
              CsvNavigateCommand.create(CSVPath.of("name"), new TrimRule()))
          .run(new ByteArrayInputStream(validCsv.getBytes(StandardCharsets.UTF_8)), out5);
      log.info("出力（一時ファイルは AES-256-GCM で暗号化）:\n{}", out5.toString(StandardCharsets.UTF_8));
    } catch (StreamProcessingException e) {
      log.error("失敗: {}", e.getMessage());
    }
  }
}
