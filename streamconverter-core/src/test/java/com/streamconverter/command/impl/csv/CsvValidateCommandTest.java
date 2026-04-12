package com.streamconverter.command.impl.csv;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.StreamProcessingException;
import com.streamconverter.test.StreamingTestUtils.TrackingInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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

    assertTrue(exception.getMessage().contains("CSV validation failed"));
    assertTrue(exception.getMessage().contains("CSV file is empty"));
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

    assertTrue(exception.getMessage().contains("Failed to parse CSV"));
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
}
