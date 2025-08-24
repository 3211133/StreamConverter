package com.streamConverter;

import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the StreamConverter application.
 *
 * <p>This class demonstrates the usage of the StreamConverter with sample commands.
 */
public class Main {
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  /** Prevent instantiation. */
  private Main() {}

  /**
   * Main method to run the StreamConverter application.
   *
   * @param args Command line arguments (not used).
   * @throws IOException If an I/O error occurs during the execution.
   */
  public static void main(String[] args) throws IOException {
    LOG.info("Starting StreamConverter application");
    IStreamCommand[] commands = {
      new SampleStreamCommand("0"), new SampleStreamCommand("1"), new SampleStreamCommand("2")
    };
    StreamConverter converter = new StreamConverter(commands);
    try (InputStream inputStream =
            new ByteArrayInputStream("any message".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      converter.run(inputStream, outputStream);
      if (LOG.isInfoEnabled()) {
        LOG.info("Processing result: {}", outputStream.toString(StandardCharsets.UTF_8));
      }
    } catch (IOException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Application execution failed: {}", e.getMessage(), e);
      }
    }

    if (LOG.isInfoEnabled()) {
      LOG.info("StreamConverter application completed");
    }
  }
}
