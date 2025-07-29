package com.streamConverter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 変換APIのリクエストDTO */
@Schema(description = "データ変換リクエスト")
public class TransformRequest {

  @NotBlank(message = "入力形式は必須です")
  @Pattern(regexp = "JSON|XML|CSV", message = "入力形式はJSON、XML、CSVのいずれかである必要があります")
  @Schema(
      description = "入力データ形式",
      example = "JSON",
      allowableValues = {"JSON", "XML", "CSV"})
  private String inputFormat;

  @NotBlank(message = "出力形式は必須です")
  @Pattern(regexp = "JSON|XML|CSV", message = "出力形式はJSON、XML、CSVのいずれかである必要があります")
  @Schema(
      description = "出力データ形式",
      example = "XML",
      allowableValues = {"JSON", "XML", "CSV"})
  private String outputFormat;

  @Schema(description = "送信先API URL", example = "http://internal.api.com/users")
  private String targetApi;

  @Schema(description = "値抽出パス (JSONPath, XPath, またはCSVカラム名)", example = "$.user.id")
  private String extractPath;

  @Schema(description = "MDCキー名 (抽出した値を格納するキー)", example = "userId")
  private String mdcKey;

  @Schema(description = "バリデーションスキーマファイルパス", example = "schema/user.json")
  private String validationSchema;

  @NotNull(message = "データは必須です")
  @Schema(description = "変換対象データ", example = "{\"user\":{\"id\":\"12345\",\"name\":\"John\"}}")
  private String data;

  // Constructors
  /** デフォルトコンストラクタ */
  public TransformRequest() {}

  /**
   * 全フィールドを指定するコンストラクタ
   *
   * @param inputFormat 入力データ形式
   * @param outputFormat 出力データ形式
   * @param targetApi 送信先API URL
   * @param extractPath 値抽出パス
   * @param mdcKey MDCキー名
   * @param validationSchema バリデーションスキーマファイルパス
   * @param data 変換対象データ
   */
  public TransformRequest(
      String inputFormat,
      String outputFormat,
      String targetApi,
      String extractPath,
      String mdcKey,
      String validationSchema,
      String data) {
    this.inputFormat = inputFormat;
    this.outputFormat = outputFormat;
    this.targetApi = targetApi;
    this.extractPath = extractPath;
    this.mdcKey = mdcKey;
    this.validationSchema = validationSchema;
    this.data = data;
  }

  // Getters and Setters
  public String getInputFormat() {
    return inputFormat;
  }

  public void setInputFormat(String inputFormat) {
    this.inputFormat = inputFormat;
  }

  public String getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  public String getTargetApi() {
    return targetApi;
  }

  public void setTargetApi(String targetApi) {
    this.targetApi = targetApi;
  }

  public String getExtractPath() {
    return extractPath;
  }

  public void setExtractPath(String extractPath) {
    this.extractPath = extractPath;
  }

  public String getMdcKey() {
    return mdcKey;
  }

  public void setMdcKey(String mdcKey) {
    this.mdcKey = mdcKey;
  }

  public String getValidationSchema() {
    return validationSchema;
  }

  public void setValidationSchema(String validationSchema) {
    this.validationSchema = validationSchema;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return "TransformRequest{"
        + "inputFormat='"
        + inputFormat
        + '\''
        + ", outputFormat='"
        + outputFormat
        + '\''
        + ", targetApi='"
        + targetApi
        + '\''
        + ", extractPath='"
        + extractPath
        + '\''
        + ", mdcKey='"
        + mdcKey
        + '\''
        + ", validationSchema='"
        + validationSchema
        + '\''
        + ", dataLength="
        + (data != null ? data.length() : 0)
        + '}';
  }
}
