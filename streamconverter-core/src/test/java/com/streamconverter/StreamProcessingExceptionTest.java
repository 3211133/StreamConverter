package com.streamconverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Unit tests for StreamProcessingException. */
class StreamProcessingExceptionTest {

  @Test
  void testConstructorWithMessage() {
    String message = "Test error message";
    StreamProcessingException exception = new StreamProcessingException(message);

    assertEquals(message, exception.getMessage());
  }

  @Test
  void testConstructorWithMessageAndCause() {
    String message = "Test error message";
    IOException cause = new IOException("Underlying IO error");

    StreamProcessingException exception = new StreamProcessingException(message, cause);

    assertEquals(message, exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  @Test
  void testConstructorWithCause() {
    IOException cause = new IOException("Underlying IO error");

    StreamProcessingException exception = new StreamProcessingException(cause);

    assertSame(cause, exception.getCause());
    assertNotNull(exception.getMessage());
  }

  @Test
  void testExceptionIsIOException() {
    StreamProcessingException exception = new StreamProcessingException("Test");

    // Should be an IOException (checked exception honouring IStreamCommand.execute() contract)
    assertNotNull(exception);
    assertEquals(IOException.class, exception.getClass().getSuperclass());
  }

  @Test
  void streamProcessingExceptionIsCaughtAsIOException() {
    IOException caught = null;
    try {
      throw new StreamProcessingException("test");
    } catch (IOException e) {
      caught = e;
    }
    assertInstanceOf(
        StreamProcessingException.class,
        caught,
        "StreamProcessingException は catch (IOException) で捕捉できること");
  }
}
