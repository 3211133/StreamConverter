package com.streamConverter.command;

import com.streamConverter.StreamProcessingException;
import com.streamConverter.command.impl.csv.CsvValidateCommand;
import com.streamConverter.command.impl.json.JsonValidateCommand;
import com.streamConverter.command.impl.xml.ValidateCommand;
import com.streamConverter.validation.ValidationResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IStreamCommandにバリデーション機能を追加するデコレーター
 *
 * <p>このクラスは既存のIStreamCommandをラップして、実行前にデータの バリデーションを行います。JSON、XML、CSVの各形式に対応し、
 * 統一されたValidationResultを提供します。
 *
 * <p>使用例:
 *
 * <pre>
 * IStreamCommand originalCommand = new SampleStreamCommand("test");
 * IStreamCommand validatedCommand = new ValidationDecorator(
 *     originalCommand,
 *     ValidationDecorator.ValidationType.JSON,
 *     "schema/user.json"
 * );
 * validatedCommand.execute(inputStream, outputStream);
 * </pre>
 */
public class ValidationDecorator implements IStreamCommand {
  private static final Logger log = LoggerFactory.getLogger(ValidationDecorator.class);

  /** サポートされるバリデーションタイプ */
  public enum ValidationType {
    JSON("JSON"),
    XML("XML"),
    CSV("CSV");

    private final String typeName;

    ValidationType(String typeName) {
      this.typeName = typeName;
    }

    public String getTypeName() {
      return typeName;
    }
  }

  private final IStreamCommand delegate;
  private final ValidationType validationType;
  private final String schemaPath;
  private final String commandName;
  private ValidationResult lastValidationResult;

  /**
   * コンストラクタ
   *
   * @param delegate ラップ対象のコマンド
   * @param validationType バリデーションタイプ
   * @param schemaPath スキーマファイルのパス
   * @throws IllegalArgumentException 無効なパラメータが指定された場合
   */
  public ValidationDecorator(
      IStreamCommand delegate, ValidationType validationType, String schemaPath) {
    this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    this.validationType = Objects.requireNonNull(validationType, "validationType cannot be null");
    this.schemaPath = validateSchemaPath(schemaPath);
    this.commandName = delegate.getClass().getSimpleName();
  }

  /**
   * CSVバリデーション用のコンストラクタ（必須カラム指定）
   *
   * @param delegate ラップ対象のコマンド
   * @param requiredColumns 必須カラム名の配列
   */
  public ValidationDecorator(IStreamCommand delegate, String[] requiredColumns) {
    this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    this.validationType = ValidationType.CSV;
    this.schemaPath = buildCsvSchemaInfo(requiredColumns);
    this.commandName = delegate.getClass().getSimpleName();
  }

  /** スキーマパスの検証 */
  private String validateSchemaPath(String path) {
    Objects.requireNonNull(path, "Schema path cannot be null");
    String trimmedPath = path.trim();
    if (trimmedPath.isEmpty()) {
      throw new IllegalArgumentException("Schema path cannot be empty");
    }
    return trimmedPath;
  }

  /** CSVの必須カラム情報からスキーマ情報を構築 */
  private String buildCsvSchemaInfo(String[] requiredColumns) {
    if (requiredColumns == null || requiredColumns.length == 0) {
      return "CSV_NO_REQUIRED_COLUMNS";
    }
    return "CSV_REQUIRED_COLUMNS[" + String.join(",", requiredColumns) + "]";
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream, "inputStream cannot be null");
    Objects.requireNonNull(outputStream, "outputStream cannot be null");

    log.info(
        "Starting validation decorator for command: {} with type: {}",
        commandName,
        validationType.getTypeName());

    long startTime = System.currentTimeMillis();

    try {
      // 入力ストリームの内容を読み取り
      byte[] inputData = inputStream.readAllBytes();

      // バリデーション実行
      performValidation(inputData);

      // バリデーション成功時のみ、元のコマンドを実行
      try (InputStream delegateInputStream = new ByteArrayInputStream(inputData)) {
        delegate.execute(delegateInputStream, outputStream);
      }

      long executionTime = System.currentTimeMillis() - startTime;

      // 成功時のValidationResultを作成
      lastValidationResult =
          ValidationResult.success(validationType.getTypeName(), schemaPath, executionTime);

      log.info("Validation decorator completed successfully for command: {}", commandName);

    } catch (StreamProcessingException e) {
      long executionTime = System.currentTimeMillis() - startTime;

      // バリデーションエラーの場合はValidationResultを作成
      if (e.getMessage().contains("validation failed")) {
        lastValidationResult =
            ValidationResult.failure(
                validationType.getTypeName(),
                schemaPath,
                java.util.Arrays.asList(e.getMessage()),
                executionTime);
      }

      log.error("Validation decorator failed for command: {} - {}", commandName, e.getMessage());
      throw e;
    } catch (Exception e) {
      long executionTime = System.currentTimeMillis() - startTime;

      lastValidationResult =
          ValidationResult.failure(
              validationType.getTypeName(),
              schemaPath,
              java.util.Arrays.asList("Unexpected error: " + e.getMessage()),
              executionTime);

      log.error("Validation decorator failed with unexpected error: {}", e.getMessage(), e);
      throw new StreamProcessingException("Validation decorator failed", e);
    }
  }

  /** バリデーション実行 */
  private void performValidation(byte[] inputData) throws IOException {
    try (InputStream validationStream = new ByteArrayInputStream(inputData)) {

      switch (validationType) {
        case JSON:
          JsonValidateCommand jsonValidator = new JsonValidateCommand(schemaPath);
          jsonValidator.consume(validationStream);
          break;

        case XML:
          ValidateCommand xmlValidator = new ValidateCommand(schemaPath);
          xmlValidator.consume(validationStream);
          break;

        case CSV:
          CsvValidateCommand csvValidator = createCsvValidator();
          csvValidator.consume(validationStream);
          break;

        default:
          throw new IllegalStateException("Unsupported validation type: " + validationType);
      }

      log.debug("Validation completed successfully for type: {}", validationType.getTypeName());
    }
  }

  /** CSVバリデーターを作成 */
  private CsvValidateCommand createCsvValidator() {
    if (schemaPath.startsWith("CSV_REQUIRED_COLUMNS[")) {
      // 必須カラム情報を抽出
      String columnsStr =
          schemaPath.substring("CSV_REQUIRED_COLUMNS[".length(), schemaPath.length() - 1);
      String[] requiredColumns = columnsStr.split(",");
      return new CsvValidateCommand(requiredColumns);
    } else {
      // 必須カラムなしの場合
      return new CsvValidateCommand(new String[0]);
    }
  }

  /**
   * 最後のバリデーション結果を取得
   *
   * @return 最後のValidationResult（未実行の場合はnull）
   */
  public ValidationResult getLastValidationResult() {
    return lastValidationResult;
  }

  /**
   * ラップされているコマンドを取得
   *
   * @return ラップされているコマンド
   */
  public IStreamCommand getDelegate() {
    return delegate;
  }

  /**
   * バリデーションタイプを取得
   *
   * @return バリデーションタイプ
   */
  public ValidationType getValidationType() {
    return validationType;
  }

  /**
   * スキーマパスを取得
   *
   * @return スキーマファイルのパス
   */
  public String getSchemaPath() {
    return schemaPath;
  }

  /**
   * コマンド名を取得
   *
   * @return コマンド名
   */
  public String getCommandName() {
    return commandName;
  }
}
