package com.streamConverter.command.impl.xml;

import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.pathHandler.FixedStaXPathHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * XML Filter Command Class
 *
 * <p>This class implements pure data extraction from XML using XPath expressions. Unlike
 * XmlNavigateCommand which applies transformations, XmlFilterCommand only extracts/filters elements
 * based on specified paths without any modifications.
 *
 * <p>Features: - Extract specific elements using XPath expressions - Preserve exact XML structure
 * of extracted elements - Memory-efficient streaming processing - Support for complex path
 * expressions including nested elements and attributes
 */
public class XmlFilterCommand extends AbstractStreamCommand {
  private static final Logger LOGGER = Logger.getLogger(XmlFilterCommand.class.getName());

  private final String xpath;
  private final FixedStaXPathHandler pathHandler;

  /**
   * Constructor for XML filtering with XPath selector.
   *
   * @param xpath the XPath expression to extract elements (e.g., "users/user/name")
   * @throws IllegalArgumentException if xpath is null or empty
   */
  public XmlFilterCommand(String xpath) {
    if (xpath == null || xpath.trim().isEmpty()) {
      throw new IllegalArgumentException("XPath cannot be null or empty");
    }
    this.xpath = xpath.trim();
    this.pathHandler = new FixedStaXPathHandler(xpath);
  }

  @Override
  protected String getCommandDetails() {
    return String.format("XmlFilterCommand(xpath='%s')", xpath);
  }

  @Override
  protected void executeInternal(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

      List<String> extractedElements = new ArrayList<>();

      XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);
      List<String> currentPath = new ArrayList<>();
      StringBuilder currentElement = new StringBuilder();
      boolean isCapturing = false;
      int captureDepth = 0;
      int currentDepth = 0;

      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      StringWriter elementWriter = new StringWriter();

      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();

        if (event.isStartElement()) {
          currentDepth++;
          String elementName = event.asStartElement().getName().getLocalPart();
          currentPath.add(elementName);

          // Check if this element matches our target path
          if (pathHandler.isTarget(currentPath) && !isCapturing) {
            isCapturing = true;
            captureDepth = currentDepth;
            currentElement.setLength(0);
            elementWriter = new StringWriter();

            try {
              XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(elementWriter);
              eventWriter.add(event);
              eventWriter.close();
            } catch (XMLStreamException e) {
              LOGGER.warning("Error writing start element: " + e.getMessage());
            }
          } else if (isCapturing && currentDepth > captureDepth) {
            // We're inside a matching element, continue capturing
            try {
              XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(elementWriter);
              eventWriter.add(event);
              eventWriter.close();
            } catch (XMLStreamException e) {
              LOGGER.warning("Error writing nested start element: " + e.getMessage());
            }
          }

        } else if (event.isEndElement()) {
          if (isCapturing) {
            try {
              XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(elementWriter);
              eventWriter.add(event);
              eventWriter.close();
            } catch (XMLStreamException e) {
              LOGGER.warning("Error writing end element: " + e.getMessage());
            }

            // If we're closing the captured element
            if (currentDepth == captureDepth) {
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
            XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(elementWriter);
            eventWriter.add(event);
            eventWriter.close();
          } catch (XMLStreamException e) {
            LOGGER.warning("Error writing content: " + e.getMessage());
          }
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
