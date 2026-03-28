package com.streamconverter.command.impl.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.command.impl.analysis.PmdXmlToMarkdownCommand.PmdViolation;
import com.streamconverter.security.SecureXmlConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * PMD XML レポートを JSON 形式に変換するコマンド
 *
 * <p>StreamConverter アーキテクチャに準拠したJSON変換実装例です。 Jackson ObjectMapperを使用して型安全で構造化されたJSON出力を生成し、
 * プログラムによる解析やAPI連携に適したデータ形式を提供します。
 *
 * <p><strong>出力JSON構造:</strong>
 *
 * <pre>
 * {
 *   "summary": {
 *     "totalViolations": 2083,
 *     "generatedAt": "2025-08-19T16:45:00.123Z"
 *   },
 *   "violations": [
 *     {
 *       "file": "streamconverter-core/src/.../Example.java",
 *       "line": 42,
 *       "rule": "MethodArgumentCouldBeFinal",
 *       "category": "Code Style",
 *       "priority": 3,
 *       "description": "Parameter 'input' is not assigned...",
 *       "class": "Example",
 *       "method": "process",
 *       "variable": "input"
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p><strong>特徴:</strong>
 *
 * <ul>
 *   <li>Jackson ObjectMapper による型安全なJSON生成
 *   <li>ISO-8601 標準タイムスタンプ
 *   <li>構造化されたサマリー情報
 *   <li>プリティプリント対応
 * </ul>
 *
 * <p><strong>使用例:</strong>
 *
 * <pre>
 * // StreamConverter パイプラインでの使用
 * StreamConverter converter = new StreamConverter(
 *     new PmdXmlToJsonCommand()
 * );
 * converter.run(pmdXmlInputStream, jsonOutputStream);
 * </pre>
 */
public class PmdXmlToJsonCommand extends AbstractStreamCommand {

  private final ObjectMapper objectMapper;

  /**
   * Creates a new command instance.
   *
   * <p>Initializes the {@link ObjectMapper} with the following configuration:
   *
   * <ul>
   *   <li>Enables pretty-printing of JSON output ({@link SerializationFeature#INDENT_OUTPUT}).
   *   <li>Disables writing dates as timestamps, using ISO-8601 format instead ({@link
   *       SerializationFeature#WRITE_DATES_AS_TIMESTAMPS}).
   * </ul>
   *
   * This configuration affects the formatting and date representation in the generated JSON
   * reports.
   */
  public PmdXmlToJsonCommand() {
    this.objectMapper =
        new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT) // Pretty print
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601
  }

  /**
   * PMD XML InputStream を JSON OutputStream に変換
   *
   * @param input PMD XML レポートの入力ストリーム
   * @param output JSON レポートの出力ストリーム
   * @throws IOException XML解析エラーまたはI/O例外の場合
   */
  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    try {
      // StreamConverter原則: ストリーム間変換
      List<PmdViolation> violations = parseXmlStream(input);
      PmdJsonReport report = createJsonReport(violations);

      // Jackson による型安全なJSON生成
      objectMapper.writeValue(output, report);

    } catch (Exception e) {
      throw new IOException("Failed to convert PMD XML to JSON: " + e.getMessage(), e);
    }
  }

  /** PMD XML ストリームからバイオレーション情報を解析 */
  private List<PmdViolation> parseXmlStream(InputStream input) throws Exception {
    DocumentBuilder builder = SecureXmlConfiguration.createSecureDocumentBuilder();
    Document doc = builder.parse(input);

    NodeList fileNodes = doc.getElementsByTagName("file");
    List<PmdViolation> violations = new ArrayList<>();

    for (int i = 0; i < fileNodes.getLength(); i++) {
      Element fileElement = (Element) fileNodes.item(i);
      String fileName = fileElement.getAttribute("name");

      NodeList violationNodes = fileElement.getElementsByTagName("violation");
      for (int j = 0; j < violationNodes.getLength(); j++) {
        Element violationElement = (Element) violationNodes.item(j);

        PmdViolation violation =
            new PmdViolation(
                extractRelativePath(fileName),
                Integer.parseInt(violationElement.getAttribute("beginline")),
                violationElement.getAttribute("rule"),
                violationElement.getAttribute("ruleset"),
                Integer.parseInt(violationElement.getAttribute("priority")),
                violationElement.getTextContent().trim(),
                violationElement.getAttribute("class"),
                violationElement.getAttribute("method"),
                violationElement.getAttribute("variable"));
        violations.add(violation);
      }
    }

    return violations;
  }

  /** StreamConverterプロジェクト内の相対パスを抽出 */
  private String extractRelativePath(String fullPath) {
    int index = fullPath.indexOf("streamconverter-");
    return index != -1 ? fullPath.substring(index) : fullPath;
  }

  /** 構造化されたJSON レポートオブジェクトを作成 */
  private PmdJsonReport createJsonReport(List<PmdViolation> violations) {
    PmdJsonSummary summary =
        new PmdJsonSummary(
            violations.size(), Instant.now().toString() // ISO-8601 標準形式
            );

    return new PmdJsonReport(summary, violations);
  }

  /**
   * JSON レポートのルートオブジェクト
   *
   * @param summary サマリー情報
   * @param violations バイオレーション一覧
   */
  public static record PmdJsonReport(PmdJsonSummary summary, List<PmdViolation> violations) {
    public PmdJsonReport {
      violations = List.copyOf(violations);
    }
  }

  /**
   * JSON レポートのサマリー情報
   *
   * @param totalViolations 総違反数
   * @param generatedAt 生成日時
   */
  public static record PmdJsonSummary(int totalViolations, String generatedAt) {}
}
