package com.streamconverter;

import static org.junit.jupiter.api.Assertions.*;

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
            (in, out) -> in.transferTo(out),
            (in, out) -> in.transferTo(out),
            (in, out) -> in.transferTo(out));

    converter.run(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result, "Multiple commands should preserve data through chain");
  }

  @Test
  public void testEmptyInputHandling() throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    StreamConverter converter = StreamConverter.create((in, out) -> in.transferTo(out));

    converter.run(input, output);

    assertEquals(0, output.toByteArray().length, "Empty input should produce empty output");
  }

  @Test
  public void testLargeDataProcessing() throws IOException {
    StringBuilder largeData = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      largeData.append("Line ").append(i).append(System.lineSeparator());
    }

    String testData = largeData.toString();
    ByteArrayInputStream input =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    StreamConverter converter = StreamConverter.create((in, out) -> in.transferTo(out));

    converter.run(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result, "Large data should be processed correctly");
  }
}
