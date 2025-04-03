package com.streamConverter;

import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Main class for the StreamConverter application.
 *
 * <p>This class demonstrates the usage of the StreamConverter with sample commands.
 */
public class Main {

  /**
   * Main method to run the StreamConverter application.
   *
   * @param args Command line arguments (not used).
   * @throws IOException If an I/O error occurs during the execution.
   */
  public static void main(String[] args) throws IOException {
    System.out.println("Hello World!");
    IStreamCommand[] commands = {
      new SampleStreamCommand("0"), new SampleStreamCommand("1"), new SampleStreamCommand("2")
    };
    StreamConverter converter = new StreamConverter(commands);
    try (InputStream inputStream = new ByteArrayInputStream("any message".getBytes());
        OutputStream outputStream = new ByteArrayOutputStream()) {
      converter.run(inputStream, outputStream);
      System.out.println("result:" + outputStream.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Goodbye World!");
  }
}
