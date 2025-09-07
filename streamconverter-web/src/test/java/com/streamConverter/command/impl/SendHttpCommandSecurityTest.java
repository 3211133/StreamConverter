package com.streamConverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SendHttpCommandのセキュリティ機能専用テスト */
class SendHttpCommandSecurityTest {

  @Test
  @DisplayName("localhost URLで例外がスローされることを確認")
  void testLocalhostValidation() {
    System.out.println("Testing localhost validation...");

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("http://localhost:8080"),
            "localhost URLはIllegalArgumentExceptionをスローするべき");

    System.out.println("Exception message: " + exception.getMessage());
    assertTrue(exception.getMessage().contains("localhost"), "エラーメッセージにlocalhostが含まれるべき");
  }

  @Test
  @DisplayName("127.0.0.1 URLで例外がスローされることを確認")
  void testLoopbackValidation() {
    System.out.println("Testing 127.0.0.1 validation...");

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("http://127.0.0.1:8080"),
            "127.0.0.1 URLはIllegalArgumentExceptionをスローするべき");

    System.out.println("Exception message: " + exception.getMessage());
    assertTrue(
        exception.getMessage().contains("localhost") || exception.getMessage().contains("private"),
        "エラーメッセージにlocalhost or privateが含まれるべき");
  }

  @Test
  @DisplayName("プライベートIP URLで例外がスローされることを確認")
  void testPrivateIpValidation() {
    System.out.println("Testing private IP validation...");

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("http://192.168.1.1"),
            "192.168.x.x URLはIllegalArgumentExceptionをスローするべき");

    System.out.println("Exception message: " + exception.getMessage());
    assertTrue(exception.getMessage().contains("private"), "エラーメッセージにprivateが含まれるべき");
  }

  @Test
  @DisplayName("有効な外部URLは許可されることを確認")
  void testValidExternalUrl() {
    System.out.println("Testing valid external URL...");

    assertDoesNotThrow(
        () -> new SendHttpCommand("https://httpbin.org/post"), "有効な外部URLは例外をスローしないべき");

    System.out.println("Valid external URL accepted successfully");
  }
}
