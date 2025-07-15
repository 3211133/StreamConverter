package com.streamConverter.examples;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.XmlNavigateCommand;
import com.streamConverter.command.impl.SendHttpCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
// import com.streamConverter.command.rule.DatabaseFetchRule; // Not accessible from outside package
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise Integration Patterns using StreamConverter.
 * 
 * <p>This demonstrates common enterprise integration scenarios:
 * - Message Translation
 * - Content-Based Router
 * - Scatter-Gather
 * - Request-Reply
 * - Message Filter
 * - Dead Letter Queue handling
 */
public class EnterpriseIntegrationPatterns {
  
  private static final ExecutorService executorService = Executors.newFixedThreadPool(20);
  
  public static void main(String[] args) {
    System.out.println("🏢 Enterprise Integration Patterns");
    System.out.println("===================================\n");
    
    try {
      // Pattern 1: Message Translation
      demonstrateMessageTranslation();
      
      // Pattern 2: Content-Based Router
      demonstrateContentBasedRouter();
      
      // Pattern 3: Scatter-Gather
      demonstrateScatterGather();
      
      // Pattern 4: Request-Reply with timeout
      demonstrateRequestReply();
      
      // Pattern 5: Message Filter
      demonstrateMessageFilter();
      
      // Pattern 6: Dead Letter Queue
      demonstrateDeadLetterQueue();
      
    } catch (Exception e) {
      System.err.println("Enterprise pattern demonstration failed: " + e.getMessage());
      e.printStackTrace();
    } finally {
      shutdownExecutor();
    }
  }
  
  /**
   * Pattern 1: Message Translation
   * Transforms message format from one system to another
   */
  private static void demonstrateMessageTranslation() throws IOException {
    System.out.println("🔄 Message Translation Pattern");
    System.out.println("===============================");
    
    // Input: Legacy system XML format
    String legacyXml = """
        <?xml version="1.0"?>
        <LegacyOrder>
          <OrderID>ORD001</OrderID>
          <Customer>
            <CustID>CUST123</CustID>
            <Name>John Doe</Name>
            <Email>john@example.com</Email>
          </Customer>
          <Items>
            <Item>
              <SKU>SKU001</SKU>
              <Quantity>2</Quantity>
              <Price>29.99</Price>
            </Item>
          </Items>
        </LegacyOrder>
        """;
    
    System.out.println("📨 Legacy XML Input:");
    System.out.println(legacyXml);
    
    // Translation pipeline: Extract key fields and transform
    IStreamCommand[] translationPipeline = {
        new XmlNavigateCommand("LegacyOrder/OrderID"),
        new SampleStreamCommand("order-translator"),
        new SampleStreamCommand("format-modernizer")
    };
    
    System.out.println("\n🔄 Translating to modern format...");
    executePattern(legacyXml, translationPipeline, "Message Translation");
    
    System.out.println("\n" + "=".repeat(60) + "\n");
  }
  
  /**
   * Pattern 2: Content-Based Router
   * Routes messages based on content
   */
  private static void demonstrateContentBasedRouter() throws IOException {
    System.out.println("🛤️ Content-Based Router Pattern");
    System.out.println("=================================");
    
    // Different message types
    String[] messages = {
        """
        <?xml version="1.0"?>
        <Message>
          <Type>ORDER</Type>
          <Priority>HIGH</Priority>
          <Data>Order processing required</Data>
        </Message>
        """,
        """
        <?xml version="1.0"?>
        <Message>
          <Type>NOTIFICATION</Type>
          <Priority>LOW</Priority>
          <Data>User notification</Data>
        </Message>
        """,
        """
        <?xml version="1.0"?>
        <Message>
          <Type>ALERT</Type>
          <Priority>CRITICAL</Priority>
          <Data>System alert</Data>
        </Message>
        """
    };
    
    System.out.println("📬 Processing different message types...");
    
    for (int i = 0; i < messages.length; i++) {
      System.out.println("\n📨 Message " + (i + 1) + ":");
      
      // Route based on message type
      IStreamCommand[] routingPipeline = {
          new XmlNavigateCommand("Message/Type"),
          new SampleStreamCommand("content-router-" + (i + 1))
      };
      
      executePattern(messages[i], routingPipeline, "Content-Based Routing");
    }
    
    System.out.println("\n" + "=".repeat(60) + "\n");
  }
  
  /**
   * Pattern 3: Scatter-Gather
   * Distributes request to multiple services and aggregates responses
   */
  private static void demonstrateScatterGather() throws IOException {
    System.out.println("📡 Scatter-Gather Pattern");
    System.out.println("==========================");
    
    String requestXml = """
        <?xml version="1.0"?>
        <PriceRequest>
          <ProductID>PROD123</ProductID>
          <Quantity>100</Quantity>
          <RequestID>REQ001</RequestID>
        </PriceRequest>
        """;
    
    System.out.println("📨 Price Request:");
    System.out.println(requestXml);
    
    System.out.println("\n🚀 Scattering request to multiple suppliers...");
    
    // Simulate multiple supplier endpoints
    String[] suppliers = {"supplier-a", "supplier-b", "supplier-c"};
    CompletableFuture<String>[] futures = new CompletableFuture[suppliers.length];
    
    for (int i = 0; i < suppliers.length; i++) {
      final String supplier = suppliers[i];
      final int supplierIndex = i;
      
      futures[i] = CompletableFuture.supplyAsync(() -> {
        try {
          // Simulate processing with each supplier
          IStreamCommand[] supplierPipeline = {
              new XmlNavigateCommand("PriceRequest/ProductID"),
              new SampleStreamCommand("supplier-" + supplier),
              new SampleStreamCommand("price-calculator-" + supplierIndex)
          };
          
          return processSupplierRequest(requestXml, supplierPipeline, supplier);
        } catch (Exception e) {
          return "Error from " + supplier + ": " + e.getMessage();
        }
      }, executorService);
    }
    
    // Gather results
    System.out.println("\n📥 Gathering responses...");
    for (int i = 0; i < futures.length; i++) {
      String result = futures[i].join();
      System.out.println("✅ " + suppliers[i] + " response: " + result.trim());
    }
    
    System.out.println("\n🎯 Aggregating best price...");
    System.out.println("✅ Best price selected and response prepared");
    
    System.out.println("\n" + "=".repeat(60) + "\n");
  }
  
  /**
   * Pattern 4: Request-Reply with timeout
   * Handles request-reply with timeout management
   */
  private static void demonstrateRequestReply() throws IOException {
    System.out.println("🔄 Request-Reply Pattern");
    System.out.println("=========================");
    
    String requestXml = """
        <?xml version="1.0"?>
        <ServiceRequest>
          <RequestID>REQ002</RequestID>
          <Service>UserValidation</Service>
          <UserID>USR456</UserID>
        </ServiceRequest>
        """;
    
    System.out.println("📨 Service Request:");
    System.out.println(requestXml);
    
    System.out.println("\n⏱️ Processing with timeout...");
    
    // Simulate request processing with timeout
    CompletableFuture<String> requestFuture = CompletableFuture.supplyAsync(() -> {
      try {
        // Simulate processing delay
        Thread.sleep(1000);
        
        IStreamCommand[] requestPipeline = {
            new XmlNavigateCommand("ServiceRequest/UserID"),
            new SampleStreamCommand("user-validator"),
            new SampleStreamCommand("response-formatter")
        };
        
        return processWithTimeout(requestXml, requestPipeline);
      } catch (Exception e) {
        return "Request processing error: " + e.getMessage();
      }
    }, executorService);
    
    try {
      // Wait for result with timeout
      String result = requestFuture.get(5, TimeUnit.SECONDS);
      System.out.println("✅ Request processed successfully:");
      System.out.println(result);
    } catch (Exception e) {
      System.out.println("⏰ Request timeout or error: " + e.getMessage());
      // Implement timeout handling
      System.out.println("🔄 Implementing timeout recovery...");
    }
    
    System.out.println("\n" + "=".repeat(60) + "\n");
  }
  
  /**
   * Pattern 5: Message Filter
   * Filters messages based on criteria
   */
  private static void demonstrateMessageFilter() throws IOException {
    System.out.println("🔍 Message Filter Pattern");
    System.out.println("==========================");
    
    String[] messages = {
        """
        <?xml version="1.0"?>
        <LogEntry>
          <Level>ERROR</Level>
          <Message>Database connection failed</Message>
          <Timestamp>2023-07-15T10:30:00Z</Timestamp>
        </LogEntry>
        """,
        """
        <?xml version="1.0"?>
        <LogEntry>
          <Level>INFO</Level>
          <Message>User login successful</Message>
          <Timestamp>2023-07-15T10:31:00Z</Timestamp>
        </LogEntry>
        """,
        """
        <?xml version="1.0"?>
        <LogEntry>
          <Level>ERROR</Level>
          <Message>Payment processing failed</Message>
          <Timestamp>2023-07-15T10:32:00Z</Timestamp>
        </LogEntry>
        """
    };
    
    System.out.println("📋 Filtering ERROR level messages...");
    
    for (int i = 0; i < messages.length; i++) {
      // Check if message should be filtered
      String level = extractLevel(messages[i]);
      
      if ("ERROR".equals(level)) {
        System.out.println("\n🚨 Processing ERROR message " + (i + 1) + ":");
        
        IStreamCommand[] filterPipeline = {
            new XmlNavigateCommand("LogEntry/Message"),
            new SampleStreamCommand("error-processor"),
            new SampleStreamCommand("alert-sender")
        };
        
        executePattern(messages[i], filterPipeline, "Error Processing");
      } else {
        System.out.println("\n✅ Filtered out " + level + " message " + (i + 1));
      }
    }
    
    System.out.println("\n" + "=".repeat(60) + "\n");
  }
  
  /**
   * Pattern 6: Dead Letter Queue
   * Handles failed messages
   */
  private static void demonstrateDeadLetterQueue() throws IOException {
    System.out.println("💀 Dead Letter Queue Pattern");
    System.out.println("=============================");
    
    String[] problematicMessages = {
        """
        <?xml version="1.0"?>
        <BadMessage>
          <InvalidField>This will cause processing error</InvalidField>
          <!-- Missing required fields -->
        </BadMessage>
        """,
        """
        <?xml version="1.0"?>
        <Message>
          <Type>INVALID_TYPE</Type>
          <Data>Unknown message type</Data>
        </Message>
        """,
        """
        This is not even valid XML!
        """
    };
    
    System.out.println("⚠️ Processing problematic messages...");
    
    for (int i = 0; i < problematicMessages.length; i++) {
      System.out.println("\n📨 Message " + (i + 1) + ":");
      
      try {
        IStreamCommand[] processingPipeline = {
            new XmlNavigateCommand("Message/Type"),
            new SampleStreamCommand("message-processor")
        };
        
        executePattern(problematicMessages[i], processingPipeline, "Normal Processing");
        System.out.println("✅ Message processed successfully");
        
      } catch (Exception e) {
        System.out.println("❌ Processing failed: " + e.getMessage());
        System.out.println("📤 Sending to Dead Letter Queue...");
        
        // Send to Dead Letter Queue
        sendToDeadLetterQueue(problematicMessages[i], e.getMessage());
      }
    }
    
    System.out.println("\n" + "=".repeat(60) + "\n");
  }
  
  /**
   * Helper method to execute a pattern
   */
  private static void executePattern(String inputXml, IStreamCommand[] pipeline, String patternName) throws IOException {
    try (InputStream inputStream = new ByteArrayInputStream(inputXml.getBytes(StandardCharsets.UTF_8));
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      
      StreamConverter converter = new StreamConverter(pipeline);
      converter.run(inputStream, outputStream);
      
      String result = outputStream.toString(StandardCharsets.UTF_8);
      System.out.println("✅ " + patternName + " result:");
      System.out.println(result.trim());
    }
  }
  
  /**
   * Helper method to process supplier requests
   */
  private static String processSupplierRequest(String requestXml, IStreamCommand[] pipeline, String supplier) throws IOException {
    try (InputStream inputStream = new ByteArrayInputStream(requestXml.getBytes(StandardCharsets.UTF_8));
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      
      StreamConverter converter = new StreamConverter(pipeline);
      converter.run(inputStream, outputStream);
      
      return outputStream.toString(StandardCharsets.UTF_8);
    }
  }
  
  /**
   * Helper method to process with timeout
   */
  private static String processWithTimeout(String requestXml, IStreamCommand[] pipeline) throws IOException {
    try (InputStream inputStream = new ByteArrayInputStream(requestXml.getBytes(StandardCharsets.UTF_8));
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      
      StreamConverter converter = new StreamConverter(pipeline);
      converter.run(inputStream, outputStream);
      
      return outputStream.toString(StandardCharsets.UTF_8);
    }
  }
  
  /**
   * Helper method to extract level from log message
   */
  private static String extractLevel(String logXml) {
    // Simple extraction for demo purposes
    if (logXml.contains("<Level>ERROR</Level>")) {
      return "ERROR";
    } else if (logXml.contains("<Level>INFO</Level>")) {
      return "INFO";
    }
    return "UNKNOWN";
  }
  
  /**
   * Helper method to send message to Dead Letter Queue
   */
  private static void sendToDeadLetterQueue(String message, String error) {
    System.out.println("💀 Dead Letter Queue Entry:");
    System.out.println("   Message: " + message.substring(0, Math.min(100, message.length())) + "...");
    System.out.println("   Error: " + error);
    System.out.println("   Timestamp: " + java.time.Instant.now());
    System.out.println("   Status: QUEUED_FOR_MANUAL_REVIEW");
  }
  
  /**
   * Helper method to shutdown executor
   */
  private static void shutdownExecutor() {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
    }
  }
}