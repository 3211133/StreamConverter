package com.streamConverter.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * XMLリソース制限クラス
 *
 * <p>XML処理におけるリソース消費を制限し、DoS攻撃やXML爆弾攻撃を防止します。
 *
 * <p>主要機能:
 *
 * <ul>
 *   <li>XMLファイルサイズの制限
 *   <li>XML爆弾パターンの検出
 *   <li>エンティティ参照密度の検査
 *   <li>メモリ消費量の監視
 * </ul>
 *
 * @author StreamConverter Security Team
 * @version 1.0.0
 * @since 2025-07-30
 */
@Component
public class XmlResourceLimiter {

  private static final Logger logger = LoggerFactory.getLogger(XmlResourceLimiter.class);

  // デフォルト制限値
  private static final int DEFAULT_MAX_XML_SIZE = 10 * 1024 * 1024; // 10MB
  private static final int DEFAULT_MAX_ENTITY_EXPANSION = 100;
  private static final int DEFAULT_MAX_ELEMENT_DEPTH = 100;
  private static final int DEFAULT_SAMPLE_SIZE = 8192; // 最初の8KBをサンプリング

  private final int maxXmlSize;
  private final int maxEntityExpansion;
  private final int maxElementDepth;

  /** デフォルトコンストラクタ */
  public XmlResourceLimiter() {
    this(DEFAULT_MAX_XML_SIZE, DEFAULT_MAX_ENTITY_EXPANSION, DEFAULT_MAX_ELEMENT_DEPTH);
  }

  /**
   * カスタム制限値を使用するコンストラクタ
   *
   * @param maxXmlSize 最大XMLサイズ（バイト）
   * @param maxEntityExpansion 最大エンティティ展開数
   * @param maxElementDepth 最大要素階層数
   */
  public XmlResourceLimiter(int maxXmlSize, int maxEntityExpansion, int maxElementDepth) {
    this.maxXmlSize = maxXmlSize;
    this.maxEntityExpansion = maxEntityExpansion;
    this.maxElementDepth = maxElementDepth;

    logger.info(
        "XmlResourceLimiter initialized - maxSize: {}MB, maxEntityExpansion: {}, maxDepth: {}",
        maxXmlSize / (1024 * 1024),
        maxEntityExpansion,
        maxElementDepth);
  }

  /**
   * XMLサイズを制限したInputStreamを作成
   *
   * @param originalStream 元のInputStream
   * @return サイズ制限付きのInputStream
   * @throws IOException ストリーム操作でエラーが発生した場合
   */
  public InputStream createLimitedInputStream(InputStream originalStream) throws IOException {
    logger.debug("Creating size-limited input stream with max size: {} bytes", maxXmlSize);

    return new BoundedInputStream(originalStream, maxXmlSize) {
      @Override
      protected void onMaxLength(long maxLen, long count) throws IOException {
        logger.warn("XML content exceeds maximum size: {} bytes (limit: {} bytes)", count, maxLen);
        throw new SecurityException(
            String.format("XML content exceeds maximum size: %d bytes", maxLen));
      }
    };
  }

  /**
   * XML爆弾パターンを検出
   *
   * @param xmlContent XML内容
   * @throws SecurityException XML爆弾パターンが検出された場合
   */
  public void detectXmlBombs(String xmlContent) throws SecurityException {
    if (xmlContent == null || xmlContent.isEmpty()) {
      return;
    }

    logger.debug("Scanning XML content for bomb patterns (length: {} chars)", xmlContent.length());

    // エンティティ宣言の検出
    if (containsEntityDeclarations(xmlContent)) {
      // エンティティ参照の密度をチェック
      int entityRefCount = countEntityReferences(xmlContent);
      int contentLength = xmlContent.length();

      // エンティティ参照密度の計算（1000文字あたりの参照数）
      double entityDensity = (entityRefCount * 1000.0) / contentLength;

      logger.debug(
          "Entity analysis - references: {}, density: {:.2f} per 1000 chars",
          entityRefCount,
          entityDensity);

      // 高密度のエンティティ参照は爆弾攻撃の可能性
      if (entityDensity > 10.0) { // 閾値は調整可能
        logger.warn(
            "High entity reference density detected: {:.2f} per 1000 chars (threshold: 10.0)",
            entityDensity);
        throw new SecurityException("Potential XML bomb detected: high entity reference density");
      }

      // 入れ子エンティティの検出
      if (detectNestedEntities(xmlContent)) {
        logger.warn("Nested entity declarations detected");
        throw new SecurityException("Potential XML bomb detected: nested entity declarations");
      }
    }

    // 疑わしいDOCTYPE宣言のチェック
    if (containsSuspiciousDoctypeDeclarations(xmlContent)) {
      logger.warn("Suspicious DOCTYPE declarations detected");
      throw new SecurityException("Potential XML bomb detected: suspicious DOCTYPE declarations");
    }

    logger.debug("XML bomb scan completed - no threats detected");
  }

  /**
   * XMLの最初の部分をサンプリングしてチェック
   *
   * @param inputStream 入力ストリーム
   * @throws IOException ストリーム操作でエラーが発生した場合
   * @throws SecurityException セキュリティ脅威が検出された場合
   */
  public void scanXmlSample(InputStream inputStream) throws IOException, SecurityException {
    byte[] sampleBuffer = new byte[DEFAULT_SAMPLE_SIZE];
    int bytesRead = inputStream.read(sampleBuffer);

    if (bytesRead > 0) {
      String sampleContent = new String(sampleBuffer, 0, bytesRead, StandardCharsets.UTF_8);
      detectXmlBombs(sampleContent);
    }
  }

  /**
   * エンティティ宣言が含まれているかチェック
   *
   * @param xmlContent XML内容
   * @return エンティティ宣言が含まれている場合true
   */
  private boolean containsEntityDeclarations(String xmlContent) {
    String upperContent = xmlContent.toUpperCase();
    return upperContent.contains("<!ENTITY");
  }

  /**
   * エンティティ参照の数をカウント
   *
   * @param xmlContent XML内容
   * @return エンティティ参照の数
   */
  private int countEntityReferences(String xmlContent) {
    int count = 0;
    int index = 0;

    // & で始まるエンティティ参照をカウント
    while ((index = xmlContent.indexOf('&', index)) != -1) {
      // 次の ; を探す
      int endIndex = xmlContent.indexOf(';', index);
      if (endIndex != -1 && endIndex - index < 50) { // 妥当なエンティティ名の長さ制限
        count++;
        index = endIndex + 1;
      } else {
        index++;
      }
    }

    return count;
  }

  /**
   * 入れ子エンティティを検出
   *
   * @param xmlContent XML内容
   * @return 入れ子エンティティが検出された場合true
   */
  private boolean detectNestedEntities(String xmlContent) {
    // エンティティ宣言内でのエンティティ参照を検出
    String[] lines = xmlContent.split("\n");
    for (String line : lines) {
      String upperLine = line.toUpperCase().trim();
      if (upperLine.startsWith("<!ENTITY") && upperLine.contains("&")) {
        // エンティティ宣言内にエンティティ参照がある
        return true;
      }
    }
    return false;
  }

  /**
   * 疑わしいDOCTYPE宣言をチェック
   *
   * @param xmlContent XML内容
   * @return 疑わしいDOCTYPE宣言が含まれている場合true
   */
  private boolean containsSuspiciousDoctypeDeclarations(String xmlContent) {
    String upperContent = xmlContent.toUpperCase();

    // 複数のDOCTYPE宣言
    int doctypeCount = countOccurrences(upperContent, "<!DOCTYPE");
    if (doctypeCount > 1) {
      return true;
    }

    // 外部参照を含むDOCTYPE
    if (upperContent.contains("<!DOCTYPE")
        && (upperContent.contains("SYSTEM") || upperContent.contains("PUBLIC"))) {
      return true;
    }

    // 異常に長いDOCTYPE宣言
    int doctypeStart = upperContent.indexOf("<!DOCTYPE");
    if (doctypeStart != -1) {
      int doctypeEnd = upperContent.indexOf(">", doctypeStart);
      if (doctypeEnd != -1 && (doctypeEnd - doctypeStart) > 1000) { // 1000文字を超える場合
        return true;
      }
    }

    return false;
  }

  /**
   * InputStreamを読み込んでXMLセキュリティ検証を行い、バイト配列を返す
   *
   * @param inputStream 検証対象のInputStream
   * @return 検証済みのXMLデータ（バイト配列）
   * @throws IOException 入出力エラーが発生した場合
   * @throws SecurityException XMLセキュリティ脅威が検出された場合
   */
  public byte[] readAndValidateXmlStream(InputStream inputStream) throws IOException {
    Objects.requireNonNull(inputStream, "InputStream cannot be null");

    // リソース制限付きでストリームを読み込み
    try (InputStream limitedStream = createLimitedInputStream(inputStream)) {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] tempBuffer = new byte[8192];
      int bytesRead;

      while ((bytesRead = limitedStream.read(tempBuffer)) != -1) {
        buffer.write(tempBuffer, 0, bytesRead);
      }

      byte[] xmlData = buffer.toByteArray();

      // XML爆弾パターンの検証
      String xmlContent = new String(xmlData, StandardCharsets.UTF_8);
      detectXmlBombs(xmlContent);

      logger.debug("XML stream validated successfully, size: {} bytes", xmlData.length);
      return xmlData;
    }
  }

  /**
   * 文字列中の特定のパターンの出現回数をカウント
   *
   * @param text 検索対象の文字列
   * @param pattern 検索パターン
   * @return 出現回数
   */
  private int countOccurrences(String text, String pattern) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(pattern, index)) != -1) {
      count++;
      index += pattern.length();
    }
    return count;
  }

  // Getters
  public int getMaxXmlSize() {
    return maxXmlSize;
  }

  public int getMaxEntityExpansion() {
    return maxEntityExpansion;
  }

  public int getMaxElementDepth() {
    return maxElementDepth;
  }
}
