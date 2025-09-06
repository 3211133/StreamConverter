package com.streamconverter.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PmdAutoFixer {

  private static final String PMD_REPORT_PATH = "build/reports/pmd/main.xml";
  private static final String SOURCE_PATH = "src/main/java";

  private final Map<String, Integer> fixCounts = new HashMap<>();
  private final Set<String> processedFiles = new HashSet<>();

  public static void main(String[] args) {
    new PmdAutoFixer().run();
  }

  public void run() {
    System.out.println("🔧 PMD Auto-Fixer Starting...");

    try {
      // Create backup
      createBackup();

      // Parse PMD report
      List<PmdViolation> violations = parsePmdReport();
      System.out.printf("📊 Found %d violations to process%n", violations.size());

      // Group violations by rule
      Map<String, List<PmdViolation>> violationsByRule =
          violations.stream().collect(Collectors.groupingBy(v -> v.rule));

      // Apply fixes
      applyFixes(violationsByRule);

      // Print summary
      printSummary();

    } catch (Exception e) {
      System.err.println("❌ Error during auto-fix: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void createBackup() throws IOException {
    Path sourcePath = Paths.get(SOURCE_PATH);
    if (!Files.exists(sourcePath)) {
      throw new IOException("Source path not found: " + sourcePath);
    }

    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    Path backupPath = Paths.get("pmd-fixes-backup-" + timestamp);

    System.out.println("💾 Creating backup at: " + backupPath.toAbsolutePath());

    copyDirectory(sourcePath, backupPath);
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    Files.walk(source)
        .forEach(
            sourcePath -> {
              try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                  Files.createDirectories(targetPath);
                } else {
                  Files.createDirectories(targetPath.getParent());
                  Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private List<PmdViolation> parsePmdReport() throws Exception {
    Path reportPath = Paths.get(PMD_REPORT_PATH);
    if (!Files.exists(reportPath)) {
      throw new IOException("PMD report not found: " + reportPath);
    }

    List<PmdViolation> violations = new ArrayList<>();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(reportPath.toFile());

    NodeList fileNodes = doc.getElementsByTagName("file");
    for (int i = 0; i < fileNodes.getLength(); i++) {
      Element fileElement = (Element) fileNodes.item(i);
      String fileName = fileElement.getAttribute("name");

      NodeList violationNodes = fileElement.getElementsByTagName("violation");
      for (int j = 0; j < violationNodes.getLength(); j++) {
        Element violationElement = (Element) violationNodes.item(j);
        violations.add(
            new PmdViolation(
                fileName,
                violationElement.getAttribute("rule"),
                Integer.parseInt(violationElement.getAttribute("beginline")),
                Integer.parseInt(violationElement.getAttribute("endline")),
                violationElement.getTextContent().trim()));
      }
    }

    return violations;
  }

  private void applyFixes(Map<String, List<PmdViolation>> violationsByRule) {
    System.out.println("\n🎯 Applying automatic fixes...");

    // Phase 1: Safe Final Keyword Additions
    fixMethodArgumentCouldBeFinal(violationsByRule.get("MethodArgumentCouldBeFinal"));
    fixLocalVariableCouldBeFinal(violationsByRule.get("LocalVariableCouldBeFinal"));

    // Phase 2: Performance Optimizations
    fixInefficientEmptyStringCheck(violationsByRule.get("InefficientEmptyStringCheck"));
    fixAppendCharacterWithChar(violationsByRule.get("AppendCharacterWithChar"));
    fixUseIndexOfChar(violationsByRule.get("UseIndexOfChar"));

    // Phase 3: Code Style Improvements
    fixRedundantFieldInitializer(violationsByRule.get("RedundantFieldInitializer"));
    fixUseUnderscoresInNumericLiterals(violationsByRule.get("UseUnderscoresInNumericLiterals"));
  }

  private void fixMethodArgumentCouldBeFinal(List<PmdViolation> violations) {
    if (violations == null || violations.isEmpty()) return;

    System.out.println("🔄 Fixing MethodArgumentCouldBeFinal violations...");

    Map<String, List<PmdViolation>> byFile =
        violations.stream().collect(Collectors.groupingBy(v -> v.fileName));

    int totalFixed = 0;

    for (Map.Entry<String, List<PmdViolation>> entry : byFile.entrySet()) {
      try {
        String content = Files.readString(Paths.get(entry.getKey()));
        // Simple pattern-based fixing for method parameters
        // This is a basic implementation - could be enhanced with proper Java parsing
        Pattern methodParamPattern = Pattern.compile("(\\([^)]*?)\\b(\\w+\\s+)(\\w+)\\s*([,)])");

        Matcher matcher = methodParamPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        int fixCount = 0;

        while (matcher.find()) {
          String paramType = matcher.group(2).trim();
          if (!paramType.startsWith("final ")) {
            matcher.appendReplacement(
                sb,
                matcher.group(1)
                    + "final "
                    + paramType
                    + " "
                    + matcher.group(3)
                    + matcher.group(4));
            fixCount++;
          }
        }
        matcher.appendTail(sb);

        if (fixCount > 0) {
          Files.writeString(Paths.get(entry.getKey()), sb.toString());
          totalFixed += fixCount;
          processedFiles.add(entry.getKey());
        }

      } catch (IOException e) {
        System.err.println("⚠️ Error processing file: " + entry.getKey() + " - " + e.getMessage());
      }
    }

    fixCounts.put("MethodArgumentCouldBeFinal", totalFixed);
  }

  private void fixLocalVariableCouldBeFinal(List<PmdViolation> violations) {
    if (violations == null || violations.isEmpty()) return;

    System.out.println("🔄 Fixing LocalVariableCouldBeFinal violations...");

    Map<String, List<PmdViolation>> byFile =
        violations.stream().collect(Collectors.groupingBy(v -> v.fileName));

    int totalFixed = 0;

    for (Map.Entry<String, List<PmdViolation>> entry : byFile.entrySet()) {
      try {
        String content = Files.readString(Paths.get(entry.getKey()));

        // Pattern for local variable declarations
        Pattern localVarPattern =
            Pattern.compile(
                "(^\\s+)((?:String|int|long|double|boolean|List|Map|Set|\\w+)(?:<[^>]+>)?)\\s+(\\w+)\\s*=",
                Pattern.MULTILINE);

        Matcher matcher = localVarPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        int fixCount = 0;

        while (matcher.find()) {
          String indent = matcher.group(1);
          String type = matcher.group(2);
          String varName = matcher.group(3);

          if (!type.startsWith("final ")) {
            matcher.appendReplacement(sb, indent + "final " + type + " " + varName + " =");
            fixCount++;
          }
        }
        matcher.appendTail(sb);

        if (fixCount > 0) {
          Files.writeString(Paths.get(entry.getKey()), sb.toString());
          totalFixed += fixCount;
          processedFiles.add(entry.getKey());
        }

      } catch (IOException e) {
        System.err.println("⚠️ Error processing file: " + entry.getKey() + " - " + e.getMessage());
      }
    }

    fixCounts.put("LocalVariableCouldBeFinal", totalFixed);
  }

  private void fixInefficientEmptyStringCheck(List<PmdViolation> violations) {
    if (violations == null || violations.isEmpty()) return;

    System.out.println("🔄 Fixing InefficientEmptyStringCheck violations...");

    int totalFixed =
        applySimpleReplacement(
            violations,
            "\\.length\\(\\)\\s*==\\s*0",
            ".isEmpty()",
            "\\.length\\(\\)\\s*!=\\s*0",
            "!$1.isEmpty()");

    fixCounts.put("InefficientEmptyStringCheck", totalFixed);
  }

  private void fixAppendCharacterWithChar(List<PmdViolation> violations) {
    if (violations == null || violations.isEmpty()) return;

    System.out.println("🔄 Fixing AppendCharacterWithChar violations...");

    int totalFixed = applySimpleReplacement(violations, "\\.append\\(\"(.)\"\\)", ".append('$1')");

    fixCounts.put("AppendCharacterWithChar", totalFixed);
  }

  private void fixUseIndexOfChar(List<PmdViolation> violations) {
    if (violations == null || violations.isEmpty()) return;

    System.out.println("🔄 Fixing UseIndexOfChar violations...");

    int totalFixed =
        applySimpleReplacement(violations, "\\.indexOf\\(\"(.)\"\\)", ".indexOf('$1')");

    fixCounts.put("UseIndexOfChar", totalFixed);
  }

  private void fixRedundantFieldInitializer(List<PmdViolation> violations) {
    if (violations == null || violations.isEmpty()) return;

    System.out.println("🔄 Fixing RedundantFieldInitializer violations...");

    int totalFixed =
        applySimpleReplacement(
            violations, "\\s*=\\s*null;", ";", "\\s*=\\s*false;", ";", "\\s*=\\s*0;", ";");

    fixCounts.put("RedundantFieldInitializer", totalFixed);
  }

  private void fixUseUnderscoresInNumericLiterals(List<PmdViolation> violations) {
    if (violations == null || violations.isEmpty()) return;

    System.out.println("🔄 Fixing UseUnderscoresInNumericLiterals violations...");

    // This would need more sophisticated number parsing
    // Placeholder implementation
    fixCounts.put("UseUnderscoresInNumericLiterals", 0);
  }

  private int applySimpleReplacement(List<PmdViolation> violations, String... replacements) {
    if (violations == null || violations.isEmpty()) return 0;

    Map<String, List<PmdViolation>> byFile =
        violations.stream().collect(Collectors.groupingBy(v -> v.fileName));

    int totalFixed = 0;

    for (String fileName : byFile.keySet()) {
      try {
        String content = Files.readString(Paths.get(fileName));
        String originalContent = content;
        int fileFixCount = 0;

        for (int i = 0; i < replacements.length; i += 2) {
          String pattern = replacements[i];
          String replacement = replacements[i + 1];
          String beforeReplacement = content;
          content = content.replaceAll(pattern, replacement);

          // Count actual replacements by checking how many times the pattern matched
          if (!content.equals(beforeReplacement)) {
            int matches = beforeReplacement.split(pattern, -1).length - 1;
            fileFixCount += matches;
          }
        }

        if (!content.equals(originalContent)) {
          Files.writeString(Paths.get(fileName), content);
          totalFixed += fileFixCount;
          processedFiles.add(fileName);
        }

      } catch (IOException e) {
        System.err.println("⚠️ Error processing file: " + fileName + " - " + e.getMessage());
      }
    }

    return totalFixed;
  }

  private void printSummary() {
    System.out.println("\n📊 Auto-Fix Summary:");
    System.out.println("━".repeat(50));

    int totalFixes = 0;
    for (Map.Entry<String, Integer> entry : fixCounts.entrySet()) {
      System.out.printf("%-35s: %3d fixes%n", entry.getKey(), entry.getValue());
      totalFixes += entry.getValue();
    }

    System.out.println("━".repeat(50));
    System.out.printf("%-35s: %3d fixes%n", "TOTAL", totalFixes);
    System.out.printf("Files processed: %d%n", processedFiles.size());

    System.out.println("\n🧪 Next steps:");
    System.out.println("   1. Review changes: git diff");
    System.out.println("   2. Run tests: ./gradlew test");
    System.out.println("   3. Run PMD again: ./gradlew pmdMain");
    System.out.println("   4. If satisfied, commit changes");

    System.out.println("\n🏁 PMD Auto-Fix Complete!");
  }

  private static class PmdViolation {
    final String fileName;
    final String rule;

    PmdViolation(String fileName, String rule, int beginLine, int endLine, String message) {
      this.fileName = fileName;
      this.rule = rule;
    }
  }
}
