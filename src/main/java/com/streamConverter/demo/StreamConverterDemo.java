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

/**
 * Comprehensive demo application for StreamConverter.
 *
 * <p>This demo showcases various features of the StreamConverter including: - CSV navigation and
 * column selection - JSON navigation with JSONPath-like expressions - XML navigation with XPath
 * expressions - Command chaining and pipeline processing
 */
public class StreamConverterDemo {

  /**
   * アプリケーションのエントリーポイント。StreamConverterのデモを実行します。
   *
   * @param args コマンドライン引数（使用されません）
   */
  public static void main(String[] args) {
    System.out.println("=== StreamConverter Demo Application ===\n");

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
      System.err.println("Demo failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void demoCSVNavigation() throws IOException {
    System.out.println("📊 CSV Navigation Demo");
    System.out.println("======================");

    String csvData =
        """
        name,age,city,salary
        John Doe,30,New York,50000
        Jane Smith,25,Los Angeles,60000
        Bob Johnson,35,Chicago,55000
        Alice Brown,28,Houston,52000
        """;

    // Demo 1a: Extract specific column by name
    System.out.println("1. Extract 'name' column:");
    runDemo(csvData, new CsvNavigateCommand("name"));

    // Demo 1b: Extract column by index
    System.out.println("\n2. Extract column at index 2 (city):");
    runDemo(csvData, new CsvNavigateCommand("2"));

    // Demo 1c: Process all columns
    System.out.println("\n3. Process all columns (formatted):");
    runDemo(csvData, new CsvNavigateCommand());

    System.out.println("\n" + "=".repeat(50) + "\n");
  }

  private static void demoJSONNavigation() throws IOException {
    System.out.println("🔍 JSON Navigation Demo");
    System.out.println("========================");

    String jsonData =
        """
        {"users":[{"name":"John","age":30,"city":"NYC"},{"name":"Jane","age":25,"city":"LA"}],"total":2}
        """;

    // Demo 2a: Extract specific property
    System.out.println("1. Extract 'total' property:");
    runDemo(jsonData, new JsonNavigateCommand("total"));

    // Demo 2b: Navigate nested properties
    System.out.println("\n2. Navigate to users array:");
    runDemo(jsonData, new JsonNavigateCommand("users"));

    // Demo 2c: Format entire JSON
    System.out.println("\n3. Format entire JSON:");
    runDemo(jsonData, new JsonNavigateCommand());

    System.out.println("\n" + "=".repeat(50) + "\n");
  }

  private static void demoXMLNavigation() throws IOException {
    System.out.println("🌲 XML Navigation Demo");
    System.out.println("======================");

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
    System.out.println("1. Extract all 'name' elements:");
    runDemo(xmlData, new XmlNavigateCommand("users/user/name"));

    // Demo 3b: Extract user elements
    System.out.println("\n2. Extract all 'user' elements:");
    runDemo(xmlData, new XmlNavigateCommand("users/user"));

    // Demo 3c: Process entire XML
    System.out.println("\n3. Process entire XML:");
    runDemo(xmlData, new XmlNavigateCommand());

    System.out.println("\n" + "=".repeat(50) + "\n");
  }

  private static void demoCommandPipeline() throws IOException {
    System.out.println("🔗 Command Pipeline Demo");
    System.out.println("=========================");

    String jsonData =
        """
        {"users":[{"name":"John","age":30},{"name":"Jane","age":25}],"total":2}
        """;

    // Demo 4: Chain JSON formatting with sample processing
    System.out.println("1. JSON formatting → Sample processing:");
    IStreamCommand[] pipeline = {
      new JsonNavigateCommand(), // Format JSON
      new SampleStreamCommand("formatter") // Add processing info
    };

    runDemo(jsonData, pipeline);

    System.out.println("\n" + "=".repeat(50) + "\n");
  }

  private static void demoComplexProcessing() throws IOException {
    System.out.println("⚙️ Complex Processing Demo");
    System.out.println("===========================");

    String csvData =
        """
        name,age,city
        John,30,NYC
        Jane,25,LA
        Bob,35,Chicago
        """;

    // Demo 5: Multi-stage processing
    System.out.println("1. Multi-stage CSV processing:");
    IStreamCommand[] complexPipeline = {
      new CsvNavigateCommand("name"), // Extract names
      new SampleStreamCommand("name-processor"), // Process names
      new SampleStreamCommand("final-formatter") // Final formatting
    };

    runDemo(csvData, complexPipeline);

    System.out.println("\n" + "=".repeat(50) + "\n");
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
      System.out.println("Input:");
      System.out.println(inputData.trim());
      System.out.println("\nOutput:");
      System.out.println(result.trim());
    }
  }
}
