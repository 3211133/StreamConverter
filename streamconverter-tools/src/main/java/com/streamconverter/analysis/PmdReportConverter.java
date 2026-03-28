package com.streamconverter.analysis;

import com.streamconverter.security.SecureXmlConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * PMD XMLレポートをAI可読形式に変換するユーティリティクラス
 *
 * <p>PMDの生XMLレポートは構造化されているが冗長で、AI解析には適していない。 このクラスは以下の形式に変換する:
 *
 * <ul>
 *   <li>Markdown要約レポート - AI可読性の高いサマリー
 *   <li>CSVレポート - スプレッドシート分析用
 *   <li>JSON構造化レポート - プログラム解析用
 * </ul>
 */
public class PmdReportConverter {
  private static final Logger LOG = LoggerFactory.getLogger(PmdReportConverter.class);

  /** Creates a new converter. */
  public PmdReportConverter() {}

  /**
   * CLI entry point for converting a PMD XML report.
   *
   * @param args arguments where args[0] is the XML file and args[1] is the optional output
   *     directory
   * @throws Exception if conversion fails
   */
  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Usage: PmdReportConverter <pmd-xml-file> [output-dir]");
      }
      System.exit(1);
    }

    String xmlFile = args[0];
    String outputDir = args.length > 1 ? args[1] : "build/reports/pmd/converted";

    PmdReportConverter converter = new PmdReportConverter();
    converter.convertReport(xmlFile, outputDir);
  }

  /**
   * Converts the given PMD XML report into multiple formats.
   *
   * @param xmlFilePath path to the PMD XML file
   * @param outputDir directory where converted reports will be written
   * @throws Exception if processing fails
   */
  public void convertReport(String xmlFilePath, String outputDir) throws Exception {
    Path xmlPath = Paths.get(xmlFilePath);
    Path outputPath = Paths.get(outputDir);
    Files.createDirectories(outputPath);

    // XML解析
    List<PmdViolation> violations = parseXmlReport(xmlPath);

    // 各形式で出力
    generateMarkdownReport(violations, outputPath.resolve("pmd-summary.md"));
    generateCsvReport(violations, outputPath.resolve("pmd-violations.csv"));
    generateJsonReport(violations, outputPath.resolve("pmd-report.json"));

    if (LOG.isInfoEnabled()) {
      LOG.info("✅ PMD報告書変換完了:");
    }
    if (LOG.isInfoEnabled()) {
      LOG.info("   📄 Markdown: {}", outputPath.resolve("pmd-summary.md"));
    }
    if (LOG.isInfoEnabled()) {
      LOG.info("   📊 CSV: {}", outputPath.resolve("pmd-violations.csv"));
    }
    if (LOG.isInfoEnabled()) {
      LOG.info("   🔗 JSON: {}", outputPath.resolve("pmd-report.json"));
    }
  }

  private List<PmdViolation> parseXmlReport(Path xmlPath) throws Exception {
    DocumentBuilder builder = SecureXmlConfiguration.createSecureDocumentBuilder();
    Document doc = builder.parse(xmlPath.toFile());

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

  private String extractRelativePath(String fullPath) {
    // StreamConverterプロジェクト内の相対パスを抽出
    int index = fullPath.indexOf("streamconverter-");
    return index != -1 ? fullPath.substring(index) : fullPath;
  }

  private void generateMarkdownReport(List<PmdViolation> violations, Path outputPath)
      throws IOException {
    StringBuilder md = new StringBuilder();

    md.append("# PMD Code Quality Analysis Report\n\n");
    md.append("**Generated**: ").append(new Date()).append("\n");
    md.append("**Total Violations**: ").append(violations.size()).append("\n\n");

    // 違反数上位のルール
    Map<String, Long> ruleStats =
        violations.stream()
            .collect(Collectors.groupingBy(PmdViolation::rule, Collectors.counting()));

    md.append("## 🎯 Top Code Smell Rules\n\n");
    md.append("| Rank | Rule | Count | Category |\n");
    md.append("|------|------|-------|----------|\n");

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
                      "| | %s | %d | %s |\n", entry.getKey(), entry.getValue(), category));
            });

    // ファイル別統計
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

    // 優先度別統計
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

    Files.writeString(outputPath, md.toString());
  }

  private void generateCsvReport(List<PmdViolation> violations, Path outputPath)
      throws IOException {
    StringBuilder csv = new StringBuilder();
    csv.append("File,Line,Rule,Category,Priority,Description,Class,Method,Variable\n");

    violations.forEach(
        v -> {
          csv.append(
              String.format(
                  "\"%s\",%d,\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                  v.file(),
                  v.line(),
                  v.rule(),
                  v.ruleset(),
                  v.priority(),
                  v.description().replace("\"", "\"\""),
                  v.className(),
                  v.method(),
                  v.variable()));
        });

    Files.writeString(outputPath, csv.toString());
  }

  private void generateJsonReport(List<PmdViolation> violations, Path outputPath)
      throws IOException {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"summary\": {\n");
    json.append("    \"totalViolations\": ").append(violations.size()).append(",\n");
    json.append("    \"generatedAt\": \"").append(new Date()).append("\"\n");
    json.append("  },\n");
    json.append("  \"violations\": [\n");

    for (int i = 0; i < violations.size(); i++) {
      PmdViolation v = violations.get(i);
      json.append("    {\n");
      json.append("      \"file\": \"").append(v.file()).append("\",\n");
      json.append("      \"line\": ").append(v.line()).append(",\n");
      json.append("      \"rule\": \"").append(v.rule()).append("\",\n");
      json.append("      \"category\": \"").append(v.ruleset()).append("\",\n");
      json.append("      \"priority\": ").append(v.priority()).append(",\n");
      json.append("      \"description\": \"")
          .append(v.description().replace("\"", "\\\""))
          .append("\",\n");
      json.append("      \"class\": \"").append(v.className()).append("\",\n");
      json.append("      \"method\": \"").append(v.method()).append("\",\n");
      json.append("      \"variable\": \"").append(v.variable()).append("\"\n");
      json.append("    }").append(i < violations.size() - 1 ? "," : "").append("\n");
    }

    json.append("  ]\n");
    json.append("}\n");

    Files.writeString(outputPath, json.toString());
  }

  /**
   * PMD違反情報を表すレコードクラス
   *
   * @param file ファイルパス
   * @param line 行番号
   * @param rule 違反ルール
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
