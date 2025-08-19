package com.streamConverter.command.impl.analysis;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.command.impl.analysis.PmdXmlToMarkdownCommand.PmdViolation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * PMD XML レポートを CSV 形式に変換するコマンド
 *
 * <p>StreamConverter アーキテクチャ準拠の実装例として、PMD XMLレポートを スプレッドシート分析に適したCSV形式に変換します。Jackson CSV
 * mapperを使用した 型安全で効率的なCSV生成を実装しています。
 *
 * <p><strong>出力CSV形式:</strong>
 *
 * <pre>
 * File,Line,Rule,Category,Priority,Description,Class,Method,Variable
 * </pre>
 *
 * <p><strong>特徴:</strong>
 *
 * <ul>
 *   <li>Jackson CsvMapper による型安全なCSV生成
 *   <li>適切なCSVエスケープ処理
 *   <li>StreamConverter設計原則準拠
 *   <li>ストリーミング処理対応
 * </ul>
 *
 * <p><strong>使用例:</strong>
 *
 * <pre>
 * // パイプライン使用例
 * StreamConverter converter = new StreamConverter(
 *     new PmdXmlToCsvCommand()
 * );
 * converter.run(pmdXmlInputStream, csvOutputStream);
 * </pre>
 */
public class PmdXmlToCsvCommand extends AbstractStreamCommand {

  private final CsvMapper csvMapper;

  public PmdXmlToCsvCommand() {
    this.csvMapper = new CsvMapper();
  }

  /**
   * PMD XML InputStream を CSV OutputStream に変換
   *
   * @param input PMD XML レポートの入力ストリーム
   * @param output CSV レポートの出力ストリーム
   * @throws IOException XML解析エラーまたはI/O例外の場合
   */
  @Override
  protected void _execute(InputStream input, OutputStream output) throws IOException {
    try {
      // StreamConverter原則に従った変換処理
      List<PmdViolation> violations = parseXmlStream(input);
      generateCsvOutput(violations, output);

    } catch (Exception e) {
      throw new IOException("Failed to convert PMD XML to CSV: " + e.getMessage(), e);
    }
  }

  @Override
  protected String getCommandDetails() {
    return "PmdXmlToCsvCommand: Converts PMD XML reports to CSV format using Jackson CsvMapper";
  }

  /**
   * PMD XML ストリームからバイオレーション情報を解析
   *
   * @param input PMD XML入力ストリーム
   * @return 解析されたバイオレーションのリスト
   * @throws Exception XML解析エラーの場合
   */
  private List<PmdViolation> parseXmlStream(InputStream input) throws Exception {
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
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

  /**
   * Jackson CsvMapper を使用してCSV出力を生成
   *
   * @param violations バイオレーション情報
   * @param output 出力ストリーム
   * @throws IOException CSV生成エラーの場合
   */
  private void generateCsvOutput(List<PmdViolation> violations, OutputStream output)
      throws IOException {
    // CSV スキーマ定義（ヘッダー付き）
    CsvSchema schema =
        csvMapper
            .schemaFor(PmdViolation.class)
            .withHeader()
            .withColumnSeparator(',')
            .withQuoteChar('"')
            .withEscapeChar('\\')
            .withLineSeparator("\n");

    // Jackson CsvMapper による型安全なCSV生成 - Listタイプ用のwriter使用
    csvMapper.writer(schema).writeValue(output, violations);
  }
}
