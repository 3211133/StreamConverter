package com.streamConverter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

/** 変換APIのレスポンスDTO */
@Schema(description = "データ変換レスポンス")
public class TransformResponse {

  @Schema(description = "変換結果データ")
  private String data;

  @Schema(description = "処理ステータス", example = "SUCCESS")
  private String status;

  @Schema(description = "実行コンテキストID", example = "EXEC-12345678-1753783572196")
  private String executionId;

  @Schema(description = "処理時間（ミリ秒）", example = "125")
  private long processingTimeMs;

  @Schema(description = "処理開始時刻")
  private LocalDateTime timestamp;

  @Schema(description = "入力データサイズ（バイト）", example = "1024")
  private long inputSize;

  @Schema(description = "出力データサイズ（バイト）", example = "2048")
  private long outputSize;

  @Schema(description = "抽出された値（MDCコンテキスト）")
  private Map<String, String> extractedValues;

  @Schema(description = "エラーメッセージ（エラー時のみ）")
  private String errorMessage;

  // Constructors
  /** デフォルトコンストラクタ */
  public TransformResponse() {}

  /**
   * ステータスと実行IDを指定するコンストラクタ
   *
   * @param status 処理ステータス
   * @param executionId 実行コンテキストID
   */
  public TransformResponse(String status, String executionId) {
    this.status = status;
    this.executionId = executionId;
    this.timestamp = LocalDateTime.now();
  }

  /**
   * 成功レスポンスを作成するファクトリーメソッド
   *
   * @param data 変換結果データ
   * @param executionId 実行コンテキストID
   * @param processingTimeMs 処理時間（ミリ秒）
   * @param inputSize 入力データサイズ
   * @param outputSize 出力データサイズ
   * @param extractedValues 抽出された値
   * @return 成功レスポンス
   */
  public static TransformResponse success(
      String data,
      String executionId,
      long processingTimeMs,
      long inputSize,
      long outputSize,
      Map<String, String> extractedValues) {
    TransformResponse response = new TransformResponse("SUCCESS", executionId);
    response.setData(data);
    response.setProcessingTimeMs(processingTimeMs);
    response.setInputSize(inputSize);
    response.setOutputSize(outputSize);
    response.setExtractedValues(extractedValues);
    return response;
  }

  /**
   * エラーレスポンスを作成するファクトリーメソッド
   *
   * @param executionId 実行コンテキストID
   * @param errorMessage エラーメッセージ
   * @return エラーレスポンス
   */
  public static TransformResponse error(String executionId, String errorMessage) {
    TransformResponse response = new TransformResponse("ERROR", executionId);
    response.setErrorMessage(errorMessage);
    return response;
  }

  // Getters and Setters
  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public long getProcessingTimeMs() {
    return processingTimeMs;
  }

  public void setProcessingTimeMs(long processingTimeMs) {
    this.processingTimeMs = processingTimeMs;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public long getInputSize() {
    return inputSize;
  }

  public void setInputSize(long inputSize) {
    this.inputSize = inputSize;
  }

  public long getOutputSize() {
    return outputSize;
  }

  public void setOutputSize(long outputSize) {
    this.outputSize = outputSize;
  }

  public Map<String, String> getExtractedValues() {
    return extractedValues;
  }

  public void setExtractedValues(Map<String, String> extractedValues) {
    this.extractedValues = extractedValues;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  public String toString() {
    return "TransformResponse{"
        + "status='"
        + status
        + '\''
        + ", executionId='"
        + executionId
        + '\''
        + ", processingTimeMs="
        + processingTimeMs
        + ", timestamp="
        + timestamp
        + ", inputSize="
        + inputSize
        + ", outputSize="
        + outputSize
        + ", extractedValuesCount="
        + (extractedValues != null ? extractedValues.size() : 0)
        + ", errorMessage='"
        + errorMessage
        + '\''
        + '}';
  }
}
