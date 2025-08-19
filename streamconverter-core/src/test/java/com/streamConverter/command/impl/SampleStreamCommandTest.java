package com.streamConverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamConverter.test.StreamingTestUtils.TrackingInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SampleStreamCommand Test")
class SampleStreamCommandTest {

  private String testInput;

  @BeforeEach
  void setUp() {
    testInput = "Hello, SampleStreamCommand!";
  }

  @Test
  @DisplayName("Default Constructor")
  void testDefaultConstructor() {
    // デフォルトコンストラクタのテスト
    SampleStreamCommand command = new SampleStreamCommand();
    assertEquals("default", command.toString().replaceAll(".*\\[id=(.*)\\]", "$1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"test", "sample", "custom"})
  @DisplayName("Constructor with Argument")
  void testConstructorWithArgument(String id) {
    // 引数付きコンストラクタのテスト
    SampleStreamCommand command = new SampleStreamCommand(id);
    assertEquals(id, command.toString().replaceAll(".*\\[id=(.*)\\]", "$1"));
  }

  @Test
  @DisplayName("Execute Normal Case: Stream Copy")
  void testExecuteWithValidStreams() throws IOException {
    // 正常系のexecuteメソッドテスト
    SampleStreamCommand command = new SampleStreamCommand("test");

    try (InputStream inputStream =
            new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      command.execute(inputStream, outputStream);

      // 結果の検証 - 入力がそのまま出力されるはず
      assertEquals(testInput, outputStream.toString(StandardCharsets.UTF_8));
    }
  }

  @Test
  @DisplayName("Execute Error Case: Null Input Stream")
  void testExecuteWithNullInputStream() {
    // null入力ストリームでのexecuteメソッドテスト
    SampleStreamCommand command = new SampleStreamCommand("test");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    Exception exception =
        assertThrows(
            IOException.class,
            () -> {
              command.execute(null, outputStream);
            });

    // The root cause should be NullPointerException
    assertTrue(
        exception.getCause() instanceof NullPointerException,
        "Root cause should be NullPointerException");
  }

  @Test
  @DisplayName("Execute Error Case: Null Output Stream")
  void testExecuteWithNullOutputStream() {
    // null出力ストリームでのexecuteメソッドテスト
    SampleStreamCommand command = new SampleStreamCommand("test");
    InputStream inputStream = new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));

    Exception exception =
        assertThrows(
            IOException.class,
            () -> {
              command.execute(inputStream, null);
            });

    // The root cause should be NullPointerException
    assertTrue(
        exception.getCause() instanceof NullPointerException,
        "Root cause should be NullPointerException");
  }

  @Test
  @DisplayName("Execute: Empty Input Stream")
  void testExecuteWithEmptyInputStream() throws IOException {
    // 空の入力ストリームでのexecuteメソッドテスト
    SampleStreamCommand command = new SampleStreamCommand("test");

    try (InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      command.execute(inputStream, outputStream);

      // 結果の検証 - 空の出力になるはず
      assertEquals("", outputStream.toString(StandardCharsets.UTF_8));
    }
  }

  @Test
  @DisplayName("ToString Output Verification")
  void testToString() {
    // toStringメソッドのテスト
    String id = "testId";
    SampleStreamCommand command = new SampleStreamCommand(id);

    String expected = "SampleStreamCommand [id=" + id + "]";
    assertEquals(expected, command.toString());
  }

  @Test
  @DisplayName("Verify streaming sample processing behavior")
  void testStreamingSampleProcessingBehavior() throws IOException {
    SampleStreamCommand command = new SampleStreamCommand("streamingTest");

    // Create moderate-sized data to observe streaming behavior
    StringBuilder inputBuilder = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      inputBuilder.append("Sample data line ").append(i).append("\n");
    }
    String inputData = inputBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(inputData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute sample processing
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during processing");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify data was processed incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read during processing");

    // Verify the content was processed correctly (SampleStreamCommand typically echoes input)
    String expectedOutput = inputData;
    assertEquals(expectedOutput, monitoringOutputStream.getContent());
  }

  @Test
  @DisplayName("Verify incremental sample stream processing")
  void testIncrementalSampleStreamProcessing() throws IOException {
    SampleStreamCommand command = new SampleStreamCommand("incrementalTest");

    // Create varied data to force incremental processing
    StringBuilder inputBuilder = new StringBuilder();
    for (int i = 1; i <= 200; i++) {
      inputBuilder.append(
          String.format(
              "Line %03d: Sample stream data with content %s\n",
              i, "A".repeat(i % 50))); // Variable length content
    }
    String inputData = inputBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(inputData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - perform incremental processing
    long processingStart = System.nanoTime();
    command.execute(trackingInputStream, monitoringOutputStream);
    long processingEnd = System.nanoTime();

    // Then - verify incremental processing characteristics
    assertTrue(monitoringOutputStream.hasWriteOccurred(), "Output should be written");
    assertTrue(trackingInputStream.isFullyRead(), "Input should be fully processed");

    // Verify substantial data was processed
    assertTrue(
        trackingInputStream.getBytesRead() > 1000,
        "Should have processed substantial amount of sample data");

    long processingTime = processingEnd - processingStart;
    assertTrue(processingTime > 0, "Processing should take measurable time");

    // Verify content correctness - SampleStreamCommand should echo input
    assertEquals(inputData, monitoringOutputStream.getContent());
  }
}
