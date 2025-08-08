package com.streamConverter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTest {
  private static final Logger log = LoggerFactory.getLogger(LoggingTest.class);

  @Test
  @DisplayName("Logging Test")
  public void testLogging() {
    // Set up a stream to capture the output
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(outputStream);
    System.setOut(printStream);
    System.setErr(printStream);

    log.info("Info message");

    // Flush the stream to ensure all output is captured
    printStream.flush();
    String output = outputStream.toString();
    System.setOut(System.out);
    System.setErr(System.err);
    // Check if the output contains the expected log messages
    assertTrue(output.contains("Info message"));
  }
}
