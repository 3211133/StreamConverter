package com.streamconverter.command.impl.charcode;

import com.streamconverter.command.AbstractStreamCommand;
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
// BEGIN CharacterConvertCommand.java
public class CharacterConvertCommand extends AbstractStreamCommand {
  private final String from;
  private final String to;

  /**
   * Constructor to initialize the character encodings for conversion.
   *
   * @param from The source character encoding.
   * @param to The target character encoding.
   * @throws IllegalArgumentException if the specified character encodings are not supported.
   */
  private CharacterConvertCommand(String from, String to) {
    super();
    this.from = from;
    this.to = to;
  }

  /**
   * Factory method to create a CharacterConvertCommand.
   *
   * @param from The source character encoding.
   * @param to The target character encoding.
   * @return a new CharacterConvertCommand instance
   * @throws IllegalArgumentException if the specified character encodings are not supported.
   */
  public static CharacterConvertCommand create(String from, String to) {
    Objects.requireNonNull(from);
    Objects.requireNonNull(to);
    if (!Charset.isSupported(from)) {
      throw new IllegalArgumentException("変換元文字コードがサポートされていません");
    }
    if (!Charset.isSupported(to)) {
      throw new IllegalArgumentException("変換先文字コードがサポートされていません");
    }
    return new CharacterConvertCommand(from, to);
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
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    // 省メモリストリーミング文字コード変換
    try (InputStreamReader reader = new InputStreamReader(inputStream, this.from);
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, this.to)) {

      // transferTo()は大容量データでメモリを大量消費するため、
      // 固定サイズバッファでストリーミング処理を実装
      char[] buffer = new char[8192]; // 8KB char buffer (16KB memory)
      while (true) {
        int charsRead = reader.read(buffer);
        if (charsRead == -1) {
          break;
        }
        writer.write(buffer, 0, charsRead);
        writer.flush(); // 即座に出力してメモリを解放
      }
    }
  }
}
