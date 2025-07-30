package com.streamConverter.api.controller;

import com.streamConverter.api.dto.BatchTransformRequest;
import com.streamConverter.api.dto.BatchTransformResponse;
import com.streamConverter.api.dto.TransformRequest;
import com.streamConverter.api.dto.TransformResponse;
import com.streamConverter.api.service.BatchTransformService;
import com.streamConverter.api.service.TransformService;
import com.streamConverter.context.ExecutionContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * StreamConverter WebAPI のメインコントローラー
 *
 * <p>データ変換、バリデーション、ヘルスチェックのエンドポイントを提供します。
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "StreamConverter API", description = "データ変換とバリデーションのAPI")
public class StreamConverterController {

  private static final Logger logger = LoggerFactory.getLogger(StreamConverterController.class);

  private final TransformService transformService;
  private final BatchTransformService batchTransformService;

  /**
   * StreamConverterControllerコンストラクタ
   *
   * @param transformService 変換サービス
   * @param batchTransformService バッチ変換サービス
   */
  @Autowired
  public StreamConverterController(
      TransformService transformService, BatchTransformService batchTransformService) {
    this.transformService = transformService;
    this.batchTransformService = batchTransformService;
  }

  /**
   * データ変換エンドポイント
   *
   * <p>指定された形式のデータを別の形式に変換し、オプションで外部APIに送信します。 値抽出、スキーマバリデーション、MDCコンテキスト管理も提供されます。
   *
   * @param request 変換リクエスト
   * @return 変換結果レスポンス
   */
  @PostMapping("/transform")
  public ResponseEntity<TransformResponse> transform(@Valid @RequestBody TransformRequest request) {
    ExecutionContext context = ExecutionContext.create();
    long startTime = System.currentTimeMillis();

    try {
      // MDCにコンテキスト情報を設定
      context.applyToMDCWithStage("API_REQUEST");

      logger.info(
          "Transform request received - inputFormat: {}, outputFormat: {}, executionId: {}",
          request.getInputFormat(),
          request.getOutputFormat(),
          context.getExecutionId());

      // リクエストの詳細ログ
      logger.debug("Transform request: {}", request);

      // 変換サービスを実行
      TransformResponse response = transformService.transform(request, context);

      // 処理時間を設定
      long processingTime = System.currentTimeMillis() - startTime;
      response.setProcessingTimeMs(processingTime);

      logger.info(
          "Transform completed successfully - executionId: {}, processingTime: {}ms",
          context.getExecutionId(),
          processingTime);

      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      logger.warn(
          "Invalid request - executionId: {}, error: {}", context.getExecutionId(), e.getMessage());

      long processingTime = System.currentTimeMillis() - startTime;
      TransformResponse errorResponse =
          TransformResponse.error(context.getExecutionId(), e.getMessage());
      errorResponse.setProcessingTimeMs(processingTime);

      return ResponseEntity.badRequest().body(errorResponse);

    } catch (Exception e) {
      logger.error(
          "Transform failed - executionId: {}, error: {}",
          context.getExecutionId(),
          e.getMessage(),
          e);

      long processingTime = System.currentTimeMillis() - startTime;
      TransformResponse errorResponse =
          TransformResponse.error(
              context.getExecutionId(), "Internal server error: " + e.getMessage());
      errorResponse.setProcessingTimeMs(processingTime);

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

    } finally {
      MDC.clear();
    }
  }

  @Operation(summary = "バリデーション専用API", description = "指定されたスキーマを使用してデータのバリデーションのみを実行します。")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "バリデーション成功"),
        @ApiResponse(responseCode = "400", description = "バリデーションエラー"),
        @ApiResponse(responseCode = "500", description = "サーバー内部エラー")
      })
  /**
   * バリデーションエンドポイント
   *
   * <p>指定されたスキーマを使用してデータのバリデーションのみを実行します。
   *
   * @param request バリデーショ��リクエスト
   * @return バリデーション結果レスポンス
   */
  @PostMapping("/validate")
  public ResponseEntity<TransformResponse> validate(@Valid @RequestBody TransformRequest request) {
    ExecutionContext context = ExecutionContext.create();
    long startTime = System.currentTimeMillis();

    try {
      context.applyToMDCWithStage("API_VALIDATE");

      logger.info(
          "Validation request received - inputFormat: {}, executionId: {}",
          request.getInputFormat(),
          context.getExecutionId());

      // バリデーションサービスを実行
      TransformResponse response = transformService.validate(request, context);

      long processingTime = System.currentTimeMillis() - startTime;
      response.setProcessingTimeMs(processingTime);

      logger.info(
          "Validation completed - executionId: {}, processingTime: {}ms",
          context.getExecutionId(),
          processingTime);

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.error(
          "Validation failed - executionId: {}, error: {}",
          context.getExecutionId(),
          e.getMessage(),
          e);

      long processingTime = System.currentTimeMillis() - startTime;
      TransformResponse errorResponse =
          TransformResponse.error(context.getExecutionId(), e.getMessage());
      errorResponse.setProcessingTimeMs(processingTime);

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

    } finally {
      MDC.clear();
    }
  }

  @Operation(summary = "バッチ変換API", description = "複数データセットの一括変換処理を実行します。非同期処理にも対応しています。")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "変換成功"),
        @ApiResponse(responseCode = "202", description = "非同期処理受付"),
        @ApiResponse(responseCode = "400", description = "リクエスト不正"),
        @ApiResponse(responseCode = "500", description = "サーバーエラー")
      })
  /**
   * バッチ変換エンドポイント
   *
   * <p>複数データセットの一括変換処理を実行します。非同期処理にも対応しています。
   *
   * @param request バッチ変換リクエスト
   * @return バッチ変換結果または受付レスポンス
   */
  @PostMapping("/batch/transform")
  public ResponseEntity<BatchTransformResponse> batchTransform(
      @Valid @RequestBody BatchTransformRequest request) {

    logger.info(
        "Batch transform request received - datasets: {}, async: {}",
        request.getDatasets().size(),
        request.isAsyncProcessing());

    try {
      if (request.isAsyncProcessing()) {
        // 非同期処理
        batchTransformService.processBatchAsync(request);
        BatchTransformResponse response =
            BatchTransformResponse.submitted(
                request.getBatchId(), "/api/v1/batch/" + request.getBatchId() + "/status");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

      } else {
        // 同期処理
        BatchTransformResponse response = batchTransformService.processBatch(request);
        return ResponseEntity.ok(response);
      }

    } catch (Exception e) {
      logger.error("Batch transform failed", e);
      BatchTransformResponse errorResponse =
          BatchTransformResponse.failure(
              request.getBatchId(), "Batch processing failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  @Operation(summary = "バッチ処理状況取得", description = "非同期バッチ処理の進捗状況を取得します。")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "状況取得成功"),
        @ApiResponse(responseCode = "404", description = "バッチが見つからない")
      })
  /**
   * バッチ処理状況取得エンドポイント
   *
   * <p>非同期バッチ処理の進捗状況を取得します。
   *
   * @param batchId バッチID
   * @return バッチ処理状況
   */
  @GetMapping("/batch/{batchId}/status")
  public ResponseEntity<BatchTransformResponse> getBatchStatus(@PathVariable String batchId) {

    logger.info("Batch status request - batchId: {}", batchId);

    try {
      BatchTransformResponse response = batchTransformService.getBatchStatus(batchId);

      if ("Batch not found"
          .equals(
              response.getErrorSummary() != null && !response.getErrorSummary().isEmpty()
                  ? response.getErrorSummary().get(0)
                  : "")) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
      }

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.error("Failed to get batch status - batchId: {}", batchId, e);
      BatchTransformResponse errorResponse =
          BatchTransformResponse.failure(batchId, "Failed to get batch status: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  @Operation(summary = "ヘルスチェック", description = "APIの正常性を確認します。")
  @ApiResponse(responseCode = "200", description = "正常")
  /**
   * ヘルスチェックエンドポイント
   *
   * <p>APIの正常性を確認します。
   *
   * @return システム稼働状態
   */
  @GetMapping("/health")
  public ResponseEntity<Object> health() {
    return ResponseEntity.ok(
        new Object() {
          public String status = "UP";
          public LocalDateTime timestamp = LocalDateTime.now();
          public String version = "1.0.0";
          public String service = "StreamConverter API";
        });
  }
}
