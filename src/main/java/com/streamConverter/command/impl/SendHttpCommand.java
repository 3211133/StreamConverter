/**
 * Copyright (c) 2023, Stream Converter Project All rights reserved.
 * 指定された通信先にOutputStreamを送信するコマンドクラス。
 *
 * <p>このクラスは、指定された通信先にOutputStreamを送信するためのコマンドを実装します。 ストリームを使用して、データを送信します。 送信先のURLはコンストラクタで指定されます。
 * 送信先のURLは、HTTP POSTリクエストを使用してデータを送信します。 送信先のURLは、HTTPまたはHTTPSで始まる必要があります。
 * 送信先のURLは、コンストラクタで指定されたURLに基づいて決定されます。
 *
 * <p>このクラスは、ストリーム変換のコマンドを実装するための抽象クラスを拡張しています。 ストリーム変換のコマンドは、ストリームを使用してデータを変換するためのものです。
 *
 * <p>レスポンスを受信してOutputStreamに書き込むことができます。
 */
package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/** 指定された通信先にOutputStreamを送信するコマンドクラス。 */
public class SendHttpCommand extends AbstractStreamCommand {

  private String url;

  /**
   * デフォルトコンストラクタ
   *
   * @param url 送信先のURL
   * @throws IllegalArgumentException URLが無効な場合
   */
  public SendHttpCommand(String url) {
    super();
    this.url = validateAndSanitizeUrl(url);
  }

  /**
   * URLの検証とサニタイゼーションを行う
   *
   * @param url 検証するURL
   * @return 検証済みURL
   * @throws IllegalArgumentException URLが無効な場合
   */
  private String validateAndSanitizeUrl(String url) {
    Objects.requireNonNull(url, "URL cannot be null");

    String trimmedUrl = url.trim();
    if (trimmedUrl.isEmpty()) {
      throw new IllegalArgumentException("URL cannot be empty");
    }

    try {
      URI uri = new URI(trimmedUrl);
      String scheme = uri.getScheme();

      if (scheme == null) {
        throw new IllegalArgumentException("URL must have a scheme (http or https)");
      }

      if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
        throw new IllegalArgumentException("Only HTTP and HTTPS protocols are allowed");
      }

      String host = uri.getHost();
      if (host == null || host.trim().isEmpty()) {
        throw new IllegalArgumentException("URL must have a valid host");
      }

      // ローカルホストや内部IPアドレスへのアクセスを防ぐ
      if (isLocalhost(host) || isPrivateIpAddress(host)) {
        throw new IllegalArgumentException(
            "Access to localhost or private IP addresses is not allowed");
      }

      return trimmedUrl;
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid URL format: " + e.getMessage(), e);
    }
  }

  /** ローカルホストかどうかを判定する */
  private boolean isLocalhost(String host) {
    return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
  }

  /** プライベートIPアドレスかどうかを判定する */
  private boolean isPrivateIpAddress(String host) {
    // 簡単なプライベートIPアドレスチェック
    return host.startsWith("192.168.")
        || host.startsWith("10.")
        || host.startsWith("172.16.")
        || host.startsWith("172.17.")
        || host.startsWith("172.18.")
        || host.startsWith("172.19.")
        || host.startsWith("172.20.")
        || host.startsWith("172.21.")
        || host.startsWith("172.22.")
        || host.startsWith("172.23.")
        || host.startsWith("172.24.")
        || host.startsWith("172.25.")
        || host.startsWith("172.26.")
        || host.startsWith("172.27.")
        || host.startsWith("172.28.")
        || host.startsWith("172.29.")
        || host.startsWith("172.30.")
        || host.startsWith("172.31.");
  }

  /**
   * ストリームを指定されたURLに送信します。
   *
   * @param inputStream 入力ストリーム
   * @param outputStream 出力ストリーム
   * @throws IOException 入出力エラーが発生した場合
   */
  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    // TODO: HTTP POSTリクエストの実装が必要
    // 現在は未実装のため、UnsupportedOperationExceptionを投げる
    throw new UnsupportedOperationException(
        "HTTP communication is not yet implemented. "
            + "This command requires implementation of HTTP POST request functionality to URL: "
            + url);
  }
}
