package com.streamconverter.test;

import com.streamConverter.CommandResult;
import com.streamconverter.controller.PmdAnalysisController;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * StreamConverter アーキテクチャ準拠の PMD 変換実装テスト
 *
 * <p>実装例としてPmdAnalysisControllerの動作確認を行います。
 */
public class PmdConverterTest {

  /** Creates a new test instance. */

  /**
   * Executes manual tests for PMD conversions.
   *
   * @param args unused
   * @throws IOException if an I/O error occurs
   */
  public static void main(String[] args) throws IOException {
    String xmlPath = "build/reports/pmd/main.xml";

    // XMLファイルの存在確認
    if (!Files.exists(Paths.get(xmlPath))) {
      System.err.println("❌ PMD XML report not found: " + xmlPath);
      System.err.println("Run './gradlew pmdMain' first to generate PMD report");
      return;
    }

    System.out.println("🚀 Testing StreamConverter PMD Analysis Implementation");
    System.out.println("Input: " + xmlPath);
    System.out.println();

    // 3つの形式での変換テスト
    testMarkdownConversion(xmlPath);
    testCsvConversion(xmlPath);
    testJsonConversion(xmlPath);

    System.out.println("🎉 All StreamConverter PMD conversion tests completed!");
  }

  private static void testMarkdownConversion(String xmlPath) throws IOException {
    System.out.println("📝 Testing Markdown conversion (AI-readable format)...");
    PmdAnalysisController controller = PmdAnalysisController.forMarkdownConversion();

    try (FileInputStream input = new FileInputStream(xmlPath);
        FileOutputStream output = new FileOutputStream("pmd-analysis.md")) {

      List<CommandResult> results = controller.process(input, output);
      CommandResult result = results.get(0);

      System.out.println("✅ Markdown conversion successful!");
      System.out.println("   " + result);
      System.out.println("   Output: pmd-analysis.md");
      System.out.println();

    } catch (Exception e) {
      System.err.println("❌ Markdown conversion failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void testCsvConversion(String xmlPath) throws IOException {
    System.out.println("📊 Testing CSV conversion (spreadsheet analysis)...");
    PmdAnalysisController controller = PmdAnalysisController.forCsvConversion();

    try (FileInputStream input = new FileInputStream(xmlPath);
        FileOutputStream output = new FileOutputStream("pmd-analysis.csv")) {

      List<CommandResult> results = controller.process(input, output);
      CommandResult result = results.get(0);

      System.out.println("✅ CSV conversion successful!");
      System.out.println("   " + result);
      System.out.println("   Output: pmd-analysis.csv");
      System.out.println();

    } catch (Exception e) {
      System.err.println("❌ CSV conversion failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void testJsonConversion(String xmlPath) throws IOException {
    System.out.println("📄 Testing JSON conversion (structured data)...");
    PmdAnalysisController controller = PmdAnalysisController.forJsonConversion();

    try (FileInputStream input = new FileInputStream(xmlPath);
        FileOutputStream output = new FileOutputStream("pmd-analysis.json")) {

      List<CommandResult> results = controller.process(input, output);
      CommandResult result = results.get(0);

      System.out.println("✅ JSON conversion successful!");
      System.out.println("   " + result);
      System.out.println("   Output: pmd-analysis.json");
      System.out.println();

    } catch (Exception e) {
      System.err.println("❌ JSON conversion failed: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
