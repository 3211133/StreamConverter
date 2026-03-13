package com.streamconverter.security;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 入力バリデーションを集約するユーティリティクラス。
 *
 * <p>各コマンドに分散していたセキュリティ検証ロジックをここに集約することで、 一貫したバリデーション基準を維持し、重複実装を防ぐ。
 *
 * <p>現在サポートするバリデーション:
 *
 * <ul>
 *   <li>URL 検証 ({@link #validateUrl(String)}) — HTTP/HTTPS スキームのみ許可
 * </ul>
 *
 * <p>注意: {@code DatabaseFetchRule} などのモジュール固有のバリデーション (SQL パラメータ化クエリ等) は依存関係の逆転を避けるため、各モジュール内に留める。
 */
public final class InputValidator {

  private InputValidator() {
    // ユーティリティクラスのインスタンス化を禁止
  }

  /**
   * URL を検証し、HTTP または HTTPS スキームのみを許可する。
   *
   * <p>以下の検証を行う:
   *
   * <ul>
   *   <li>null / 空文字チェック
   *   <li>URI パース可能性チェック
   *   <li>スキームが {@code http} または {@code https} であることの確認
   *   <li>ホスト名が存在することの確認
   * </ul>
   *
   * <p>ローカルホストやプライベート IP アドレスのブロックは呼び出し側コマンドの責務とする。 SSRF 対策が必要な場合は {@code SendHttpCommand}
   * のバリデーションロジックを参照のこと。
   *
   * @param url 検証する URL 文字列
   * @throws IllegalArgumentException url が null、空、不正な形式、または HTTP/HTTPS 以外のスキームの場合
   */
  public static void validateUrl(String url) {
    if (url == null) {
      throw new IllegalArgumentException("URL cannot be null");
    }

    String trimmedUrl = url.trim();
    if (trimmedUrl.isEmpty()) {
      throw new IllegalArgumentException("URL cannot be empty");
    }

    URI uri;
    try {
      uri = new URI(trimmedUrl);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid URL format: " + e.getMessage(), e);
    }

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
  }
}
