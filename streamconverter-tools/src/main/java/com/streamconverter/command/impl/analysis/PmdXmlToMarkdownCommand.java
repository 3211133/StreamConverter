package com.streamconverter.command.impl.analysis;

import com.streamconverter.command.AbstractStreamCommand;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link PmdViolation} オブジェクトのストリームを AI 可読性の高い Markdown 形式に変換するコマンド。
 *
 * <p>入力: {@link PmdXmlToViolationsCommand} が出力する {@link java.io.ObjectOutputStream} ストリーム
 *
 * <p>出力: Markdown レポート（ルール別統計・ファイル別統計・優先度分布）
 */
public class PmdXmlToMarkdownCommand extends AbstractStreamCommand {

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    List<PmdViolation> violations = readAll(input);
    String report = generateMarkdownReport(violations);
    output.write(report.getBytes("UTF-8"));
  }

  private List<PmdViolation> readAll(InputStream input) throws IOException {
    List<PmdViolation> result = new ArrayList<>();
    try (ObjectInputStream ois = new ObjectInputStream(input)) {
      while (true) {
        try {
          result.add((PmdViolation) ois.readObject());
        } catch (EOFException e) {
          break;
        } catch (ClassNotFoundException | ClassCastException e) {
          throw new IOException("Unexpected object type in stream: expected PmdViolation", e);
        }
      }
    }
    return result;
  }

  private String generateMarkdownReport(List<PmdViolation> violations) {
    StringBuilder md = new StringBuilder();
    md.append("# PMD Code Quality Analysis Report\n\n");
    md.append("**Generated**: ").append(Instant.now()).append("\n");
    md.append("**Total Violations**: ").append(violations.size()).append("\n\n");

    generateTopRulesSection(md, violations);
    generateFileStatisticsSection(md, violations);
    generatePriorityDistributionSection(md, violations);

    return md.toString();
  }

  private void generateTopRulesSection(StringBuilder md, List<PmdViolation> violations) {
    Map<String, Long> ruleStats =
        violations.stream()
            .collect(Collectors.groupingBy(PmdViolation::rule, Collectors.counting()));

    md.append("## \uD83C\uDFAF Top Code Smell Rules\n\n");
    md.append("| Rank | Rule | Count | Category |\n");
    md.append("|------|------|-------|----------|\n");

    int[] rank = {1};
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
                      "| %d | %s | %d | %s |\n",
                      rank[0]++, entry.getKey(), entry.getValue(), category));
            });
  }

  private void generateFileStatisticsSection(StringBuilder md, List<PmdViolation> violations) {
    Map<String, Long> fileStats =
        violations.stream()
            .collect(Collectors.groupingBy(PmdViolation::file, Collectors.counting()));

    md.append("\n## \uD83D\uDCC1 Files with Most Issues\n\n");
    md.append("| File | Violations |\n");
    md.append("|------|------------|\n");

    fileStats.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(15)
        .forEach(
            entry ->
                md.append(
                    String.format(
                        "| %s | %d |\n",
                        entry.getKey().replaceAll(".*/(\\w+\\.java)", "$1"), entry.getValue())));
  }

  private void generatePriorityDistributionSection(
      StringBuilder md, List<PmdViolation> violations) {
    Map<Integer, Long> priorityStats =
        violations.stream()
            .collect(Collectors.groupingBy(PmdViolation::priority, Collectors.counting()));

    md.append("\n## \u26A1 Priority Distribution\n\n");
    md.append("| Priority | Count | Description |\n");
    md.append("|----------|-------|-------------|\n");

    priorityStats.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              String desc =
                  switch (entry.getKey()) {
                    case 1 -> "\uD83D\uDD34 High - Critical issues";
                    case 2 -> "\uD83D\uDFE1 Medium - Important issues";
                    case 3 -> "\uD83D\uDFE2 Low - Minor issues";
                    case 4 -> "\u2139\uFE0F Info - Informational";
                    default -> "\u2753 Unknown";
                  };
              md.append(
                  String.format("| %d | %d | %s |\n", entry.getKey(), entry.getValue(), desc));
            });
  }
}
