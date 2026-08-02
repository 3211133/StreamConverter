package com.streamconverter.command.impl;

import com.google.common.net.InetAddresses;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * SSRF（Server Side Request Forgery）防御のためのURL検証を担うクラス。
 *
 * <p>{@link SendHttpCommand} から検証責務を分離したもので、次の2段階の防御を提供する。
 *
 * <ul>
 *   <li>{@link #validateAndSanitizeUrl(String)} — コマンド生成時のスキーム／ホスト検証
 *   <li>{@link #revalidateHostForSsrf(String)} — 送信直前の再DNS解決による DNS rebinding 検出
 * </ul>
 *
 * <p>DNS解決は {@link InetAddressResolver} 経由で行うため、テストから解決結果を差し替えられる。
 */
final class SsrfUrlValidator {

  private final InetAddressResolver inetAddressResolver;

  SsrfUrlValidator(InetAddressResolver inetAddressResolver) {
    this.inetAddressResolver = Objects.requireNonNull(inetAddressResolver);
  }

  /**
   * URLの検証とサニタイゼーションを行う
   *
   * @param url 検証するURL
   * @return 検証済みURL
   * @throws IllegalArgumentException URLが無効な場合
   */
  String validateAndSanitizeUrl(String url) {
    Objects.requireNonNull(url, "URL cannot be null");

    String trimmedUrl = url.trim();
    if (trimmedUrl.isEmpty()) {
      throw new IllegalArgumentException("URL cannot be empty");
    }

    try {
      URI uri = new URI(trimmedUrl);
      validateScheme(uri.getScheme());
      validateHost(uri.getHost());
      return trimmedUrl;
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid URL format: " + e.getMessage(), e);
    }
  }

  /** スキームが http / https であることを検証する。 */
  private static void validateScheme(String scheme) {
    if (scheme == null) {
      throw new IllegalArgumentException("URL must have a scheme (http or https)");
    }

    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      throw new IllegalArgumentException("Only HTTP and HTTPS protocols are allowed");
    }
  }

  /** ホストが存在し、かつローカルホスト／内部IPでないことを検証する。 */
  private void validateHost(String host) {
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("URL must have a valid host");
    }

    // ローカルホストや内部IPアドレスへのアクセスを防ぐ
    if (isLocalhost(host) || isPrivateIpAddress(host)) {
      throw new IllegalArgumentException(
          "Access to localhost or private IP addresses is not allowed");
    }
  }

  /**
   * execute()直前にホスト名を再DNS解決してSSRF（DNS rebinding）を検出する。
   *
   * <p>リテラルIPはDNS rebindingの対象外なのでスキップする。ホスト名の場合のみ再解決を行い、 localhost判定またはプライベートIP判定に変化していれば {@link
   * IOException} をスローする。
   *
   * @param url 検証済みのURL
   * @throws IOException DNS rebinding を検出した場合、または再解決に失敗した場合
   */
  void revalidateHostForSsrf(String url) throws IOException {
    try {
      URI uri = new URI(url);
      String host = uri.getHost();
      if (host == null || InetAddresses.isInetAddress(host)) {
        return;
      }
      if (isLocalhost(host)) {
        throw new IOException("DNS rebinding detected: host resolved to localhost: " + host);
      }
      InetAddress nonRoutable = findNonRoutableAddress(inetAddressResolver.getAllByName(host));
      if (nonRoutable != null) {
        throw new IOException(
            "DNS rebinding detected: host resolved to non-routable address: " + nonRoutable);
      }
    } catch (URISyntaxException | UnknownHostException e) {
      throw new IOException("SSRF revalidation failed: " + e.getMessage(), e);
    }
  }

  /** 解決結果のうち最初の非ルータブルアドレスを返す。すべてルータブルなら null を返す。 */
  private static InetAddress findNonRoutableAddress(InetAddress[] addresses) {
    for (InetAddress address : addresses) {
      if (isNonRoutable(address)) {
        return address;
      }
    }
    return null;
  }

  /** ローカルホストかどうかを判定する */
  // AvoidUsingHardCodedIP: ループバックアドレスの拒否そのものが目的であり、
  // これらのリテラルは設定値ではなく SSRF 防御の判定基準である。
  @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
  private boolean isLocalhost(String host) {
    if (host == null) {
      return false;
    }

    // Handle IPv6 addresses with brackets
    String cleanHost =
        host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;

    return "localhost".equalsIgnoreCase(cleanHost)
        || "127.0.0.1".equals(cleanHost)
        || "::1".equals(cleanHost);
  }

  /** ホストがプライベートIPに解決されるかを判定する。 リテラルIPはGuavaで即解析し、ホスト名はDNS解決後に検査する。 解決不能なホスト名は例外をスローしてアクセスを拒否する。 */
  private boolean isPrivateIpAddress(String host) {
    // まずリテラルIPとして解析を試みる
    if (InetAddresses.isInetAddress(host)) {
      InetAddress address = InetAddresses.forString(host);
      return isNonRoutable(address);
    }
    // ホスト名: DNS解決して全アドレスを検査する
    try {
      return findNonRoutableAddress(inetAddressResolver.getAllByName(host)) != null;
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Cannot resolve hostname: " + host, e);
    }
  }

  private static boolean isNonRoutable(InetAddress address) {
    return address.isSiteLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isAnyLocalAddress()
        || address.isMulticastAddress();
  }
}
