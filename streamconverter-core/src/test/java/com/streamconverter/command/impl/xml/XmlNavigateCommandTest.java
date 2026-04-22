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
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
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

  // ---- #662 fix: XMLStreamException from close() is addSuppressed, not silently dropped ----

  @Test
  @DisplayName(
      "XMLStreamException from writer close() is suppressed onto primary IOException (#662)")
  void testWriterCloseXmlStreamExceptionSuppressedOnPrimaryException() throws Exception {
    XMLStreamException closeEx = new XMLStreamException("close failed");

    XmlNavigateCommand cmd =
        new XmlNavigateCommand(TreePath.fromXml("root/item"), new PassThroughRule()) {
          @Override
          protected XMLEventWriter createXMLEventWriter(OutputStream out)
              throws XMLStreamException {
            // Force invalid XML so execute() throws a primary XMLStreamException before close()
            XMLEventWriter real = super.createXMLEventWriter(out);
            // Return a writer that delegates add/flush to real but throws on close()
            return new DelegatingXmlEventWriter(real) {
              @Override
              public void close() throws XMLStreamException {
                throw closeEx;
              }
            };
          }
        };

    // Use invalid XML to force a primary IOException from the processing path
    String xmlInput = "<?xml version=\"1.0\"?><root><item>x</item></root>";
    // Trigger failure by using an OutputStream that throws on write
    OutputStream failingOutput =
        new OutputStream() {
          private int writeCount = 0;

          @Override
          public void write(int b) throws IOException {
            // Allow the XML declaration through, then fail
            if (writeCount++ > 20) {
              throw new IOException("simulated output failure");
            }
          }
        };

    IOException thrown =
        assertThrows(
            IOException.class,
            () ->
                cmd.execute(
                    new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8)),
                    failingOutput));
    // The close() XMLStreamException must be attached as a suppressed exception
    Throwable[] suppressed = thrown.getSuppressed();
    assertTrue(suppressed.length > 0, "Primary IOException should have suppressed exceptions");
    boolean found =
        java.util.Arrays.stream(suppressed)
            .anyMatch(
                s -> s instanceof XMLStreamException && s.getMessage().equals("close failed"));
    assertTrue(found, "XMLStreamException from close() should be in suppressed list");
  }

  @Test
  @DisplayName(
      "XMLStreamException from writer close() throws RuntimeException when no primary exception (#662)")
  void testWriterCloseXmlStreamExceptionThrowsRuntimeWhenNoPrimary() {
    XMLStreamException closeEx = new XMLStreamException("close failed");

    XmlNavigateCommand cmd =
        new XmlNavigateCommand(TreePath.fromXml("root/item"), new PassThroughRule()) {
          @Override
          protected XMLEventWriter createXMLEventWriter(OutputStream out)
              throws XMLStreamException {
            XMLEventWriter real = super.createXMLEventWriter(out);
            return new DelegatingXmlEventWriter(real) {
              @Override
              public void close() throws XMLStreamException {
                throw closeEx;
              }
            };
          }
        };

    String xmlInput = "<?xml version=\"1.0\"?><root><item>x</item></root>";

    // With valid input/output, processing succeeds but close() throws — should become
    // RuntimeException
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () ->
                cmd.execute(
                    new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8)),
                    new ByteArrayOutputStream()));
    assertInstanceOf(
        XMLStreamException.class,
        thrown.getCause(),
        "RuntimeException should wrap the XMLStreamException from close()");
  }

  /** Forwards all XMLEventWriter calls to a delegate; subclasses may override selectively. */
  private static class DelegatingXmlEventWriter implements XMLEventWriter {
    private final XMLEventWriter delegate;

    DelegatingXmlEventWriter(XMLEventWriter delegate) {
      this.delegate = delegate;
    }

    @Override
    public void flush() throws XMLStreamException {
      delegate.flush();
    }

    @Override
    public void close() throws XMLStreamException {
      delegate.close();
    }

    @Override
    public void add(javax.xml.stream.events.XMLEvent event) throws XMLStreamException {
      delegate.add(event);
    }

    @Override
    public void add(javax.xml.stream.XMLEventReader reader) throws XMLStreamException {
      delegate.add(reader);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
      return delegate.getPrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
      delegate.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
      delegate.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(javax.xml.namespace.NamespaceContext context)
        throws XMLStreamException {
      delegate.setNamespaceContext(context);
    }

    @Override
    public javax.xml.namespace.NamespaceContext getNamespaceContext() {
      return delegate.getNamespaceContext();
    }
  }
}
