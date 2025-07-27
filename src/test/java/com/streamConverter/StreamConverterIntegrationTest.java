package com.streamConverter;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.SendHttpCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Integration tests for StreamConverter with multiple commands. */
public class StreamConverterIntegrationTest {

  @Test
  public void testMultipleCommandsChain() throws IOException {
    String testData = "test data for processing";
    ByteArrayInputStream input =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    StreamConverter converter =
        StreamConverter.create(
            new SampleStreamCommand("first"),
            new SampleStreamCommand("second"),
            new SampleStreamCommand("third"));

    converter.run(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result, "Multiple commands should preserve data through chain");
  }

  @Test
  public void testEmptyInputHandling() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    StreamConverter converter = StreamConverter.create(new SampleStreamCommand("test"));

    converter.run(input, output);

    assertEquals(0, output.toByteArray().length, "Empty input should produce empty output");
  }

  @Test
  public void testLargeDataProcessing() throws IOException {
    StringBuilder largeData = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      largeData.append("Line ").append(i).append("\n");
    }

    String testData = largeData.toString();
    ByteArrayInputStream input =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    StreamConverter converter = StreamConverter.create(new SampleStreamCommand("large"));

    converter.run(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result, "Large data should be processed correctly");
  }

  @Test
  public void testInvalidHttpUrlHandling() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("invalid-url"),
        "Invalid URL should throw IllegalArgumentException");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("ftp://example.com"),
        "Non-HTTP protocol should throw IllegalArgumentException");

    assertThrows(
        NullPointerException.class,
        () -> new SendHttpCommand(null),
        "Null URL should throw NullPointerException");
  }

  @Test
  public void testValidHttpUrlCreation() {
    assertDoesNotThrow(
        () -> new SendHttpCommand("https://httpbin.org/post"),
        "Valid HTTPS URL should not throw exception");

    assertDoesNotThrow(
        () -> new SendHttpCommand("http://httpbin.org/post"),
        "Valid HTTP URL should not throw exception");
  }
}
