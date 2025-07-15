package com.streamConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  void testExceptionIsRuntimeException() {
    StreamProcessingException exception = new StreamProcessingException("Test");

    // Should be a RuntimeException
    assertNotNull(exception);
    assertEquals(RuntimeException.class, exception.getClass().getSuperclass());
  }
}
