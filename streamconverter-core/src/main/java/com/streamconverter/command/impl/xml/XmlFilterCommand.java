package com.streamconverter.command.impl.xml;

import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.path.IPath;
import com.streamconverter.security.SecureXmlConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML Filter Command Class
 *
 * <p>This class implements pure data extraction from XML using TreePath expressions. Unlike
 * XmlNavigateCommand which applies transformations, XmlFilterCommand only extracts/filters elements
 * based on specified paths without any modifications.
 *
 * <p>Features: - Extract specific elements using TreePath expressions - Preserve exact XML
 * structure of extracted elements - Memory-efficient streaming processing - Support for complex
 * path expressions including nested elements and attributes
 */
public class XmlFilterCommand extends AbstractStreamCommand {
  private static final Logger logger = LoggerFactory.getLogger(XmlFilterCommand.class);

  private final IPath<List<String>> xpath;

  private XmlFilterCommand(IPath<List<String>> xpath) {
    this.xpath = xpath;
  }

  /**
   * Factory method for XML filtering with typed path selector.
   *
   * @param xpath the typed path to extract elements
   * @return an XmlFilterCommand instance
   * @throws IllegalArgumentException if xpath is null
   */
  public static XmlFilterCommand create(IPath<List<String>> xpath) {
    if (xpath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
    return new XmlFilterCommand(xpath);
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
