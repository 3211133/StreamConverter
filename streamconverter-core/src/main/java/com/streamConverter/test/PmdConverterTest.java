package com.streamConverter.test;

import com.streamConverter.CommandResult;
import com.streamConverter.controller.PmdAnalysisController;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StreamConverter アーキテクチャ準拠の PMD 変換実装テスト
 *
 * <p>実装例としてPmdAnalysisControllerの動作確認を行います。
 */
public class PmdConverterTest {

  private static final Logger LOG = LoggerFactory.getLogger(PmdConverterTest.class);

  public static void main(String[] args) throws IOException {
    String xmlPath = "build/reports/pmd/main.xml";

    // XMLファイルの存在確認
    if (!Files.exists(Paths.get(xmlPath))) {
      if (LOG.isErrorEnabled()) {
        LOG.error("❌ PMD XML report not found: {}", xmlPath);
        LOG.error("Run './gradlew pmdMain' first to generate PMD report");
      }
      return;
    }

    if (LOG.isInfoEnabled()) {
      LOG.info("🚀 Testing StreamConverter PMD Analysis Implementation");
      LOG.info("Input: {}", xmlPath);
    }

    // 3つの形式での変換テスト
    testMarkdownConversion(xmlPath);
    testCsvConversion(xmlPath);
    testJsonConversion(xmlPath);

    if (LOG.isInfoEnabled()) {
      LOG.info("🎉 All StreamConverter PMD conversion tests completed!");
    }
  }

  private static void testMarkdownConversion(String xmlPath) throws IOException {
    if (LOG.isInfoEnabled()) {
      LOG.info("📝 Testing Markdown conversion (AI-readable format)...");
    }
    PmdAnalysisController controller = PmdAnalysisController.forMarkdownConversion();

    try (FileInputStream input = new FileInputStream(xmlPath);
        FileOutputStream output = new FileOutputStream("pmd-analysis.md")) {

      List<CommandResult> results = controller.process(input, output);
      CommandResult result = results.get(0);

      if (LOG.isInfoEnabled()) {
        LOG.info("✅ Markdown conversion successful!");
        LOG.info("   {}", result);
        LOG.info("   Output: pmd-analysis.md");
      }

    } catch (Exception e) {
      LOG.error("❌ Markdown conversion failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void testCsvConversion(String xmlPath) throws IOException {
    if (LOG.isInfoEnabled()) {
      LOG.info("📊 Testing CSV conversion (spreadsheet analysis)...");
    }
    PmdAnalysisController controller = PmdAnalysisController.forCsvConversion();

    try (FileInputStream input = new FileInputStream(xmlPath);
        FileOutputStream output = new FileOutputStream("pmd-analysis.csv")) {

      List<CommandResult> results = controller.process(input, output);
      CommandResult result = results.get(0);

      LOG.info("✅ CSV conversion successful!");
      LOG.info("   " + result);
      LOG.info("   Output: pmd-analysis.csv");

    } catch (Exception e) {
      LOG.error("❌ CSV conversion failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void testJsonConversion(String xmlPath) throws IOException {
    LOG.info("📄 Testing JSON conversion (structured data)...");
    PmdAnalysisController controller = PmdAnalysisController.forJsonConversion();

    try (FileInputStream input = new FileInputStream(xmlPath);
        FileOutputStream output = new FileOutputStream("pmd-analysis.json")) {

      List<CommandResult> results = controller.process(input, output);
      CommandResult result = results.get(0);

      LOG.info("✅ JSON conversion successful!");
      LOG.info("   " + result);
      LOG.info("   Output: pmd-analysis.json");

    } catch (Exception e) {
      LOG.error("❌ JSON conversion failed: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
