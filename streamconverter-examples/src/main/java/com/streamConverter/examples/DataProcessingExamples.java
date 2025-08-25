package com.streamConverter.examples;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.csv.CsvNavigateCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.impl.xml.XmlNavigateCommand;
import com.streamConverter.command.rule.PassThroughRule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Real-world data processing examples using StreamConverter.
 *
 * <p>This class demonstrates practical use cases including: - Data extraction from various formats
 * - Data transformation pipelines - Format conversion workflows - Performance optimization
 * techniques
 */
public class DataProcessingExamples {

  private static final Logger logger = LoggerFactory.getLogger(DataProcessingExamples.class);

  /**
   * アプリケーションのエントリーポイント。実用的なデータ処理例を実行します。
   *
   * @param args コマンドライン引数（使用されません）
   */
  public static void main(String[] args) {
    logger.info("🚀 StreamConverter - Real-world Examples");
    logger.info("=========================================\n");

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
      logger.error("Example failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Example 1: Processing employee data from CSV format */
  private static void employeeDataProcessing() throws IOException {
    logger.info("👥 Employee Data Processing");
    logger.info("============================");

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
    logger.info("📋 Extract employee names:");
    processData(employeeData, CsvNavigateCommand.create("name", new PassThroughRule()));

    // Extract salary information for budget analysis
    logger.info("\n💰 Extract salary information:");
    processData(employeeData, CsvNavigateCommand.create("salary", new PassThroughRule()));

    // Extract department information for organization chart
    logger.info("\n🏢 Extract department information:");
    processData(employeeData, CsvNavigateCommand.create("department", new PassThroughRule()));

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Example 2: Processing API response data */
  private static void apiResponseProcessing() throws IOException {
    logger.info("🌐 API Response Processing");
    logger.info("===========================");

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
    logger.info("📊 Extract API status:");
    processData(apiResponse, JsonNavigateCommand.create("status", new PassThroughRule()));

    // Extract user data for processing
    logger.info("\n👤 Extract user data:");
    processData(apiResponse, JsonNavigateCommand.create("data", new PassThroughRule()));

    // Format entire response for logging
    logger.info("\n📝 Format entire response:");
    processData(apiResponse, JsonNavigateCommand.createForAll(new PassThroughRule()));

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Example 3: Configuration file processing */
  private static void configurationProcessing() throws IOException {
    logger.info("⚙️ Configuration Processing");
    logger.info("============================");

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
    logger.info("🗄️ Extract database configuration:");
    processData(
        configXml, XmlNavigateCommand.create("configuration/database", new PassThroughRule()));

    // Extract server configuration
    logger.info("\n🖥️ Extract server configuration:");
    processData(
        configXml, XmlNavigateCommand.create("configuration/server", new PassThroughRule()));

    // Extract logging configuration
    logger.info("\n📊 Extract logging configuration:");
    processData(
        configXml, XmlNavigateCommand.create("configuration/logging", new PassThroughRule()));

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Example 4: Log analysis workflow */
  private static void logAnalysis() throws IOException {
    logger.info("📈 Log Analysis Workflow");
    logger.info("=========================");

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
      CsvNavigateCommand.create("level", new PassThroughRule()), // Extract log levels
      new SampleStreamCommand("level-analyzer") // Analyze log levels
    };

    logger.info("🔍 Log level analysis:");
    processData(logData, analysisPipeline);

    // Service-specific analysis
    logger.info("\n🔧 Service analysis:");
    processData(logData, CsvNavigateCommand.create("service", new PassThroughRule()));

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Example 5: Data format conversion */
  private static void dataFormatConversion() throws IOException {
    logger.info("🔄 Data Format Conversion");
    logger.info("==========================");

    String productData =
        """
        product_id,name,price,category
        P001,Laptop,999.99,Electronics
        P002,Chair,149.99,Furniture
        P003,Book,29.99,Education
        """;

    // Multi-stage conversion pipeline
    IStreamCommand[] conversionPipeline = {
      CsvNavigateCommand.create("name", new PassThroughRule()), // Extract product names
      new SampleStreamCommand("name-processor"), // Process names
      new SampleStreamCommand("format-converter") // Convert format
    };

    logger.info("🛍️ Product name conversion pipeline:");
    processData(productData, conversionPipeline);

    // Price extraction for financial analysis
    logger.info("\n💲 Price extraction:");
    processData(productData, CsvNavigateCommand.create("price", new PassThroughRule()));

    logger.info("\n" + "=".repeat(60) + "\n");
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
      logger.info("Output:");
      logger.info(result.trim());
    }
  }
}
