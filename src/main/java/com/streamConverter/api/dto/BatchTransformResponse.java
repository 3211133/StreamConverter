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

  /**
   * バッチIDを取得します。
   *
   * @return バッチID
   */
  public String getBatchId() {
    return batchId;
  }

  /**
   * バッチIDを設定します。
   *
   * @param batchId バッチID
   */
  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  /**
   * 処理状況を取得します。
   *
   * @return 処理状況
   */
  public String getStatus() {
    return status;
  }

  /**
   * 処理状況を設定します。
   *
   * @param status 処理状況
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * 処理開始時刻を取得します。
   *
   * @return 処理開始時刻
   */
  public String getStartTime() {
    return startTime;
  }

  /**
   * 処理開始時刻を設定します。
   *
   * @param startTime 処理開始時刻
   */
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  /**
   * 処理完了時刻を取得します。
   *
   * @return 処理���了時刻
   */
  public String getEndTime() {
    return endTime;
  }

  /**
   * 処理完了時刻を設定します。
   *
   * @param endTime 処理完了時刻
   */
  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  /**
   * 総処理時間（ミリ秒）を取得します。
   *
   * @return 総処理時間（ミリ秒）
   */
  public Long getTotalProcessingTimeMs() {
    return totalProcessingTimeMs;
  }

  /**
   * 総処理時間（ミリ秒）を設定します。
   *
   * @param totalProcessingTimeMs 総処理時間（ミリ秒）
   */
  public void setTotalProcessingTimeMs(Long totalProcessingTimeMs) {
    this.totalProcessingTimeMs = totalProcessingTimeMs;
  }

  /**
   * 処理対象総数を取得します。
   *
   * @return 処理対象総数
   */
  public int getTotalCount() {
    return totalCount;
  }

  /**
   * 処理対象総数を設定します。
   *
   * @param totalCount 処理対象総数
   */
  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }

  /**
   * 成功数を取得します。
   *
   * @return 成功数
   */
  public int getSuccessCount() {
    return successCount;
  }

  /**
   * 成功数を設定します。
   *
   * @param successCount 成功数
   */
  public void setSuccessCount(int successCount) {
    this.successCount = successCount;
  }

  /**
   * 失敗数を取得します。
   *
   * @return 失敗数
   */
  public int getFailureCount() {
    return failureCount;
  }

  /**
   * 失敗数を設定します。
   *
   * @param failureCount 失敗数
   */
  public void setFailureCount(int failureCount) {
    this.failureCount = failureCount;
  }

  /**
   * 個別処理結果のリストを取得します。
   *
   * @return 個別処理結果のリスト
   */
  public List<TransformResponse> getResults() {
    return results;
  }

  /**
   * 個別処理結果のリストを設定します。
   *
   * @param results 個別処理結果のリスト
   */
  public void setResults(List<TransformResponse> results) {
    this.results = results;
    calculateStatistics(); // 結果設定時に統計を再計算
  }

  /**
   * 進捗率（0-100）を取得します。
   *
   * @return 進捗率
   */
  public double getProgressPercentage() {
    return progressPercentage;
  }

  /**
   * 進捗率（0-100）を設定します。
   *
   * @param progressPercentage 進捗率
   */
  public void setProgressPercentage(double progressPercentage) {
    this.progressPercentage = progressPercentage;
  }

  /**
   * 処理統計情報を取得します。
   *
   * @return 処理統計情報
   */
  public Map<String, Object> getStatistics() {
    return statistics;
  }

  /**
   * 処理統計情報を設定します。
   *
   * @param statistics 処理統計情報
   */
  public void setStatistics(Map<String, Object> statistics) {
    this.statistics = statistics;
  }

  /**
   * エラーサマリーのリストを取得します。
   *
   * @return エラーサマリーのリスト
   */
  public List<String> getErrorSummary() {
    return errorSummary;
  }

  /**
   * エラーサマリーのリストを設定します。
   *
   * @param errorSummary エラーサマリーのリスト
   */
  public void setErrorSummary(List<String> errorSummary) {
    this.errorSummary = errorSummary;
  }

  /**
   * 非同期処理の場合のステータスURLを取���します。
   *
   * @return ステータスURL
   */
  public String getStatusUrl() {
    return statusUrl;
  }

  /**
   * 非同期処理の場合のステータスURLを設定します。
   *
   * @param statusUrl ステータスURL
   */
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
