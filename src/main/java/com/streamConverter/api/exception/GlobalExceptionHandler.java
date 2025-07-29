package com.streamConverter.api.exception;

import com.streamConverter.StreamProcessingException;
import com.streamConverter.api.dto.TransformResponse;
import com.streamConverter.context.ExecutionContext;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * グローバル例外ハンドラー
 *
 * <p>StreamConverter WebAPIで発生する例外を統一的に処理し、 適切なHTTPステータスコードとエラーレスポンスを返します。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * バリデーションエラーのハンドリング
   *
   * @param ex バリデーション例外
   * @param request Webリクエスト
   * @return エラーレスポンス
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, WebRequest request) {

    String executionId = MDC.get(ExecutionContext.EXECUTION_ID_KEY);
    if (executionId == null) {
      executionId = "UNKNOWN";
    }

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });

    logger.warn("Validation failed - executionId: {}, errors: {}", executionId, errors);

    ErrorResponse errorResponse =
        new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            executionId,
            LocalDateTime.now(),
            errors);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * 制約違反例外のハンドリング
   *
   * @param ex 制約違反例外
   * @param request Webリクエスト
   * @return エラーレスポンス
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {

    String executionId = getExecutionId();

    Map<String, String> errors = new HashMap<>();
    ex.getConstraintViolations()
        .forEach(
            violation -> {
              String fieldName = violation.getPropertyPath().toString();
              String errorMessage = violation.getMessage();
              errors.put(fieldName, errorMessage);
            });

    logger.warn("Constraint violation - executionId: {}, errors: {}", executionId, errors);

    ErrorResponse errorResponse =
        new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Request constraint violation",
            executionId,
            LocalDateTime.now(),
            errors);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * StreamProcessingExceptionのハンドリング
   *
   * @param ex ストリーム処理例外
   * @param request Webリクエスト
   * @return エラーレスポンス
   */
  @ExceptionHandler(StreamProcessingException.class)
  public ResponseEntity<TransformResponse> handleStreamProcessingException(
      StreamProcessingException ex, WebRequest request) {

    String executionId = getExecutionId();

    logger.error(
        "Stream processing failed - executionId: {}, error: {}", executionId, ex.getMessage(), ex);

    TransformResponse errorResponse = TransformResponse.error(executionId, ex.getMessage());

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
  }

  /**
   * IllegalArgumentExceptionのハンドリング
   *
   * @param ex 不正引数例外
   * @param request Webリクエスト
   * @return エラーレスポンス
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<TransformResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {

    String executionId = getExecutionId();

    logger.warn("Invalid argument - executionId: {}, error: {}", executionId, ex.getMessage());

    TransformResponse errorResponse = TransformResponse.error(executionId, ex.getMessage());

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * 一般的な例外のハンドリング
   *
   * @param ex 一般例外
   * @param request Webリクエスト
   * @return エラーレスポンス
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<TransformResponse> handleGeneralException(
      Exception ex, WebRequest request) {

    String executionId = getExecutionId();

    logger.error("Unexpected error - executionId: {}, error: {}", executionId, ex.getMessage(), ex);

    TransformResponse errorResponse =
        TransformResponse.error(executionId, "Internal server error: " + ex.getMessage());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /**
   * MDCから実行IDを取得
   *
   * @return 実行ID、取得できない場合は"UNKNOWN"
   */
  private String getExecutionId() {
    String executionId = MDC.get(ExecutionContext.EXECUTION_ID_KEY);
    return executionId != null ? executionId : "UNKNOWN";
  }

  /** エラーレスポンス用のDTOクラス */
  public static class ErrorResponse {
    private String errorCode;
    private String message;
    private String executionId;
    private LocalDateTime timestamp;
    private Map<String, String> details;

    /**
     * ErrorResponseコンストラクタ
     *
     * @param errorCode エラーコード
     * @param message エラーメッセージ
     * @param executionId 実行コンテキストID
     * @param timestamp タイムスタンプ
     * @param details エラー詳細情報
     */
    public ErrorResponse(
        String errorCode,
        String message,
        String executionId,
        LocalDateTime timestamp,
        Map<String, String> details) {
      this.errorCode = errorCode;
      this.message = message;
      this.executionId = executionId;
      this.timestamp = timestamp;
      this.details = details;
    }

    // Getters and setters
    /**
     * エラーコードを取得
     *
     * @return エラーコード
     */
    public String getErrorCode() {
      return errorCode;
    }

    public void setErrorCode(String errorCode) {
      this.errorCode = errorCode;
    }

    /**
     * エラーメッセージを取得
     *
     * @return エラーメッセージ
     */
    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    /**
     * 実行コンテキストIDを取得
     *
     * @return 実行コンテキストID
     */
    public String getExecutionId() {
      return executionId;
    }

    public void setExecutionId(String executionId) {
      this.executionId = executionId;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
      this.timestamp = timestamp;
    }

    /**
     * エラー詳細情報を取得
     *
     * @return エラー詳細情報のマップ
     */
    public Map<String, String> getDetails() {
      return details;
    }

    public void setDetails(Map<String, String> details) {
      this.details = details;
    }
  }
}
