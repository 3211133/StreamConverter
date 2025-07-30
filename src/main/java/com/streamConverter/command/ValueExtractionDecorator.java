package com.streamConverter.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamConverter.StreamProcessingException;
import com.streamConverter.context.ExecutionContext;
import com.streamConverter.security.SecureXmlProcessor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 指定されたパスから値を抽出してMDCに格納するデコレータ
 *
 * <p>JSON、XML、CSVの各形式から指定されたパスで値を抽出し、 MDCコンテキストに格納してからメインコマンドを実行します。
 *
 * <p>対応フォーマット:
 *
 * <ul>
 *   <li>JSON: JSONPath形式 (例: "$.user.id", "$.items[0].name")
 *   <li>XML: XPath形式 (例: "//user/@id", "/root/item[1]/name/text()")
 *   <li>CSV: カラム名形式 (例: "user_id", "column_name")
 * </ul>
 *
 * <p>使用例:
 *
 * <pre>
 * IStreamCommand decoratedCommand = new ValueExtractionDecorator(
 *     baseCommand,
 *     "JSON",        // データ形式
 *     "$.user.id",   // 抽出パス
 *     "userId"       // MDCキー名
 * );
 * decoratedCommand.execute(inputStream, outputStream);
 * </pre>
 */
public class ValueExtractionDecorator implements IContextAwareStreamCommand {
  private static final Logger logger = LoggerFactory.getLogger(ValueExtractionDecorator.class);

  private final IStreamCommand delegateCommand;
  private final String dataFormat;
  private final String extractionPath;
  private final String mdcKey;
  private final ObjectMapper objectMapper;
  private final SecureXmlProcessor secureXmlProcessor;

  /**
   * コンストラクタ
   *
   * @param delegateCommand 実行するメインコマンド
   * @param dataFormat データ形式 ("JSON", "XML", "CSV")
   * @param extractionPath 抽出パス (JSONPath, XPath, またはカラム名)
   * @param mdcKey MDCに格納する際のキー名
   * @throws IllegalArgumentException 引数がnullまたは無効な場合
   */
  public ValueExtractionDecorator(
      IStreamCommand delegateCommand, String dataFormat, String extractionPath, String mdcKey) {
    this.delegateCommand =
        Objects.requireNonNull(delegateCommand, "Delegate command cannot be null");
    this.dataFormat = validateDataFormat(dataFormat);
    this.extractionPath = validateExtractionPath(extractionPath);
    this.mdcKey = validateMdcKey(mdcKey);
    this.objectMapper = new ObjectMapper();

    // セキュアなXML処理コンポーネントの初期化
    this.secureXmlProcessor = new SecureXmlProcessor();
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    ExecutionContext context = ExecutionContext.create();
    execute(inputStream, outputStream, context);
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream, ExecutionContext context)
      throws IOException {
    Objects.requireNonNull(inputStream, "InputStream cannot be null");
    Objects.requireNonNull(outputStream, "OutputStream cannot be null");
    Objects.requireNonNull(context, "ExecutionContext cannot be null");

    logger.info(
        "Starting value extraction with context - format: {}, path: {}, mdcKey: {}, contextId: {}",
        dataFormat,
        extractionPath,
        mdcKey,
        context.getExecutionId());

    try {
      // 入力ストリームを読み込み（複数回読む必要があるため）
      byte[] inputBytes = inputStream.readAllBytes();
      if (inputBytes.length == 0) {
        logger.warn("Input stream is empty, skipping value extraction");
        delegateCommand.execute(new ByteArrayInputStream(inputBytes), outputStream);
        return;
      }

      // 値を抽出してMDCに設定
      String extractedValue = extractValue(inputBytes);
      if (extractedValue != null && !extractedValue.trim().isEmpty()) {
        // ExecutionContextのユーザーコンテキストに保存
        context.setUserContext(mdcKey, extractedValue);
        // MDCにも設定
        MDC.put(mdcKey, extractedValue);
        logger.debug(
            "Extracted value '{}' and stored in context/MDC with key '{}'", extractedValue, mdcKey);
      } else {
        logger.warn(
            "Failed to extract value from path '{}' in format '{}'", extractionPath, dataFormat);
      }

      // コンテキストをMDCに適用
      context.applyToMDC();

      // メインコマンドを実行
      try (InputStream delegateInputStream = new ByteArrayInputStream(inputBytes)) {
        if (delegateCommand instanceof IContextAwareStreamCommand) {
          ((IContextAwareStreamCommand) delegateCommand)
              .execute(delegateInputStream, outputStream, context);
        } else {
          delegateCommand.execute(delegateInputStream, outputStream);
        }
      }

    } catch (Exception e) {
      logger.error("Value extraction failed: {}", e.getMessage(), e);
      throw new IOException("Value extraction failed", e);
    }
  }

  /**
   * データ形式に応じて値を抽出
   *
   * @param inputBytes 入力データのバイト配列
   * @return 抽出された値、見つからない場合はnull
   * @throws StreamProcessingException 抽出処理でエラーが発生した場合
   */
  private String extractValue(byte[] inputBytes) throws StreamProcessingException {
    String inputString = new String(inputBytes, StandardCharsets.UTF_8);

    try {
      switch (dataFormat.toUpperCase()) {
        case "JSON":
          return extractFromJson(inputString);
        case "XML":
          return extractFromXml(inputString);
        case "CSV":
          return extractFromCsv(inputString);
        default:
          throw new StreamProcessingException("Unsupported data format: " + dataFormat);
      }
    } catch (Exception e) {
      throw new StreamProcessingException(
          String.format(
              "Value extraction failed - format: %s, path: %s", dataFormat, extractionPath),
          e);
    }
  }

  /** JSONから値を抽出 */
  private String extractFromJson(String jsonString) throws Exception {
    JsonNode rootNode = objectMapper.readTree(jsonString);

    // シンプルなJSONPath実装（基本的なパス表記のみサポート）
    String[] pathParts = extractionPath.replace("$.", "").split("\\.");
    JsonNode currentNode = rootNode;

    for (String part : pathParts) {
      if (currentNode == null) {
        return null;
      }

      // 配列インデックスの処理 (例: items[0])
      if (part.contains("[") && part.contains("]")) {
        String fieldName = part.substring(0, part.indexOf('['));
        String indexStr = part.substring(part.indexOf('[') + 1, part.indexOf(']'));
        int index = Integer.parseInt(indexStr);

        currentNode = currentNode.get(fieldName);
        if (currentNode != null && currentNode.isArray() && index < currentNode.size()) {
          currentNode = currentNode.get(index);
        } else {
          return null;
        }
      } else {
        currentNode = currentNode.get(part);
      }
    }

    return currentNode != null ? currentNode.asText() : null;
  }

  /** XMLから値を抽出（DocumentBuilderを使用しない安全な実装） */
  private String extractFromXml(String xmlString) throws Exception {
    logger.debug("Extracting XML value using secure StAX processor");

    // DocumentBuilderを使用せず、StAXベースのセキュアプロセッサを使用
    return secureXmlProcessor.extractValueSafely(xmlString, extractionPath);
  }

  // 危険なDocumentBuilder使用メソッドを削除し、SecureXmlProcessorに置き換え

  // XPath処理はSecureXmlProcessorに統合されたため削除

  // XPath処理メソッドはSecureXmlProcessorに統合されたため削除

  /** CSVから値を抽出 */
  private String extractFromCsv(String csvString) throws Exception {
    String[] lines = csvString.split("\n");
    if (lines.length < 2) {
      return null; // ヘッダーとデータが必要
    }

    // ヘッダー行から列インデックスを検索
    String[] headers = lines[0].split(",");
    int columnIndex = -1;
    for (int i = 0; i < headers.length; i++) {
      if (headers[i].trim().equals(extractionPath)) {
        columnIndex = i;
        break;
      }
    }

    if (columnIndex == -1) {
      return null; // 指定された列名が見つからない
    }

    // データ行から値を取得（最初のデータ行を使用）
    String[] dataFields = lines[1].split(",");
    if (columnIndex < dataFields.length) {
      return dataFields[columnIndex].trim();
    }

    return null;
  }

  /** データ形式の検証 */
  private String validateDataFormat(String format) {
    if (format == null || format.trim().isEmpty()) {
      throw new IllegalArgumentException("Data format cannot be null or empty");
    }
    String upperFormat = format.trim().toUpperCase();
    if (!upperFormat.equals("JSON") && !upperFormat.equals("XML") && !upperFormat.equals("CSV")) {
      throw new IllegalArgumentException(
          "Unsupported data format: " + format + ". Supported formats: JSON, XML, CSV");
    }
    return upperFormat;
  }

  /** 抽出パスの検証 */
  private String validateExtractionPath(String path) {
    if (path == null || path.trim().isEmpty()) {
      throw new IllegalArgumentException("Extraction path cannot be null or empty");
    }
    return path.trim();
  }

  /** MDCキーの検証 */
  private String validateMdcKey(String key) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("MDC key cannot be null or empty");
    }
    return key.trim();
  }

  // Getters
  /**
   * データ形式を取得
   *
   * @return データ形式
   */
  public String getDataFormat() {
    return dataFormat;
  }

  /**
   * 抽出パスを取得
   *
   * @return 抽出パス
   */
  public String getExtractionPath() {
    return extractionPath;
  }

  /**
   * MDCキーを取得
   *
   * @return MDCキー
   */
  public String getMdcKey() {
    return mdcKey;
  }
}
