package com.streamconverter.command.impl.xml;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.path.ITreeMatcher;
import com.streamconverter.security.SecureXmlConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML Extract Command
 *
 * <p>指定パスにマッチした XML 要素を抽出して出力する。変換は行わない。 変換を行う場合は {@link XmlWalker} を使用すること。
 *
 * <p>特徴: - {@link ITreeMatcher#matches(java.util.List)} による反復マッチで抽出対象を判定 - マッチした要素の XML
 * 構造をそのまま保持して出力 - Jackson StAX Streaming API による省メモリ処理
 */
public class XmlExtractCommand implements IStreamCommand {
  private static final Logger logger = LoggerFactory.getLogger(XmlExtractCommand.class);

  private final ITreeMatcher xpath;

  private XmlExtractCommand(ITreeMatcher xpath) {
    this.xpath = xpath;
  }

  /**
   * XML 抽出コマンドを生成する。
   *
   * @param xpath 抽出対象を判定する {@link ITreeMatcher}
   * @return XmlExtractCommand インスタンス
   * @throws IllegalArgumentException xpath が null の場合
   */
  public static XmlExtractCommand create(ITreeMatcher xpath) {
    if (xpath == null) {
      throw new IllegalArgumentException("xpath cannot be null");
    }
    return new XmlExtractCommand(xpath);
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      XMLEventReader reader = createReader(inputStream);
      processEvents(reader, writer);
    }
  }

  private XMLEventReader createReader(InputStream inputStream) throws IOException {
    XMLInputFactory inputFactory = SecureXmlConfiguration.createSecureXMLInputFactory();
    try {
      return inputFactory.createXMLEventReader(inputStream);
    } catch (XMLStreamException e) {
      throw new IOException("Error creating XML reader: " + e.getMessage(), e);
    }
  }

  private void processEvents(XMLEventReader reader, Writer writer) throws IOException {
    try {
      XmlExtractionState state =
          new XmlExtractionState(SecureXmlConfiguration.createSecureXMLOutputFactory());
      drainEvents(reader, writer, state);
      if (state.isCaptureOpen()) {
        state.abortCapture();
      }
      String remaining = state.firstElementConsumed ? null : state.firstExtractedElement;
      XmlOutputEmitter.writeRemaining(writer, remaining, state.isWrappedOutput);
    } catch (XMLStreamException e) {
      throw new IOException("Error processing XML: " + e.getMessage(), e);
    } finally {
      closeReader(reader);
    }
  }

  private void drainEvents(XMLEventReader reader, Writer writer, XmlExtractionState state)
      throws XMLStreamException, IOException {
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        handleStartElement(event, state);
      } else if (event.isEndElement()) {
        handleEndElement(event, writer, state);
      } else if (state.isCapturing()) {
        addEventToCapture(event, state);
      }
    }
  }

  private void closeReader(XMLEventReader reader) {
    try {
      reader.close();
    } catch (XMLStreamException e) {
      logger.warn("Error closing XML reader: {}", e.getMessage(), e);
    }
  }

  private void handleStartElement(XMLEvent event, XmlExtractionState state) throws IOException {
    state.currentDepth++;
    state.currentPath.add(event.asStartElement().getName().getLocalPart());
    if (xpath.matches(state.currentPath) && !state.isCapturing()) {
      try {
        state.startCapture(state.currentDepth, event);
      } catch (XMLStreamException e) {
        state.resetCapture();
        throw new IOException("Error writing start element at path: " + state.currentPath, e);
      }
    } else if (state.isCapturing() && state.currentDepth > state.captureDepth) {
      addEventToCapture(event, state);
    }
  }

  private void handleEndElement(XMLEvent event, Writer writer, XmlExtractionState state)
      throws IOException, XMLStreamException {
    if (state.isCapturing()) {
      addEventToCapture(event, state);
      if (state.isCapturing() && state.currentDepth == state.captureDepth) {
        XmlOutputEmitter.emit(writer, state.finishCapture(), state);
      }
    }
    state.currentPath.remove(state.currentPath.size() - 1);
    state.currentDepth--;
  }

  private void addEventToCapture(XMLEvent event, XmlExtractionState state) throws IOException {
    try {
      state.addToCapture(event);
    } catch (XMLStreamException e) {
      state.resetCapture();
      throw new IOException("Error writing XML content", e);
    }
  }
}
