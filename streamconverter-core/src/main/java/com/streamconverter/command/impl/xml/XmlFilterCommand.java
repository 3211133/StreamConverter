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

      XMLInputFactory inputFactory = SecureXmlConfiguration.createSecureXMLInputFactory();

      XMLEventReader reader;
      try {
        reader = inputFactory.createXMLEventReader(inputStream);
      } catch (XMLStreamException e) {
        throw new IOException("Error creating XML reader: " + e.getMessage(), e);
      }

      try {
        List<String> currentPath = new ArrayList<>();
        boolean isCapturing = false;
        int captureDepth = 0;
        int currentDepth = 0;
        String firstExtractedElement = null;
        boolean isWrappedOutput = false;

        CaptureSession captureSession = new CaptureSession(XMLOutputFactory.newInstance());

        while (reader.hasNext()) {
          XMLEvent event = reader.nextEvent();

          if (event.isStartElement()) {
            currentDepth++;
            String elementName = event.asStartElement().getName().getLocalPart();
            currentPath.add(elementName);

            // Check if this element matches our target path
            if (xpath.matches(currentPath) && !isCapturing) {
              isCapturing = true;
              captureDepth = currentDepth;
              try {
                captureSession.start(event);
              } catch (XMLStreamException e) {
                // Reset capturing state to avoid leaving the capture session half-open
                isCapturing = false;
                captureDepth = 0;
                captureSession.abort();
                LOGGER.warn("Error writing start element", e);
              }
            } else if (isCapturing && currentDepth > captureDepth) {
              // We're inside a matching element, continue capturing with the same writer
              try {
                captureSession.add(event);
              } catch (XMLStreamException e) {
                // Abort capture to avoid writing corrupt partial state
                isCapturing = false;
                captureDepth = 0;
                captureSession.abort();
                LOGGER.warn("Error writing nested start element; aborting capture", e);
              }
            }

          } else if (event.isEndElement()) {
            if (isCapturing) {
              try {
                captureSession.add(event);
              } catch (XMLStreamException e) {
                // Abort capture to avoid corrupt state propagation
                isCapturing = false;
                captureDepth = 0;
                captureSession.abort();
                LOGGER.warn("Error writing end element; aborting capture", e);
              }

              // If we're closing the captured element (re-check isCapturing in case catch reset it)
              if (isCapturing && currentDepth == captureDepth) {
                String extractedElement = captureSession.finish();
                if (isWrappedOutput) {
                  writer.write(extractedElement);
                  writer.flush();
                } else if (firstExtractedElement == null) {
                  firstExtractedElement = extractedElement;
                } else {
                  writeWrappedOutputStart(writer, firstExtractedElement);
                  isWrappedOutput = true;
                  firstExtractedElement = null;
                  writer.write(extractedElement);
                  writer.flush();
                }
                isCapturing = false;
                captureDepth = 0;
              }
            }

            currentPath.remove(currentPath.size() - 1);
            currentDepth--;

          } else if (isCapturing) {
            // Characters, comments, etc. inside captured element
            try {
              captureSession.add(event);
            } catch (XMLStreamException e) {
              // Abort capture to avoid corrupt state propagation
              isCapturing = false;
              captureDepth = 0;
              captureSession.abort();
              LOGGER.warn("Error writing content; aborting capture", e);
            }
          }
        }

        // Ensure writer is closed if capture was interrupted
        if (captureSession.isOpen()) {
          captureSession.abort();
        }

        writeRemainingOutput(writer, firstExtractedElement, isWrappedOutput);

      } catch (XMLStreamException e) {
        throw new IOException("Error processing XML: " + e.getMessage(), e);
      } finally {
        try {
          reader.close();
        } catch (XMLStreamException e) {
          LOGGER.warn("Error closing XML reader: {}", e.getMessage(), e);
        }
      }
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
