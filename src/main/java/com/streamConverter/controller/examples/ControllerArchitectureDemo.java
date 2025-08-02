package com.streamConverter.controller.examples;

import com.streamConverter.CommandResult;
import com.streamConverter.controller.CsvProcessingController;
import com.streamConverter.controller.IStreamController;
import com.streamConverter.controller.JsonProcessingController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstration of the Controller Architecture implementing the ideal pattern: External Systems →
 * Controller → StreamConverter → Commands
 *
 * <p>This demo shows how the Controller layer provides a clean separation of concerns:
 *
 * <ul>
 *   <li>Controllers manage command configuration and external I/O
 *   <li>StreamConverter handles command orchestration and stream processing
 *   <li>Commands focus purely on stream transformation logic
 * </ul>
 *
 * <p>This architecture replaces the direct usage patterns seen in Main.java and Examples, providing
 * better abstraction and easier integration with external systems.
 *
 * @author StreamConverter Team
 * @version 1.0
 * @since 1.0
 */
public class ControllerArchitectureDemo {

  private static final Logger log = LoggerFactory.getLogger(ControllerArchitectureDemo.class);

  /**
   * Main demonstration method showing various controller usage patterns.
   *
   * @param args command line arguments (not used)
   */
  public static void main(String[] args) {
    log.info("=== Controller Architecture Demonstration ===");
    log.info(
        "Implementing ideal architecture: External → Controller → StreamConverter → Commands\n");

    try {
      // Demo 1: Basic CSV Processing
      demonstrateCsvProcessing();

      // Demo 2: Advanced JSON Processing
      demonstrateJsonProcessing();

      // Demo 3: External System Integration
      demonstrateExternalSystemIntegration();

      // Demo 4: Controller Configuration Comparison
      demonstrateConfigurationComparison();

      log.info("✅ All controller architecture demonstrations completed successfully!");

    } catch (Exception e) {
      log.error("❌ Demo failed: {}", e.getMessage(), e);
    }
  }

  /** Demonstrates CSV processing with different controller configurations. */
  private static void demonstrateCsvProcessing() throws IOException {
    log.info("📊 CSV Processing Controller Demo");
    log.info("==================================");

    String csvData =
        "id,name,email,department,salary\n"
            + "1,John Doe,john@example.com,Engineering,75000\n"
            + "2,Jane Smith,jane@example.com,Marketing,65000\n"
            + "3,Bob Johnson,bob@example.com,Sales,70000\n";

    // Example 1: Simple column extraction
    log.info("1. Column Extraction Controller:");
    IStreamController controller1 = CsvProcessingController.forColumnExtraction("name");
    processWithController(csvData, controller1);

    // Example 2: Complex processing pipeline
    log.info("\n2. Complex Processing Controller:");
    IStreamController controller2 =
        CsvProcessingController.forComplexProcessing("email", "email-validator");
    processWithController(csvData, controller2);

    // Example 3: Pass-through processing
    log.info("\n3. Pass-through Processing Controller:");
    IStreamController controller3 = CsvProcessingController.forPassThrough();
    processWithController(csvData, controller3);

    log.info("\n" + "=".repeat(60) + "\n");
  }

  /** Demonstrates advanced JSON processing scenarios. */
  private static void demonstrateJsonProcessing() throws IOException {
    log.info("🔍 JSON Processing Controller Demo");
    log.info("===================================");

    String jsonData =
        """
            {
                "users": [
                    {"id": 1, "name": "John", "profile": {"email": "john@example.com", "role": "admin"}},
                    {"id": 2, "name": "Jane", "profile": {"email": "jane@example.com", "role": "user"}}
                ],
                "metadata": {"total": 2, "version": "1.0"}
            }
            """;

    // Example 1: Simple property extraction
    log.info("1. Property Extraction Controller:");
    IStreamController controller1 =
        JsonProcessingController.forPropertyExtraction("metadata", false);
    processWithController(jsonData, controller1);

    // Example 2: Property extraction with validation
    log.info("\n2. Validated Property Extraction Controller:");
    IStreamController controller2 = JsonProcessingController.forPropertyExtraction("users", true);
    processWithController(jsonData, controller2);

    // Example 3: Multi-stage transformation
    log.info("\n3. Multi-stage Transformation Controller:");
    IStreamController controller3 =
        JsonProcessingController.forTransformation(
            "users", "user-processor", "role-extractor", "final-formatter");
    processWithController(jsonData, controller3);

    // Example 4: JSON formatting only
    log.info("\n4. JSON Formatting Controller:");
    IStreamController controller4 = JsonProcessingController.forFormatting();
    processWithController(jsonData, controller4);

    log.info("\n" + "=".repeat(60) + "\n");
  }

  /** Demonstrates how controllers integrate with external systems. */
  private static void demonstrateExternalSystemIntegration() throws IOException {
    log.info("🌐 External System Integration Demo");
    log.info("====================================");

    // Simulate external system integration
    String csvData = "product,price,category\nLaptop,999.99,Electronics\nBook,19.99,Education\n";

    log.info("1. File-based Integration:");
    demonstrateFileBasedIntegration(csvData);

    log.info("\n2. Stream-based Integration:");
    demonstrateStreamBasedIntegration(csvData);

    log.info("\n3. External System Routing:");
    demonstrateExternalSystemRouting();

    log.info("\n" + "=".repeat(60) + "\n");
  }

  /** Demonstrates configuration comparison between old and new approaches. */
  private static void demonstrateConfigurationComparison() {
    log.info("⚖️ Configuration Approach Comparison");
    log.info("====================================");

    log.info("OLD APPROACH (Main.java style):");
    log.info("  ❌ External systems directly create StreamConverter");
    log.info("  ❌ Command configuration scattered in client code");
    log.info("  ❌ No standardized I/O management");
    log.info("  ❌ Tight coupling between external systems and StreamConverter");
    log.info("  Code example:");
    log.info("    IStreamCommand[] commands = {new CsvNavigateCommand(\"name\")};");
    log.info("    StreamConverter converter = new StreamConverter(commands);");
    log.info("    converter.run(inputStream, outputStream);");

    log.info("\nNEW APPROACH (Controller layer):");
    log.info("  ✅ Controllers encapsulate command configuration");
    log.info("  ✅ Standardized I/O management and error handling");
    log.info("  ✅ Clean separation of concerns");
    log.info("  ✅ Easy external system integration");
    log.info("  Code example:");
    log.info(
        "    IStreamController controller = CsvProcessingController.forColumnExtraction(\"name\");");
    log.info("    List<CommandResult> results = controller.process(inputStream, outputStream);");

    log.info("\nARCHITECTURE BENEFITS:");
    log.info("  🏗️ Ideal Architecture: External → Controller → StreamConverter → Commands");
    log.info("  🔧 Controllers hide complex configuration rules");
    log.info("  📡 Controllers manage external I/O connections");
    log.info("  🎯 StreamConverter focuses on command orchestration");
    log.info("  ⚡ Commands focus purely on stream transformation");

    log.info("\n" + "=".repeat(60) + "\n");
  }

  /** Helper method to process data with a controller and display results. */
  private static void processWithController(String inputData, IStreamController controller)
      throws IOException {
    log.info("Controller: {}", controller.getClass().getSimpleName());
    log.info("Configuration: {}", controller.getConfigurationDescription());
    log.info(
        "Input Type: {} → Output Type: {}",
        controller.getInputDataType(),
        controller.getOutputDataType());

    try (InputStream inputStream =
            new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      long startTime = System.currentTimeMillis();
      List<CommandResult> results = controller.process(inputStream, outputStream);
      long duration = System.currentTimeMillis() - startTime;

      String output = outputStream.toString(StandardCharsets.UTF_8);

      log.info("Processing completed in {}ms", duration);
      log.info("Commands executed: {}", results.size());
      log.info(
          "Output preview: {}", output.length() > 100 ? output.substring(0, 100) + "..." : output);
    }
  }

  /** Demonstrates file-based integration with external systems. */
  private static void demonstrateFileBasedIntegration(String csvData) throws IOException {
    // Simulate external system providing file input
    String tempInputFile = "/tmp/input.csv";
    String tempOutputFile = "/tmp/output.txt";

    // External system writes input file
    try (FileOutputStream fos = new FileOutputStream(tempInputFile)) {
      fos.write(csvData.getBytes(StandardCharsets.UTF_8));
    }

    // Controller processes file streams
    IStreamController controller = CsvProcessingController.forColumnExtraction("product");

    try (FileInputStream inputStream = new FileInputStream(tempInputFile);
        FileOutputStream outputStream = new FileOutputStream(tempOutputFile)) {

      log.info("Processing file: {} → {}", tempInputFile, tempOutputFile);
      log.info("Controller: {}", controller.getConfigurationDescription());

      List<CommandResult> results = controller.process(inputStream, outputStream);
      log.info("File processing completed with {} command results", results.size());
    }

    // Clean up temporary files
    new java.io.File(tempInputFile).delete();
    new java.io.File(tempOutputFile).delete();
  }

  /** Demonstrates stream-based integration with external systems. */
  private static void demonstrateStreamBasedIntegration(String csvData) throws IOException {
    // Simulate external system providing streams
    ExternalSystemSimulator externalSystem = new ExternalSystemSimulator();

    try (InputStream inputStream = externalSystem.getDataStream(csvData);
        OutputStream outputStream = externalSystem.getOutputSink()) {

      IStreamController controller =
          CsvProcessingController.forComplexProcessing("category", "categorizer");

      log.info("Processing external system streams");
      log.info("Controller: {}", controller.getConfigurationDescription());

      List<CommandResult> results = controller.process(inputStream, outputStream);
      log.info("Stream processing completed with {} command results", results.size());

      // External system can examine results
      externalSystem.handleResults(results);
    }
  }

  /** Demonstrates external system routing based on data types. */
  private static void demonstrateExternalSystemRouting() {
    log.info("External System Routing Examples:");

    // Show how external systems can route to appropriate controllers
    IStreamController[] controllers = {
      CsvProcessingController.forColumnExtraction("data"),
      JsonProcessingController.forPropertyExtraction("data", false),
      CsvProcessingController.forComplexProcessing("info", "processor")
    };

    for (IStreamController controller : controllers) {
      log.info(
          "  {} processes {} → {} ({})",
          controller.getClass().getSimpleName(),
          controller.getInputDataType(),
          controller.getOutputDataType(),
          controller.isConfigured() ? "ready" : "needs configuration");
    }
  }

  /** Simulated external system for demonstration purposes. */
  private static class ExternalSystemSimulator {

    public InputStream getDataStream(String data) {
      return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    }

    public OutputStream getOutputSink() {
      return new ByteArrayOutputStream();
    }

    public void handleResults(List<CommandResult> results) {
      log.info("External system received {} results from processing", results.size());
      for (CommandResult result : results) {
        log.info(
            "  Command '{}' executed in {}ms",
            result.getCommandName(),
            result.getExecutionTimeMillis());
      }
    }
  }
}
