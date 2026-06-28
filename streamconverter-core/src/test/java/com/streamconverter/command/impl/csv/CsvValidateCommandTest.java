package com.streamconverter.command.impl.csv;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.StreamProcessingException;
import com.streamconverter.test.StreamingTestUtils.TrackingInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CsvValidateCommandクラスのテスト */
@DisplayName("CsvValidateCommand Tests")
public class CsvValidateCommandTest {

  @Test
  @DisplayName("Constructor with valid required columns")
  void testConstructorWithValidRequiredColumns() {
    String[] requiredColumns = {"id", "name", "email"};

    assertDoesNotThrow(
        () -> {
          CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);
          assertNotNull(command);
        });
  }

  @Test
  @DisplayName("Constructor with null required columns throws exception")
  void testConstructorWithNullRequiredColumns() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> CsvValidateCommand.create((String[]) null));
    assertEquals("Required columns cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("Constructor with empty required columns array")
  void testConstructorWithEmptyRequiredColumns() {
    String[] emptyColumns = {};

    assertDoesNotThrow(
        () -> {
          CsvValidateCommand command = CsvValidateCommand.create(emptyColumns);
          assertNotNull(command);
        });
  }

  @Test
  @DisplayName("Constructor with hasHeader parameter")
  void testConstructorWithHasHeaderParameter() {
    String[] requiredColumns = {"id", "name", "email"};

    assertDoesNotThrow(
        () -> {
          CsvValidateCommand command = CsvValidateCommand.create(true, 10, requiredColumns);
          assertNotNull(command);
        });

    assertDoesNotThrow(
        () -> {
          CsvValidateCommand command = CsvValidateCommand.create(false, 10, requiredColumns);
          assertNotNull(command);
        });
  }

  @Test
  @DisplayName("Valid CSV with required columns validation succeeds")
  void testValidCsvWithRequiredColumnsSuccess() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    String validCsv =
        """
        id,name,email,department
        1,John Doe,john@example.com,Engineering
        2,Jane Smith,jane@example.com,Marketing
        3,Bob Johnson,bob@example.com,Sales
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(validCsv.getBytes(StandardCharsets.UTF_8));

    // バリデーション成功 - 例外がスローされないことを確認
    assertDoesNotThrow(() -> command.consume(inputStream));
  }

  @Test
  @DisplayName("CSV missing required columns validation fails")
  void testCsvMissingRequiredColumnsFailure() throws IOException {
    String[] requiredColumns = {"id", "name", "email", "phone"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    String csvMissingColumns =
        """
        id,name,department
        1,John Doe,Engineering
        2,Jane Smith,Marketing
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvMissingColumns.getBytes(StandardCharsets.UTF_8));

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(inputStream));

    assertTrue(exception.getMessage().contains("CSV validation failed"));
    assertTrue(exception.getMessage().contains("Missing required columns"));
  }

  @Test
  @DisplayName("CSV with duplicate headers validation fails")
  void testCsvWithDuplicateHeadersFailure() throws IOException {
    String[] requiredColumns = {"id", "name"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    String csvWithDuplicates =
        """
        id,name,name,email
        1,John Doe,John,john@example.com
        2,Jane Smith,Jane,jane@example.com
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvWithDuplicates.getBytes(StandardCharsets.UTF_8));

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(inputStream));

    assertTrue(exception.getMessage().contains("CSV validation failed"));
    assertTrue(exception.getMessage().contains("Duplicate column"));
  }

  @Test
  @DisplayName("CSV with inconsistent row length validation fails")
  void testCsvWithInconsistentRowLengthFailure() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    String csvInconsistentRows =
        """
        id,name,email,department
        1,John Doe,john@example.com,Engineering
        2,Jane Smith
        3,Bob Johnson,bob@example.com,Sales,Extra
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvInconsistentRows.getBytes(StandardCharsets.UTF_8));

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(inputStream));

    assertTrue(exception.getMessage().contains("CSV validation failed"));
    assertTrue(exception.getMessage().contains("Data row 2 has inconsistent number of columns"));
  }

  @Test
  @DisplayName("Empty CSV input validation fails")
  void testEmptyCsvInputFailure() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(inputStream));

    assertTrue(exception.getMessage().contains("CSVファイルが空です"));
  }

  @Test
  @DisplayName("Empty CSV input without header validation fails")
  void testEmptyCsvInputWithoutHeaderFailure() throws IOException {
    String[] requiredColumns = {};
    CsvValidateCommand command = CsvValidateCommand.create(false, 10, requiredColumns);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(inputStream));

    assertTrue(exception.getMessage().contains("CSVファイルが空です"));
  }

  @Test
  @DisplayName("CSV with only header validation fails")
  void testCsvWithOnlyHeaderFailure() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    String csvOnlyHeader = "id,name,email,department\n";

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvOnlyHeader.getBytes(StandardCharsets.UTF_8));

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(inputStream));

    assertTrue(exception.getMessage().contains("CSV validation failed"));
    assertTrue(exception.getMessage().contains("CSV file contains only header"));
  }

  @Test
  @DisplayName("CSV without header validation with hasHeader=false")
  void testCsvWithoutHeaderValidation() throws IOException {
    String[] requiredColumns = {}; // ヘッダーなしの場合は必須カラムなし
    CsvValidateCommand command = CsvValidateCommand.create(false, 10, requiredColumns);

    String csvWithoutHeader =
        """
        1,John Doe,john@example.com,Engineering
        2,Jane Smith,jane@example.com,Marketing
        3,Bob Johnson,bob@example.com,Sales
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvWithoutHeader.getBytes(StandardCharsets.UTF_8));

    // ヘッダーなしCSVのバリデーション成功
    assertDoesNotThrow(() -> command.consume(inputStream));
  }

  @Test
  @DisplayName("Large CSV validation")
  void testLargeCsvValidation() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    // 大きなCSVデータを作成
    StringBuilder largeCsv = new StringBuilder();
    largeCsv.append("id,name,email,department\n");

    for (int i = 1; i <= 1000; i++) {
      largeCsv.append(
          String.format("%d,User%d,user%d@example.com,Department%d%n", i, i, i, i % 10));
    }

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(largeCsv.toString().getBytes(StandardCharsets.UTF_8));

    // 大きなCSVでもバリデーションが成功することを確認
    assertDoesNotThrow(() -> command.consume(inputStream));
  }

  @Test
  @DisplayName("CSV with special characters validation")
  void testCsvWithSpecialCharactersValidation() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    String csvWithSpecialChars =
        """
        id,name,email,notes
        1,"José María Aznar-López","jose@example.com","Comment with, comma"
        2,"田中 太郎","tanaka@example.jp","日本語のコメント"
        3,"O'Connor, Patrick","patrick@example.com","Quote's test"
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvWithSpecialChars.getBytes(StandardCharsets.UTF_8));

    // 特殊文字を含むCSVのバリデーションが成功することを確認
    assertDoesNotThrow(() -> command.consume(inputStream));
  }

  @Test
  @DisplayName("Null input stream throws exception")
  void testNullInputStream() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> command.consume(null));

    assertEquals("InputStream cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("CSV with quoted fields containing newlines")
  void testCsvWithQuotedFieldsContainingNewlines() throws IOException {
    String[] requiredColumns = {"id", "name", "description"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    String csvWithNewlines =
        """
        id,name,description
        1,"John Doe","Multi-line
        description here"
        2,"Jane Smith","Another description"
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvWithNewlines.getBytes(StandardCharsets.UTF_8));

    // 改行を含むクォートされたフィールドのバリデーションが成功することを確認
    assertDoesNotThrow(() -> command.consume(inputStream));
  }

  @Test
  @DisplayName("CSV with empty fields validation")
  void testCsvWithEmptyFieldsValidation() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    String csvWithEmptyFields =
        """
        id,name,email,department
        1,John Doe,john@example.com,Engineering
        2,,jane@example.com,
        3,Bob Johnson,,Sales
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvWithEmptyFields.getBytes(StandardCharsets.UTF_8));

    // 空フィールドを含むCSVのバリデーションが成功することを確認
    // (空フィールドは許可される)
    assertDoesNotThrow(() -> command.consume(inputStream));
  }

  @Test
  @DisplayName("Malformed CSV with unclosed quotes validation fails")
  void testMalformedCsvWithUnclosedQuotes() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    String malformedCsv =
        """
        id,name,email
        1,"John Doe,john@example.com
        2,Jane Smith,jane@example.com
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(malformedCsv.getBytes(StandardCharsets.UTF_8));

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(inputStream));

    assertTrue(exception.getMessage().contains("CSV形式エラー"));
  }

  @Test
  @DisplayName("CSV validation with no required columns succeeds")
  void testCsvValidationWithNoRequiredColumns() throws IOException {
    String[] requiredColumns = {}; // 必須カラムなし
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    String csvAnyStructure =
        """
        col1,col2,col3
        value1,value2,value3
        value4,value5,value6
        """;

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvAnyStructure.getBytes(StandardCharsets.UTF_8));

    // 必須カラムなしの場合は任意の構造で成功
    assertDoesNotThrow(() -> command.consume(inputStream));
  }

  @Test
  @DisplayName("Validation consumes the full input stream")
  void testValidationConsumesFullInputStream() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(requiredColumns);

    StringBuilder csvBuilder = new StringBuilder();
    csvBuilder.append("id,name,email,department\n");
    for (int i = 1; i <= 50; i++) {
      csvBuilder.append(String.format("%d,User%d,user%d@example.com,Dept%d%n", i, i, i, i % 5));
    }
    String csvData = csvBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(csvData.getBytes(StandardCharsets.UTF_8));

    assertDoesNotThrow(() -> command.consume(trackingInputStream));

    assertTrue(
        trackingInputStream.isFullyRead(),
        "InputStream should be fully consumed during validation");
    assertTrue(
        trackingInputStream.getTotalBytes() > 1000,
        "Should have processed substantial amount of data");
  }

  @Test
  @DisplayName("maxErrorsToReport で指定した件数を超えてエラーが報告されない")
  void maxErrorsToReport_doesNotExceedLimit() throws IOException {
    int maxErrors = 2;
    String[] requiredColumns = {};
    CsvValidateCommand command = CsvValidateCommand.create(true, maxErrors, requiredColumns);

    // ヘッダー行2列 + データ行3行（各行が1列しかなく、列不足エラーになる）
    String csv = "id,name\n1\n2\n3\n";
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(inputStream));

    String msg = exception.getMessage();
    assertTrue(
        msg.contains("2 error(s)"),
        "maxErrorsToReport=2 なのに '2 error(s)' が含まれていない。実際のメッセージ: " + msg);
    assertFalse(
        msg.contains("... and more errors (limit reached)"),
        "maxErrorsToReport=2 のとき '... and more errors' は含まれるべきでない。実際のメッセージ: " + msg);
    assertFalse(
        msg.contains("3 error(s)"),
        "maxErrorsToReport=2 なのに '3 error(s)' が含まれていた。実際のメッセージ: " + msg);
  }

  @Test
  @DisplayName("ヘッダーのみのCSVでも maxErrorsToReport の上限を超えてエラーが報告されない")
  void maxErrorsToReport_headerOnlyCsvDoesNotExceedLimit() throws IOException {
    // 空セルを含むヘッダー行のみ（データ行なし）の入力では、
    // ヘッダー検証エラーと「データ行なし」エラーの2系統の追加経路が同時に発生する。
    // maxErrorsToReport=1 のとき、報告されるエラーは経路によらず1件に制限されるべき。
    String csv = "id,\n";

    // 前提確認: 上限が十分大きければこの入力は2系統のエラーを2件とも報告する。
    // ヘッダー検証仕様の変更で前提が崩れた場合、上限迂回の検証が成立しなくなるため
    // ここで検出する。
    CsvValidateCommand uncapped = CsvValidateCommand.create(true, 10);
    ByteArrayInputStream uncappedInput =
        new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    StreamProcessingException uncappedException =
        assertThrows(StreamProcessingException.class, () -> uncapped.consume(uncappedInput));
    assertTrue(
        uncappedException.getMessage().contains("2 error(s)"),
        "前提: この入力は上限が十分大きいとき2件のエラーを報告するはず。実際のメッセージ: " + uncappedException.getMessage());

    CsvValidateCommand command = CsvValidateCommand.create(true, 1);
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(inputStream));

    String msg = exception.getMessage();
    assertTrue(
        msg.contains("1 error(s)"),
        "maxErrorsToReport=1 なのに '1 error(s)' が含まれていない。実際のメッセージ: " + msg);
    assertFalse(
        msg.contains("2 error(s)"),
        "maxErrorsToReport=1 なのに '2 error(s)' が含まれていた。実際のメッセージ: " + msg);
  }

  @Test
  @DisplayName("エラーメッセージ切り詰め時もプレフィックスを含めた全体が1000文字以下に収まる")
  void errorMessageTruncation_totalLengthIncludingPrefixWithinLimit() throws IOException {
    String[] requiredColumns = {"id", "name", "email"};
    CsvValidateCommand command = CsvValidateCommand.create(true, 200, requiredColumns);

    StringBuilder csvBuilder = new StringBuilder();
    csvBuilder.append("id,name,email\n");
    for (int i = 1; i <= 40; i++) {
      csvBuilder.append(String.format("%d,User%d,user%d@example.com,extra_col_%d%n", i, i, i, i));
    }

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvBuilder.toString().getBytes(StandardCharsets.UTF_8));

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(inputStream));

    String msg = exception.getMessage();
    // 仕様: 例外メッセージ全体が 1000 文字以下であるべき
    assertTrue(
        msg.length() <= 1000,
        "Exception message should be at most 1000 chars, but was " + msg.length());
    // Phase 4 で例外型が変わったため、ユーザーメッセージ形式も変わっている
    // 本質的な要件は「メッセージが切り詰められたときに全体が1000文字以下」であること
    assertTrue(msg.contains("CSV validation failed with"), "メッセージに検証エラー情報を含むべき");
  }

  @Test
  @DisplayName("#731: CsvValidateCommand が CSV バリデーション失敗時に IOException をスローする")
  void csvValidateCommand_validationFailureThrowsIOException() throws IOException {
    CsvValidateCommand command = CsvValidateCommand.create("id", "name");
    String csvInput = "id,age\n1,30\n";

    java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvInput.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        IOException.class,
        () -> command.execute(inputStream, outputStream),
        "CsvValidateCommand.execute() は IOException をスローするべき");
  }

  @Test
  @DisplayName("読み取り中に I/O 障害が発生した場合は検証成功として扱われない")
  void midStreamIoErrorIsNotTreatedAsSuccess() throws IOException {
    // ネットワーク切断・ディスクエラー等で入力が途中で切断された場合、
    // 障害発生前までのデータだけで検証を成立させてはならない。
    // 切り詰められた入力の黙認は欠損データの見逃しに直結するため、
    // I/O 障害は例外としてエラー報告されるべき。
    CsvValidateCommand command = CsvValidateCommand.create(true, 10);
    byte[] csv = "id,name\n1,Alice\n".getBytes(StandardCharsets.UTF_8);
    try (InputStream failingStream =
        new InputStream() {
          private int pos = 0;

          @Override
          public int read() throws IOException {
            if (pos >= csv.length) {
              throw new IOException("simulated connection loss");
            }
            return csv[pos++] & 0xFF;
          }

          @Override
          public int read(byte[] b, int off, int len) throws IOException {
            // 「初回呼び出しで全データを供給し、以降の呼び出しで I/O 障害」という
            // 再現条件を安定して成立させるため直接オーバーライドする
            // （継承したデフォルト実装に任せると障害が Reader 層へ届く保証がない）
            if (len == 0) {
              return 0;
            }
            if (pos >= csv.length) {
              throw new IOException("simulated connection loss");
            }
            int n = Math.min(len, csv.length - pos);
            System.arraycopy(csv, pos, b, off, n);
            pos += n;
            return n;
          }
        }) {
      assertThrows(
          IOException.class, () -> command.consume(failingStream), "読み取り中の I/O 障害は例外として報告されるべき");
    }
  }

  @Test
  @DisplayName("入力ストリームの I/O 障害はパース失敗と区別できるメッセージで報告される")
  void ioErrorIsNotLabeledAsParseFailure() {
    // ネットワーク切断・パイプ切断等の I/O 障害は CSV の内容不正とは原因が異なるため、
    // オペレーターが切り分けられるメッセージで報告されるべき。
    // close() 時の IOException を使うのは、opencsv の readNext() が読み取り中の
    // IOException を EOF として扱うため、I/O 障害が IOException として観測できる
    // 経路がストリームのクローズ時に限られることによる。
    CsvValidateCommand command = CsvValidateCommand.create(true, 10);
    byte[] csv = "id,name\n1,Alice\n".getBytes(StandardCharsets.UTF_8);
    InputStream failingStream =
        new ByteArrayInputStream(csv) {
          @Override
          public void close() throws IOException {
            throw new IOException("simulated connection loss");
          }
        };

    StreamProcessingException exception =
        assertThrows(StreamProcessingException.class, () -> command.consume(failingStream));

    String msg = exception.getMessage();
    // Phase 4: I/O 障害は InternalSystemException でラップされる
    // メッセージは「CSV形式エラー」ではなく「読み取り失敗」であるべき
    assertFalse(msg.contains("CSV形式エラー"), "I/O 障害がパース失敗としてラベルされている。実際のメッセージ: " + msg);
    assertTrue(msg.contains("読み取りに失敗"), "I/O 障害は '読み取りに失敗' として報告されるべき。実際のメッセージ: " + msg);
  }
}
