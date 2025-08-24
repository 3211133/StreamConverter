package com.streamConverter.command.impl.xml;

import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.command.rule.IRule;
import com.streamConverter.command.rule.PassThroughRule;
import com.streamConverter.pathHandler.FixedStaXPathHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * XML Navigate Command Class
 *
 * <p>This class implements command for targeted XML transformation using XPath. It identifies
 * specific elements using XPath expressions and applies IRule transformations to those elements
 * while preserving the overall XML structure.
 */
public class XmlNavigateCommand extends AbstractStreamCommand {
  private static final Logger LOGGER = Logger.getLogger(XmlNavigateCommand.class.getName());
  private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();

  private String xpath;
  private IRule rule;
  private FixedStaXPathHandler pathHandler;

  /**
   * Constructor for XML navigation with XPath selector and transformation rule.
   *
   * @param xpath the XPath expression to select elements (e.g., "users/user/name")
   * @param rule the transformation rule to apply to selected elements
   * @throws IllegalArgumentException if rule is null
   */
  public XmlNavigateCommand(String xpath, IRule rule) {
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    this.xpath = xpath;
    this.rule = rule;
    if (xpath != null) {
      this.pathHandler = new FixedStaXPathHandler(xpath);
    }
  }

  /**
   * Constructor for XML navigation with XPath selector using PassThroughRule.
   *
   * @param xpath the XPath expression to select elements (e.g., "users/user/name")
   * @deprecated This constructor uses PassThroughRule by default, which may not be the intended
   *     behavior. Use {@link #XmlNavigateCommand(String, IRule)} to explicitly specify the
   *     transformation rule. For data extraction without transformation, use {@link
   *     #extractOnly(String)}.
   */
  @Deprecated(since = "1.2.0", forRemoval = true)
  public XmlNavigateCommand(String xpath) {
    this(xpath, new PassThroughRule());
  }

  /**
   * Default constructor - processes entire XML with PassThroughRule.
   *
   * @deprecated This constructor uses PassThroughRule by default, which may not be the intended
   *     behavior. Use {@link #XmlNavigateCommand(String, IRule)} to explicitly specify the
   *     transformation rule. For data extraction without transformation, use {@link #extractAll()}.
   */
  @Deprecated(since = "1.2.0", forRemoval = true)
  public XmlNavigateCommand() {
    this(null, new PassThroughRule());
  }

  /**
   * Factory method for creating an XML navigation command that extracts data without
   * transformation. This method makes the intention explicit: extract data from the specified XPath
   * as-is.
   *
   * @param xpath the XPath expression to select elements (e.g., "users/user/name")
   * @return an XmlNavigateCommand that extracts the specified XPath without transformation
   */
  public static XmlNavigateCommand extractOnly(String xpath) {
    return new XmlNavigateCommand(xpath, new PassThroughRule());
  }

  /**
   * Factory method for creating an XML navigation command that processes entire XML without
   * transformation. This method makes the intention explicit: process all XML data as-is.
   *
   * @return an XmlNavigateCommand that processes entire XML without transformation
   */
  public static XmlNavigateCommand extractAll() {
    return new XmlNavigateCommand(null, new PassThroughRule());
  }

  @Override
  protected String getCommandDetails() {
    if (xpath != null) {
      return String.format("XmlNavigateCommand(xpath='%s')", xpath);
    } else {
      return "XmlNavigateCommand(entire XML)";
    }
  }

  @Override
  protected void executeInternal(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      if (xpath == null) {
        // Apply rule to entire XML content
        applyRuleToEntireXml(inputStream, writer);
      } else {
        // Apply rule to specific XPath elements while preserving structure
        applyRuleToXmlPath(inputStream, writer);
      }
    } catch (XMLStreamException e) {
      throw new IOException("XML processing error", e);
    }
  }

  /** Apply transformation rule to entire XML content */
  private void applyRuleToEntireXml(InputStream inputStream, Writer writer)
      throws IOException, XMLStreamException {
    XMLEventReader eventReader = null;
    XMLEventWriter eventWriter = null;

    try {
      eventReader = createXMLEventReader(inputStream);
      eventWriter = createXMLEventWriter(writer);
      processXmlEvents(eventReader, eventWriter, (event, data) -> rule.apply(data));
    } catch (XMLStreamException e) {
      handleXmlException(e);
    } finally {
      closeResources(eventReader, eventWriter);
    }
  }

  /**
   * Apply transformation rule to specific XPath elements while preserving XML structure This is a
   * simplified implementation - production version would need proper XPath library
   */
  private void applyRuleToXmlPath(InputStream inputStream, Writer writer)
      throws IOException, XMLStreamException {
    XMLEventReader eventReader = null;
    XMLEventWriter eventWriter = null;

    try {
      eventReader = createXMLEventReader(inputStream);
      eventWriter = createXMLEventWriter(writer);
      navigateXmlWithRule(eventReader, eventWriter, pathHandler, rule);
    } catch (XMLStreamException e) {
      handleXmlException(e);
    } finally {
      closeResources(eventReader, eventWriter);
    }
  }

  private void navigateXmlWithRule(
      XMLEventReader eventReader,
      XMLEventWriter eventWriter,
      FixedStaXPathHandler pathHandler,
      IRule rule)
      throws XMLStreamException {
    List<String> currentPath = new ArrayList<>();
    boolean inTargetElement = false;
    int targetDepth = 0;

    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();

      if (event.isStartElement()) {
        String elementName = event.asStartElement().getName().getLocalPart();
        currentPath.add(elementName);

        if (pathHandler.isTarget(currentPath)) {
          inTargetElement = true;
          targetDepth = currentPath.size();
          eventWriter.add(event);
        } else if (inTargetElement) {
          eventWriter.add(event);
        }
      } else if (event.isEndElement()) {
        if (inTargetElement) {
          eventWriter.add(event);
          if (currentPath.size() == targetDepth) {
            inTargetElement = false;
            // Add newline as simple characters
            eventWriter.add(EVENT_FACTORY.createCharacters("\n"));
          }
        }
        currentPath.remove(currentPath.size() - 1);
      } else if (event.isCharacters() && inTargetElement) {
        // Apply rule to character data in target elements
        String originalData = event.asCharacters().getData();
        String transformedData = rule.apply(originalData);
        if (!originalData.equals(transformedData)) {
          // Create new character event with transformed data
          XMLEvent transformedEvent = EVENT_FACTORY.createCharacters(transformedData);
          eventWriter.add(transformedEvent);
        } else {
          eventWriter.add(event);
        }
      }
    }
  }

  // Common factory methods and utilities
  private XMLEventReader createXMLEventReader(InputStream inputStream) throws XMLStreamException {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);
    if (!reader.hasNext()) {
      throw new XMLStreamException("Empty XML input");
    }
    return reader;
  }

  private XMLEventWriter createXMLEventWriter(Writer writer) throws XMLStreamException {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    return outputFactory.createXMLEventWriter(writer);
  }

  private void handleXmlException(XMLStreamException e) throws IOException {
    String message = e.getMessage();
    if (message != null && (message.contains("unclosed") || message.contains("end"))) {
      throw new IOException("Invalid XML format - unclosed tags", e);
    }
    throw new IOException("Invalid XML format", e);
  }

  @FunctionalInterface
  private interface CharacterDataProcessor {
    String process(XMLEvent event, String data);
  }

  private void processXmlEvents(
      XMLEventReader eventReader, XMLEventWriter eventWriter, CharacterDataProcessor processor)
      throws XMLStreamException {
    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();

      if (event.isCharacters()) {
        String originalData = event.asCharacters().getData();
        String transformedData = processor.process(event, originalData);

        if (!originalData.equals(transformedData)) {
          XMLEvent transformedEvent = EVENT_FACTORY.createCharacters(transformedData);
          eventWriter.add(transformedEvent);
        } else {
          eventWriter.add(event);
        }
      } else {
        eventWriter.add(event);
      }
    }
    eventWriter.flush();
  }

  private void closeResources(XMLEventReader eventReader, XMLEventWriter eventWriter) {
    if (eventReader != null) {
      try {
        eventReader.close();
      } catch (XMLStreamException e) {
        LOGGER.warning("Failed to close XMLEventReader: " + e.getMessage());
      }
    }
    if (eventWriter != null) {
      try {
        eventWriter.close();
      } catch (XMLStreamException e) {
        LOGGER.warning("Failed to close XMLEventWriter: " + e.getMessage());
      }
    }
  }
}
