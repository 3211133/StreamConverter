package com.streamconverter.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * クラスパスリソースの取得を行うユーティリティクラス
 *
 * <p>JAR内にバンドルされたリソースを安全に取得します。
 *
 * <p>セキュリティ：
 *
 * <ul>
 *   <li>ClassLoaderはリソース名の完全一致検索を行う（パストラバーサルやファイルシステムのトラバーサルは発生しない）
 *   <li>JAR内リソースは読み取り専用（改ざん不可）
 *   <li>シンボリックリンク攻撃は不可能（ファイルシステムではない）
 *   <li>相対パス（..）やバックスラッシュを含むパスは単に「存在しないリソース」として扱われる
 * </ul>
 *
 * <p>用途：
 *
 * <ul>
 *   <li>スキーマファイル（XSD, JSON Schema等）
 *   <li>テンプレートファイル
 *   <li>設定ファイル（application.properties等）
 * </ul>
 */
public final class ClasspathResourceValidator {

  private ClasspathResourceValidator() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * クラスパスリソースを取得します
   *
   * <p>ClassLoaderを使用してリソースを取得します。リソース名は完全一致で検索されるため、 ファイルシステムのようなパストラバーサルは発生しません。
   *
   * <p>注意事項：
   *
   * <ul>
   *   <li>リソース名はスラッシュ（/）区切りで指定（例: "schemas/test.xsd"）
   *   <li>先頭にスラッシュを含めない（ClassLoaderの仕様）
   *   <li>バックスラッシュや相対パス（..）は単に存在しないリソースとして扱われる
   * </ul>
   *
   * @param resourcePath クラスパスからの相対パス（例: "schemas/test.xsd"）
   * @return リソースのInputStream
   * @throws NullPointerException パスがnullの場合
   * @throws IllegalArgumentException パスが空、またはリソースが存在しない場合
   */
  public static InputStream getResourceAsStream(String resourcePath) {
    Objects.requireNonNull(resourcePath, "Resource path cannot be null");

    // 先頭スラッシュを除去（ClassLoaderの仕様に合わせる）
    String normalizedPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

    if (normalizedPath.isEmpty()) {
      throw new IllegalArgumentException("Resource path cannot be empty");
    }

    // Context ClassLoaderを優先し、なければクラスのClassLoaderを使用
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = ClasspathResourceValidator.class.getClassLoader();
    }

    InputStream stream = classLoader.getResourceAsStream(normalizedPath);

    if (stream == null) {
      throw new IllegalArgumentException("Resource not found: " + normalizedPath);
    }

    return stream;
  }

  /**
   * クラスパスリソースのURLを取得します
   *
   * <p>ClassLoaderを使用してリソースを取得します。リソース名は完全一致で検索されるため、 ファイルシステムのようなパストラバーサルは発生しません。
   *
   * <p>注意事項：
   *
   * <ul>
   *   <li>リソース名はスラッシュ（/）区切りで指定（例: "schemas/test.xsd"）
   *   <li>先頭にスラッシュを含めない（ClassLoaderの仕様）
   *   <li>バックスラッシュや相対パス（..）は単に存在しないリソースとして扱われる
   * </ul>
   *
   * @param resourcePath クラスパスからの相対パス
   * @return リソースのURL
   * @throws NullPointerException パスがnullの場合
   * @throws IllegalArgumentException パスが空、またはリソースが存在しない場合
   */
  public static URL getResourceUrl(String resourcePath) {
    Objects.requireNonNull(resourcePath, "Resource path cannot be null");

    // 先頭スラッシュを除去（ClassLoaderの仕様に合わせる）
    String normalizedPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

    if (normalizedPath.isEmpty()) {
      throw new IllegalArgumentException("Resource path cannot be empty");
    }

    // Context ClassLoaderを優先し、なければクラスのClassLoaderを使用
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = ClasspathResourceValidator.class.getClassLoader();
    }

    URL url = classLoader.getResource(normalizedPath);

    if (url == null) {
      throw new IllegalArgumentException("Resource not found: " + normalizedPath);
    }

    return url;
  }
}
