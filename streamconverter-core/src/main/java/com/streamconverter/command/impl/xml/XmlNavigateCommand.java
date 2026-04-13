package com.streamconverter.command.impl.xml;

import com.streamconverter.command.AbstractStreamCommand;
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
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML Navigate Command Class
 *
 * <p>This class implements command for targeted XML transformation using XPath. It identifies
 * specific elements using XPath expressions and applies IRule transformations to those elements
 * while preserving the overall XML structure.
 */
public class XmlNavigateCommand extends AbstractStreamCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlNavigateCommand.class);
  private static final XMLEventFactory EVENT_FACTORY = XMLEventFactory.newInstance();

  private TreePath treePath;
  private IRule rule;

  /**
   * Constructor for XML navigation with TreePath selector and transformation rule.
   *
   * @param treePath the TreePath to select elements
   * @param rule the transformation rule to apply to selected elements
   * @throws IllegalArgumentException if treePath or rule is null
   */
  private XmlNavigateCommand(TreePath treePath, IRule rule) {
    this.treePath = treePath;
    this.rule = rule;
  }

  /**
   * Factory method for creating an XML navigation command with TreePath and rule.
   *
   * @param treePath the TreePath to select elements
   * @param rule the transformation rule to apply to selected elements
   * @return an XmlNavigateCommand that transforms the specified TreePath elements with the given
   *     rule
   * @throws IllegalArgumentException if treePath or rule is null
   */
  public static XmlNavigateCommand create(TreePath treePath, IRule rule) {
    if (treePath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    return new XmlNavigateCommand(treePath, rule);
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    XMLEventReader eventReader = null;
    XMLEventWriter eventWriter = null;

    try {
      eventReader = createXMLEventReader(inputStream);
      eventWriter = createXMLEventWriter(outputStream);
      navigateXmlWithRule(eventReader, eventWriter, treePath, rule);
    } catch (XMLStreamException e) {
      handleXmlException(e);
    } finally {
      closeResources(eventReader, eventWriter);
    }
  }

  private void navigateXmlWithRule(
      XMLEventReader eventReader, XMLEventWriter eventWriter, TreePath treePath, IRule rule)
      throws XMLStreamException {
    List<String> currentPath = new ArrayList<>();

    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();

      if (event.isStartElement()) {
        currentPath.add(event.asStartElement().getName().getLocalPart());
      } else if (event.isEndElement()) {
        currentPath.remove(currentPath.size() - 1);
      } else if (event.isCharacters() && treePath.matches(currentPath)) {
        String transformed = rule.apply(event.asCharacters().getData());
        event = EVENT_FACTORY.createCharacters(transformed);
      }

      eventWriter.add(event);
    }
  }

  // Common factory methods and utilities
  private XMLEventReader createXMLEventReader(InputStream inputStream) throws XMLStreamException {
    XMLInputFactory inputFactory = SecureXmlConfiguration.createSecureXMLInputFactory();
    XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);
    if (!reader.hasNext()) {
      throw new XMLStreamException("Empty XML input");
    }
    return reader;
  }

  private XMLEventWriter createXMLEventWriter(OutputStream outputStream) throws XMLStreamException {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    return outputFactory.createXMLEventWriter(outputStream);
  }

  private void handleXmlException(XMLStreamException e) throws IOException {
    String message = e.getMessage();
    if (message != null && (message.contains("unclosed") || message.contains("end"))) {
      throw new IOException("Invalid XML format - unclosed tags", e);
    }
    throw new IOException("Invalid XML format", e);
  }

  private void closeResources(XMLEventReader eventReader, XMLEventWriter eventWriter) {
    if (eventWriter != null) {
      try {
        eventWriter.close();
      } catch (XMLStreamException e) {
        LOGGER.warn("Failed to close XMLEventWriter", e);
      }
    }
    if (eventReader != null) {
      try {
        eventReader.close();
      } catch (XMLStreamException e) {
        LOGGER.warn("Failed to close XMLEventReader", e);
      }
    }
  }
}
