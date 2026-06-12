package com.streamconverter;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for UncheckedStreamException. */
@DisplayName("UncheckedStreamException Tests")
class UncheckedStreamExceptionTest {

  @Test
  @DisplayName("getCause()は渡したIOExceptionと同一インスタンスをIOException型で返すこと")
  void testGetCauseReturnsSameIOExceptionInstance() {
    IOException original = new IOException("underlying I/O failure");

    UncheckedStreamException exception = new UncheckedStreamException(original);

    IOException cause = exception.getCause();
    assertSame(original, cause, "getCause()はコンストラクタに渡したIOExceptionと同一インスタンスを返すこと");
  }

  @Test
  @DisplayName("cause がnullの場合はNullPointerExceptionになること")
  void testNullCauseThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> new UncheckedStreamException(null),
        "cause にnullを渡した場合はNullPointerExceptionがスローされること");
  }
}
