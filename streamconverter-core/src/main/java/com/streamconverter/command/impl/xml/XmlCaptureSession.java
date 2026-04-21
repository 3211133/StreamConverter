package com.streamconverter.command.impl.xml;

import java.io.StringWriter;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the lifecycle of a single XML element capture using a streaming XMLEventWriter.
 *
 * <p>A session proceeds through three states: idle → open (after {@link #start}) → idle (after
 * {@link #finish} or {@link #abort}). Writing to a closed session is a no-op.
 */
final class XmlCaptureSession {
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlCaptureSession.class);

  private final XMLOutputFactory outputFactory;
  private StringWriter elementWriter = new StringWriter();
  private XMLEventWriter eventWriter;
  private boolean open;

  XmlCaptureSession(XMLOutputFactory outputFactory) {
    this.outputFactory = outputFactory;
  }

  void start(XMLEvent startEvent) throws XMLStreamException {
    elementWriter = new StringWriter();
    eventWriter = outputFactory.createXMLEventWriter(elementWriter);
    open = true;
    eventWriter.add(startEvent);
  }

  void add(XMLEvent event) throws XMLStreamException {
    if (open) {
      eventWriter.add(event);
    }
  }

  String finish() {
    abort();
    return elementWriter.toString();
  }

  void abort() {
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

  boolean isOpen() {
    return open;
  }
}
