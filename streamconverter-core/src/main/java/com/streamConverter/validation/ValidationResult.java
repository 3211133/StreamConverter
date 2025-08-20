package com.streamConverter.validation; // NOPMD - PackageCase

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
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class ValidationResult {

  private final String validationType;
  private final String schemaPath;
  private final boolean isValid;
  private final List<String> errors;
  private final List<String> warnings;
  private final Instant validationTime;
  private final long timeMillis;
  private final String dataSource;

  /** プライベートコンストラクタ（Builderパターン使用） */
  @SuppressWarnings("PMD.LawOfDemeter")
  private ValidationResult(final Builder builder) {
    this.validationType = builder.validationType;
    this.schemaPath = builder.schemaPath;
    this.isValid = builder.isValid;
    this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
    this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
    this.validationTime = builder.validationTime;
    this.timeMillis = builder.timeMillis;
    this.dataSource = builder.dataSource;
  }

  /**
   * 成功した場合のバリデーション結果を作成
   *
   * @param validationType バリデーションタイプ（"JSON", "XML", "CSV"など）
   * @param schemaPath スキーマファイルのパス
   * @param timeMillis 実行時間（ミリ秒）
   * @return 成功を表すValidationResult
   */
  public static ValidationResult success(
      final String validationType, final String schemaPath, final long timeMillis) {
    return builder()
        .validationType(validationType)
        .schemaPath(schemaPath)
        .success(true)
        .executionTime(timeMillis)
        .build();
  }

  /**
   * 失敗した場合のバリデーション結果を作成
   *
   * @param validationType バリデーションタイプ（"JSON", "XML", "CSV"など）
   * @param schemaPath スキーマファイルのパス
   * @param errors エラーメッセージのリスト
   * @param timeMillis 実行時間（ミリ秒）
   * @return 失敗を表すValidationResult
   */
  public static ValidationResult failure(
      final String validationType,
      final String schemaPath,
      final List<String> errors,
      final long timeMillis) {
    final Builder builder =
        builder()
            .validationType(validationType)
            .schemaPath(schemaPath)
            .success(false)
            .executionTime(timeMillis);

    if (errors != null) {
      for (final String error : errors) {
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
    return timeMillis;
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
    return String.format(
        "ValidationResult{type=%s, schema=%s, valid=%s, errors=%d, warnings=%d, executionTimeMs=%d}",
        validationType, schemaPath, isValid, errors.size(), warnings.size(), timeMillis);
  }

  @Override
  public boolean equals(final Object other) {
    boolean result = false;
    if (this == other) {
      result = true;
    } else if (other instanceof ValidationResult) {
      final ValidationResult that = (ValidationResult) other;
      result =
          isValid == that.isValid
              && timeMillis == that.timeMillis
              && Objects.equals(validationType, that.validationType)
              && Objects.equals(schemaPath, that.schemaPath)
              && Objects.equals(errors, that.errors)
              && Objects.equals(warnings, that.warnings)
              && Objects.equals(validationTime, that.validationTime)
              && Objects.equals(dataSource, that.dataSource);
    }
    return result;
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
        timeMillis,
        dataSource);
  }

  /** ValidationResult作成用のBuilderクラス */
  @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
  public static class Builder {
    private String validationType;
    private String schemaPath;
    private boolean isValid;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private Instant validationTime = Instant.now();
    private long timeMillis;
    private String dataSource;

    /**
     * バリデーションタイプを設定
     *
     * @param validationType バリデーションタイプ
     * @return Builder
     */
    public Builder validationType(final String validationType) {
      this.validationType = validationType;
      return this;
    }

    /**
     * スキーマパスを設定
     *
     * @param schemaPath スキーマファイルのパス
     * @return Builder
     */
    public Builder schemaPath(final String schemaPath) {
      this.schemaPath = schemaPath;
      return this;
    }

    /**
     * バリデーション成功/失敗フラグを設定
     *
     * @param success 成功の場合true
     * @return Builder
     */
    public Builder success(final boolean success) {
      this.isValid = success;
      return this;
    }

    /**
     * エラーメッセージを追加
     *
     * @param error エラーメッセージ
     * @return Builder
     */
    public Builder addError(final String error) {
      if (error != null && !error.isBlank()) {
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
    public Builder addWarning(final String warning) {
      if (warning != null && !warning.isBlank()) {
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
    public Builder validationTime(final Instant validationTime) {
      this.validationTime = validationTime;
      return this;
    }

    /**
     * 実行時間を設定
     *
     * @param timeMillis 実行時間（ミリ秒）
     * @return Builder
     */
    public Builder executionTime(final long timeMillis) {
      this.timeMillis = timeMillis;
      return this;
    }

    /**
     * データソース情報を設定
     *
     * @param dataSource データソース情報
     * @return Builder
     */
    public Builder dataSource(final String dataSource) {
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
      requireNonBlank(validationType, "Validation type");
      requireNonBlank(schemaPath, "Schema path");

      if (isValid && !errors.isEmpty()) {
        throw new IllegalStateException(
            "ValidationResult cannot be marked as valid when errors are present");
      }
      if (!isValid && errors.isEmpty()) {
        addError("Validation failed (no specific error message)");
      }

      return new ValidationResult(this);
    }

    private void requireNonBlank(final String value, final String name) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(name + " cannot be null or empty");
      }
    }
  }
}
