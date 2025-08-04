package com.streamConverter.command.impl;

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

  private String xpath;
  private IRule rule;
  private FixedStaXPathHandler pathHandler;

  /**
   * Constructor for XML navigation with XPath selector and transformation rule.
   *
   * @param xpath the XPath expression to select elements (e.g., "users/user/name")
   * @param rule the transformation rule to apply to selected elements
   */
  public XmlNavigateCommand(String xpath, IRule rule) {
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
   */
  public XmlNavigateCommand(String xpath) {
    this(xpath, new PassThroughRule());
  }

  /** Default constructor - processes entire XML with PassThroughRule. */
  public XmlNavigateCommand() {
    this(null, new PassThroughRule());
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
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
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
    // For simplicity, read entire XML as string and apply rule
    // In production, this would use proper DOM/SAX parsing
    StringBuilder xmlBuilder = new StringBuilder();
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);

    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();
      xmlBuilder.append(event.toString());
    }

    String transformedXml = rule.apply(xmlBuilder.toString());
    writer.write(transformedXml);
    writer.flush();
    eventReader.close();
  }

  /**
   * Apply transformation rule to specific XPath elements while preserving XML structure This is a
   * simplified implementation - production version would need proper XPath library
   */
  private void applyRuleToXmlPath(InputStream inputStream, Writer writer)
      throws IOException, XMLStreamException {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

    XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);
    XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);

    // For now, apply rule-aware navigation using existing pathHandler
    if (pathHandler != null) {
      navigateXmlWithRule(eventReader, eventWriter, pathHandler, rule);
    } else {
      // Fallback to entire XML processing
      applyRuleToEntireXml(inputStream, writer);
      return;
    }

    eventWriter.flush();
    eventReader.close();
    eventWriter.close();
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
            // Add newline as simple characters (XMLEventFactory not available in all JVMs)
            eventWriter.add(javax.xml.stream.XMLEventFactory.newInstance().createCharacters("\n"));
          }
        }
        currentPath.remove(currentPath.size() - 1);
      } else if (event.isCharacters() && inTargetElement) {
        // Apply rule to character data in target elements
        String originalData = event.asCharacters().getData();
        String transformedData = rule.apply(originalData);
        if (!originalData.equals(transformedData)) {
          // Create new character event with transformed data
          XMLEvent transformedEvent =
              javax.xml.stream.XMLEventFactory.newInstance().createCharacters(transformedData);
          eventWriter.add(transformedEvent);
        } else {
          eventWriter.add(event);
        }
      }
    }
  }

  private XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
}
