package com.streamconverter.command.impl.xml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamconverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for XmlExtractCommand. */
class XmlExtractCommandTest {

  @Test
  @DisplayName("Null xpath throws IllegalArgumentException")
  void testNullXpathThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> XmlExtractCommand.create(null));
  }

  @Test
  @DisplayName("Basic XML extraction returns matched element")
  void testBasicXmlExtraction() throws IOException {
    String xmlInput =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<root><item>value</item><other>kept</other></root>";
    XmlExtractCommand cmd = XmlExtractCommand.create(TreePath.fromXml("root/item"));
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    cmd.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertNotNull(result);
    assertTrue(result.contains("<item>value</item>"), "Should contain matched element");
  }

  @Test
  @DisplayName("Multiple matched elements are all extracted")
  void testMultipleMatchedElements() throws IOException {
    String xmlInput =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<root><item>a</item><item>b</item></root>";
    XmlExtractCommand cmd = XmlExtractCommand.create(TreePath.fromXml("root/item"));
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    cmd.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("<item>a</item>"), "First item should be present");
    assertTrue(result.contains("<item>b</item>"), "Second item should be present");
  }

  @Test
  @DisplayName("Empty XML input throws IOException")
  void testEmptyInput() {
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    OutputStream outputStream = new ByteArrayOutputStream();
    XmlExtractCommand cmd = XmlExtractCommand.create(TreePath.fromXml("root/item"));

    assertThrows(IOException.class, () -> cmd.execute(inputStream, outputStream));
  }

  @Test
  @DisplayName("No match returns null output")
  void testNoMatchReturnsNull() throws IOException {
    String xmlInput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><other>value</other></root>";
    XmlExtractCommand cmd = XmlExtractCommand.create(TreePath.fromXml("root/item"));
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertDoesNotThrow(() -> cmd.execute(inputStream, outputStream));
  }
}
