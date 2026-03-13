package com.streamconverter.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** InputValidatorのテストクラス */
class InputValidatorTest {

  // ---- validateUrl: 正常系 ----

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://example.com",
        "https://example.com",
        "https://example.com/path?query=1",
        "HTTP://EXAMPLE.COM",
        "HTTPS://EXAMPLE.COM/path",
      })
  @DisplayName("有効なHTTP/HTTPS URLは例外を投げない")
  void validateUrl_validUrls_doesNotThrow(String url) {
    assertDoesNotThrow(() -> InputValidator.validateUrl(url));
  }

  @Test
  @DisplayName("前後に空白を含むURLはトリムされて正常に受け付けられる")
  void validateUrl_urlWithWhitespace_doesNotThrow() {
    assertDoesNotThrow(() -> InputValidator.validateUrl("  https://example.com  "));
  }

  // ---- validateUrl: null / 空 ----

  @Test
  @DisplayName("null URLはIllegalArgumentExceptionを投げる")
  void validateUrl_null_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> InputValidator.validateUrl(null));
  }

  @Test
  @DisplayName("空文字URLはIllegalArgumentExceptionを投げる")
  void validateUrl_empty_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> InputValidator.validateUrl(""));
  }

  @Test
  @DisplayName("空白のみのURLはIllegalArgumentExceptionを投げる")
  void validateUrl_blank_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> InputValidator.validateUrl("   "));
  }

  // ---- validateUrl: URIパース失敗 ----

  @Test
  @DisplayName("不正な形式のURLはIllegalArgumentExceptionを投げる")
  void validateUrl_malformedUri_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateUrl("http://[invalid"));
  }

  // ---- validateUrl: 非HTTP(S)スキーム ----

  @ParameterizedTest
  @ValueSource(strings = {"ftp://example.com", "file:///etc/passwd", "javascript:alert(1)"})
  @DisplayName("HTTP/HTTPS以外のスキームはIllegalArgumentExceptionを投げる")
  void validateUrl_nonHttpScheme_throwsIllegalArgumentException(String url) {
    assertThrows(IllegalArgumentException.class, () -> InputValidator.validateUrl(url));
  }

  // ---- validateUrl: ホスト欠落 ----

  @Test
  @DisplayName("ホスト名がないURLはIllegalArgumentExceptionを投げる")
  void validateUrl_missingHost_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateUrl("http:///path"));
  }

  @Test
  @DisplayName("スキームのみのURLはIllegalArgumentExceptionを投げる")
  void validateUrl_schemeOnly_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateUrl("https://"));
  }
}
