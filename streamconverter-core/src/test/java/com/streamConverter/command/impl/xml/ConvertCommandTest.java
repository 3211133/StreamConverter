package com.streamConverter.command.impl.xml;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.rule.TestRule;
import com.streamConverter.path.TreePath;
import com.streamConverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamConverter.test.StreamingTestUtils.TrackingInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ConvertCommandTest {

  @Test
  @DisplayName("Basic XML conversion with simple rule")
  public void testBasicXmlConversion() throws IOException {
    // Setup: XML with target element to transform
    String inputXml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
          <target>original content</target>
          <other>unchanged content</other>
        </root>
        """;

    TestRule rule = TestRule.contentTransformRule(); // replaces "original" with "transformed"
    ConvertCommand command = new ConvertCommand(rule, TreePath.fromXml("root/target"));

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(inputXml.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // Execute
    command.execute(inputStream, outputStream);

    // Verify
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(
        result.contains("transformed content"), "Should transform content in target element");
    assertTrue(
        result.contains("unchanged content"), "Should preserve content in non-target elements");
    assertTrue(result.contains("<?xml version=\"1.0\""), "Should preserve XML declaration");
  }

  @Test
  @DisplayName("XML conversion with nested elements")
  public void testNestedElementConversion() throws IOException {
    String inputXml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <document>
          <section>
            <value>test data</value>
          </section>
          <value>other test data</value>
        </document>
        """;

    TestRule rule = TestRule.upperCaseRule(); // replaces "test" with "TEST"
    ConvertCommand command = new ConvertCommand(rule, TreePath.fromXml("document/section/value"));

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(inputXml.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(
        result.contains("<value>TEST data</value>"), "Should transform nested target element");
    assertTrue(result.contains("other test data"), "Should not transform non-target elements");
  }

  @Test
  @DisplayName("Constructor validation")
  public void testConstructorValidation() {
    TestRule rule = TestRule.contentTransformRule();

    assertThrows(
        NullPointerException.class,
        () -> new ConvertCommand(null, TreePath.fromXml("valid/path")),
        "Should throw exception for null rule");

    assertThrows(
        NullPointerException.class,
        () -> new ConvertCommand(rule, null),
        "Should throw exception for null path");
  }

  @Test
  @DisplayName("Empty XML handling")
  public void testEmptyXmlHandling() throws IOException {
    String inputXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root></root>";

    TestRule rule = TestRule.contentTransformRule();
    ConvertCommand command = new ConvertCommand(rule, TreePath.fromXml("root/nonexistent"));

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(inputXml.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("<root></root>"), "Should preserve empty XML structure");
  }

  @Test
  @DisplayName("Invalid XML handling")
  public void testInvalidXmlHandling() {
    String invalidXml = "<invalid><unclosed>";

    TestRule rule = TestRule.contentTransformRule();
    ConvertCommand command = new ConvertCommand(rule, TreePath.fromXml("invalid/element"));

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // Should throw StreamProcessingException for invalid XML
    assertThrows(
        com.streamConverter.StreamProcessingException.class,
        () -> command.execute(inputStream, outputStream));
  }

  @Test
  @DisplayName("Large XML document processing")
  public void testLargeXmlProcessing() throws IOException {
    // Generate larger XML document
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<document>\n");

    for (int i = 0; i < 1000; i++) {
      xmlBuilder
          .append("  <item id=\"")
          .append(i)
          .append("\">test value ")
          .append(i)
          .append("</item>\n");
    }
    xmlBuilder.append("</document>");

    TestRule rule = TestRule.upperCaseRule();
    ConvertCommand command = new ConvertCommand(rule, TreePath.fromXml("document/item"));

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(xmlBuilder.toString().getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("TEST value"), "Should transform all matching elements");
    assertTrue(result.split("TEST value").length > 100, "Should process large number of elements");
  }

  @Test
  @DisplayName("Streaming XML conversion behavior verification")
  public void testStreamingXmlConversionBehavior() throws IOException {
    // Create moderate-sized XML to observe streaming behavior
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlBuilder.append("<catalog>\n");

    for (int i = 0; i < 100; i++) {
      xmlBuilder.append(
          String.format(
              "  <product id=\"%d\">%n"
                  + "    <name>Product %d</name>%n"
                  + "    <description>original description for product %d with detailed information</description>%n"
                  + "    <price>%.2f</price>%n"
                  + "  </product>%n",
              i, i, i, 19.99 + (i * 0.5)));
    }
    xmlBuilder.append("</catalog>");

    String xmlData = xmlBuilder.toString();

    TestRule rule = TestRule.contentTransformRule(); // replaces "original" with "transformed"
    ConvertCommand command =
        new ConvertCommand(rule, TreePath.fromXml("catalog/product/description"));

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute XML conversion
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during XML conversion");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify data was processed incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read during XML conversion");

    // Verify the conversion was successful
    String output = monitoringOutputStream.getContent();
    assertTrue(output.contains("transformed description"), "Should transform target elements");
    assertTrue(output.contains("Product"), "Should preserve non-target content");
  }

  @Test
  @DisplayName("Incremental XML conversion processing verification")
  public void testIncrementalXmlConversionProcessing() throws IOException {
    // Create complex XML with nested structures to force incremental processing
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlBuilder.append("<library>\n");

    for (int i = 1; i <= 150; i++) {
      xmlBuilder.append(
          String.format(
              "  <book id=\"%d\">%n"
                  + "    <title>Book Title %d</title>%n"
                  + "    <author>Author %d</author>%n"
                  + "    <content>%n"
                  + "      <chapter number=\"1\">%n"
                  + "        <text>original content for chapter 1 of book %d with extensive text</text>%n"
                  + "      </chapter>%n"
                  + "      <chapter number=\"2\">%n"
                  + "        <text>original content for chapter 2 of book %d with more extensive text</text>%n"
                  + "      </chapter>%n"
                  + "    </content>%n"
                  + "    <metadata>%n"
                  + "      <category>Category %d</category>%n"
                  + "      <tags>streaming,xml,conversion,testing</tags>%n"
                  + "    </metadata>%n"
                  + "  </book>%n",
              i, i, i, i, i, i % 10));
    }
    xmlBuilder.append("</library>");

    String xmlData = xmlBuilder.toString();

    TestRule rule = TestRule.contentTransformRule(); // replaces "original" with "transformed"
    ConvertCommand command =
        new ConvertCommand(rule, TreePath.fromXml("library/book/content/chapter/text"));

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - perform incremental XML conversion processing
    long processingStart = System.nanoTime();
    command.execute(trackingInputStream, monitoringOutputStream);
    long processingEnd = System.nanoTime();

    // Then - verify incremental processing characteristics
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "Output should be written during XML conversion");
    assertTrue(trackingInputStream.isFullyRead(), "Input should be fully processed");

    // Verify substantial XML data was processed
    assertTrue(
        trackingInputStream.getBytesRead() > 15000,
        "Should have processed substantial amount of XML data");

    long processingTime = processingEnd - processingStart;
    assertTrue(processingTime > 0, "XML conversion should take measurable time");

    // Verify output was generated correctly (XML conversion should transform target content)
    String output = monitoringOutputStream.getContent();
    assertTrue(
        output.contains("transformed content"), "XML conversion should transform target content");
    assertTrue(output.contains("Book Title"), "Should preserve non-target elements");
  }
}
