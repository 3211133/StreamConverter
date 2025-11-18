package com.streamconverter.util;

import java.io.IOException;
import java.nio.file.Files;
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
   * @throws NullPointerException パスがnullの場合
   * @throws IllegalArgumentException パスが空または空白のみの場合
   * @throws SecurityException resources配下外へのアクセスが検出された場合
   */
  public static String validate(String resourcePath) {
    Objects.requireNonNull(resourcePath, "Resource path cannot be null");

    String trimmed = resourcePath.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Resource path cannot be empty");
    }

    // Paths.get() がプラットフォーム固有の処理を実行
    Path inputPath = Paths.get(trimmed);

    // 絶対パスを拒否（ライブラリが判定）
    if (inputPath.isAbsolute()) {
      throw new SecurityException("Absolute path is not allowed: " + trimmed);
    }

    // ベースパスを正規化
    Path basePath = Paths.get(RESOURCE_BASE).normalize();

    // リソースパスを解決して正規化
    Path resolved = basePath.resolve(inputPath).normalize();

    // ベースディレクトリ外へのアクセスを防止
    if (!resolved.startsWith(basePath)) {
      throw new SecurityException(
          "Resource path attempts to access outside resource directory: " + trimmed);
    }

    // シンボリックリンク経由の脱出を防止（ファイルが存在する場合のみ）
    if (Files.exists(resolved)) {
      try {
        Path realPath = resolved.toRealPath();
        Path realBase = basePath.toRealPath();

        if (!realPath.startsWith(realBase)) {
          throw new SecurityException(
              "Resource path escapes resource directory via symbolic link: " + trimmed);
        }

        return realPath.toString();
      } catch (IOException e) {
        throw new SecurityException("Unable to resolve real path: " + trimmed, e);
      }
    }

    return resolved.toString();
  }
}
