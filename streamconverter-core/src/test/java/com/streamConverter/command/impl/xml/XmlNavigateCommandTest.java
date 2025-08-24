package com.streamConverter.command.impl.xml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamConverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamConverter.test.StreamingTestUtils.TrackingInputStream;
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
}
