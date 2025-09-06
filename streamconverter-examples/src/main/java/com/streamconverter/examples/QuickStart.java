package com.streamconverter.examples;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.SampleStreamCommand;
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
      // Example 1: CSV column extraction
      csvExample();

      // Example 2: JSON property extraction
      jsonExample();

      // Example 3: XML element extraction
      xmlExample();

      // Example 4: Command pipeline
      pipelineExample();

      log.info("✅ All examples completed successfully!");

    } catch (Exception e) {
      log.error("❌ Example failed: {}", e.getMessage(), e);
    }
  }

  /** Example 1: CSV column extraction */
  private static void csvExample() throws IOException {
    log.info("📊 CSV Example");
    log.info("===============");

    String csvData = "name,age,city\nJohn,30,NYC\nJane,25,LA\n";

    // Extract name column
    IStreamCommand csvCommand =
        CsvNavigateCommand.create(new CSVPath("name"), new PassThroughRule());
    String result = processData(csvData, csvCommand);

    log.info("Input CSV:");
    log.info(csvData);
    log.info("Extracted 'name' column:");
    log.info(result);
    log.info("");
  }

  /** Example 2: JSON property extraction */
  private static void jsonExample() throws IOException {
    log.info("🔍 JSON Example");
    log.info("================");

    String jsonData = "{\"name\":\"John\",\"age\":30,\"city\":\"NYC\"}";

    // Extract name property
    IStreamCommand jsonCommand =
        JsonNavigateCommand.create(TreePath.fromXml("name"), new PassThroughRule());
    String result = processData(jsonData, jsonCommand);

    log.info("Input JSON:");
    log.info(jsonData);
    log.info("Extracted 'name' property:");
    log.info(result);
    log.info("");
  }

  /** Example 3: XML element extraction */
  private static void xmlExample() throws IOException {
    log.info("🌲 XML Example");
    log.info("===============");

    String xmlData =
        """
        <?xml version="1.0"?>
        <person>
          <name>John</name>
          <age>30</age>
          <city>NYC</city>
        </person>
        """;

    // Extract name element
    IStreamCommand xmlCommand =
        XmlNavigateCommand.create(TreePath.fromXml("person/name"), new PassThroughRule());
    String result = processData(xmlData, xmlCommand);

    log.info("Input XML:");
    log.info(xmlData);
    log.info("Extracted 'name' element:");
    log.info(result);
    log.info("");
  }

  /** Example 4: Command pipeline */
  private static void pipelineExample() throws IOException {
    log.info("🔗 Pipeline Example");
    log.info("====================");

    String csvData = "id,name,status\n1,John,active\n2,Jane,inactive\n";

    // Create processing pipeline
    IStreamCommand[] pipeline = {
      CsvNavigateCommand.create(new CSVPath("name"), new PassThroughRule()), // Extract names
      new SampleStreamCommand("processor") // Process names
    };

    String result = processData(csvData, pipeline);

    log.info("Input CSV:");
    log.info(csvData);
    log.info("Pipeline result (name extraction + processing):");
    log.info(result);
    log.info("");
  }

  /** Helper method to process data with a single command */
  private static String processData(String inputData, IStreamCommand command) throws IOException {
    return processData(inputData, new IStreamCommand[] {command});
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
