package com.streamconverter.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * リソースパスの検証と正規化を行うユーティリティクラス
 *
 * <p>製品内部のリソースがresources配下であることを保証します。 パストラバーサル攻撃(CWE-22)を防止します。
 */
public final class ResourcePathValidator {

  private static final String RESOURCE_BASE = "resources";

  private ResourcePathValidator() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * リソースパスを正規化しresources配下であることを検証します
   *
   * <p>ベストプラクティス: normalize() → resolve() → normalize() → startsWith()
   *
   * @param resourcePath リソースパス
   * @return 正規化されたパス文字列
   * @throws IllegalArgumentException パスがnullまたは空の場合
   * @throws SecurityException resources配下外へのアクセスが検出された場合
   */
  public static String validate(String resourcePath) {
    Objects.requireNonNull(resourcePath, "Resource path cannot be null");

    String trimmed = resourcePath.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Resource path cannot be empty");
    }

    // バックスラッシュをスラッシュに正規化（Windows/Unix互換性）
    String normalized = trimmed.replace("\\", "/");

    // ベースパスを正規化
    Path basePath = Paths.get(RESOURCE_BASE).normalize();

    // リソースパスを解決して正規化
    Path resolved = basePath.resolve(normalized).normalize();

    // ベースディレクトリ外へのアクセスを防止
    if (!resolved.startsWith(basePath)) {
      throw new SecurityException(
          "Resource path attempts to access outside resource directory: " + trimmed);
    }

    return resolved.toString();
  }
}
