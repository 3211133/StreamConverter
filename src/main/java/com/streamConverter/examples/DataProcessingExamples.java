package com.streamConverter.examples;

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
 * Real-world data processing examples using StreamConverter.
 *
 * <p>This class demonstrates practical use cases including: - Data extraction from various formats
 * - Data transformation pipelines - Format conversion workflows - Performance optimization
 * techniques
 */
public class DataProcessingExamples {

  /**
   * アプリケーションのエントリーポイント。実用的なデータ処理例を実行します。
   *
   * @param args コマンドライン引数（使用されません）
   */
  public static void main(String[] args) {
    System.out.println("🚀 StreamConverter - Real-world Examples");
    System.out.println("=========================================\n");

    try {
      // Example 1: Employee data processing
      employeeDataProcessing();

      // Example 2: API response processing
      apiResponseProcessing();

      // Example 3: Configuration file processing
      configurationProcessing();

      // Example 4: Log analysis
      logAnalysis();

      // Example 5: Data format conversion
      dataFormatConversion();

    } catch (Exception e) {
      System.err.println("Example failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Example 1: Processing employee data from CSV format */
  private static void employeeDataProcessing() throws IOException {
    System.out.println("👥 Employee Data Processing");
    System.out.println("============================");

    String employeeData =
        """
        employee_id,name,department,salary,location
        E001,John Smith,Engineering,75000,New York
        E002,Jane Doe,Marketing,65000,Los Angeles
        E003,Bob Johnson,Engineering,80000,Chicago
        E004,Alice Brown,HR,60000,Houston
        E005,Charlie Wilson,Engineering,70000,Seattle
        """;

    // Extract employee names for a directory
    System.out.println("📋 Extract employee names:");
    processData(employeeData, new CsvNavigateCommand("name"));

    // Extract salary information for budget analysis
    System.out.println("\n💰 Extract salary information:");
    processData(employeeData, new CsvNavigateCommand("salary"));

    // Extract department information for organization chart
    System.out.println("\n🏢 Extract department information:");
    processData(employeeData, new CsvNavigateCommand("department"));

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /** Example 2: Processing API response data */
  private static void apiResponseProcessing() throws IOException {
    System.out.println("🌐 API Response Processing");
    System.out.println("===========================");

    String apiResponse =
        """
        {
          "status": "success",
          "data": {
            "users": [
              {"id": 1, "name": "John", "email": "john@example.com", "active": true},
              {"id": 2, "name": "Jane", "email": "jane@example.com", "active": false}
            ],
            "pagination": {
              "total": 2,
              "page": 1,
              "per_page": 10
            }
          }
        }
        """;

    // Extract status for monitoring
    System.out.println("📊 Extract API status:");
    processData(apiResponse, new JsonNavigateCommand("status"));

    // Extract user data for processing
    System.out.println("\n👤 Extract user data:");
    processData(apiResponse, new JsonNavigateCommand("data"));

    // Format entire response for logging
    System.out.println("\n📝 Format entire response:");
    processData(apiResponse, new JsonNavigateCommand());

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /** Example 3: Configuration file processing */
  private static void configurationProcessing() throws IOException {
    System.out.println("⚙️ Configuration Processing");
    System.out.println("============================");

    String configXml =
        """
        <?xml version="1.0"?>
        <configuration>
          <database>
            <host>localhost</host>
            <port>5432</port>
            <name>myapp</name>
          </database>
          <server>
            <port>8080</port>
            <ssl>true</ssl>
          </server>
          <logging>
            <level>INFO</level>
            <file>app.log</file>
          </logging>
        </configuration>
        """;

    // Extract database configuration
    System.out.println("🗄️ Extract database configuration:");
    processData(configXml, new XmlNavigateCommand("configuration/database"));

    // Extract server configuration
    System.out.println("\n🖥️ Extract server configuration:");
    processData(configXml, new XmlNavigateCommand("configuration/server"));

    // Extract logging configuration
    System.out.println("\n📊 Extract logging configuration:");
    processData(configXml, new XmlNavigateCommand("configuration/logging"));

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /** Example 4: Log analysis workflow */
  private static void logAnalysis() throws IOException {
    System.out.println("📈 Log Analysis Workflow");
    System.out.println("=========================");

    String logData =
        """
        timestamp,level,service,message
        2023-07-15T10:30:00Z,INFO,auth-service,User login successful
        2023-07-15T10:31:00Z,ERROR,payment-service,Payment processing failed
        2023-07-15T10:32:00Z,WARN,auth-service,Rate limit exceeded
        2023-07-15T10:33:00Z,INFO,order-service,Order placed successfully
        """;

    // Create analysis pipeline
    IStreamCommand[] analysisPipeline = {
      new CsvNavigateCommand("level"), // Extract log levels
      new SampleStreamCommand("level-analyzer") // Analyze log levels
    };

    System.out.println("🔍 Log level analysis:");
    processData(logData, analysisPipeline);

    // Service-specific analysis
    System.out.println("\n🔧 Service analysis:");
    processData(logData, new CsvNavigateCommand("service"));

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /** Example 5: Data format conversion */
  private static void dataFormatConversion() throws IOException {
    System.out.println("🔄 Data Format Conversion");
    System.out.println("==========================");

    String productData =
        """
        product_id,name,price,category
        P001,Laptop,999.99,Electronics
        P002,Chair,149.99,Furniture
        P003,Book,29.99,Education
        """;

    // Multi-stage conversion pipeline
    IStreamCommand[] conversionPipeline = {
      new CsvNavigateCommand("name"), // Extract product names
      new SampleStreamCommand("name-processor"), // Process names
      new SampleStreamCommand("format-converter") // Convert format
    };

    System.out.println("🛍️ Product name conversion pipeline:");
    processData(productData, conversionPipeline);

    // Price extraction for financial analysis
    System.out.println("\n💲 Price extraction:");
    processData(productData, new CsvNavigateCommand("price"));

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /** Helper method to process data with given commands */
  private static void processData(String inputData, IStreamCommand command) throws IOException {
    processData(inputData, new IStreamCommand[] {command});
  }

  /** Helper method to process data with command pipeline */
  private static void processData(String inputData, IStreamCommand[] commands) throws IOException {
    try (InputStream inputStream =
            new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      StreamConverter converter = new StreamConverter(commands);
      converter.run(inputStream, outputStream);

      String result = outputStream.toString(StandardCharsets.UTF_8);
      System.out.println("Output:");
      System.out.println(result.trim());
    }
  }
}
