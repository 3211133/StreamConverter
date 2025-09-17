package com.streamconverter.command.impl;

import com.streamconverter.command.AbstractStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
public class LineEndingNormalizeCommand extends AbstractStreamCommand {
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
  protected void executeInternal(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    logger.debug("Starting line ending normalization to: {}", targetType);

    // Stream processing for memory efficiency
    try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      if (targetType == LineEndingType.PRESERVE_INPUT) {
        // For PRESERVE_INPUT, copy directly without modification
        char[] buffer = new char[8192];
        int bytesRead;
        while ((bytesRead = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, bytesRead);
        }
      } else {
        // Process character by character for line ending normalization
        String targetSeparator = targetType.getSeparator();
        int current;

        while ((current = reader.read()) != -1) {
          if (current == '\r') {
            // Handle CR - could be CR, CRLF, or standalone CR
            int next = reader.read();
            if (next == '\n') {
              // CRLF -> convert to target
              writer.write(targetSeparator);
            } else {
              // Standalone CR -> convert to target
              writer.write(targetSeparator);
              // Write the next character that wasn't part of line ending
              if (next != -1) {
                writer.write(next);
              }
            }
          } else if (current == '\n') {
            // LF -> convert to target (handles Unix style)
            writer.write(targetSeparator);
          } else {
            // Regular character
            writer.write(current);
          }
        }
      }

      writer.flush();
    }

    logger.debug("Line ending normalization completed successfully");
  }

  @Override
  protected String getCommandDetails() {
    return String.format(
        "LineEndingNormalizeCommand(target=%s, separator='%s')",
        targetType,
        targetType.getSeparator() != null
            ? escapeLineSeparator(targetType.getSeparator())
            : "preserve");
  }

  /**
   * Escapes line separator characters for display purposes.
   *
   * @param separator the line separator to escape
   * @return escaped string representation
   */
  private String escapeLineSeparator(String separator) {
    if (separator == null) return "null";
    return separator.replace("\r", "\\r").replace("\n", "\\n");
  }
}
