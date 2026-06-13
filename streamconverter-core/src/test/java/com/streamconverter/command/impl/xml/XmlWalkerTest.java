package com.streamconverter.command.impl.xml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamconverter.StreamProcessingException;
import com.streamconverter.UncheckedStreamException;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for XmlWalker. */
class XmlWalkerTest {

  private XmlWalker command;

  @BeforeEach
  void setUp() {
    command = XmlWalker.create(TreePath.fromXml("root/item"), new PassThroughRule());
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
        () -> XmlWalker.create(null, new PassThroughRule()),
        "Should throw IllegalArgumentException for null treePath");
  }

  @Test
  @DisplayName("Null rule throws IllegalArgumentException")
  void testNullRuleThrowsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> XmlWalker.create(TreePath.fromXml("root/item"), null),
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
    XmlWalker upperCaseCommand =
        XmlWalker.create(TreePath.fromXml("root/item"), value -> value.toUpperCase());

    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    upperCaseCommand.execute(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("<item>ORIGINAL</item>"), "Should apply rule to matched element");
    assertTrue(
        result.contains("<other>unchanged</other>"), "Should not transform non-matched elements");
  }

  @Test
  @DisplayName("Rule is applied to the whole text content even when split by a CDATA boundary")
  void testRuleAppliedToWholeTextAcrossCdataBoundary() throws IOException {
    // Arrange - <item> のテキストコンテンツは "userName"（CDATA 境界で2つの
    // Characters イベントに分割されて届く）
    String xmlInput =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><item>user<![CDATA[Name]]></item></root>";
    XmlWalker snakeCaseCommand =
        XmlWalker.create(
            TreePath.fromXml("root/item"),
            com.streamconverter.command.rule.impl.casing.CamelToSnakeCaseRule.create());

    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // Act
    snakeCaseCommand.execute(inputStream, outputStream);

    // Assert - テキストコンテンツ全体 "userName" への変換結果 "user_name" が出力されるべき。
    // 断片ごとに適用されると "user" + "name" = "username" になり語境界が失われる
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(
        result.contains("<item>user_name</item>"),
        "Rule should be applied to the whole text content of the node, but got: " + result);
  }

  @Test
  @DisplayName("CDATA in non-matched nodes is emitted as infoset-equivalent escaped text")
  void testCdataInNonMatchedNodeEmittedAsEscapedText() throws IOException {
    // Arrange - coalescing により非対象ノードの CDATA もテキストイベントに正規化されるため、
    // 字面は <![CDATA[x<y]]> のままではなく infoset 等価なエスケープ済みテキストになる
    String xmlInput =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<root><item>target</item><other><![CDATA[x<y]]></other></root>";
    InputStream inputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // Act
    command.execute(inputStream, outputStream);

    // Assert - CDATA の内容はエスケープ済みテキストとして保存される（infoset 等価）
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(
        result.contains("<other>x&lt;y</other>"),
        "CDATA content should be preserved as infoset-equivalent escaped text, but got: " + result);
    assertFalse(
        result.contains("<![CDATA["),
        "CDATA sections are not preserved verbatim under coalescing, but got: " + result);
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
    XmlWalker nameCommand =
        XmlWalker.create(TreePath.fromXml("root/users/user/name"), new PassThroughRule());
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
    XmlWalker failingRuleCommand =
        XmlWalker.create(
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
    XmlWalker cmd = XmlWalker.create(TreePath.fromXml("root/item"), new PassThroughRule());
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

  // ---- #662: XMLStreamException from close() must be addSuppressed or rethrown ----

  @Test
  @DisplayName("close() の XMLStreamException はプライマリ例外の suppressed に追加される（#662）")
  void testWriterCloseXmlStreamExceptionSuppressedOnPrimaryException() throws IOException {
    XMLStreamException closeEx = new XMLStreamException("close failed");

    XmlWalker cmd =
        new XmlWalker(TreePath.fromXml("root/item"), new PassThroughRule()) {
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

    IOException thrown;
    try (OutputStream failingOutput =
        new OutputStream() {
          private int count = 0;

          @Override
          public void write(int b) throws IOException {
            if (count++ > 20) {
              throw new IOException("simulated output failure");
            }
          }
        }) {
      thrown =
          assertThrows(
              IOException.class,
              () ->
                  cmd.execute(
                      new ByteArrayInputStream(
                          "<?xml version=\"1.0\"?><root><item>x</item></root>"
                              .getBytes(StandardCharsets.UTF_8)),
                      failingOutput));
    }

    boolean found =
        java.util.Arrays.stream(thrown.getSuppressed())
            .anyMatch(
                s ->
                    s instanceof IOException
                        && s.getCause() instanceof XMLStreamException
                        && "close failed".equals(s.getCause().getMessage()));
    assertTrue(found, "close() の XMLStreamException は IOException にラップされて suppressed に含まれるべき");
  }

  @Test
  @DisplayName("close() の XMLStreamException はプライマリ例外なし時に IOException でラップされる（#662）")
  void testWriterCloseXmlStreamExceptionThrowsRuntimeWhenNoPrimary() {
    XMLStreamException closeEx = new XMLStreamException("close failed");

    XmlWalker cmd =
        new XmlWalker(TreePath.fromXml("root/item"), new PassThroughRule()) {
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

    IOException thrown =
        assertThrows(
            IOException.class,
            () ->
                cmd.execute(
                    new ByteArrayInputStream(
                        "<?xml version=\"1.0\"?><root><item>x</item></root>"
                            .getBytes(StandardCharsets.UTF_8)),
                    new ByteArrayOutputStream()));

    assertInstanceOf(
        XMLStreamException.class, thrown.getCause(), "IOException は XMLStreamException をラップするべき");
  }

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
    public void add(javax.xml.stream.events.XMLEvent e) throws XMLStreamException {
      delegate.add(e);
    }

    @Override
    public void add(javax.xml.stream.XMLEventReader r) throws XMLStreamException {
      delegate.add(r);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
      return delegate.getPrefix(uri);
    }

    @Override
    public void setPrefix(String p, String uri) throws XMLStreamException {
      delegate.setPrefix(p, uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
      delegate.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(javax.xml.namespace.NamespaceContext ctx)
        throws XMLStreamException {
      delegate.setNamespaceContext(ctx);
    }

    @Override
    public javax.xml.namespace.NamespaceContext getNamespaceContext() {
      return delegate.getNamespaceContext();
    }
  }

  @Test
  @DisplayName("XmlWalker は XMLEventFactory を static final フィールドとして保持しない（スレッドセーフ保証のため）")
  void xmlWalker_hasNoStaticFinalXmlEventFactoryField() {
    boolean hasStaticFinalEventFactory =
        Arrays.stream(XmlWalker.class.getDeclaredFields())
            .filter(f -> XMLEventFactory.class.isAssignableFrom(f.getType()))
            .anyMatch(
                f -> {
                  int mod = f.getModifiers();
                  return Modifier.isStatic(mod) && Modifier.isFinal(mod);
                });

    assertFalse(
        hasStaticFinalEventFactory,
        "XMLEventFactory should not be held as static final field — thread safety is not guaranteed by the spec");
  }

  @Test
  @DisplayName("[#741] ルール層の UncheckedStreamException キャリアは境界で unwrap され IOException として伝播する")
  void testRuleCarrierIsUnwrappedToIOException() {
    StreamProcessingException ruleFailure =
        new StreamProcessingException("simulated rule I/O failure");
    XmlWalker walker =
        XmlWalker.create(
            TreePath.fromXml("root/item"),
            input -> {
              throw new UncheckedStreamException(ruleFailure);
            });
    InputStream input =
        new ByteArrayInputStream(
            "<?xml version=\"1.0\"?><root><item>value</item></root>"
                .getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    IOException thrown =
        assertThrows(
            IOException.class,
            () -> walker.execute(input, output),
            "ルール層の I/O 失敗キャリアは IOException として伝播すること");
    assertSame(ruleFailure, thrown, "キャリアの cause がラップされずそのまま伝播すること");
  }
}
