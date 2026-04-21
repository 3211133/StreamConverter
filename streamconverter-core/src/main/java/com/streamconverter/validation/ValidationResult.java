package com.streamconverter.validation;

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
public final class ValidationResult {

  private final String validationType;
  private final String schemaPath;
  private final boolean valid;
  private final List<String> errors;
  private final List<String> warnings;
  private final Instant validationTime;
  private final long execTimeMillis;
  private final String dataSource;

  private ValidationResult(
      final String validationType,
      final String schemaPath,
      final boolean valid,
      final List<String> errors,
      final List<String> warnings,
      final Instant validationTime,
      final long execTimeMillis,
      final String dataSource) {
    this.validationType = validationType;
    this.schemaPath = schemaPath;
    this.valid = valid;
    this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    this.validationTime = validationTime;
    this.execTimeMillis = execTimeMillis;
    this.dataSource = dataSource;
  }

  /**
   * 成功した場合のバリデーション結果を作成
   *
   * @param validationType バリデーションタイプ（"JSON", "XML", "CSV"など）
   * @param schemaPath スキーマファイルのパス
   * @param execTimeMillis 実行時間（ミリ秒）
   * @return 成功を表すValidationResult
   */
  public static ValidationResult success(
      final String validationType, final String schemaPath, final long execTimeMillis) {
    return builder()
        .validationType(validationType)
        .schemaPath(schemaPath)
        .success(true)
        .executionTimeMillis(execTimeMillis)
        .build();
  }

  /**
   * 失敗した場合のバリデーション結果を作成
   *
   * @param validationType バリデーションタイプ（"JSON", "XML", "CSV"など）
   * @param schemaPath スキーマファイルのパス
   * @param errors エラーメッセージのリスト
   * @param execTimeMillis 実行時間（ミリ秒）
   * @return 失敗を表すValidationResult
   */
  public static ValidationResult failure(
      final String validationType,
      final String schemaPath,
      final List<String> errors,
      final long execTimeMillis) {
    final Builder builder =
        builder()
            .validationType(validationType)
            .schemaPath(schemaPath)
            .success(false)
            .executionTimeMillis(execTimeMillis);

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
    return valid;
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
    return execTimeMillis;
  }

  /**
   * データソース情報を取得
   *
   * @return データソース情報
   */
  public String getDataSource() {
    return dataSource;
  }

  @Override
  public String toString() {
    return String.format(
        "ValidationResult{type=%s, schema=%s, valid=%s, errors=%d, warnings=%d, executionTimeMs=%d}",
        validationType, schemaPath, valid, errors.size(), warnings.size(), execTimeMillis);
  }

  @Override
  public boolean equals(final Object other) {
    boolean result = false;
    if (this == other) {
      result = true;
    } else if (other instanceof ValidationResult) {
      final ValidationResult that = (ValidationResult) other;
      result =
          valid == that.valid
              && execTimeMillis == that.execTimeMillis
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
        valid,
        errors,
        warnings,
        validationTime,
        execTimeMillis,
        dataSource);
  }

  /** ValidationResult作成用のBuilderクラス */
  @SuppressWarnings("PMD.TooManyMethods")
  // Fluent builder requires one setter per field plus addError/addWarning/build/validation helpers.
  public static class Builder {
    private String typeVal;
    private String schemaPathVal;
    private boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private Instant timeVal;
    private long execTimeMillis;
    private String dataSourceVal;

    /** Creates a builder with empty error/warning lists and default timestamps. */
    public Builder() {
      this.errors = new ArrayList<>();
      this.warnings = new ArrayList<>();
      this.timeVal = Instant.now();
    }

    /**
     * バリデーションタイプを設定
     *
     * @param validationType バリデーションタイプ
     * @return Builder
     */
    public Builder validationType(final String validationType) {
      this.typeVal = validationType;
      return this;
    }

    /**
     * スキーマパスを設定
     *
     * @param schemaPath スキーマファイルのパス
     * @return Builder
     */
    public Builder schemaPath(final String schemaPath) {
      this.schemaPathVal = schemaPath;
      return this;
    }

    /**
     * バリデーション成功/失敗フラグを設定
     *
     * @param success 成功の場合true
     * @return Builder
     */
    public Builder success(final boolean success) {
      this.valid = success;
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
      this.timeVal = validationTime;
      return this;
    }

    /**
     * 実行時間を設定
     *
     * @param execTimeMillis 実行時間（ミリ秒）
     * @return Builder
     */
    public Builder executionTimeMillis(final long execTimeMillis) {
      this.execTimeMillis = execTimeMillis;
      return this;
    }

    /**
     * データソース情報を設定
     *
     * @param dataSource データソース情報
     * @return Builder
     */
    public Builder dataSource(final String dataSource) {
      this.dataSourceVal = dataSource;
      return this;
    }

    /**
     * ValidationResultインスタンスを構築
     *
     * @return ValidationResult
     * @throws IllegalStateException 必須フィールドが設定されていない場合
     */
    public ValidationResult build() {
      requireNonBlank(typeVal, "Validation type");
      requireNonBlank(schemaPathVal, "Schema path");
      ensureConsistency();
      return new ValidationResult(
          typeVal, schemaPathVal, valid, errors, warnings, timeVal, execTimeMillis, dataSourceVal);
    }

    private static void requireNonBlank(final String value, final String field) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(field + " cannot be null or empty");
      }
    }

    private void ensureConsistency() {
      if (valid && !errors.isEmpty()) {
        throw new IllegalStateException(
            "ValidationResult cannot be marked as valid when errors are present");
      }
      if (!valid && errors.isEmpty()) {
        addError("Validation failed (no specific error message)");
      }
    }
  }
}
