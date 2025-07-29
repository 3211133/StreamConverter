package com.streamConverter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * バッチ変換レスポンスDTO
 *
 * <p>バッチ変換処理の結果を返すためのデータ転送オブジェクトです。
 *
 * <p>処理状況:
 *
 * <ul>
 *   <li>SUBMITTED: バッチ処理が受け付けられた
 *   <li>PROCESSING: バッチ処理中
 *   <li>COMPLETED: すべて完了
 *   <li>PARTIAL_SUCCESS: 一部成功
 *   <li>FAILED: 全て失敗
 * </ul>
 *
 * @author StreamConverter
 * @version 1.0.0
 * @since 2025-07-29
 */
@Schema(description = "バッチ変換レスポンス")
public class BatchTransformResponse {

  @Schema(description = "バッチID", example = "BATCH-12345")
  private String batchId;

  @Schema(
      description = "処理状況",
      example = "COMPLETED",
      allowableValues = {"SUBMITTED", "PROCESSING", "COMPLETED", "PARTIAL_SUCCESS", "FAILED"})
  private String status;

  @Schema(description = "処理開始時刻")
  private String startTime;

  @Schema(description = "処理完了時刻")
  private String endTime;

  @Schema(description = "総処理時間（ミリ秒）")
  private Long totalProcessingTimeMs;

  @Schema(description = "処理対象総数")
  private int totalCount;

  @Schema(description = "成功数")
  private int successCount;

  @Schema(description = "失敗数")
  private int failureCount;

  @Schema(description = "個別処理結果")
  private List<TransformResponse> results;

  @Schema(description = "進捗率（0-100）")
  private double progressPercentage;

  @Schema(description = "処理統計情報")
  private Map<String, Object> statistics;

  @Schema(description = "エラーサマリー")
  private List<String> errorSummary;

  @Schema(description = "非同期処理の場合のステータスURL")
  private String statusUrl;

  /** デフォルトコンストラクタ */
  public BatchTransformResponse() {
    this.statistics = new HashMap<>();
    this.startTime = Instant.now().toString();
  }

  /**
   * 成功レスポンス作成
   *
   * @param batchId バッチID
   * @param results 処理結果
   * @param processingTimeMs 処理時間
   * @return 成功レスポンス
   */
  public static BatchTransformResponse success(
      String batchId, List<TransformResponse> results, long processingTimeMs) {
    BatchTransformResponse response = new BatchTransformResponse();
    response.setBatchId(batchId);
    response.setStatus("COMPLETED");
    response.setResults(results);
    response.setTotalProcessingTimeMs(processingTimeMs);
    response.setEndTime(Instant.now().toString());

    // 統計情報の計算
    response.calculateStatistics();

    return response;
  }

  /**
   * 部分成功レスポンス作成
   *
   * @param batchId バッチID
   * @param results 処理結果
   * @param processingTimeMs 処理時間
   * @return 部分成功レスポンス
   */
  public static BatchTransformResponse partialSuccess(
      String batchId, List<TransformResponse> results, long processingTimeMs) {
    BatchTransformResponse response = new BatchTransformResponse();
    response.setBatchId(batchId);
    response.setStatus("PARTIAL_SUCCESS");
    response.setResults(results);
    response.setTotalProcessingTimeMs(processingTimeMs);
    response.setEndTime(Instant.now().toString());

    response.calculateStatistics();

    return response;
  }

  /**
   * 失敗レスポンス作成
   *
   * @param batchId バッチID
   * @param errorMessage エラーメッセージ
   * @return 失敗レスポンス
   */
  public static BatchTransformResponse failure(String batchId, String errorMessage) {
    BatchTransformResponse response = new BatchTransformResponse();
    response.setBatchId(batchId);
    response.setStatus("FAILED");
    response.setEndTime(Instant.now().toString());
    response.setErrorSummary(List.of(errorMessage));

    return response;
  }

  /**
   * 非同期処理受付レスポンス作成
   *
   * @param batchId バッチID
   * @param statusUrl ステータス確認URL
   * @return 受付レスポンス
   */
  public static BatchTransformResponse submitted(String batchId, String statusUrl) {
    BatchTransformResponse response = new BatchTransformResponse();
    response.setBatchId(batchId);
    response.setStatus("SUBMITTED");
    response.setStatusUrl(statusUrl);

    return response;
  }

  /** 統計情報を計算する */
  private void calculateStatistics() {
    if (results != null) {
      totalCount = results.size();
      successCount = (int) results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
      failureCount = totalCount - successCount;
      progressPercentage =
          totalCount > 0 ? (double) (successCount + failureCount) / totalCount * 100 : 0;

      // 詳細統計情報
      statistics.put(
          "averageProcessingTime",
          results.stream().mapToLong(TransformResponse::getProcessingTimeMs).average().orElse(0.0));
      statistics.put(
          "totalInputSize", results.stream().mapToLong(TransformResponse::getInputSize).sum());
      statistics.put(
          "totalOutputSize", results.stream().mapToLong(TransformResponse::getOutputSize).sum());
    }
  }

  // Getters and Setters

  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public Long getTotalProcessingTimeMs() {
    return totalProcessingTimeMs;
  }

  public void setTotalProcessingTimeMs(Long totalProcessingTimeMs) {
    this.totalProcessingTimeMs = totalProcessingTimeMs;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }

  public int getSuccessCount() {
    return successCount;
  }

  public void setSuccessCount(int successCount) {
    this.successCount = successCount;
  }

  public int getFailureCount() {
    return failureCount;
  }

  public void setFailureCount(int failureCount) {
    this.failureCount = failureCount;
  }

  public List<TransformResponse> getResults() {
    return results;
  }

  public void setResults(List<TransformResponse> results) {
    this.results = results;
    calculateStatistics(); // 結果設定時に統計を再計算
  }

  public double getProgressPercentage() {
    return progressPercentage;
  }

  public void setProgressPercentage(double progressPercentage) {
    this.progressPercentage = progressPercentage;
  }

  public Map<String, Object> getStatistics() {
    return statistics;
  }

  public void setStatistics(Map<String, Object> statistics) {
    this.statistics = statistics;
  }

  public List<String> getErrorSummary() {
    return errorSummary;
  }

  public void setErrorSummary(List<String> errorSummary) {
    this.errorSummary = errorSummary;
  }

  public String getStatusUrl() {
    return statusUrl;
  }

  public void setStatusUrl(String statusUrl) {
    this.statusUrl = statusUrl;
  }

  @Override
  public String toString() {
    return String.format(
        "BatchTransformResponse{batchId='%s', status='%s', totalCount=%d, successCount=%d, failureCount=%d, progressPercentage=%.1f%%}",
        batchId, status, totalCount, successCount, failureCount, progressPercentage);
  }
}
