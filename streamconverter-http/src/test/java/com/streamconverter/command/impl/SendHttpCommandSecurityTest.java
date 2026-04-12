package com.streamconverter.command.impl;

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

  @Test
  @DisplayName("無効なプロトコル（ftp, file, javascript）はブロックされる")
  void testInvalidProtocolBlocked() {
    IllegalArgumentException ftpEx =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("ftp://example.com"),
            "FTPプロトコルはIllegalArgumentExceptionをスローするべき");
    assertTrue(
        ftpEx.getMessage().contains("HTTP and HTTPS"),
        "エラーメッセージに許可プロトコルの説明が含まれるべき");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("file:///tmp/test"),
        "fileプロトコルはIllegalArgumentExceptionをスローするべき");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("javascript:alert('xss')"),
        "JavaScriptプロトコルはIllegalArgumentExceptionをスローするべき");
  }

  @Test
  @DisplayName("スキームなしのURLはブロックされる")
  void testNoSchemeBlocked() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("example.com/api"),
            "スキームなしのURLはIllegalArgumentExceptionをスローするべき");
    assertTrue(
        ex.getMessage().contains("scheme"),
        "エラーメッセージにschemeに関する説明が含まれるべき");
  }

  @Test
  @DisplayName("IPv6ループバックアドレス（[::1]）はブロックされる")
  void testIpv6LoopbackBlocked() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("http://[::1]:8080"),
            "IPv6 localhostアクセスはIllegalArgumentExceptionをスローするべき");
    assertTrue(
        ex.getMessage().contains("localhost") || ex.getMessage().contains("private"),
        "エラーメッセージにlocalhost or privateが含まれるべき");
  }

  @Test
  @DisplayName("10.x.x.x および 172.16.x.x のプライベートIPレンジはブロックされる")
  void testPrivateIpRangesBlocked() {
    IllegalArgumentException ex10 =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("http://10.0.0.1"),
            "10.x.x.xアクセスはIllegalArgumentExceptionをスローするべき");
    assertTrue(
        ex10.getMessage().contains("private") || ex10.getMessage().contains("localhost"),
        "エラーメッセージにprivate or localhostが含まれるべき");

    IllegalArgumentException ex172 =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("http://172.16.0.1"),
            "172.16-31.x.xアクセスはIllegalArgumentExceptionをスローするべき");
    assertTrue(
        ex172.getMessage().contains("private") || ex172.getMessage().contains("localhost"),
        "エラーメッセージにprivate or localhostが含まれるべき");
  }
}
