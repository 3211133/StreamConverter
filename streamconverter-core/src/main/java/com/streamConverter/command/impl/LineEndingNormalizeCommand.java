package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
   * @throws IllegalArgumentException if targetType is null
   */
  public LineEndingNormalizeCommand(LineEndingType targetType) {
    this.targetType = Objects.requireNonNull(targetType, "Target type cannot be null");
  }

  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    logger.debug("Starting line ending normalization to: {}", targetType);

    // Read all input as bytes to preserve exact line ending detection
    byte[] inputBytes = inputStream.readAllBytes();
    String inputText = new String(inputBytes, StandardCharsets.UTF_8);

    if (inputText.isEmpty()) {
      return; // Empty input, nothing to process
    }

    // Normalize line endings based on target type
    String normalizedText = normalizeLineEndings(inputText);

    try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      writer.write(normalizedText);
      writer.flush();
    }

    logger.debug("Line ending normalization completed successfully");
  }

  /**
   * Normalizes all line endings in the text to the target format.
   *
   * @param text the input text
   * @return text with normalized line endings
   */
  private String normalizeLineEndings(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }

    if (targetType == LineEndingType.PRESERVE_INPUT) {
      // For PRESERVE_INPUT, we return the text as-is
      return text;
    }

    // First normalize all line endings to \n (this handles mixed line endings)
    String normalized = text.replace("\r\n", "\n").replace("\r", "\n");

    // Then convert to target format if it's not Unix
    String targetSeparator = targetType.getSeparator();
    if (!"\n".equals(targetSeparator)) {
      normalized = normalized.replace("\n", targetSeparator);
    }

    return normalized;
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
