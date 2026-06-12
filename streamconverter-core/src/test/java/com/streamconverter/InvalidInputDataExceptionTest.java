package com.streamconverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for InvalidInputDataException. */
@DisplayName("InvalidInputDataException Tests")
class InvalidInputDataExceptionTest {

  @Test
  @DisplayName("メッセージのみのコンストラクタはmessageを保持すること")
  void testConstructorWithMessage() {
    String message = "Invalid value in column 'age'";

    InvalidInputDataException exception = new InvalidInputDataException(message);

    assertEquals(message, exception.getMessage(), "コンストラクタに渡したmessageが保持されること");
  }

  @Test
  @DisplayName("メッセージとcauseを指定するコンストラクタは両方を保持すること")
  void testConstructorWithMessageAndCause() {
    String message = "Failed to parse CSV row";
    IllegalArgumentException cause = new IllegalArgumentException("Unexpected token");

    InvalidInputDataException exception = new InvalidInputDataException(message, cause);

    assertEquals(message, exception.getMessage(), "コンストラクタに渡したmessageが保持されること");
    assertSame(cause, exception.getCause(), "コンストラクタに渡したcauseが同一インスタンスで保持されること");
  }

  @Test
  @DisplayName("StreamProcessingExceptionとしてcatchできること")
  void testCaughtAsStreamProcessingException() {
    StreamProcessingException caught = null;
    try {
      throw new InvalidInputDataException("invalid input");
    } catch (StreamProcessingException e) {
      caught = e;
    }

    assertInstanceOf(
        InvalidInputDataException.class,
        caught,
        "InvalidInputDataExceptionはcatch (StreamProcessingException)で捕捉できること");
  }

  @Test
  @DisplayName("IOExceptionとしてcatchできること")
  void testCaughtAsIOException() {
    IOException caught = null;
    try {
      throw new InvalidInputDataException("invalid input");
    } catch (IOException e) {
      caught = e;
    }

    assertInstanceOf(
        InvalidInputDataException.class,
        caught,
        "InvalidInputDataExceptionはcatch (IOException)で捕捉できること");
  }
}
