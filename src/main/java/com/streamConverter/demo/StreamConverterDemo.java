package com.streamConverter.demo;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.CsvNavigateCommand;
import com.streamConverter.command.impl.JsonNavigateCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.XmlNavigateCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive demo application for StreamConverter.
 *
 * <p>This demo showcases various features of the StreamConverter including: - CSV navigation and
 * column selection - JSON navigation with JSONPath-like expressions - XML navigation with XPath
 * expressions - Command chaining and pipeline processing
 */
public class StreamConverterDemo {

  private static final Logger logger = LoggerFactory.getLogger(StreamConverterDemo.class);

  /**
   * アプリケーションのエントリーポイント。StreamConverterのデモを実行します。
   *
   * @param args コマンドライン引数（使用されません）
   */
  public static void main(String[] args) {
    logger.info("=== StreamConverter Demo Application ===\n");

    try {
      // Demo 1: CSV Navigation
      demoCSVNavigation();

      // Demo 2: JSON Navigation
      demoJSONNavigation();

      // Demo 3: XML Navigation
      demoXMLNavigation();

      // Demo 4: Command Pipeline
      demoCommandPipeline();

      // Demo 5: Complex Processing Chain
      demoComplexProcessing();

    } catch (Exception e) {
      logger.error("Demo failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void demoCSVNavigation() throws IOException {
    logger.info("📊 CSV Navigation Demo");
    logger.info("======================");

    String csvData =
        """
        name,age,city,salary
        John Doe,30,New York,50000
        Jane Smith,25,Los Angeles,60000
        Bob Johnson,35,Chicago,55000
        Alice Brown,28,Houston,52000
        """;

    // Demo 1a: Extract specific column by name
    logger.info("1. Extract 'name' column:");
    runDemo(csvData, new CsvNavigateCommand("name"));

    // Demo 1b: Extract column by index
    logger.info("\n2. Extract column at index 2 (city):");
    runDemo(csvData, new CsvNavigateCommand("2"));

    // Demo 1c: Process all columns
    logger.info("\n3. Process all columns (formatted):");
    runDemo(csvData, new CsvNavigateCommand());

    logger.info("\n" + "=".repeat(50) + "\n");
  }

  private static void demoJSONNavigation() throws IOException {
    logger.info("🔍 JSON Navigation Demo");
    logger.info("========================");

    String jsonData =
        """
        {"users":[{"name":"John","age":30,"city":"NYC"},{"name":"Jane","age":25,"city":"LA"}],"total":2}
        """;

    // Demo 2a: Extract specific property
    logger.info("1. Extract 'total' property:");
    runDemo(jsonData, new JsonNavigateCommand("total"));

    // Demo 2b: Navigate nested properties
    logger.info("\n2. Navigate to users array:");
    runDemo(jsonData, new JsonNavigateCommand("users"));

    // Demo 2c: Format entire JSON
    logger.info("\n3. Format entire JSON:");
    runDemo(jsonData, new JsonNavigateCommand());

    logger.info("\n" + "=".repeat(50) + "\n");
  }

  private static void demoXMLNavigation() throws IOException {
    logger.info("🌲 XML Navigation Demo");
    logger.info("======================");

    String xmlData =
        """
        <?xml version="1.0"?>
        <users>
          <user>
            <name>John</name>
            <age>30</age>
            <city>NYC</city>
          </user>
          <user>
            <name>Jane</name>
            <age>25</age>
            <city>LA</city>
          </user>
        </users>
        """;

    // Demo 3a: Extract specific elements
    logger.info("1. Extract all 'name' elements:");
    runDemo(xmlData, new XmlNavigateCommand("users/user/name"));

    // Demo 3b: Extract user elements
    logger.info("\n2. Extract all 'user' elements:");
    runDemo(xmlData, new XmlNavigateCommand("users/user"));

    // Demo 3c: Process entire XML
    logger.info("\n3. Process entire XML:");
    runDemo(xmlData, new XmlNavigateCommand());

    logger.info("\n" + "=".repeat(50) + "\n");
  }

  private static void demoCommandPipeline() throws IOException {
    logger.info("🔗 Command Pipeline Demo");
    logger.info("=========================");

    String jsonData =
        """
        {"users":[{"name":"John","age":30},{"name":"Jane","age":25}],"total":2}
        """;

    // Demo 4: Chain JSON formatting with sample processing
    logger.info("1. JSON formatting → Sample processing:");
    IStreamCommand[] pipeline = {
      new JsonNavigateCommand(), // Format JSON
      new SampleStreamCommand("formatter") // Add processing info
    };

    runDemo(jsonData, pipeline);

    logger.info("\n" + "=".repeat(50) + "\n");
  }

  private static void demoComplexProcessing() throws IOException {
    logger.info("⚙️ Complex Processing Demo");
    logger.info("===========================");

    String csvData =
        """
        name,age,city
        John,30,NYC
        Jane,25,LA
        Bob,35,Chicago
        """;

    // Demo 5: Multi-stage processing
    logger.info("1. Multi-stage CSV processing:");
    IStreamCommand[] complexPipeline = {
      new CsvNavigateCommand("name"), // Extract names
      new SampleStreamCommand("name-processor"), // Process names
      new SampleStreamCommand("final-formatter") // Final formatting
    };

    runDemo(csvData, complexPipeline);

    logger.info("\n" + "=".repeat(50) + "\n");
  }

  private static void runDemo(String inputData, IStreamCommand command) throws IOException {
    runDemo(inputData, new IStreamCommand[] {command});
  }

  private static void runDemo(String inputData, IStreamCommand[] commands) throws IOException {
    try (InputStream inputStream =
            new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      StreamConverter converter = new StreamConverter(commands);
      converter.run(inputStream, outputStream);

      String result = outputStream.toString(StandardCharsets.UTF_8);
      logger.info("Input:");
      logger.info(inputData.trim());
      logger.info("\nOutput:");
      logger.info(result.trim());
    }
  }
}
