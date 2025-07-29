package com.streamConverter.api.service;

import com.streamConverter.api.dto.BatchTransformRequest;
import com.streamConverter.api.dto.BatchTransformResponse;
import com.streamConverter.api.dto.TransformRequest;
import com.streamConverter.api.dto.TransformResponse;
import com.streamConverter.context.ExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

/**
 * バッチ変換処理サービス
 *
 * <p>複数データセットの一括変換処理と非同期処理管理を提供します。
 *
 * <p>主要機能:
 *
 * <ul>
 *   <li>複数データセットの並列処理
 *   <li>非同期バッチ処理
 *   <li>進捗状況の追跡
 *   <li>エラー処理とリトライ
 * </ul>
 *
 * @author StreamConverter
 * @version 1.0.0
 * @since 2025-07-29
 */
@Service
@EnableAsync
public class BatchTransformService {

  private static final Logger logger = LoggerFactory.getLogger(BatchTransformService.class);

  @Autowired private TransformService transformService;

  // バッチ処理状況の管理
  private final ConcurrentHashMap<String, BatchTransformResponse> batchStatus =
      new ConcurrentHashMap<>();

  // 並列処理用のExecutorService
  private final ExecutorService batchExecutor = Executors.newFixedThreadPool(10);

  /**
   * 同期バッチ変換処理
   *
   * <p>複数データセットを並列処理で変換し、すべての結果を待機して返します。
   *
   * @param request バッチ変換リクエスト
   * @return バッチ変換結果
   */
  public BatchTransformResponse processBatch(BatchTransformRequest request) {
    String batchId = generateBatchId(request.getBatchId());
    logger.info(
        "Starting synchronous batch processing - batchId: {}, datasets: {}",
        batchId,
        request.getDatasets().size());

    long startTime = System.currentTimeMillis();

    try {
      List<TransformResponse> results =
          processDatasetsParallel(
              request.getDatasets(),
              request.getParallelism(),
              request.isContinueOnError(),
              batchId);

      long processingTime = System.currentTimeMillis() - startTime;

      BatchTransformResponse response = createBatchResponse(batchId, results, processingTime);
      logger.info(
          "Batch processing completed - batchId: {}, success: {}, failure: {}, time: {}ms",
          batchId,
          response.getSuccessCount(),
          response.getFailureCount(),
          processingTime);

      return response;

    } catch (Exception e) {
      logger.error("Batch processing failed - batchId: {}", batchId, e);
      return BatchTransformResponse.failure(batchId, "Batch processing failed: " + e.getMessage());
    }
  }

  /**
   * 非同期バッチ変換処理
   *
   * <p>バッチ処理を非同期で開始し、即座にレスポンスを返します。
   *
   * @param request バッチ変換リクエスト
   * @return バッチ受付レスポンス
   */
  @Async
  public CompletableFuture<BatchTransformResponse> processBatchAsync(
      BatchTransformRequest request) {
    String batchId = generateBatchId(request.getBatchId());
    String statusUrl = "/api/v1/batch/" + batchId + "/status";

    logger.info(
        "Starting asynchronous batch processing - batchId: {}, datasets: {}",
        batchId,
        request.getDatasets().size());

    // 初期状態をレジストリに登録
    BatchTransformResponse initialStatus = BatchTransformResponse.submitted(batchId, statusUrl);
    batchStatus.put(batchId, initialStatus);

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // 処理中状態に更新
            initialStatus.setStatus("PROCESSING");
            batchStatus.put(batchId, initialStatus);

            long startTime = System.currentTimeMillis();

            List<TransformResponse> results =
                processDatasetsParallel(
                    request.getDatasets(),
                    request.getParallelism(),
                    request.isContinueOnError(),
                    batchId);

            long processingTime = System.currentTimeMillis() - startTime;

            BatchTransformResponse finalResponse =
                createBatchResponse(batchId, results, processingTime);
            batchStatus.put(batchId, finalResponse);

            // 通知URLが指定されている場合はコールバック実行
            if (request.getNotificationUrl() != null) {
              sendNotification(request.getNotificationUrl(), finalResponse);
            }

            logger.info(
                "Async batch processing completed - batchId: {}, success: {}, failure: {}, time: {}ms",
                batchId,
                finalResponse.getSuccessCount(),
                finalResponse.getFailureCount(),
                processingTime);

            return finalResponse;

          } catch (Exception e) {
            logger.error("Async batch processing failed - batchId: {}", batchId, e);
            BatchTransformResponse errorResponse =
                BatchTransformResponse.failure(
                    batchId, "Async processing failed: " + e.getMessage());
            batchStatus.put(batchId, errorResponse);
            return errorResponse;
          }
        },
        batchExecutor);
  }

  /**
   * バッチ処理状況を取得
   *
   * @param batchId バッチID
   * @return バッチ処理状況
   */
  public BatchTransformResponse getBatchStatus(String batchId) {
    BatchTransformResponse status = batchStatus.get(batchId);
    if (status == null) {
      logger.warn("Batch status not found - batchId: {}", batchId);
      return BatchTransformResponse.failure(batchId, "Batch not found");
    }
    return status;
  }

  /**
   * 複数データセットを並列処理
   *
   * @param datasets 処理対象データセット
   * @param parallelism 並列数
   * @param continueOnError エラー継続フラグ
   * @param batchId バッチID
   * @return 変換結果リスト
   */
  private List<TransformResponse> processDatasetsParallel(
      List<TransformRequest> datasets, int parallelism, boolean continueOnError, String batchId) {
    List<Future<TransformResponse>> futures = new ArrayList<>();
    ExecutorService executor = Executors.newFixedThreadPool(parallelism);

    try {
      // 各データセットを非同期で処理
      for (int i = 0; i < datasets.size(); i++) {
        final int index = i;
        final TransformRequest dataset = datasets.get(i);

        Future<TransformResponse> future =
            executor.submit(
                () -> {
                  ExecutionContext context = null;
                  try {
                    context =
                        ExecutionContext.builder()
                            .globalContext("batchId", batchId)
                            .globalContext("datasetIndex", String.valueOf(index))
                            .build();

                    return transformService.transform(dataset, context);

                  } catch (Exception e) {
                    logger.error(
                        "Dataset processing failed - batchId: {}, index: {}", batchId, index, e);
                    return TransformResponse.error(
                        context != null ? context.getExecutionId() : "UNKNOWN",
                        "Dataset processing failed: " + e.getMessage());
                  }
                });

        futures.add(future);
      }

      // 結果を収集
      List<TransformResponse> results = new ArrayList<>();
      for (Future<TransformResponse> future : futures) {
        try {
          TransformResponse result = future.get();
          results.add(result);

          // エラー時の処理制御
          if ("ERROR".equals(result.getStatus()) && !continueOnError) {
            logger.warn("Stopping batch processing due to error - batchId: {}", batchId);
            break;
          }

        } catch (Exception e) {
          logger.error("Failed to get result from future", e);
          results.add(
              TransformResponse.error("UNKNOWN", "Future execution failed: " + e.getMessage()));

          if (!continueOnError) {
            break;
          }
        }
      }

      return results;

    } finally {
      executor.shutdown();
    }
  }

  /**
   * バッチレスポンスを作成
   *
   * @param batchId バッチID
   * @param results 処理結果
   * @param processingTime 処理時間
   * @return バッチレスポンス
   */
  private BatchTransformResponse createBatchResponse(
      String batchId, List<TransformResponse> results, long processingTime) {
    long successCount = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
    long failureCount = results.size() - successCount;

    if (failureCount == 0) {
      return BatchTransformResponse.success(batchId, results, processingTime);
    } else if (successCount > 0) {
      return BatchTransformResponse.partialSuccess(batchId, results, processingTime);
    } else {
      BatchTransformResponse response =
          BatchTransformResponse.failure(batchId, "All datasets failed");
      response.setResults(results);
      response.setTotalProcessingTimeMs(processingTime);
      return response;
    }
  }

  /**
   * バッチIDを生成
   *
   * @param requestBatchId リクエストのバッチID
   * @return 生成されたバッチID
   */
  private String generateBatchId(String requestBatchId) {
    if (requestBatchId != null && !requestBatchId.trim().isEmpty()) {
      return requestBatchId;
    }
    return "BATCH-" + UUID.randomUUID().toString().substring(0, 8);
  }

  /**
   * 完了通知を送信
   *
   * @param notificationUrl 通知URL
   * @param response バッチレスポンス
   */
  private void sendNotification(String notificationUrl, BatchTransformResponse response) {
    // 実際の通知送信実装（HTTPクライアント等を使用）
    logger.info("Sending notification to {} for batch {}", notificationUrl, response.getBatchId());
    // TODO: HTTP POST で通知を送信する実装
  }

  /** サービス停止時のクリーンアップ */
  public void shutdown() {
    logger.info("Shutting down BatchTransformService");
    batchExecutor.shutdown();
  }
}
