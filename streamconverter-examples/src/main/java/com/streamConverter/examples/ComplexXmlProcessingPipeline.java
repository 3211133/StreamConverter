package com.streamConverter.examples;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.SendHttpCommand;
import com.streamConverter.command.impl.xml.XmlNavigateCommand;
import com.streamConverter.command.rule.PassThroughRule;
import com.streamConverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Complex XML processing pipeline demonstrating: HTTPS → XML parsing → DB lookup → API call → XML
 * transformation → Response
 *
 * <p>This example showcases a real-world enterprise integration scenario where: 1. Large XML is
 * received via HTTPS 2. Specific tag values are extracted as keys 3. Database lookup is performed
 * using those keys 4. Backend API is called with the retrieved values 5. Returned XML is processed
 * with reverse transformation 6. Final response is sent back to the original requester
 */
public class ComplexXmlProcessingPipeline {

  private static final Logger logger = LoggerFactory.getLogger(ComplexXmlProcessingPipeline.class);
  private static final ExecutorService executor = Executors.newFixedThreadPool(10);

  /**
   * アプリケーションのエントリーポイント。複雑なXML処理パイプラインの例を実行します。
   *
   * @param args コマンドライン引数（使用されません）
   */
  public static void main(String[] args) {
    logger.info("🌐 Complex XML Processing Pipeline");
    logger.info("==================================");

    try {
      // Example 1: Basic pipeline demonstration
      demonstrateBasicPipeline();

      // Example 2: Full end-to-end processing
      demonstrateFullProcessing();

      // Example 3: Error handling and recovery
      demonstrateErrorHandling();

      // Example 4: Performance optimization
      demonstratePerformanceOptimization();

    } catch (Exception e) {
      logger.error("Pipeline demonstration failed: {}", e.getMessage(), e);
    } finally {
      executor.shutdown();
    }
  }

  /** Demonstrates basic pipeline structure */
  private static void demonstrateBasicPipeline() throws IOException {
    System.out.println("📋 Basic Pipeline Structure");
    System.out.println("============================");

    // Sample incoming XML with user request
    String incomingXml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <request>
          <header>
            <requestId>REQ001</requestId>
            <timestamp>2023-07-15T10:30:00Z</timestamp>
            <source>mobile-app</source>
          </header>
          <body>
            <user>
              <userId>USR123</userId>
              <sessionId>SES456</sessionId>
              <action>getUserProfile</action>
            </user>
            <parameters>
              <param name="includePreferences">true</param>
              <param name="format">json</param>
            </parameters>
          </body>
        </request>
        """;

    System.out.println("📨 Incoming XML:");
    System.out.println(incomingXml);

    // Step 1: Extract user ID for DB lookup
    System.out.println("\n🔍 Step 1: Extract user ID for DB lookup");
    IStreamCommand extractUserId =
        XmlNavigateCommand.create(
            TreePath.fromXmlPath("request/body/user/userId"), new PassThroughRule());
    processStep(incomingXml, extractUserId, "User ID extraction");

    // Step 2: Extract session ID for validation
    System.out.println("\n🔍 Step 2: Extract session ID for validation");
    IStreamCommand extractSessionId =
        XmlNavigateCommand.create(
            TreePath.fromXmlPath("request/body/user/sessionId"), new PassThroughRule());
    processStep(incomingXml, extractSessionId, "Session ID extraction");

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /** Demonstrates full end-to-end processing */
  private static void demonstrateFullProcessing() throws IOException {
    System.out.println("🔄 Full End-to-End Processing");
    System.out.println("==============================");

    // Large XML document simulation
    String largeXmlRequest = generateLargeXmlRequest();

    System.out.println("📊 Processing large XML request...");
    System.out.println("Request size: " + largeXmlRequest.length() + " characters");

    // Create comprehensive processing pipeline
    IStreamCommand[] fullPipeline = createFullProcessingPipeline();

    long startTime = System.currentTimeMillis();

    try (InputStream inputStream =
            new ByteArrayInputStream(largeXmlRequest.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      StreamConverter converter = new StreamConverter(fullPipeline);
      converter.run(inputStream, outputStream);

      long processingTime = System.currentTimeMillis() - startTime;

      String result = outputStream.toString(StandardCharsets.UTF_8);

      System.out.println("\n✅ Processing completed!");
      System.out.println("⏱️ Processing time: " + processingTime + " ms");
      System.out.println("📤 Response size: " + result.length() + " characters");

      // Show sample of result
      String[] lines = result.split("\n");
      System.out.println("\n📋 Sample response (first 10 lines):");
      for (int i = 0; i < Math.min(10, lines.length); i++) {
        System.out.println(lines[i]);
      }
      if (lines.length > 10) {
        System.out.println("... (" + (lines.length - 10) + " more lines)");
      }
    }

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /** Demonstrates error handling and recovery */
  private static void demonstrateErrorHandling() throws IOException {
    System.out.println("⚠️ Error Handling and Recovery");
    System.out.println("===============================");

    // Invalid XML for testing error handling
    String invalidXml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <request>
          <header>
            <requestId>REQ002</requestId>
            <!-- Missing closing tag for demonstration -->
          </header>
        """;

    System.out.println("🔍 Testing error handling with invalid XML...");

    try {
      IStreamCommand xmlProcessor =
          XmlNavigateCommand.create(
              TreePath.fromXmlPath("request/header/requestId"), new PassThroughRule());
      processStep(invalidXml, xmlProcessor, "Error handling test");
    } catch (Exception e) {
      System.out.println("❌ Expected error caught: " + e.getMessage());
      System.out.println("🔄 Implementing recovery strategy...");

      // Recovery: Try to process what we can
      String recoveredXml =
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <error>
            <code>INVALID_XML</code>
            <message>Request XML is malformed</message>
            <timestamp>2023-07-15T10:30:00Z</timestamp>
          </error>
          """;

      processStep(
          recoveredXml,
          XmlNavigateCommand.create(TreePath.fromXmlPath("error"), new PassThroughRule()),
          "Recovery processing");
    }

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /** Demonstrates performance optimization techniques */
  private static void demonstratePerformanceOptimization() throws IOException {
    System.out.println("⚡ Performance Optimization");
    System.out.println("============================");

    // Generate multiple requests for concurrent processing
    String[] requests = generateMultipleRequests();

    System.out.println("🔄 Processing " + requests.length + " requests concurrently...");

    long startTime = System.currentTimeMillis();

    // Process requests concurrently
    CompletableFuture<String>[] futures = new CompletableFuture[requests.length];

    for (int i = 0; i < requests.length; i++) {
      final String request = requests[i];
      final int requestId = i + 1;

      futures[i] =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return processRequestConcurrently(request, requestId);
                } catch (Exception e) {
                  return "Error processing request " + requestId + ": " + e.getMessage();
                }
              },
              executor);
    }

    // Wait for all requests to complete
    CompletableFuture.allOf(futures).join();

    long totalTime = System.currentTimeMillis() - startTime;

    System.out.println("✅ All requests processed!");
    System.out.println("⏱️ Total time: " + totalTime + " ms");
    System.out.println("📊 Average time per request: " + (totalTime / requests.length) + " ms");
    System.out.println(
        "🚀 Throughput: " + (requests.length * 1000.0 / totalTime) + " requests/second");

    // Show some results
    System.out.println("\n📋 Sample results:");
    for (int i = 0; i < Math.min(3, futures.length); i++) {
      System.out.println(
          "Request "
              + (i + 1)
              + " result: "
              + futures[i].join().substring(0, Math.min(100, futures[i].join().length()))
              + "...");
    }

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /** Creates a comprehensive processing pipeline */
  private static IStreamCommand[] createFullProcessingPipeline() {
    return new IStreamCommand[] {
      // Step 1: Extract user ID from incoming XML
      XmlNavigateCommand.create(
          TreePath.fromXmlPath("request/body/user/userId"), new PassThroughRule()),

      // Step 2: Database lookup (simulated with sample processing)
      new SampleStreamCommand("database-lookup-simulator"),

      // Step 3: API call to backend service (simulated)
      new SendHttpCommand("https://api.backend.example.com/user/profile"),

      // Step 4: Process returned XML
      XmlNavigateCommand.create(TreePath.fromXmlPath("response/data"), new PassThroughRule())
    };
  }

  /** Generates a large XML request for testing */
  private static String generateLargeXmlRequest() {
    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<request>\n");
    xml.append("  <header>\n");
    xml.append("    <requestId>REQ_LARGE_001</requestId>\n");
    xml.append("    <timestamp>2023-07-15T10:30:00Z</timestamp>\n");
    xml.append("    <source>enterprise-system</source>\n");
    xml.append("  </header>\n");
    xml.append("  <body>\n");
    xml.append("    <user>\n");
    xml.append("      <userId>USR_LARGE_123</userId>\n");
    xml.append("      <sessionId>SES_LARGE_456</sessionId>\n");
    xml.append("      <action>bulkUserOperation</action>\n");
    xml.append("    </user>\n");
    xml.append("    <data>\n");

    // Add many data elements to simulate large XML
    for (int i = 1; i <= 1000; i++) {
      xml.append("      <record>\n");
      xml.append("        <id>").append(i).append("</id>\n");
      xml.append("        <name>Record_").append(i).append("</name>\n");
      xml.append("        <value>").append(Math.random() * 1000).append("</value>\n");
      xml.append("        <status>active</status>\n");
      xml.append("      </record>\n");
    }

    xml.append("    </data>\n");
    xml.append("  </body>\n");
    xml.append("</request>");

    return xml.toString();
  }

  /** Generates multiple requests for concurrent processing */
  private static String[] generateMultipleRequests() {
    String[] requests = new String[50];

    for (int i = 0; i < requests.length; i++) {
      requests[i] =
          String.format(
              """
          <?xml version="1.0" encoding="UTF-8"?>
          <request>
            <header>
              <requestId>REQ_%03d</requestId>
              <timestamp>2023-07-15T10:30:00Z</timestamp>
              <source>load-test</source>
            </header>
            <body>
              <user>
                <userId>USR_%03d</userId>
                <sessionId>SES_%03d</sessionId>
                <action>getProfile</action>
              </user>
              <parameters>
                <param name="format">xml</param>
              </parameters>
            </body>
          </request>
          """,
              i + 1, i + 1, i + 1);
    }

    return requests;
  }

  /** Processes a single request concurrently */
  private static String processRequestConcurrently(String request, int requestId)
      throws IOException {
    try (InputStream inputStream =
            new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      // Simulate processing pipeline
      IStreamCommand[] pipeline = {
        XmlNavigateCommand.create(
            TreePath.fromXmlPath("request/body/user/userId"), new PassThroughRule())
      };

      StreamConverter converter = new StreamConverter(pipeline);
      converter.run(inputStream, outputStream);

      return "Request "
          + requestId
          + " processed: "
          + outputStream.toString(StandardCharsets.UTF_8);
    }
  }

  /** Helper method to process a single step and show results */
  private static void processStep(String inputXml, IStreamCommand command, String stepName)
      throws IOException {
    try (InputStream inputStream =
            new ByteArrayInputStream(inputXml.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      StreamConverter converter = new StreamConverter(new IStreamCommand[] {command});
      converter.run(inputStream, outputStream);

      String result = outputStream.toString(StandardCharsets.UTF_8);
      System.out.println("✅ " + stepName + " result:");
      System.out.println(result.trim());
    }
  }
}
