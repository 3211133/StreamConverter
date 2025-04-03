package com.streamConverter.command.impl.charaCode;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Converts the character encoding of a stream from one encoding to another.
 *
 * <p>This class extends the AbstractStreamCommand and implements the conversion of character
 * encodings.
 */
// BEGIN convert.java
public class convert extends AbstractStreamCommand {
  private String from;
  private String to;

  /**
   * Constructor to initialize the character encodings for conversion.
   *
   * @param from The source character encoding.
   * @param to The target character encoding.
   * @throws IllegalArgumentException if the specified character encodings are not supported.
   */
  public convert(String from, String to) {
    Objects.requireNonNull(from);
    Objects.requireNonNull(to);
    if (Charset.isSupported(from) == false) {
      throw new IllegalArgumentException("変換元文字コードがサポートされていません");
    }
    if (Charset.isSupported(to) == false) {
      throw new IllegalArgumentException("変換先文字コードがサポートされていません");
    }
    this.from = from;
    this.to = to;
  }

  /**
   * Executes the character encoding conversion on the provided input stream and writes the result
   * to the output stream.
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  @Override
  public void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    // 文字コードを変換する
    try (InputStreamReader reader = new InputStreamReader(inputStream, this.from);
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, this.to); ) {
      reader.transferTo(writer);
    }
  }
}
