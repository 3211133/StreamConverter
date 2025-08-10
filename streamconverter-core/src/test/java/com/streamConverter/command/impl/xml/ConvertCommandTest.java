package com.streamConverter.command.impl.xml;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.rule.TestRule;
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
    ConvertCommand command = new ConvertCommand(rule, "root/target");

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
    ConvertCommand command = new ConvertCommand(rule, "document/section/value");

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
        () -> new ConvertCommand(null, "valid/path"),
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
    ConvertCommand command = new ConvertCommand(rule, "root/nonexistent");

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
    ConvertCommand command = new ConvertCommand(rule, "invalid/element");

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
    ConvertCommand command = new ConvertCommand(rule, "document/item");

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(xmlBuilder.toString().getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("TEST value"), "Should transform all matching elements");
    assertTrue(result.split("TEST value").length > 100, "Should process large number of elements");
  }
}
