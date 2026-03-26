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

      List<String> extractedElements = new ArrayList<>();

      XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);
      List<String> currentPath = new ArrayList<>();
      boolean isCapturing = false;
      int captureDepth = 0;
      int currentDepth = 0;

      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      StringWriter elementWriter = new StringWriter();
      XMLEventWriter eventWriter = null;

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
            elementWriter = new StringWriter();
            try {
              eventWriter = outputFactory.createXMLEventWriter(elementWriter);
              eventWriter.add(event);
            } catch (XMLStreamException e) {
              // Reset capturing state to avoid leaving isCapturing=true with eventWriter=null
              isCapturing = false;
              captureDepth = 0;
              LOGGER.warn("Error writing start element", e);
            }
          } else if (isCapturing && currentDepth > captureDepth) {
            // We're inside a matching element, continue capturing with the same writer
            try {
              if (eventWriter != null) {
                eventWriter.add(event);
              }
            } catch (XMLStreamException e) {
              // Abort capture to avoid writing corrupt partial state
              isCapturing = false;
              captureDepth = 0;
              eventWriter = null;
              LOGGER.warn("Error writing nested start element; aborting capture", e);
            }
          }

        } else if (event.isEndElement()) {
          if (isCapturing) {
            try {
              if (eventWriter != null) {
                eventWriter.add(event);
              }
            } catch (XMLStreamException e) {
              // Abort capture to avoid corrupt state propagation
              isCapturing = false;
              captureDepth = 0;
              eventWriter = null;
              LOGGER.warn("Error writing end element; aborting capture", e);
            }

            // If we're closing the captured element
            if (currentDepth == captureDepth) {
              try {
                if (eventWriter != null) {
                  eventWriter.close();
                  eventWriter = null;
                }
              } catch (XMLStreamException e) {
                eventWriter = null;
                LOGGER.warn("Error closing event writer: {}", e.getMessage(), e);
              }
              extractedElements.add(elementWriter.toString());
              isCapturing = false;
              captureDepth = 0;
            }
          }

          currentPath.remove(currentPath.size() - 1);
          currentDepth--;

        } else if (isCapturing) {
          // Characters, comments, etc. inside captured element
          try {
            if (eventWriter != null) {
              eventWriter.add(event);
            }
          } catch (XMLStreamException e) {
            // Abort capture to avoid corrupt state propagation
            isCapturing = false;
            captureDepth = 0;
            eventWriter = null;
            LOGGER.warn("Error writing content; aborting capture", e);
          }
        }
      }

      // Ensure writer is closed if capture was interrupted
      if (eventWriter != null) {
        try {
          eventWriter.close();
        } catch (XMLStreamException e) {
          LOGGER.warn("Error closing event writer: {}", e.getMessage(), e);
        }
      }

      // Write extracted elements to output
      writeExtractedElements(writer, extractedElements);

      reader.close();
    } catch (XMLStreamException e) {
      throw new IOException("Error processing XML: " + e.getMessage(), e);
    }
  }

  /**
   * Write extracted XML elements to the output writer
   *
   * @param writer the output writer
   * @param elements list of extracted XML elements
   * @throws IOException if writing fails
   */
  private void writeExtractedElements(Writer writer, List<String> elements) throws IOException {
    if (elements.isEmpty()) {
      // No matching elements found - output empty XML fragment
      writer.write("");
      writer.flush();
      return;
    }

    if (elements.size() == 1) {
      // Single element - write directly
      writer.write(elements.get(0));
    } else {
      // Multiple elements - wrap in a root element
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      writer.write("<filtered-results>");
      for (String element : elements) {
        writer.write(element);
      }
      writer.write("</filtered-results>");
    }

    writer.flush();
  }
}
