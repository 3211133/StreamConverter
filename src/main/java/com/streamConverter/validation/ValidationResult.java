package com.streamConverter.validation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * バリデーション結果を統一的に表現するクラス
 *
 * <p>JSON、XML、CSVなど、すべてのバリデーション処理の結果を一貫した形式で提供します。 バリデーションの成功/失敗、エラーメッセージ、実行時間などの情報を含みます。
 *
 * <p>使用例:
 *
 * <pre>
 * ValidationResult result = ValidationResult.builder()
 *     .validationType("JSON")
 *     .schemaPath("schema/user.json")
 *     .success(false)
 *     .addError("Field 'email' is required")
 *     .addError("Field 'age' must be a number")
 *     .build();
 * </pre>
 */
public class ValidationResult {

  private final String validationType;
  private final String schemaPath;
  private final boolean isValid;
  private final List<String> errors;
  private final List<String> warnings;
  private final Instant validationTime;
  private final long executionTimeMillis;
  private final String dataSource;

  /** プライベートコンストラクタ（Builderパターン使用） */
  private ValidationResult(Builder builder) {
    this.validationType = builder.validationType;
    this.schemaPath = builder.schemaPath;
    this.isValid = builder.isValid;
    this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
    this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
    this.validationTime = builder.validationTime;
    this.executionTimeMillis = builder.executionTimeMillis;
    this.dataSource = builder.dataSource;
  }

  /**
   * 成功した場合のバリデーション結果を作成
   *
   * @param validationType バリデーションタイプ（"JSON", "XML", "CSV"など）
   * @param schemaPath スキーマファイルのパス
   * @param executionTimeMillis 実行時間（ミリ秒）
   * @return 成功を表すValidationResult
   */
  public static ValidationResult success(
      String validationType, String schemaPath, long executionTimeMillis) {
    return builder()
        .validationType(validationType)
        .schemaPath(schemaPath)
        .success(true)
        .executionTimeMillis(executionTimeMillis)
        .build();
  }

  /**
   * 失敗した場合のバリデーション結果を作成
   *
   * @param validationType バリデーションタイプ（"JSON", "XML", "CSV"など）
   * @param schemaPath スキーマファイルのパス
   * @param errors エラーメッセージのリスト
   * @param executionTimeMillis 実行時間（ミリ秒）
   * @return 失敗を表すValidationResult
   */
  public static ValidationResult failure(
      String validationType, String schemaPath, List<String> errors, long executionTimeMillis) {
    Builder builder =
        builder()
            .validationType(validationType)
            .schemaPath(schemaPath)
            .success(false)
            .executionTimeMillis(executionTimeMillis);

    if (errors != null) {
      for (String error : errors) {
        builder.addError(error);
      }
    }

    return builder.build();
  }

  /**
   * Builderインスタンスを作成
   *
   * @return 新しいBuilderインスタンス
   */
  public static Builder builder() {
    return new Builder();
  }

  // Getters

  /**
   * バリデーションタイプを取得
   *
   * @return バリデーションタイプ
   */
  public String getValidationType() {
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
   * バリデーション結果が有効かどうかを取得
   *
   * @return 有効な場合true、無効な場合false
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * エラーメッセージのリストを取得
   *
   * @return エラーメッセージのリスト（読み取り専用）
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * 警告メッセージのリストを取得
   *
   * @return 警告メッセージのリスト（読み取り専用）
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * バリデーション実行時刻を取得
   *
   * @return 実行時刻
   */
  public Instant getValidationTime() {
    return validationTime;
  }

  /**
   * 実行時間を取得
   *
   * @return 実行時間（ミリ秒）
   */
  public long getExecutionTimeMillis() {
    return executionTimeMillis;
  }

  /**
   * データソース情報を取得
   *
   * @return データソース情報
   */
  public String getDataSource() {
    return dataSource;
  }

  /**
   * エラーの数を取得
   *
   * @return エラーの数
   */
  public int getErrorCount() {
    return errors.size();
  }

  /**
   * 警告の数を取得
   *
   * @return 警告の数
   */
  public int getWarningCount() {
    return warnings.size();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ValidationResult{");
    sb.append("type=").append(validationType);
    sb.append(", schema=").append(schemaPath);
    sb.append(", valid=").append(isValid);
    sb.append(", errors=").append(errors.size());
    sb.append(", warnings=").append(warnings.size());
    sb.append(", executionTimeMs=").append(executionTimeMillis);
    sb.append("}");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValidationResult that = (ValidationResult) o;
    return isValid == that.isValid
        && executionTimeMillis == that.executionTimeMillis
        && Objects.equals(validationType, that.validationType)
        && Objects.equals(schemaPath, that.schemaPath)
        && Objects.equals(errors, that.errors)
        && Objects.equals(warnings, that.warnings)
        && Objects.equals(validationTime, that.validationTime)
        && Objects.equals(dataSource, that.dataSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        validationType,
        schemaPath,
        isValid,
        errors,
        warnings,
        validationTime,
        executionTimeMillis,
        dataSource);
  }

  /** ValidationResult作成用のBuilderクラス */
  public static class Builder {
    private String validationType;
    private String schemaPath;
    private boolean isValid;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private Instant validationTime = Instant.now();
    private long executionTimeMillis;
    private String dataSource;

    /**
     * バリデーションタイプを設定
     *
     * @param validationType バリデーションタイプ
     * @return Builder
     */
    public Builder validationType(String validationType) {
      this.validationType = validationType;
      return this;
    }

    /**
     * スキーマパスを設定
     *
     * @param schemaPath スキーマファイルのパス
     * @return Builder
     */
    public Builder schemaPath(String schemaPath) {
      this.schemaPath = schemaPath;
      return this;
    }

    /**
     * バリデーション成功/失敗フラグを設定
     *
     * @param success 成功の場合true
     * @return Builder
     */
    public Builder success(boolean success) {
      this.isValid = success;
      return this;
    }

    /**
     * エラーメッセージを追加
     *
     * @param error エラーメッセージ
     * @return Builder
     */
    public Builder addError(String error) {
      if (error != null && !error.trim().isEmpty()) {
        this.errors.add(error.trim());
      }
      return this;
    }

    /**
     * 警告メッセージを追加
     *
     * @param warning 警告メッセージ
     * @return Builder
     */
    public Builder addWarning(String warning) {
      if (warning != null && !warning.trim().isEmpty()) {
        this.warnings.add(warning.trim());
      }
      return this;
    }

    /**
     * バリデーション実行時刻を設定
     *
     * @param validationTime 実行時刻
     * @return Builder
     */
    public Builder validationTime(Instant validationTime) {
      this.validationTime = validationTime;
      return this;
    }

    /**
     * 実行時間を設定
     *
     * @param executionTimeMillis 実行時間（ミリ秒）
     * @return Builder
     */
    public Builder executionTimeMillis(long executionTimeMillis) {
      this.executionTimeMillis = executionTimeMillis;
      return this;
    }

    /**
     * データソース情報を設定
     *
     * @param dataSource データソース情報
     * @return Builder
     */
    public Builder dataSource(String dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    /**
     * ValidationResultインスタンスを構築
     *
     * @return ValidationResult
     * @throws IllegalStateException 必須フィールドが設定されていない場合
     */
    public ValidationResult build() {
      // バリデーションタイプの検証
      if (validationType == null || validationType.trim().isEmpty()) {
        throw new IllegalArgumentException("Validation type cannot be null or empty");
      }

      // スキーマパスの検証
      if (schemaPath == null || schemaPath.trim().isEmpty()) {
        throw new IllegalArgumentException("Schema path cannot be null or empty");
      }

      // 成功フラグとエラーの整合性チェック
      if (isValid && !errors.isEmpty()) {
        throw new IllegalStateException(
            "ValidationResult cannot be marked as valid when errors are present");
      }
      if (!isValid && errors.isEmpty()) {
        addError("Validation failed (no specific error message)");
      }

      return new ValidationResult(this);
    }
  }
}
