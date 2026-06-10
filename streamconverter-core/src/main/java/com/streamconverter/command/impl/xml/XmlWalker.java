package com.streamconverter.command.impl.xml;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.rule.IRule;
import com.streamconverter.path.TreePath;
import com.streamconverter.security.SecureXmlConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML Walker for applying transformations to XML data.
 *
 * <p>This command navigates through XML structures and applies transformations using rules while
 * preserving the overall XML structure. It identifies specific elements using TreePath
 * slash-delimited path expressions (e.g., {@code product/name}) and applies {@link IRule}
 * transformations to the text content of matching nodes.
 *
 * <p>The full XML event stream — including the XML declaration, all elements, attributes, and text
 * nodes — is written to the output. Only character data at nodes whose path exactly matches the
 * configured {@link TreePath} is transformed by the rule; all other events are passed through
 * unchanged.
 */
public class XmlWalker implements IStreamCommand {
  private static final Logger logger = LoggerFactory.getLogger(XmlWalker.class);

  private final TreePath treePath;
  private final IRule rule;

  /**
   * Constructor for XML navigation with TreePath selector and transformation rule.
   *
   * @param treePath the TreePath to select elements
   * @param rule the transformation rule to apply to selected elements
   * @throws IllegalArgumentException if treePath or rule is null
   */
  XmlWalker(TreePath treePath, IRule rule) {
    if (treePath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    this.treePath = treePath;
    this.rule = rule;
  }

  /**
   * Factory method for creating an XML navigation command with TreePath and rule.
   *
   * @param treePath the TreePath to select elements
   * @param rule the transformation rule to apply to selected elements
   * @return an XmlWalker that transforms the specified TreePath elements with the given rule
   * @throws IllegalArgumentException if treePath or rule is null
   */
  public static XmlWalker create(TreePath treePath, IRule rule) {
    if (treePath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    return new XmlWalker(treePath, rule);
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    XMLEventReader eventReader = null;
    XMLEventWriter eventWriter = null;
    IOException primaryException = null;

    try {
      eventReader = createXMLEventReader(inputStream);
      eventWriter = createXMLEventWriter(outputStream);
      navigateXmlWithRule(eventReader, eventWriter, treePath, rule);
      eventWriter.flush();
    } catch (XMLStreamException e) {
      primaryException = buildXmlException(e);
    } finally {
      primaryException = XmlStreamResources.closeAll(eventReader, eventWriter, primaryException);
    }

    if (primaryException != null) {
      throw primaryException;
    }
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  // IRule.apply() declares no checked exceptions; any RuntimeException must be caught and
  // re-thrown as XMLStreamException so the caller's error-handling path is not bypassed.
  private void navigateXmlWithRule(
      XMLEventReader eventReader, XMLEventWriter eventWriter, TreePath treePath, IRule rule)
      throws XMLStreamException {
    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
    List<String> currentPath = new ArrayList<>();

    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();

      if (event.isStartElement()) {
        currentPath.add(event.asStartElement().getName().getLocalPart());
      } else if (event.isEndElement()) {
        if (currentPath.isEmpty()) {
          logger.warn(
              "Unexpected end element '{}' with empty path stack — possible malformed XML",
              event.asEndElement().getName().getLocalPart());
        } else {
          currentPath.remove(currentPath.size() - 1);
        }
      } else if (event.isCharacters() && treePath.matches(currentPath)) {
        String data = event.asCharacters().getData();
        String transformed;
        try {
          transformed = rule.apply(data);
        } catch (RuntimeException ruleEx) {
          throw new XMLStreamException("Rule application failed at path " + currentPath, ruleEx);
        }
        // Write every event unconditionally; character events at the target path are replaced
        // above.
        event = eventFactory.createCharacters(transformed);
      }

      eventWriter.add(event);
    }
  }

  private XMLEventReader createXMLEventReader(InputStream inputStream) throws XMLStreamException {
    XMLInputFactory inputFactory = SecureXmlConfiguration.createSecureXMLInputFactory();
    // テキスト断片化防止のため coalescing を有効化する。CDATA 境界やパーサ内部バッファで
    // 分割された連続テキストを 1 つの Characters イベントに結合し、ルールがテキスト
    // コンテンツ全体に適用されることを保証する (#762)。XmlWalker ローカルの設定であり、
    // SecureXmlConfiguration を共有する他コマンドには影響しない。
    inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);
    if (!reader.hasNext()) {
      try {
        reader.close();
      } catch (XMLStreamException closeEx) {
        logger.warn("Failed to close empty XMLEventReader", closeEx);
      }
      throw new XMLStreamException("Empty XML input");
    }
    return reader;
  }

  /**
   * 出力ストリームへの XMLEventWriter を作成する。
   *
   * <p>テスト時にオーバーライドしてファクトリをモック可能にするため protected スコープとしている。
   *
   * @param outputStream 書き込み先ストリーム
   * @return 設定済み XMLEventWriter
   * @throws XMLStreamException ライターの生成に失敗した場合
   */
  protected XMLEventWriter createXMLEventWriter(OutputStream outputStream)
      throws XMLStreamException {
    return SecureXmlConfiguration.createSecureXMLOutputFactory().createXMLEventWriter(outputStream);
  }

  private IOException buildXmlException(XMLStreamException e) {
    String location = "";
    if (e.getLocation() != null) {
      int line = e.getLocation().getLineNumber();
      int column = e.getLocation().getColumnNumber();
      if (line > 0 && column > 0) {
        location = String.format(" at line %d, column %d", line, column);
      }
    }
    logger.error("XML processing failed{}", location, e);
    return new IOException("XML processing failed" + location, e);
  }
}
