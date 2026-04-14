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
 * XML Navigate Command for applying transformations to XML data.
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
    super();
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
    IOException primaryException = null;

    try {
      eventReader = createXMLEventReader(inputStream);
      eventWriter = createXMLEventWriter(outputStream);
      navigateXmlWithRule(eventReader, eventWriter, treePath, rule);
      eventWriter.flush();
    } catch (XMLStreamException e) {
      primaryException = buildXmlException(e);
      throw primaryException;
    } finally {
      closeResources(eventReader, eventWriter, primaryException);
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
        if (!currentPath.isEmpty()) {
          currentPath.remove(currentPath.size() - 1);
        } else {
          if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(
                "Unexpected end element '{}' with empty path stack — possible malformed XML",
                event.asEndElement().getName().getLocalPart());
          }
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
        event = EVENT_FACTORY.createCharacters(transformed);
      }

      eventWriter.add(event);
    }
  }

  private XMLEventReader createXMLEventReader(InputStream inputStream) throws XMLStreamException {
    XMLInputFactory inputFactory = SecureXmlConfiguration.createSecureXMLInputFactory();
    XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);
    if (!reader.hasNext()) {
      try {
        reader.close();
      } catch (XMLStreamException closeEx) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn("Failed to close empty XMLEventReader", closeEx);
        }
      }
      throw new XMLStreamException("Empty XML input");
    }
    return reader;
  }

  private XMLEventWriter createXMLEventWriter(OutputStream outputStream) throws XMLStreamException {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    return outputFactory.createXMLEventWriter(outputStream);
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
    LOGGER.error("XML processing failed{}", location, e);
    return new IOException("XML processing failed" + location, e);
  }

  private void closeResources(
      XMLEventReader eventReader, XMLEventWriter eventWriter, IOException primaryException) {
    if (eventWriter != null) {
      try {
        eventWriter.flush();
      } catch (XMLStreamException e) {
        LOGGER.warn("Failed to flush XMLEventWriter during cleanup", e);
      }
      try {
        eventWriter.close();
      } catch (XMLStreamException e) {
        LOGGER.warn("Failed to close XMLEventWriter", e);
      } catch (RuntimeException e) {
        if (primaryException != null) {
          primaryException.addSuppressed(e);
        } else {
          throw e;
        }
      }
    }
    if (eventReader != null) {
      try {
        eventReader.close();
      } catch (XMLStreamException e) {
        LOGGER.warn("Failed to close XMLEventReader", e);
      } catch (RuntimeException e) {
        if (primaryException != null) {
          primaryException.addSuppressed(e);
        } else {
          throw e;
        }
      }
    }
  }
}
