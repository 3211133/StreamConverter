package com.streamconverter.command.impl.xml;

import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.path.IPath;
import com.streamconverter.security.SecureXmlConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
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
@SuppressWarnings("PMD.TooManyMethods")
// CaptureSession は XMLEventWriter のライフサイクルを管理する内部状態機械であり、
// このクラス外に公開する必要がないため private static inner class として保持している。
// 別ファイルに分離すると package-private にせざるを得ず、パッケージ境界が意図せず広がる。
public class XmlFilterCommand extends AbstractStreamCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlFilterCommand.class);

  private final IPath<List<String>> xpath;

  /**
   * Constructor for XML filtering with typed TreePath selector.
   *
   * @param xpath the typed TreePath to extract elements
   * @throws IllegalArgumentException if xpath is null
   */
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
      ExtractionState state = new ExtractionState(XMLOutputFactory.newInstance());
      drainEvents(reader, writer, state);
      if (state.isCaptureOpen()) {
        state.abortCapture();
      }
      String remaining = state.firstElementConsumed ? null : state.firstExtractedElement;
      writeRemainingOutput(writer, remaining, state.isWrappedOutput);
    } catch (XMLStreamException e) {
      throw new IOException("Error processing XML: " + e.getMessage(), e);
    } finally {
      closeReader(reader);
    }
  }

  private void drainEvents(XMLEventReader reader, Writer writer, ExtractionState state)
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
      LOGGER.warn("Error closing XML reader: {}", e.getMessage(), e);
    }
  }

  private void handleStartElement(XMLEvent event, ExtractionState state) throws IOException {
    state.currentDepth++;
    state.currentPath.add(event.asStartElement().getName().getLocalPart());
    if (xpath.matches(state.currentPath) && !state.isCapturing()) {
      state.startCapture(state.currentDepth);
      try {
        state.captureSession.start(event);
      } catch (XMLStreamException e) {
        state.resetCapture();
        throw new IOException("Error writing start element", e);
      }
    } else if (state.isCapturing() && state.currentDepth > state.captureDepth) {
      addEventToCapture(event, state);
    }
  }

  private void handleEndElement(XMLEvent event, Writer writer, ExtractionState state)
      throws IOException {
    if (state.isCapturing()) {
      addEventToCapture(event, state);
      if (state.isCapturing() && state.currentDepth == state.captureDepth) {
        String extracted = state.finishCapture();
        emitExtracted(writer, extracted, state);
      }
    }
    state.currentPath.remove(state.currentPath.size() - 1);
    state.currentDepth--;
  }

  private void addEventToCapture(XMLEvent event, ExtractionState state) throws IOException {
    try {
      state.captureSession.add(event);
    } catch (XMLStreamException e) {
      state.resetCapture();
      throw new IOException("Error writing XML content", e);
    }
  }

  private void emitExtracted(Writer writer, String extracted, ExtractionState state)
      throws IOException {
    if (state.isWrappedOutput) {
      writer.write(extracted);
      writer.flush();
    } else if (state.firstExtractedElement == null) {
      state.firstExtractedElement = extracted;
    } else {
      writeWrappedOutputStart(writer, state.firstExtractedElement);
      state.isWrappedOutput = true;
      state.firstElementConsumed = true;
      writer.write(extracted);
      writer.flush();
    }
  }

  private static final class ExtractionState {
    final List<String> currentPath = new ArrayList<>();
    final CaptureSession captureSession;
    int currentDepth;
    int captureDepth;
    boolean capturing;
    boolean firstElementConsumed;
    String firstExtractedElement;
    boolean isWrappedOutput;

    ExtractionState(XMLOutputFactory factory) {
      this.captureSession = new CaptureSession(factory);
    }

    boolean isCapturing() {
      return capturing;
    }

    boolean isCaptureOpen() {
      return captureSession.isOpen();
    }

    void startCapture(int depth) {
      this.capturing = true;
      this.captureDepth = depth;
    }

    void resetCapture() {
      this.capturing = false;
      this.captureDepth = 0;
      captureSession.abort();
    }

    String finishCapture() {
      String result = captureSession.finish();
      this.capturing = false;
      this.captureDepth = 0;
      return result;
    }

    void abortCapture() {
      captureSession.abort();
    }
  }

  /**
   * Write extracted XML elements to the output writer
   *
   * @param writer the output writer
   * @param elements list of extracted XML elements
   * @throws IOException if writing fails
   */
  private void writeWrappedOutputStart(Writer writer, String firstElement) throws IOException {
    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    writer.write("<filtered-results>");
    writer.write(firstElement);
  }

  private void writeRemainingOutput(
      Writer writer, String firstExtractedElement, boolean isWrappedOutput) throws IOException {
    if (firstExtractedElement != null) {
      writer.write(firstExtractedElement);
    }
    if (isWrappedOutput) {
      writer.write("</filtered-results>");
    }
    writer.flush();
  }

  private static final class CaptureSession {
    private final XMLOutputFactory outputFactory;

    private StringWriter elementWriter = new StringWriter();
    private XMLEventWriter eventWriter;
    private boolean open;

    private CaptureSession(XMLOutputFactory outputFactory) {
      this.outputFactory = outputFactory;
    }

    private void start(XMLEvent startEvent) throws XMLStreamException {
      elementWriter = new StringWriter();
      eventWriter = outputFactory.createXMLEventWriter(elementWriter);
      open = true;
      eventWriter.add(startEvent);
    }

    private void add(XMLEvent event) throws XMLStreamException {
      if (open) {
        eventWriter.add(event);
      }
    }

    private String finish() {
      abort();
      return elementWriter.toString();
    }

    private void abort() {
      if (!open) {
        return;
      }
      try {
        eventWriter.close();
      } catch (XMLStreamException e) {
        LOGGER.warn("Error closing event writer: {}", e.getMessage(), e);
      } finally {
        open = false;
      }
    }

    private boolean isOpen() {
      return open;
    }
  }
}
