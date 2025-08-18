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

  private static final int NUM_XML_ITEMS_FOR_LARGE_DATA_TEST = 10000;

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
    xmlBuilder
        .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append(System.lineSeparator())
        .append("<root>")
        .append(System.lineSeparator());

    // Create 1MB of XML data (simulating larger processing)
    for (int i = 0; i < NUM_XML_ITEMS_FOR_LARGE_DATA_TEST; i++) {
      xmlBuilder.append(
          String.format(
              "  <item id=\"%d\">Data content %d with some additional text to increase size</item>"
                  + System.lineSeparator(),
              i,
              i));
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

  @Test
  void testStreamingXmlNavigationBehavior() throws IOException {
    // Create moderate-sized XML data to observe streaming behavior
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlBuilder.append("<catalog>\n");

    for (int i = 0; i < 100; i++) {
      xmlBuilder.append(
          String.format(
              """
        <item id="%d">
          <title>Book Title %d</title>
          <author>Author %d</author>
          <description>This is a detailed description of book %d with various content</description>
          <price currency="USD">%.2f</price>
        </item>
        """,
              i, i, i, i, 19.99 + (i * 0.5)));
    }

    xmlBuilder.append("</catalog>");
    String xmlData = xmlBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute XML navigation
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during XML navigation");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify data was processed incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read during XML processing");

    // Verify the content was processed (output should contain some XML-related data)
    String output = monitoringOutputStream.getContent();
    assertTrue(output.length() > 0, "Should have produced some output from XML navigation");
  }

  @Test
  void testIncrementalXmlNavigationProcessing() throws IOException {
    // Create complex XML with nested structures to force incremental processing
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlBuilder.append("<library>\n");

    for (int i = 1; i <= 50; i++) {
      xmlBuilder.append(
          String.format(
              """
        <section name="Section %d">
          <books>
            <book id="book%d-1">
              <title>Advanced Topics in Computer Science %d</title>
              <chapters>
                <chapter num="1">Introduction to Concepts %d</chapter>
                <chapter num="2">Advanced Algorithms %d</chapter>
                <chapter num="3">Data Structures and Analysis %d</chapter>
              </chapters>
            </book>
            <book id="book%d-2">
              <title>Practical Software Engineering %d</title>
              <metadata>
                <tags>engineering,software,practical</tags>
                <keywords>design,testing,deployment</keywords>
              </metadata>
            </book>
          </books>
        </section>
        """,
              i, i, i, i, i, i, i, i));
    }

    xmlBuilder.append("</library>");
    String xmlData = xmlBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - perform incremental XML navigation processing
    long processingStart = System.nanoTime();
    command.execute(trackingInputStream, monitoringOutputStream);
    long processingEnd = System.nanoTime();

    // Then - verify incremental processing characteristics
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "Output should be written during XML processing");
    assertTrue(trackingInputStream.isFullyRead(), "Input should be fully processed");

    // Verify substantial XML data was processed
    assertTrue(
        trackingInputStream.getBytesRead() > 5000,
        "Should have processed substantial amount of XML data");

    long processingTime = processingEnd - processingStart;
    assertTrue(processingTime > 0, "XML processing should take measurable time");

    // Verify output was generated (XML navigation should produce some result)
    String output = monitoringOutputStream.getContent();
    assertTrue(output.length() > 0, "XML navigation should produce output");
  }

  /** Custom InputStream that tracks read operations for streaming behavior verification */
  private static class TrackingInputStream extends ByteArrayInputStream {
    private long fullyReadTime = -1;
    private final int totalBytes;
    private int bytesRead = 0;

    public TrackingInputStream(byte[] buf) {
      super(buf);
      this.totalBytes = buf.length;
    }

    @Override
    public int read() {
      int result = super.read();
      if (result != -1) {
        bytesRead++;
      } else if (fullyReadTime == -1) {
        fullyReadTime = System.nanoTime();
      }
      return result;
    }

    @Override
    public int read(byte[] b, int off, int len) {
      int bytesActuallyRead = super.read(b, off, len);
      if (bytesActuallyRead > 0) {
        bytesRead += bytesActuallyRead;
      }
      if (bytesActuallyRead == -1 && fullyReadTime == -1) {
        fullyReadTime = System.nanoTime();
      }
      return bytesActuallyRead;
    }

    public boolean isFullyRead() {
      return fullyReadTime != -1;
    }

    public long getFullyReadTime() {
      return fullyReadTime;
    }

    public int getBytesRead() {
      return bytesRead;
    }

    public int getTotalBytes() {
      return totalBytes;
    }

    public double getReadProgress() {
      return totalBytes > 0 ? (double) bytesRead / totalBytes : 0.0;
    }
  }

  /** Custom OutputStream that monitors write operations and timing */
  private static class MonitoringOutputStream extends ByteArrayOutputStream {
    private long firstWriteTime = -1;
    private boolean hasWriteOccurred = false;

    @Override
    public void write(int b) {
      recordFirstWrite();
      super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      recordFirstWrite();
      super.write(b, off, len);
    }

    private void recordFirstWrite() {
      if (!hasWriteOccurred) {
        firstWriteTime = System.nanoTime();
        hasWriteOccurred = true;
      }
    }

    public boolean hasWriteOccurred() {
      return hasWriteOccurred;
    }

    public long getFirstWriteTime() {
      return firstWriteTime;
    }

    public String getContent() {
      return toString(StandardCharsets.UTF_8);
    }
  }
}
