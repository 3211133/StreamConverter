package com.streamconverter.examples;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.impl.xml.XmlNavigateCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quick start examples for StreamConverter.
 *
 * <p>This provides simple, runnable examples of core functionality.
 */
public class QuickStart {
  private static final Logger log = LoggerFactory.getLogger(QuickStart.class);

  /**
   * アプリケーションのエントリーポイント。StreamConverterの基本的な使用例を実行します。
   *
   * @param args コマンドライン引数（使用されません）
   */
  public static void main(String[] args) {
    log.info("🚀 StreamConverter Quick Start Examples");
    log.info("========================================\n");

    try {
      // Example 1: CSV pipeline
      csvExample();

      // Example 2: JSON pipeline
      jsonExample();

      // Example 3: XML pipeline
      xmlExample();

      // Example 4: Multi-step pipeline
      pipelineExample();

      log.info("✅ All examples completed successfully!");

    } catch (Exception e) {
      log.error("❌ Example failed: {}", e.getMessage(), e);
    }
  }

  /** Example 1: CSV pipeline */
  private static void csvExample() throws IOException {
    log.info("📊 CSV Pipeline Example");
    log.info("========================");

    String csvData = "name,age,city\nJohn,30,NYC\nJane,25,LA\n";

    // Create pipeline: Extract name column + Process
    // Note: The lambda pass-through is a placeholder for demonstration.
    // In real applications, replace with actual processing commands.
    IStreamCommand[] pipeline = {
      CsvNavigateCommand.create(new CSVPath("name"), new PassThroughRule()),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };
    String result = processData(csvData, pipeline);

    log.info("Input CSV:");
    log.info(csvData);
    log.info("Pipeline result (name extraction + processing):");
    log.info(result);
    log.info("");
  }

  /** Example 2: JSON pipeline */
  private static void jsonExample() throws IOException {
    log.info("🔍 JSON Pipeline Example");
    log.info("=========================");

    String jsonData = "{\"name\":\"John\",\"age\":30,\"city\":\"NYC\"}";

    // Create pipeline: Extract name property + Process
    // Note: The lambda pass-through is a placeholder for demonstration.
    // In real applications, replace with actual processing commands.
    IStreamCommand[] pipeline = {
      JsonNavigateCommand.create(TreePath.fromJson("$.name"), new PassThroughRule()),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };
    String result = processData(jsonData, pipeline);

    log.info("Input JSON:");
    log.info(jsonData);
    log.info("Pipeline result (name extraction + processing):");
    log.info(result);
    log.info("");
  }

  /** Example 3: XML pipeline */
  private static void xmlExample() throws IOException {
    log.info("🌲 XML Pipeline Example");
    log.info("========================");

    String xmlData =
        """
        <?xml version="1.0"?>
        <person>
          <name>John</name>
          <age>30</age>
          <city>NYC</city>
        </person>
        """;

    // Create pipeline: Extract name element + Process
    // Note: The lambda pass-through is a placeholder for demonstration.
    // In real applications, replace with actual processing commands.
    IStreamCommand[] pipeline = {
      XmlNavigateCommand.create(TreePath.fromXml("person/name"), new PassThroughRule()),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };
    String result = processData(xmlData, pipeline);

    log.info("Input XML:");
    log.info(xmlData);
    log.info("Pipeline result (name extraction + processing):");
    log.info(result);
    log.info("");
  }

  /** Example 4: Multi-step pipeline */
  private static void pipelineExample() throws IOException {
    log.info("🔗 Multi-Step Pipeline Example");
    log.info("================================");

    String csvData = "id,name,status\n1,John,active\n2,Jane,inactive\n";

    // Create processing pipeline
    IStreamCommand[] pipeline = {
      CsvNavigateCommand.create(new CSVPath("name"), new PassThroughRule()), // Extract names
      (IStreamCommand) (in, out) -> in.transferTo(out) // Process names
    };

    String result = processData(csvData, pipeline);

    log.info("Input CSV:");
    log.info(csvData);
    log.info("Pipeline result (name extraction + processing):");
    log.info(result);
    log.info("");
  }

  /** Helper method to process data with command pipeline */
  private static String processData(String inputData, IStreamCommand[] commands)
      throws IOException {
    try (InputStream inputStream =
            new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      StreamConverter converter = new StreamConverter(commands);
      converter.run(inputStream, outputStream);

      return outputStream.toString(StandardCharsets.UTF_8);
    }
  }
}
