package com.streamconverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** SendHttpCommandのセキュリティ機能専用テスト */
class SendHttpCommandSecurityTest {

  @Test
  @DisplayName("localhost URLで例外がスローされることを確認")
  void testLocalhostValidation() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("http://localhost:8080"),
            "localhost URLはIllegalArgumentExceptionをスローするべき");

    assertTrue(exception.getMessage().contains("localhost"), "エラーメッセージにlocalhostが含まれるべき");
  }

  @Test
  @DisplayName("127.0.0.1 URLで例外がスローされることを確認")
  void testLoopbackValidation() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("http://127.0.0.1:8080"),
            "127.0.0.1 URLはIllegalArgumentExceptionをスローするべき");

    assertTrue(
        exception.getMessage().contains("localhost") || exception.getMessage().contains("private"),
        "エラーメッセージにlocalhost or privateが含まれるべき");
  }

  @Test
  @DisplayName("プライベートIP URLで例外がスローされることを確認")
  void testPrivateIpValidation() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("http://192.168.1.1"),
            "192.168.x.x URLはIllegalArgumentExceptionをスローするべき");

    assertTrue(exception.getMessage().contains("private"), "エラーメッセージにprivateが含まれるべき");
  }

  @Test
  @DisplayName("有効な外部URLは許可されることを確認")
  void testValidExternalUrl() {
    assertDoesNotThrow(
        () -> new SendHttpCommand("https://httpbin.org/post"), "有効な外部URLは例外をスローしないべき");
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

  // ---- #653 fix: hostname DNS resolution ----

  @Test
  @DisplayName("解決不能なホスト名は IllegalArgumentException をスローする（#653）")
  void testUnresolvableHostThrows() {
    // ホスト名がDNS解決できない場合、修正前は false を返して通過させていた。
    // 修正後は IllegalArgumentException をスローする。
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SendHttpCommand("http://this-host-does-not-exist.invalid"),
            "解決不能なホスト名はブロックされるべき");
    assertTrue(
        ex.getMessage().contains("resolve") || ex.getMessage().contains("Cannot"),
        "エラーメッセージにhostname解決失敗の説明が含まれるべき");
  }

  @Test
  @Tag("network")
  @DisplayName("ループバックIPに解決されるホスト名はブロックされる（DNS経由・#653）")
  void testHostnameResolvingToLoopbackIsBlocked() {
    // nip.io は 127.0.0.1.nip.io → 127.0.0.1 に解決する公開サービス。
    // DNS解決後の検査が機能していれば IllegalArgumentException がスローされる。
    // ネットワーク依存のため @Tag("network") で分離。
    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("http://127.0.0.1.nip.io"),
        "127.0.0.1 に解決されるホスト名はブロックされるべき");
  }
}
