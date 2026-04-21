package com.streamconverter.command.impl.xml;

import java.io.IOException;
import java.io.Writer;

/** Writes filtered XML elements to the output writer, managing single vs. wrapped output format. */
final class XmlOutputEmitter {

  private XmlOutputEmitter() {}

  static void emit(Writer writer, String extracted, XmlExtractionState state) throws IOException {
    if (state.isWrappedOutput) {
      writer.write(extracted);
      writer.flush();
    } else if (state.firstExtractedElement == null) {
      state.firstExtractedElement = extracted;
    } else {
      writeWrappedStart(writer, state.firstExtractedElement);
      state.isWrappedOutput = true;
      state.firstElementConsumed = true;
      writer.write(extracted);
      writer.flush();
    }
  }

  static void writeRemaining(Writer writer, String firstExtractedElement, boolean isWrappedOutput)
      throws IOException {
    if (firstExtractedElement != null) {
      writer.write(firstExtractedElement);
    }
    if (isWrappedOutput) {
      writer.write("</filtered-results>");
    }
    writer.flush();
  }

  private static void writeWrappedStart(Writer writer, String firstElement) throws IOException {
    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    writer.write("<filtered-results>");
    writer.write(firstElement);
  }
}
