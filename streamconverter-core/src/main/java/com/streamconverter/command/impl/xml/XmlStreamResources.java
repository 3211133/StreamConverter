package com.streamconverter.command.impl.xml;

import java.io.IOException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles close/flush of XMLEventReader and XMLEventWriter, accumulating exceptions rather than
 * throwing from finally blocks.
 */
final class XmlStreamResources {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlStreamResources.class);

  private XmlStreamResources() {}

  /**
   * Closes writer and reader, accumulating any close/flush exceptions onto {@code primary}. Always
   * returns (never throws); call site decides whether to throw the result.
   */
  static IOException closeAll(XMLEventReader reader, XMLEventWriter writer, IOException primary) {
    IOException result = closeWriter(writer, primary);
    result = closeReader(reader, result);
    return result;
  }

  private static IOException closeWriter(XMLEventWriter writer, IOException primary) {
    if (writer == null) {
      return primary;
    }
    IOException afterFlush = flush(writer, primary);
    return closeCloseable(writer::close, afterFlush);
  }

  private static IOException flush(XMLEventWriter writer, IOException primary) {
    try {
      writer.flush();
      return primary;
    } catch (XMLStreamException e) {
      LOGGER.warn("Failed to flush XMLEventWriter during cleanup", e);
      return accumulate(primary, toIOException(e));
    }
  }

  private static IOException closeReader(XMLEventReader reader, IOException primary) {
    if (reader == null) {
      return primary;
    }
    return closeCloseable(reader::close, primary);
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  // XMLEventReader/Writer.close() declare XMLStreamException but implementations may also throw
  // RuntimeException; both must be caught to avoid masking the primary exception.
  private static IOException closeCloseable(XmlCloseable closeable, IOException primary) {
    try {
      closeable.close();
      return primary;
    } catch (XMLStreamException e) {
      return accumulate(primary, toIOException(e));
    } catch (RuntimeException e) {
      return accumulate(primary, new IOException("Failed to close XML resource", e));
    }
  }

  private static IOException accumulate(IOException primary, IOException next) {
    if (primary == null) {
      return next;
    }
    primary.addSuppressed(next);
    return primary;
  }

  private static IOException toIOException(XMLStreamException e) {
    String location = "";
    if (e.getLocation() != null) {
      int line = e.getLocation().getLineNumber();
      int column = e.getLocation().getColumnNumber();
      if (line > 0 && column > 0) {
        location = String.format(" at line %d, column %d", line, column);
      }
    }
    return new IOException("XML processing failed" + location, e);
  }

  @FunctionalInterface
  interface XmlCloseable {
    void close() throws XMLStreamException;
  }
}
