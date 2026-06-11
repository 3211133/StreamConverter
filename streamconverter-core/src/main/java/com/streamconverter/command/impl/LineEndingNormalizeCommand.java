package com.streamconverter.command.impl;

import com.streamconverter.command.IStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Line ending normalization command.
 *
 * <p>This command normalizes line endings in text streams to a specified format. It can convert
 * between different line ending styles (Unix LF, Windows CRLF, classic Mac CR) or preserve the
 * input format.
 *
 * <p>The command processes data efficiently while preserving the exact structure of the input,
 * including whether the input ends with a line terminator.
 */
public class LineEndingNormalizeCommand implements IStreamCommand {
  private static final Logger logger = LoggerFactory.getLogger(LineEndingNormalizeCommand.class);

  /** Supported line ending types for normalization. */
  public enum LineEndingType {
    /** Unix/Linux style line endings (LF only) */
    UNIX("\n"),

    /** Windows style line endings (CRLF) */
    WINDOWS("\r\n"),

    /** Classic Mac style line endings (CR only) - rarely used in modern systems */
    MAC_CLASSIC("\r"),

    /** Preserve input line endings as-is */
    PRESERVE_INPUT(null),

    /** Use system default line separator */
    SYSTEM_DEFAULT(System.lineSeparator());

    private final String separator;

    LineEndingType(String separator) {
      this.separator = separator;
    }

    /**
     * Gets the line separator string for this type.
     *
     * @return the line separator string, or null for PRESERVE_INPUT
     */
    public String getSeparator() {
      return separator;
    }
  }

  private final LineEndingType targetType;

  /**
   * Creates a new line ending normalization command.
   *
   * @param targetType the target line ending type to convert to
   * @throws NullPointerException if targetType is null
   */
  public LineEndingNormalizeCommand(LineEndingType targetType) {
    this.targetType = Objects.requireNonNull(targetType, "Target type cannot be null");
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    logger.debug("Starting line ending normalization to: {}", targetType);

    // Stream processing for memory efficiency
    try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      if (targetType == LineEndingType.PRESERVE_INPUT) {
        copy(reader, writer);
      } else {
        normalize(reader, writer, targetType.getSeparator());
      }

      writer.flush();
    }

    logger.debug("Line ending normalization completed successfully");
  }

  private static void copy(Reader reader, Writer writer) throws IOException {
    // For PRESERVE_INPUT, copy directly without modification
    char[] buffer = new char[8192];
    int charsRead;
    // Stream the source as-is until the reader reaches EOF.
    while ((charsRead = reader.read(buffer)) != -1) {
      writer.write(buffer, 0, charsRead);
    }
  }

  private static void normalize(Reader reader, Writer writer, String targetSeparator)
      throws IOException {
    // Process character by character for line ending normalization
    PushbackReader pushbackReader = new PushbackReader(reader);
    int current;
    // Read one code unit at a time so CR, LF, and CRLF can be normalized consistently.
    while ((current = pushbackReader.read()) != -1) {
      if (current == '\r') {
        handleCarriageReturn(pushbackReader, writer, targetSeparator);
        continue;
      }
      if (current == '\n') {
        writer.write(targetSeparator);
        continue;
      }
      writer.write(current);
    }
  }

  private static void handleCarriageReturn(
      PushbackReader reader, Writer writer, String targetSeparator) throws IOException {
    // Handle CR - could be CR, CRLF, or standalone CR
    int next = reader.read();
    writer.write(targetSeparator);
    if (next != -1 && next != '\n') {
      // The look-ahead character was not part of the line ending. Push it back so the
      // main loop processes it; this keeps consecutive CRs (e.g. \r\r) handled as
      // separate line endings instead of being written through as raw characters.
      reader.unread(next);
    }
  }
}
