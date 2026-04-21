package com.streamconverter.command.impl.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Mutable traversal state for a single XmlFilterCommand execution.
 *
 * <p>Tracks the current path, capture depth, and accumulated output state as the event stream is
 * drained. Owns a {@link XmlCaptureSession} that handles XMLEventWriter lifecycle.
 */
final class XmlExtractionState {
  final List<String> currentPath = new ArrayList<>();
  private final XmlCaptureSession captureSession;
  int currentDepth;
  int captureDepth;
  boolean capturing;
  boolean firstElementConsumed;
  String firstExtractedElement;
  boolean isWrappedOutput;

  XmlExtractionState(XMLOutputFactory factory) {
    this.captureSession = new XmlCaptureSession(factory);
  }

  boolean isCapturing() {
    return capturing;
  }

  boolean isCaptureOpen() {
    return captureSession.isOpen();
  }

  void startCapture(int depth, XMLEvent startEvent) throws XMLStreamException {
    this.capturing = true;
    this.captureDepth = depth;
    captureSession.start(startEvent);
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

  void addToCapture(XMLEvent event) throws XMLStreamException {
    captureSession.add(event);
  }

  void abortCapture() {
    captureSession.abort();
  }
}
