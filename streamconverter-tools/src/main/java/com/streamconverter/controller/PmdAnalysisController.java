package com.streamconverter.controller;

import com.streamConverter.command.CommandConfig;
import com.streamConverter.controller.AbstractStreamController;
import com.streamconverter.command.impl.analysis.PmdXmlToCsvCommand;
import com.streamconverter.command.impl.analysis.PmdXmlToJsonCommand;
import com.streamconverter.command.impl.analysis.PmdXmlToMarkdownCommand;

/**
 * PMD解析レポート変換のためのController
 *
 * <p>StreamConverterアーキテクチャの Controller層 実装例として、 PMD XMLレポートを複数形式（Markdown, CSV, JSON）に変換する処理を
 * 統一インターフェースで提供します。
 *
 * <p><strong>アーキテクチャ例証:</strong>
 *
 * <ul>
 *   <li>Controller層: 複雑な設定ルールの隠蔽
 *   <li>StreamConverter層: パイプライン管理と並行処理
 *   <li>Command層: 純粋な変換処理の実装
 * </ul>
 *
 * <p><strong>提供形式:</strong>
 *
 * <ul>
 *   <li><strong>Markdown</strong>: AI可読性重視の要約レポート
 *   <li><strong>CSV</strong>: スプレッドシート分析用詳細データ
 *   <li><strong>JSON</strong>: プログラム解析用構造化データ
 * </ul>
 *
 * <p><strong>使用例:</strong>
 *
 * <pre>
 * PmdAnalysisController controller = PmdAnalysisController.forMarkdownConversion();
 * try (FileInputStream xmlInput = new FileInputStream("pmd-report.xml");
 *      FileOutputStream mdOutput = new FileOutputStream("analysis.md")) {
 *   List&lt;CommandResult&gt; results = controller.process(xmlInput, mdOutput);
 *   // 処理結果の確認...
 * }
 * </pre>
 */
public class PmdAnalysisController extends AbstractStreamController {

  private final ConversionFormat targetFormat;

  /** 変換対象フォーマット */
  public enum ConversionFormat {
    /** Markdown AI-readable analysis report. */
    MARKDOWN("Markdown AI-readable analysis report"),
    /** CSV spreadsheet-compatible data. */
    CSV("CSV spreadsheet-compatible data"),
    /** JSON structured analysis data. */
    JSON("JSON structured analysis data");

    private final String description;

    ConversionFormat(String description) {
      this.description = description;
    }

    /**
     * Gets the human-readable description.
     *
     * @return description text
     */
    public String getDescription() {
      return description;
    }
  }

  private PmdAnalysisController(ConversionFormat format) {
    this.targetFormat = format;
  }

  /**
   * Markdown形式変換用のControllerを作成
   *
   * @return AI可読Markdown形式に変換するController
   */
  public static PmdAnalysisController forMarkdownConversion() {
    return new PmdAnalysisController(ConversionFormat.MARKDOWN);
  }

  /**
   * CSV形式変換用のControllerを作成
   *
   * @return スプレッドシート分析用CSV形式に変換するController
   */
  public static PmdAnalysisController forCsvConversion() {
    return new PmdAnalysisController(ConversionFormat.CSV);
  }

  /**
   * JSON形式変換用のControllerを作成
   *
   * @return 構造化JSON形式に変換するController
   */
  public static PmdAnalysisController forJsonConversion() {
    return new PmdAnalysisController(ConversionFormat.JSON);
  }

  @Override
  protected CommandConfig[] configureCommands() {
    return new CommandConfig[] {createCommandConfig()};
  }

  /** 指定フォーマットに対応するCommandConfigを生成 */
  private CommandConfig createCommandConfig() {
    return switch (targetFormat) {
      case MARKDOWN ->
          new CommandConfig(PmdXmlToMarkdownCommand.class, "PMD XML to Markdown conversion");
      case CSV -> new CommandConfig(PmdXmlToCsvCommand.class, "PMD XML to CSV conversion");
      case JSON -> new CommandConfig(PmdXmlToJsonCommand.class, "PMD XML to JSON conversion");
    };
  }

  @Override
  public String getConfigurationDescription() {
    return String.format(
        "PMD Analysis Controller configured for %s format conversion: %s",
        targetFormat.name().toLowerCase(), targetFormat.getDescription());
  }

  @Override
  public String getInputDataType() {
    return "PMD_XML_REPORT";
  }

  @Override
  public String getOutputDataType() {
    return "PMD_" + targetFormat.name() + "_ANALYSIS";
  }
}
