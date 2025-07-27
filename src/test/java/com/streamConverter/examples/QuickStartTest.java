package com.streamConverter.examples;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test class for QuickStart example to ensure it runs without errors. */
public class QuickStartTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  @BeforeEach
  public void setUpStreams() {
    System.setOut(new PrintStream(outContent));
  }

  @AfterEach
  public void restoreStreams() {
    System.setOut(originalOut);
  }

  @Test
  public void testQuickStartRunsWithoutError() {
    assertDoesNotThrow(
        () -> {
          QuickStart.main(new String[] {});
        },
        "QuickStart should run without throwing any exception");
  }

  @Test
  public void testQuickStartProducesOutput() throws IOException {
    QuickStart.main(new String[] {});
    String output = outContent.toString();
    assertFalse(output.isEmpty(), "QuickStart should produce some output");
  }
}
