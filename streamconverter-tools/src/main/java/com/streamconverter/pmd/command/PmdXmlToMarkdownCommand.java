package com.streamconverter.pmd.command;

import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.pmd.PmdViolation;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@link PmdViolation} オブジェクトのストリームを AI 可読性の高い Markdown 形式に変換するコマンド。
 *
 * <p>入力: {@link PmdXmlToViolationsCommand} が出力する {@link java.io.ObjectOutputStream} ストリーム
 *
 * <p>出力: Markdown レポート（ルール別統計・ファイル別統計・優先度分布）
 *
 * <p>違反オブジェクトは1件読むたびにカウントに加算して捨てるため、件数によらずカウント用の Map のみメモリに保持する。
 */
public class PmdXmlToMarkdownCommand extends AbstractStreamCommand {

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    Stats stats = collectStats(input);
    String report = generateMarkdownReport(stats);
    output.write(report.getBytes("UTF-8"));
  }

  private Stats collectStats(InputStream input) throws IOException {
    Stats stats = new Stats();
    try (ObjectInputStream ois = new ObjectInputStream(input)) {
      while (true) {
        try {
          PmdViolation v = (PmdViolation) ois.readObject();
          stats.totalViolations++;
          stats.ruleCount.merge(v.rule(), new RuleStat(v.ruleset(), 1L), RuleStat::add);
          stats.fileCount.merge(v.file(), 1L, Long::sum);
          stats.priorityCount.merge(v.priority(), 1L, Long::sum);
        } catch (EOFException e) {
          break;
        } catch (ClassNotFoundException | ClassCastException e) {
          throw new IOException("Unexpected object type in stream: expected PmdViolation", e);
        }
      }
    }
    return stats;
  }

  private String generateMarkdownReport(Stats stats) {
    StringBuilder md = new StringBuilder();
    md.append("# PMD Code Quality Analysis Report\n\n");
    md.append("**Generated**: ").append(Instant.now()).append("\n");
    md.append("**Total Violations**: ").append(stats.totalViolations).append("\n\n");

    generateTopRulesSection(md, stats);
    generateFileStatisticsSection(md, stats);
    generatePriorityDistributionSection(md, stats);

    return md.toString();
  }

  private void generateTopRulesSection(StringBuilder md, Stats stats) {
    md.append("## \uD83C\uDFAF Top Code Smell Rules\n\n");
    md.append("| Rank | Rule | Count | Category |\n");
    md.append("|------|------|-------|----------|\n");

    int[] rank = {1};
    stats.ruleCount.entrySet().stream()
        .sorted(
            Map.Entry.<String, RuleStat>comparingByValue((a, b) -> Long.compare(b.count, a.count)))
        .limit(20)
        .forEach(
            entry ->
                md.append(
                    String.format(
                        "| %d | %s | %d | %s |\n",
                        rank[0]++,
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().category)));
  }

  private void generateFileStatisticsSection(StringBuilder md, Stats stats) {
    md.append("\n## \uD83D\uDCC1 Files with Most Issues\n\n");
    md.append("| File | Violations |\n");
    md.append("|------|------------|\n");

    stats.fileCount.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(15)
        .forEach(
            entry ->
                md.append(
                    String.format(
                        "| %s | %d |\n",
                        entry.getKey().replaceAll(".*/(\\w+\\.java)", "$1"), entry.getValue())));
  }

  private void generatePriorityDistributionSection(StringBuilder md, Stats stats) {
    md.append("\n## \u26A1 Priority Distribution\n\n");
    md.append("| Priority | Count | Description |\n");
    md.append("|----------|-------|-------------|\n");

    stats.priorityCount.forEach(
        (priority, count) -> {
          String desc =
              switch (priority) {
                case 1 -> "\uD83D\uDD34 High - Critical issues";
                case 2 -> "\uD83D\uDFE1 Medium - Important issues";
                case 3 -> "\uD83D\uDFE2 Low - Minor issues";
                case 4 -> "\u2139\uFE0F Info - Informational";
                default -> "\u2753 Unknown";
              };
          md.append(String.format("| %d | %d | %s |\n", priority, count, desc));
        });
  }

  private static class Stats {
    int totalViolations = 0;
    final Map<String, RuleStat> ruleCount = new LinkedHashMap<>();
    final Map<String, Long> fileCount = new LinkedHashMap<>();
    final Map<Integer, Long> priorityCount = new TreeMap<>();
  }

  private record RuleStat(String category, long count) {
    static RuleStat add(RuleStat a, RuleStat b) {
      return new RuleStat(a.category, a.count + b.count);
    }
  }
}
