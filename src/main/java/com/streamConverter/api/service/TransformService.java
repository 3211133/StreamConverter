package com.streamConverter.api.service;

import com.streamConverter.api.dto.TransformRequest;
import com.streamConverter.api.dto.TransformResponse;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.ValueExtractionDecorator;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.csv.CsvValidateCommand;
import com.streamConverter.command.impl.json.JsonValidateCommand;
import com.streamConverter.command.impl.xml.ValidateCommand;
import com.streamConverter.context.ExecutionContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * データ変換処理を行うサービスクラス
 *
 * <p>StreamConverterの機能をWebAPI向けにラップし、 リクエスト/レスポンスの変換とエラーハンドリングを提供します。
 */
@Service
public class TransformService {

  private static final Logger logger = LoggerFactory.getLogger(TransformService.class);

  @Autowired private CacheService cacheService;

  /** TransformServiceコンストラクタ */
  public TransformService() {
    // StreamConverterは動的にコマンドを組み立てるため、インスタンス化は行わない
  }

  /**
   * データ変換処理を実行
   *
   * @param request 変換リクエスト
   * @param context 実行コンテキスト
   * @return 変換結果
   * @throws Exception 変換処理でエラーが発生した場合
   */
  public TransformResponse transform(TransformRequest request, ExecutionContext context)
      throws Exception {
    Objects.requireNonNull(request, "Request cannot be null");
    Objects.requireNonNull(context, "ExecutionContext cannot be null");

    logger.info("Starting data transformation - executionId: {}", context.getExecutionId());

    // キャッシュから変換設定を取得
    CacheService.TransformConfig cachedConfig =
        cacheService.getTransformConfig(
            request.getInputFormat(), request.getOutputFormat(), request.getExtractPath());
    logger.debug("Using cached transform config: {}", cachedConfig);

    // 入力データサイズを記録
    long inputSize = request.getData().getBytes(StandardCharsets.UTF_8).length;

    try (InputStream inputStream =
            new ByteArrayInputStream(request.getData().getBytes(StandardCharsets.UTF_8));
        OutputStream outputStream = new ByteArrayOutputStream()) {

      // パイプラインを構築
      IStreamCommand pipeline = buildTransformPipeline(request);

      // 変換を実行（単一コマンド実行）
      pipeline.execute(inputStream, outputStream);

      // 結果データ
      String resultData = outputStream.toString();
      long outputSize = resultData.getBytes(StandardCharsets.UTF_8).length;

      // MDCから抽出された値を取得
      Map<String, String> extractedValues = getExtractedValues();

      logger.info(
          "Transformation completed - executionId: {}, inputSize: {}bytes, outputSize: {}bytes",
          context.getExecutionId(),
          inputSize,
          outputSize);

      return TransformResponse.success(
          resultData, context.getExecutionId(), 0L, inputSize, outputSize, extractedValues);

    } catch (Exception e) {
      logger.error("Transformation failed - executionId: {}", context.getExecutionId(), e);
      throw e;
    }
  }

  /**
   * バリデーション処理を実行
   *
   * @param request バリデーションリクエスト
   * @param context 実行コンテキスト
   * @return バリデーション結果
   * @throws Exception バリデーション処理でエラーが発生した場合
   */
  public TransformResponse validate(TransformRequest request, ExecutionContext context)
      throws Exception {
    Objects.requireNonNull(request, "Request cannot be null");
    Objects.requireNonNull(context, "ExecutionContext cannot be null");

    logger.info("Starting data validation - executionId: {}", context.getExecutionId());

    // バリデーション結果をキャッシュから確認
    if (request.getValidationSchema() != null) {
      boolean cachedResult =
          cacheService.validateAndCache(
              request.getData(), request.getInputFormat(), request.getValidationSchema());
      logger.debug("Cached validation result: {}", cachedResult);
    }

    // 入力データサイズを記録
    long inputSize = request.getData().getBytes(StandardCharsets.UTF_8).length;

    try (InputStream inputStream =
        new ByteArrayInputStream(request.getData().getBytes(StandardCharsets.UTF_8))) {

      // バリデーションコマンドを構築
      IStreamCommand validationCommand = buildValidationCommand(request);

      // バリデーションを実行（ConsumerCommandなので出力ストリームは不要）
      if (validationCommand instanceof com.streamConverter.command.ConsumerCommand) {
        ((com.streamConverter.command.ConsumerCommand) validationCommand).consume(inputStream);
      } else {
        // 念のため通常の実行パスも提供
        try (OutputStream outputStream = new ByteArrayOutputStream()) {
          validationCommand.execute(inputStream, outputStream);
        }
      }

      logger.info("Validation completed successfully - executionId: {}", context.getExecutionId());

      return TransformResponse.success(
          "Validation successful", context.getExecutionId(), 0L, inputSize, 0L, new HashMap<>());

    } catch (Exception e) {
      logger.error("Validation failed - executionId: {}", context.getExecutionId(), e);
      throw e;
    }
  }

  /**
   * 変換パイプラインを構築
   *
   * @param request 変換リクエスト
   * @return 構築されたパイプラインコマンド
   */
  private IStreamCommand buildTransformPipeline(TransformRequest request) {
    IStreamCommand baseCommand = new SampleStreamCommand();

    // 値抽出が必要な場合はDecoratorを適用
    if (request.getExtractPath() != null
        && !request.getExtractPath().trim().isEmpty()
        && request.getMdcKey() != null
        && !request.getMdcKey().trim().isEmpty()) {

      logger.debug(
          "Adding value extraction - format: {}, path: {}, key: {}",
          request.getInputFormat(),
          request.getExtractPath(),
          request.getMdcKey());

      baseCommand =
          new ValueExtractionDecorator(
              baseCommand, request.getInputFormat(), request.getExtractPath(), request.getMdcKey());
    }

    // TODO: 将来的には以下の機能を追加
    // - バリデーションDecorator
    // - 形式変換Command
    // - 外部API送信Command

    return baseCommand;
  }

  /**
   * バリデーションコマンドを構築
   *
   * @param request バリデーションリクエスト
   * @return 構築されたバリデーションコマンド
   * @throws Exception コマンド構築でエラーが発生した場合
   */
  private IStreamCommand buildValidationCommand(TransformRequest request) throws Exception {
    if (request.getValidationSchema() == null || request.getValidationSchema().trim().isEmpty()) {
      throw new IllegalArgumentException("Validation schema is required for validation");
    }

    String inputFormat = request.getInputFormat().toUpperCase();
    switch (inputFormat) {
      case "JSON":
        return new JsonValidateCommand(request.getValidationSchema());
      case "XML":
        return new ValidateCommand(request.getValidationSchema());
      case "CSV":
        // CSVバリデーションの場合、スキーマファイルから必須カラムを読み取る必要がある
        // 簡略化のため、カンマ区切りの文字列として扱う
        String[] columns = request.getValidationSchema().split(",");
        return new CsvValidateCommand(columns);
      default:
        throw new IllegalArgumentException("Unsupported input format: " + inputFormat);
    }
  }

  /**
   * MDCから抽出された値を取得（StreamConverter固有のキーを除外）
   *
   * @return 抽出された値のマップ
   */
  private Map<String, String> getExtractedValues() {
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();
    if (mdcContext == null) {
      return new HashMap<>();
    }

    // StreamConverter固有のキーを除外
    Map<String, String> extractedValues = new HashMap<>();
    mdcContext.forEach(
        (key, value) -> {
          if (!key.equals(ExecutionContext.EXECUTION_ID_KEY)
              && !key.equals(ExecutionContext.START_TIME_KEY)
              && !key.equals(ExecutionContext.COMMAND_SEQUENCE_KEY)
              && !key.equals(ExecutionContext.THREAD_NAME_KEY)
              && !key.equals(ExecutionContext.STAGE_KEY)) {
            extractedValues.put(key, value);
          }
        });

    return extractedValues;
  }
}
