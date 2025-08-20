package com.streamConverter.command.impl.analysis;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * PMD XML レポートを AI 可読性の高い Markdown 形式に変換するコマンド
 *
 * <p>StreamConverter アーキテクチャに基づく実装例として、InputStreamからOutputStreamへの
 * 純粋な変換処理を提供します。PMDの冗長なXMLレポートをMarkdown要約形式に変換し、 AI分析や人間による可読性を向上させます。
 *
 * <p><strong>変換仕様:</strong>
 *
 * <ul>
 *   <li>ルール別違反統計（上位20位）
 *   <li>ファイル別問題統計（上位15ファイル）
 *   <li>優先度分布と影響度分析
 *   <li>標準化されたISO-8601タイムスタンプ
 * </ul>
 *
 * <p><strong>使用例:</strong>
 *
 * <pre>
 * // StreamConverter パイプラインでの使用
 * StreamConverter converter = new StreamConverter(
 *     new PmdXmlToMarkdownCommand()
 * );
 * converter.run(pmdXmlInputStream, markdownOutputStream);
 * </pre>
 */
public class PmdXmlToMarkdownCommand extends AbstractStreamCommand {
  /** Creates a new command instance. */
  public PmdXmlToMarkdownCommand() {}

  /**
   * PMD XML InputStream を Markdown OutputStream に変換
   *
   * @param input PMD XML レポートの入力ストリーム
   * @param output Markdown レポートの出力ストリーム
   * @throws IOException XML解析エラーまたはI/O例外の場合
   */
  @Override
  protected void _execute(InputStream input, OutputStream output) throws IOException {
    try {
      // StreamConverter原則: InputStreamから読み取り、OutputStreamに書き込み
      List<PmdViolation> violations = parseXmlStream(input);
      String markdownReport = generateMarkdownReport(violations);
      output.write(markdownReport.getBytes("UTF-8"));

    } catch (Exception e) {
      throw new IOException("Failed to convert PMD XML to Markdown: " + e.getMessage(), e);
    }
  }

  @Override
  protected String getCommandDetails() {
    return "PmdXmlToMarkdownCommand: Converts PMD XML reports to AI-readable Markdown format";
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
   * バイオレーション情報から AI 可読 Markdown レポートを生成
   *
   * @param violations 解析されたバイオレーション情報
   * @return Markdown形式のレポート文字列
   */
  private String generateMarkdownReport(List<PmdViolation> violations) {
    StringBuilder md = new StringBuilder();

    // ヘッダー情報（ISO-8601標準形式）
    md.append("# PMD Code Quality Analysis Report\n\n");
    md.append("**Generated**: ").append(Instant.now().toString()).append("\n");
    md.append("**Total Violations**: ").append(violations.size()).append("\n\n");

    // 違反数上位のルール分析
    generateTopRulesSection(md, violations);

    // ファイル別問題統計
    generateFileStatisticsSection(md, violations);

    // 優先度分布分析
    generatePriorityDistributionSection(md, violations);

    return md.toString();
  }

  /** 上位ルール違反セクションを生成 */
  private void generateTopRulesSection(StringBuilder md, List<PmdViolation> violations) {
    Map<String, Long> ruleStats =
        violations.stream()
            .collect(Collectors.groupingBy(PmdViolation::rule, Collectors.counting()));

    md.append("## 🎯 Top Code Smell Rules\n\n");
    md.append("| Rank | Rule | Count | Category |\n");
    md.append("|------|------|-------|----------|\n");

    int rank = 1;
    ruleStats.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(20)
        .forEach(
            entry -> {
              String category =
                  violations.stream()
                      .filter(v -> v.rule().equals(entry.getKey()))
                      .findFirst()
                      .map(PmdViolation::ruleset)
                      .orElse("Unknown");
              md.append(
                  String.format(
                      "| %d | %s | %d | %s |\n", rank, entry.getKey(), entry.getValue(), category));
            });
  }

  /** ファイル統計セクションを生成 */
  private void generateFileStatisticsSection(StringBuilder md, List<PmdViolation> violations) {
    Map<String, Long> fileStats =
        violations.stream()
            .collect(Collectors.groupingBy(PmdViolation::file, Collectors.counting()));

    md.append("\n## 📁 Files with Most Issues\n\n");
    md.append("| File | Violations |\n");
    md.append("|------|------------|\n");

    fileStats.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(15)
        .forEach(
            entry -> {
              md.append(
                  String.format(
                      "| %s | %d |\n",
                      entry.getKey().replaceAll(".*/(\\w+\\.java)", "$1"), entry.getValue()));
            });
  }

  /** 優先度分布セクションを生成 */
  private void generatePriorityDistributionSection(
      StringBuilder md, List<PmdViolation> violations) {
    Map<Integer, Long> priorityStats =
        violations.stream()
            .collect(Collectors.groupingBy(PmdViolation::priority, Collectors.counting()));

    md.append("\n## ⚡ Priority Distribution\n\n");
    md.append("| Priority | Count | Description |\n");
    md.append("|----------|-------|-------------|\n");

    priorityStats.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              String desc =
                  switch (entry.getKey()) {
                    case 1 -> "🔴 High - Critical issues";
                    case 2 -> "🟡 Medium - Important issues";
                    case 3 -> "🟢 Low - Minor issues";
                    case 4 -> "ℹ️ Info - Informational";
                    default -> "❓ Unknown";
                  };
              md.append(
                  String.format("| %d | %d | %s |\n", entry.getKey(), entry.getValue(), desc));
            });
  }

  /**
   * PMD違反情報を表すレコードクラス
   *
   * @param file ファイルパス
   * @param line 行番号
   * @param rule ルール名
   * @param ruleset ルールセット
   * @param priority 優先度
   * @param description 説明
   * @param className クラス名
   * @param method メソッド名
   * @param variable 変数名
   */
  public record PmdViolation(
      String file,
      int line,
      String rule,
      String ruleset,
      int priority,
      String description,
      String className,
      String method,
      String variable) {}
}
