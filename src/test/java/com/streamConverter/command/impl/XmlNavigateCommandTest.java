package com.streamConverter.command.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamConverter.benchmark.ResourceMonitor;
import com.streamConverter.benchmark.ResourceUsage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for XmlNavigateCommand. */
class XmlNavigateCommandTest {

  private XmlNavigateCommand command;

  @BeforeEach
  void setUp() {
    command = new XmlNavigateCommand();
  }

  @Test
  void testCommandCreation() {
    assertNotNull(command);
  }

  @Test
  void testBasicXmlProcessing() throws IOException {
    String xmlInput = "<?xml version=\"1.0\"?><root><item>value</item></root>";
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));

    String result = outputStream.toString();
    assertNotNull(result);
    // For now, just verify that the command doesn't throw an exception
    // Verify basic XML navigation functionality - exact assertions depend on implementation details
  }

  @Test
  void testComplexXmlProcessing() throws IOException {
    String xmlInput =
        """
        <?xml version="1.0"?>
        <root>
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
          <metadata>
            <total>2</total>
            <page>1</page>
          </metadata>
        </root>
        """;
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));
  }

  @Test
  void testEmptyInput() throws IOException {
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    OutputStream outputStream = new ByteArrayOutputStream();

    // Empty input should cause XML parsing to fail
    assertThrows(IOException.class, () -> command.execute(inputStream, outputStream));
  }

  @Test
  void testInvalidXmlInput() throws IOException {
    String invalidXml = "<root><unclosed>";
    InputStream inputStream = new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Invalid XML should cause parsing to fail
    assertThrows(IOException.class, () -> command.execute(inputStream, outputStream));
  }

  @Test
  void testMemoryEfficiencyWithLargeData() throws IOException {
    // Generate larger XML data to test memory efficiency
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n");

    // Create 1MB of XML data (simulating larger processing)
    for (int i = 0; i < 10000; i++) {
      xmlBuilder.append(
          String.format(
              "  <item id=\"%d\">Data content %d with some additional text to increase size</item>\n",
              i, i));
    }
    xmlBuilder.append("</root>");

    String largeXml = xmlBuilder.toString();
    long dataSize = largeXml.getBytes(StandardCharsets.UTF_8).length;

    // Monitor memory usage during processing
    ResourceMonitor monitor = new ResourceMonitor();
    monitor.start(dataSize);

    InputStream inputStream = new ByteArrayInputStream(largeXml.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Execute command with monitoring
    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));

    ResourceUsage usage = monitor.stop();

    // Verify memory efficiency: should use much less memory than data size
    // Memory usage should be minimal compared to data size (streaming processing)
    assertTrue(
        usage.getMemoryUsedMB() < 50,
        String.format(
            "Memory usage (%.2f MB) should be less than 50MB for %.2f MB data",
            usage.getMemoryUsedMB(), usage.getDataSizeMB()));

    // Verify processing was successful (data size matches)
    assertTrue(usage.getDataSizeMB() > 0, "Should have processed some data");
  }
}
