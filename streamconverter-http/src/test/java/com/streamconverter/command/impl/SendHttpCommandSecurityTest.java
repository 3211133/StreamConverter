package com.streamconverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
  @Tag("network")
  @DisabledIfSystemProperty(named = "skipNetworkTests", matches = "true")
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
    assertTrue(ftpEx.getMessage().contains("HTTP and HTTPS"), "エラーメッセージに許可プロトコルの説明が含まれるべき");

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
    assertTrue(ex.getMessage().contains("scheme"), "エラーメッセージにschemeに関する説明が含まれるべき");
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

  // ---- #653: ホスト名を DNS 解決してプライベート IP を検出する ----

  @Test
  @Tag("network")
  @DisabledIfSystemProperty(named = "skipNetworkTests", matches = "true")
  @DisplayName("解決不能なホスト名は IllegalArgumentException をスローする（#653）")
  void testUnresolvableHostThrows() {
    // 修正前: InetAddresses.forString() がホスト名で IllegalArgumentException → catch して false
    // を返す。つまり解決不能ホスト名が通過する。
    // 修正後: InetAddress.getAllByName() で解決を試みて失敗したら IllegalArgumentException をスロー。
    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("http://this-host-does-not-exist.invalid"),
        "解決不能なホスト名はブロックされるべき");
  }

  // ---- #715: DNS rebinding TOCTOU ----

  /** 1回目は外部IP、2回目以降はプライベートIPを返すDNSスタブ。 */
  static final class RebindingDnsStub implements InetAddressResolver {
    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public InetAddress[] getAllByName(String host) throws UnknownHostException {
      if (callCount.incrementAndGet() == 1) {
        return new InetAddress[] {InetAddress.getByName("8.8.8.8")}; // 外部IP
      }
      return new InetAddress[] {InetAddress.getByName("192.168.1.1")}; // プライベートIP
    }

    int getCallCount() {
      return callCount.get();
    }
  }

  @Test
  @Tag("known-bug")
  @DisplayName("Bug証明 #715: DNS rebinding TOCTOU - 2回目のDNS解決がプライベートIPを返す状態でexecute()が例外をスローしない")
  void bug_715_dnsRebinding_executeSucceedsWhenSecondResolutionReturnsPrivateIp() throws Exception {
    // Arrange: 1回目=外部IP（コンストラクタ検証をパス）、2回目=プライベートIP（DNS rebinding後）
    RebindingDnsStub stub = new RebindingDnsStub();

    WebClient dummyWebClient =
        WebClient.builder()
            .exchangeFunction(req -> Mono.just(ClientResponse.create(HttpStatus.OK).build()))
            .build();

    // Act 1: コンストラクタ（1回目DNS解決 → 外部IP → 検証パス）
    SendHttpCommand command =
        new SendHttpCommand("http://rebind.example.test", dummyWebClient, stub);

    assertEquals(1, stub.getCallCount(), "コンストラクタで1回DNS解決されるべき");

    // この時点でスタブの2回目以降はプライベートIPを返す状態になっている
    // つまり「DNS rebinding後」の状態を模倣している
    InetAddress[] secondResolution = stub.getAllByName("rebind.example.test");
    boolean secondResolutionIsPrivate =
        java.util.Arrays.stream(secondResolution).anyMatch(InetAddress::isSiteLocalAddress);
    assertTrue(secondResolutionIsPrivate, "前提: 2回目のDNS解決はプライベートIPを返す");

    // Act 2: execute() — 修正後なら「ホスト名が再解決されてプライベートIPになった」ことを
    // 検出して IOException をスローするべき。バグがある今は何も検証せず正常終了する。
    // assertThrows で IOException が出ることを期待するが、バグがある今は出ないため FAIL する。
    assertThrows(
        java.io.IOException.class,
        () ->
            command.execute(new ByteArrayInputStream("x".getBytes()), new ByteArrayOutputStream()),
        "【バグ #715】execute()はDNS rebinding後のプライベートIPへの接続をブロックすべきだが、例外をスローしない");
  }
}
