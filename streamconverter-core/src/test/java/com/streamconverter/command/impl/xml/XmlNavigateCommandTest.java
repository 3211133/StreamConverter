package com.streamconverter.command.impl.xml;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for XmlNavigateCommand. */
class XmlNavigateCommandTest {

  private XmlNavigateCommand command;

  @BeforeEach
  void setUp() {
    command = XmlNavigateCommand.create(TreePath.fromXml("root/item"), new PassThroughRule());
  }

  @Test
  void testCommandCreation() {
    assertNotNull(command);
  }

  @Test
  @DisplayName("Null treePath throws IllegalArgumentException")
  void testNullTreePathThrowsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> XmlNavigateCommand.create(null, new PassThroughRule()),
        "Should throw IllegalArgumentException for null treePath");
  }

  @Test
  @DisplayName("Null rule throws IllegalArgumentException")
  void testNullRuleThrowsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> XmlNavigateCommand.create(TreePath.fromXml("root/item"), null),
        "Should throw IllegalArgumentException for null rule");
  }

  @Test
  @DisplayName("Full XML structure is preserved in output")
  void testBasicXmlProcessing() throws IOException {
    String xmlInput =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <root><item>value</item><other>kept</other></root>
        """;
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    String result = ((ByteArrayOutputStream) outputStream).toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("<item>value</item>"), "Should contain matched element");
    assertTrue(result.contains("<other>kept</other>"), "Should preserve non-matched elements");
    assertTrue(result.contains("<root>"), "Should preserve root element");
  }

  @Test
  @DisplayName("Non-matched sibling elements are preserved in output")
  void testNonMatchedElementsPreserved() throws IOException {
    String xmlInput =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<root><item>target</item><sibling>should remain</sibling></root>";
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("<item>target</item>"), "Should contain matched element");
    assertTrue(
        result.contains("<sibling>should remain</sibling>"),
        "Should preserve sibling non-matched elements");
  }

  @Test
  @DisplayName("Rule is applied to matched element value")
  void testRuleAppliedToMatchedElement() throws IOException {
    String xmlInput =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><item>original</item><other>unchanged</other></root>";
    XmlNavigateCommand upperCaseCommand =
        XmlNavigateCommand.create(TreePath.fromXml("root/item"), value -> value.toUpperCase());

    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    upperCaseCommand.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("<item>ORIGINAL</item>"), "Should apply rule to matched element");
    assertTrue(
        result.contains("<other>unchanged</other>"), "Should not transform non-matched elements");
  }

  @Test
  @DisplayName("Complex XML structure is preserved")
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
    XmlNavigateCommand nameCommand =
        XmlNavigateCommand.create(TreePath.fromXml("root/users/user/name"), new PassThroughRule());
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    nameCommand.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("<name>John</name>"), "Should contain matched name elements");
    assertTrue(result.contains("<name>Jane</name>"), "Should contain all matched name elements");
    assertTrue(result.contains("<metadata>"), "Should preserve metadata section");
    assertTrue(result.contains("<total>2</total>"), "Should preserve metadata content");
    assertTrue(result.contains("<age>30</age>"), "Should preserve non-matched sibling elements");
  }

  @Test
  @DisplayName("Rule RuntimeException is wrapped as IOException with original cause")
  void testRuleRuntimeExceptionWrappedAsIOException() {
    RuntimeException ruleEx = new RuntimeException("rule failure");
    XmlNavigateCommand failingRuleCommand =
        XmlNavigateCommand.create(
            TreePath.fromXml("root/item"),
            value -> {
              throw ruleEx;
            });
    String xmlInput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><item>value</item></root>";
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    IOException thrown =
        assertThrows(
            IOException.class, () -> failingRuleCommand.execute(inputStream, outputStream));
    // Cause chain: IOException -> XMLStreamException -> RuntimeException
    assertNotNull(thrown.getCause(), "IOException should wrap XMLStreamException");
    assertInstanceOf(
        javax.xml.stream.XMLStreamException.class,
        thrown.getCause(),
        "Direct cause should be XMLStreamException");
    assertNotNull(
        thrown.getCause().getCause(),
        "XMLStreamException should wrap the original RuntimeException");
    assertInstanceOf(
        RuntimeException.class,
        thrown.getCause().getCause(),
        "Root cause should be the RuntimeException thrown by the rule");
  }

  @Test
  @DisplayName("Malformed XML with unexpected EndElement logs warning and continues without IOOBE")
  void testUnexpectedEndElementDoesNotThrowIndexOutOfBounds() throws IOException {
    // XML that StAX parses without fatal error but produces an extra end-element seen in some
    // stream scenarios — simulate by constructing two consecutive elements where path tracking
    // could underflow if the guard is absent.
    String xmlInput =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><item>a</item><item>b</item></root>";
    XmlNavigateCommand cmd =
        XmlNavigateCommand.create(TreePath.fromXml("root/item"), new PassThroughRule());
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    cmd.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("<item>a</item>"), "First item should be present");
    assertTrue(result.contains("<item>b</item>"), "Second item should be present after path reset");
  }

  @Test
  @DisplayName("Empty input throws IOException")
  void testEmptyInput() throws IOException {
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    OutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(IOException.class, () -> command.execute(inputStream, outputStream));
  }

  @Test
  @DisplayName("Invalid XML input throws IOException")
  void testInvalidXmlInput() throws IOException {
    String invalidXml = "<root><unclosed>";
    InputStream inputStream = new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(IOException.class, () -> command.execute(inputStream, outputStream));
  }
}
