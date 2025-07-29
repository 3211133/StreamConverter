package com.streamConverter.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * バッチ変換リクエストDTO
 *
 * <p>複数データセットの一括変換処理をリクエストするためのデータ転送オブジェクトです。
 *
 * <p>主要機能:
 *
 * <ul>
 *   <li>複数データセットの一括変換
 *   <li>非同期処理対応
 *   <li>進捗状況通知
 * </ul>
 *
 * @author StreamConverter
 * @version 1.0.0
 * @since 2025-07-29
 */
@Schema(description = "バッチ変換リクエスト")
public class BatchTransformRequest {

  @Schema(description = "バッチID（オプション）")
  private String batchId;

  @NotEmpty(message = "データセットは必須です")
  @Size(min = 1, max = 100, message = "データセットは1-100個まで対応")
  @Valid
  @Schema(description = "変換対象データセット（最大100件）", required = true)
  private List<TransformRequest> datasets;

  @Schema(description = "非同期処理フラグ（true: 非同期, false: 同期）", defaultValue = "false")
  private boolean asyncProcessing = false;

  @Schema(description = "進捗通知URL（非同期処理時のコールバック）")
  private String notificationUrl;

  @Schema(description = "並列処理数（1-10、デフォルト：3）", defaultValue = "3")
  private int parallelism = 3;

  @Schema(description = "エラー発生時の継続フラグ（true: 継続, false: 停止）", defaultValue = "true")
  private boolean continueOnError = true;

  @Schema(description = "プライオリティ（HIGH, NORMAL, LOW）", defaultValue = "NORMAL")
  private String priority = "NORMAL";

  /** デフォルトコンストラクタ */
  public BatchTransformRequest() {}

  /**
   * バッチリクエスト作成コンストラクタ
   *
   * @param datasets 変換対象データセット
   */
  public BatchTransformRequest(List<TransformRequest> datasets) {
    this.datasets = datasets;
  }

  /**
   * 完全コンストラクタ
   *
   * @param batchId バッチID
   * @param datasets 変換対象データセット
   * @param asyncProcessing 非同期処理フラグ
   * @param notificationUrl 進捗通知URL
   * @param parallelism 並列処理数
   * @param continueOnError エラー継続フラグ
   * @param priority プライオリティ
   */
  public BatchTransformRequest(
      String batchId,
      List<TransformRequest> datasets,
      boolean asyncProcessing,
      String notificationUrl,
      int parallelism,
      boolean continueOnError,
      String priority) {
    this.batchId = batchId;
    this.datasets = datasets;
    this.asyncProcessing = asyncProcessing;
    this.notificationUrl = notificationUrl;
    this.parallelism = parallelism;
    this.continueOnError = continueOnError;
    this.priority = priority;
  }

  // Getters and Setters

  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  public List<TransformRequest> getDatasets() {
    return datasets;
  }

  public void setDatasets(List<TransformRequest> datasets) {
    this.datasets = datasets;
  }

  public boolean isAsyncProcessing() {
    return asyncProcessing;
  }

  public void setAsyncProcessing(boolean asyncProcessing) {
    this.asyncProcessing = asyncProcessing;
  }

  public String getNotificationUrl() {
    return notificationUrl;
  }

  public void setNotificationUrl(String notificationUrl) {
    this.notificationUrl = notificationUrl;
  }

  public int getParallelism() {
    return parallelism;
  }

  public void setParallelism(int parallelism) {
    this.parallelism = Math.max(1, Math.min(10, parallelism)); // 1-10の範囲に制限
  }

  public boolean isContinueOnError() {
    return continueOnError;
  }

  public void setContinueOnError(boolean continueOnError) {
    this.continueOnError = continueOnError;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  @Override
  public String toString() {
    return String.format(
        "BatchTransformRequest{batchId='%s', datasets=%d, asyncProcessing=%s, parallelism=%d, priority='%s'}",
        batchId, datasets != null ? datasets.size() : 0, asyncProcessing, parallelism, priority);
  }
}
