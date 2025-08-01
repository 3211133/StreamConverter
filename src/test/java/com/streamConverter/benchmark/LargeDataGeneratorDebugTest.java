package com.streamConverter.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Debug test to investigate the LargeDataGenerator size discrepancy issue.
 *
 * <p>This test creates small streams (1KB, 5KB) for XML, JSON, CSV formats, measures actual vs
 * expected sizes, logs detailed information about header/footer/record sizes, and verifies document
 * well-formedness.
 */
public class LargeDataGeneratorDebugTest {

  @TempDir Path tempDir;

  @Test
  public void debugXmlSizeDiscrepancy() throws Exception {
    System.out.println("\n=== XML Size Debug Analysis ===");

    // Test small sizes to understand the pattern
    long[] testSizes = {1024, 5120}; // 1KB, 5KB

    for (long targetSize : testSizes) {
      System.out.printf(
          "\n--- Testing XML with target size: %d bytes (%.1f KB) ---\n",
          targetSize, targetSize / 1024.0);

      debugXmlGeneration(targetSize);
    }
  }

  @Test
  public void debugJsonSizeDiscrepancy() throws Exception {
    System.out.println("\n=== JSON Size Debug Analysis ===");

    long[] testSizes = {1024, 5120}; // 1KB, 5KB

    for (long targetSize : testSizes) {
      System.out.printf(
          "\n--- Testing JSON with target size: %d bytes (%.1f KB) ---\n",
          targetSize, targetSize / 1024.0);

      debugJsonGeneration(targetSize);
    }
  }

  @Test
  public void debugCsvSizeDiscrepancy() throws Exception {
    System.out.println("\n=== CSV Size Debug Analysis ===");

    long[] testSizes = {1024, 5120}; // 1KB, 5KB

    for (long targetSize : testSizes) {
      System.out.printf(
          "\n--- Testing CSV with target size: %d bytes (%.1f KB) ---\n",
          targetSize, targetSize / 1024.0);

      debugCsvGeneration(targetSize);
    }
  }

  private void debugXmlGeneration(long targetSize) throws Exception {
    // Test file-based generation
    Path xmlFile = LargeDataGenerator.generateLargeXmlFile(targetSize);
    long actualFileSize = Files.size(xmlFile);

    System.out.printf("File-based generation:\n");
    System.out.printf("  Target size: %d bytes\n", targetSize);
    System.out.printf("  Actual size: %d bytes\n", actualFileSize);
    System.out.printf(
        "  Difference: %d bytes (%.1f%%)\n",
        actualFileSize - targetSize, ((double) (actualFileSize - targetSize) / targetSize) * 100);

    // Test stream-based generation
    InputStream xmlStream = LargeDataGenerator.createLargeDataStream("XML", targetSize);
    byte[] streamData = readStreamCompletely(xmlStream);
    long actualStreamSize = streamData.length;

    System.out.printf("Stream-based generation:\n");
    System.out.printf("  Target size: %d bytes\n", targetSize);
    System.out.printf("  Actual size: %d bytes\n", actualStreamSize);
    System.out.printf(
        "  Difference: %d bytes (%.1f%%)\n",
        actualStreamSize - targetSize,
        ((double) (actualStreamSize - targetSize) / targetSize) * 100);

    // Analyze XML structure
    analyzeXmlStructure(streamData);

    // Verify well-formedness
    verifyXmlWellFormedness(streamData);

    // Clean up
    Files.deleteIfExists(xmlFile);
  }

  private void debugJsonGeneration(long targetSize) throws Exception {
    // Test file-based generation
    Path jsonFile = LargeDataGenerator.generateLargeJsonFile(targetSize);
    long actualFileSize = Files.size(jsonFile);

    System.out.printf("File-based generation:\n");
    System.out.printf("  Target size: %d bytes\n", targetSize);
    System.out.printf("  Actual size: %d bytes\n", actualFileSize);
    System.out.printf(
        "  Difference: %d bytes (%.1f%%)\n",
        actualFileSize - targetSize, ((double) (actualFileSize - targetSize) / targetSize) * 100);

    // Test stream-based generation
    InputStream jsonStream = LargeDataGenerator.createLargeDataStream("JSON", targetSize);
    byte[] streamData = readStreamCompletely(jsonStream);
    long actualStreamSize = streamData.length;

    System.out.printf("Stream-based generation:\n");
    System.out.printf("  Target size: %d bytes\n", targetSize);
    System.out.printf("  Actual size: %d bytes\n", actualStreamSize);
    System.out.printf(
        "  Difference: %d bytes (%.1f%%)\n",
        actualStreamSize - targetSize,
        ((double) (actualStreamSize - targetSize) / targetSize) * 100);

    // Analyze JSON structure
    analyzeJsonStructure(streamData);

    // Verify well-formedness
    verifyJsonWellFormedness(streamData);

    // Clean up
    Files.deleteIfExists(jsonFile);
  }

  private void debugCsvGeneration(long targetSize) throws Exception {
    // Test file-based generation
    Path csvFile = LargeDataGenerator.generateLargeCsvFile(targetSize);
    long actualFileSize = Files.size(csvFile);

    System.out.printf("File-based generation:\n");
    System.out.printf("  Target size: %d bytes\n", targetSize);
    System.out.printf("  Actual size: %d bytes\n", actualFileSize);
    System.out.printf(
        "  Difference: %d bytes (%.1f%%)\n",
        actualFileSize - targetSize, ((double) (actualFileSize - targetSize) / targetSize) * 100);

    // Test stream-based generation
    InputStream csvStream = LargeDataGenerator.createLargeDataStream("CSV", targetSize);
    byte[] streamData = readStreamCompletely(csvStream);
    long actualStreamSize = streamData.length;

    System.out.printf("Stream-based generation:\n");
    System.out.printf("  Target size: %d bytes\n", targetSize);
    System.out.printf("  Actual size: %d bytes\n", actualStreamSize);
    System.out.printf(
        "  Difference: %d bytes (%.1f%%)\n",
        actualStreamSize - targetSize,
        ((double) (actualStreamSize - targetSize) / targetSize) * 100);

    // Analyze CSV structure
    analyzeCsvStructure(streamData);

    // Clean up
    Files.deleteIfExists(csvFile);
  }

  private byte[] readStreamCompletely(InputStream stream) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = new byte[8192];
    int bytesRead;

    while ((bytesRead = stream.read(data)) != -1) {
      buffer.write(data, 0, bytesRead);
    }

    stream.close();
    return buffer.toByteArray();
  }

  private void analyzeXmlStructure(byte[] xmlData) throws Exception {
    String xmlContent = new String(xmlData, "UTF-8");

    // Find header
    String xmlDeclaration = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    String rootStart = "<orders>\n";
    String rootEnd = "</orders>\n";

    int headerSize = xmlDeclaration.getBytes("UTF-8").length + rootStart.getBytes("UTF-8").length;
    int footerSize = rootEnd.getBytes("UTF-8").length;

    System.out.printf("XML Structure Analysis:\n");
    System.out.printf("  Header size: %d bytes\n", headerSize);
    System.out.printf("  Footer size: %d bytes\n", footerSize);
    System.out.printf("  Total overhead: %d bytes\n", headerSize + footerSize);

    // Count records
    int recordCount = countOccurrences(xmlContent, "<order id=");
    System.out.printf("  Record count: %d\n", recordCount);

    if (recordCount > 0) {
      int contentSize = xmlData.length - headerSize - footerSize;
      double avgRecordSize = (double) contentSize / recordCount;
      System.out.printf("  Average record size: %.1f bytes\n", avgRecordSize);
    }

    // Show first few lines for debugging
    String[] lines = xmlContent.split("\n");
    System.out.printf("  First 5 lines:\n");
    for (int i = 0; i < Math.min(5, lines.length); i++) {
      System.out.printf("    Line %d: %s\n", i + 1, lines[i]);
    }

    System.out.printf("  Last 3 lines:\n");
    for (int i = Math.max(0, lines.length - 3); i < lines.length; i++) {
      System.out.printf("    Line %d: %s\n", i + 1, lines[i]);
    }
  }

  private void analyzeJsonStructure(byte[] jsonData) throws Exception {
    String jsonContent = new String(jsonData, "UTF-8");

    String jsonStart = "{\n  \"orders\": [\n";
    String jsonEnd = "\n  ]\n}\n";

    int headerSize = jsonStart.getBytes("UTF-8").length;
    int footerSize = jsonEnd.getBytes("UTF-8").length;

    System.out.printf("JSON Structure Analysis:\n");
    System.out.printf("  Header size: %d bytes\n", headerSize);
    System.out.printf("  Footer size: %d bytes\n", footerSize);
    System.out.printf("  Total overhead: %d bytes\n", headerSize + footerSize);

    // Count records (look for "id": pattern)
    int recordCount = countOccurrences(jsonContent, "\"id\":");
    System.out.printf("  Record count: %d\n", recordCount);

    if (recordCount > 0) {
      int contentSize = jsonData.length - headerSize - footerSize;
      double avgRecordSize = (double) contentSize / recordCount;
      System.out.printf("  Average record size: %.1f bytes\n", avgRecordSize);
    }

    // Count separators
    int separatorCount = countOccurrences(jsonContent, ",\n");
    System.out.printf("  Separator count: %d\n", separatorCount);

    // Show first few lines for debugging
    String[] lines = jsonContent.split("\n");
    System.out.printf("  First 5 lines:\n");
    for (int i = 0; i < Math.min(5, lines.length); i++) {
      System.out.printf("    Line %d: %s\n", i + 1, lines[i]);
    }

    System.out.printf("  Last 3 lines:\n");
    for (int i = Math.max(0, lines.length - 3); i < lines.length; i++) {
      System.out.printf("    Line %d: %s\n", i + 1, lines[i]);
    }
  }

  private void analyzeCsvStructure(byte[] csvData) throws Exception {
    String csvContent = new String(csvData, "UTF-8");

    String header = "id,name,city,product,quantity,price,timestamp\n";
    int headerSize = header.getBytes("UTF-8").length;

    System.out.printf("CSV Structure Analysis:\n");
    System.out.printf("  Header size: %d bytes\n", headerSize);
    System.out.printf("  Footer size: 0 bytes (CSV has no footer)\n");
    System.out.printf("  Total overhead: %d bytes\n", headerSize);

    // Count records (lines minus header)
    String[] lines = csvContent.split("\n");
    int recordCount = lines.length - 1; // minus header
    if (lines[lines.length - 1].trim().isEmpty()) {
      recordCount--; // minus empty last line if present
    }

    System.out.printf("  Record count: %d\n", recordCount);

    if (recordCount > 0) {
      int contentSize = csvData.length - headerSize;
      double avgRecordSize = (double) contentSize / recordCount;
      System.out.printf("  Average record size: %.1f bytes\n", avgRecordSize);
    }

    // Show first few lines for debugging
    System.out.printf("  First 3 lines:\n");
    for (int i = 0; i < Math.min(3, lines.length); i++) {
      System.out.printf("    Line %d: %s\n", i + 1, lines[i]);
    }

    if (lines.length > 3) {
      System.out.printf("  Last 2 lines:\n");
      for (int i = Math.max(0, lines.length - 2); i < lines.length; i++) {
        System.out.printf("    Line %d: %s\n", i + 1, lines[i]);
      }
    }
  }

  private void verifyXmlWellFormedness(byte[] xmlData) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();

      // Parse the XML to check well-formedness
      builder.parse(new java.io.ByteArrayInputStream(xmlData));

      System.out.printf("  XML Well-formedness: VALID\n");
    } catch (Exception e) {
      System.out.printf("  XML Well-formedness: INVALID - %s\n", e.getMessage());
    }
  }

  private void verifyJsonWellFormedness(byte[] jsonData) {
    try {
      String jsonContent = new String(jsonData, "UTF-8");

      // Basic JSON validation - check balanced braces and brackets
      int braceCount = 0;
      int bracketCount = 0;
      boolean inString = false;
      boolean escaped = false;

      for (char c : jsonContent.toCharArray()) {
        if (escaped) {
          escaped = false;
          continue;
        }

        if (c == '\\') {
          escaped = true;
          continue;
        }

        if (c == '"') {
          inString = !inString;
          continue;
        }

        if (!inString) {
          if (c == '{') braceCount++;
          else if (c == '}') braceCount--;
          else if (c == '[') bracketCount++;
          else if (c == ']') bracketCount--;
        }
      }

      if (braceCount == 0 && bracketCount == 0) {
        System.out.printf("  JSON Well-formedness: VALID (balanced braces/brackets)\n");
      } else {
        System.out.printf(
            "  JSON Well-formedness: INVALID (unbalanced - braces: %d, brackets: %d)\n",
            braceCount, bracketCount);
      }
    } catch (Exception e) {
      System.out.printf("  JSON Well-formedness: ERROR - %s\n", e.getMessage());
    }
  }

  private int countOccurrences(String text, String pattern) {
    int count = 0;
    int index = 0;

    while ((index = text.indexOf(pattern, index)) != -1) {
      count++;
      index += pattern.length();
    }

    return count;
  }

  @Test
  public void debugSingleRecordSizes() throws Exception {
    System.out.println("\n=== Single Record Size Analysis ===");

    // Generate single records to understand their size
    java.util.Random random = new java.util.Random(42);

    // Use reflection to access private methods for testing
    java.lang.reflect.Method xmlMethod =
        LargeDataGenerator.class.getDeclaredMethod(
            "generateXmlRecord", int.class, java.util.Random.class);
    xmlMethod.setAccessible(true);
    String xmlRecord = (String) xmlMethod.invoke(null, 1, random);

    java.lang.reflect.Method jsonMethod =
        LargeDataGenerator.class.getDeclaredMethod(
            "generateJsonRecord", int.class, java.util.Random.class);
    jsonMethod.setAccessible(true);
    String jsonRecord = (String) jsonMethod.invoke(null, 1, random);

    java.lang.reflect.Method csvMethod =
        LargeDataGenerator.class.getDeclaredMethod(
            "generateCsvRecord", int.class, java.util.Random.class);
    csvMethod.setAccessible(true);
    String csvRecord = (String) csvMethod.invoke(null, 1, random);

    System.out.printf("Single Record Sizes:\n");
    System.out.printf("  XML record: %d bytes\n", xmlRecord.getBytes("UTF-8").length);
    System.out.printf("  JSON record: %d bytes\n", jsonRecord.getBytes("UTF-8").length);
    System.out.printf("  CSV record: %d bytes\n", csvRecord.getBytes("UTF-8").length);

    System.out.printf("\nSample XML record:\n%s\n", xmlRecord);
    System.out.printf("Sample JSON record:\n%s\n", jsonRecord);
    System.out.printf("Sample CSV record:\n%s\n", csvRecord);
  }

  @Test
  public void debugStreamGenerationLogic() throws Exception {
    System.out.println("\n=== Stream Generation Logic Debug ===");

    long targetSize = 2048; // 2KB for detailed analysis

    // Test XML stream generation step by step
    InputStream xmlStream = LargeDataGenerator.createLargeDataStream("XML", targetSize);

    System.out.printf("Reading XML stream in chunks to understand generation pattern:\n");
    byte[] buffer = new byte[512]; // Small buffer to see chunking behavior
    int totalRead = 0;
    int chunkNumber = 1;

    while (true) {
      int bytesRead = xmlStream.read(buffer);
      if (bytesRead == -1) break;

      totalRead += bytesRead;
      String chunk = new String(buffer, 0, bytesRead, "UTF-8");

      System.out.printf("Chunk %d: %d bytes (total: %d)\n", chunkNumber++, bytesRead, totalRead);

      // Show first few lines of each chunk
      String[] lines = chunk.split("\n");
      for (int i = 0; i < Math.min(3, lines.length); i++) {
        System.out.printf("  %s\n", lines[i]);
      }
      if (lines.length > 3) {
        System.out.printf("  ... (%d more lines)\n", lines.length - 3);
      }
      System.out.println();
    }

    System.out.printf("Final total read: %d bytes (target was %d)\n", totalRead, targetSize);
    System.out.printf(
        "Difference: %d bytes (%.1f%%)\n",
        totalRead - targetSize, ((double) (totalRead - targetSize) / targetSize) * 100);
  }
}
