package com.streamconverter.security;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ファイルパスのセキュリティ検証を提供するユーティリティクラス
 *
 * <p>このクラスは、パストラバーサル攻撃や その他のファイルシステム関連のセキュリティ脆弱性を防ぐため、 安全なパス検証機能を提供します。
 *
 * <p>主な機能:
 *
 * <ul>
 *   <li>パストラバーサル攻撃防止のためのパス正規化
 *   <li>絶対パスの拒否
 *   <li>".."シーケンスの検出と拒否
 *   <li>許可されたベースディレクトリ内のファイルであることの検証
 *   <li>シンボリックリンクの検証
 * </ul>
 *
 * @since 1.0.0
 */
public class SecurePathValidator {

  private static final Logger logger = LoggerFactory.getLogger(SecurePathValidator.class);
  private static final Logger securityLogger =
      LoggerFactory.getLogger("com.streamconverter.security");

  private SecurePathValidator() {
    // ユーティリティクラスのため、インスタンス化を禁止
  }

  /**
   * パスがベースディレクトリ内にあることを検証します
   *
   * @param filePath 検証対象のファイルパス
   * @param baseDirectory 許可されたベースディレクトリ
   * @throws SecurityException パストラバーサル攻撃の試みが検出された場合
   * @throws IllegalArgumentException パスがnullまたは空の場合
   */
  public static void validatePathWithinBase(String filePath, String baseDirectory) {
    validatePathWithinBase(filePath, baseDirectory, true);
  }

  /**
   * パスがベースディレクトリ内にあることを検証します
   *
   * @param filePath 検証対象のファイルパス
   * @param baseDirectory 許可されたベースディレクトリ
   * @param requireFileExists ファイルの存在を必須とするかどうか
   * @throws SecurityException パストラバーサル攻撃の試みが検出された場合
   * @throws IllegalArgumentException パスがnullまたは空の場合
   */
  public static void validatePathWithinBase(
      String filePath, String baseDirectory, boolean requireFileExists) {
    // 基本的な入力検証
    if (filePath == null || filePath.trim().isEmpty()) {
      throw new IllegalArgumentException("File path cannot be null or empty");
    }
    if (baseDirectory == null || baseDirectory.trim().isEmpty()) {
      throw new IllegalArgumentException("Base directory cannot be null or empty");
    }

    String trimmedPath = filePath.trim();

    // 1. 絶対パスの拒否
    Path path = Paths.get(trimmedPath);
    if (path.isAbsolute()) {
      securityLogger.warn("Rejected absolute path: {}", trimmedPath);
      throw new SecurityException("Absolute paths are not allowed: " + trimmedPath);
    }

    // 2. ".."シーケンスの検出
    if (trimmedPath.contains("..")) {
      securityLogger.warn("Path traversal detected in path: {}", trimmedPath);
      throw new SecurityException("Path traversal sequence (..) detected: " + trimmedPath);
    }

    // 3. パスの正規化と範囲検証
    try {
      Path basePath = Paths.get(baseDirectory).toAbsolutePath().normalize();
      Path fullPath = basePath.resolve(trimmedPath).normalize();

      // ベースディレクトリ内であることを確認
      if (!fullPath.startsWith(basePath)) {
        securityLogger.warn("Path {} is outside allowed directory {}", fullPath, basePath);
        throw new SecurityException("Path is outside allowed directory: " + trimmedPath);
      }

      // 4. シンボリックリンクの検証（オプション）
      if (Files.exists(fullPath)) {
        Path realPath = fullPath.toRealPath();
        if (!realPath.startsWith(basePath)) {
          securityLogger.warn(
              "Symlink target {} is outside allowed directory {}", realPath, basePath);
          throw new SecurityException(
              "Symbolic link target is outside allowed directory: " + trimmedPath);
        }
      } else if (requireFileExists) {
        throw new IllegalArgumentException("File does not exist: " + trimmedPath);
      }

      securityLogger.debug("Path validation successful: {}", trimmedPath);

    } catch (SecurityException | IllegalArgumentException e) {
      throw e;
    } catch (IOException e) {
      logger.error("Error during path validation: {}", e.getMessage(), e);
      throw new SecurityException("Failed to validate path: " + e.getMessage(), e);
    }
  }

  /**
   * パスが安全であることを検証します（相対パスのみ許可、".."を拒否）
   *
   * @param filePath 検証対象のファイルパス
   * @throws SecurityException パストラバーサル攻撃の試みが検出された場合
   * @throws IllegalArgumentException パスがnullまたは空の場合
   */
  public static void validateRelativePath(String filePath) {
    if (filePath == null || filePath.trim().isEmpty()) {
      throw new IllegalArgumentException("File path cannot be null or empty");
    }

    String trimmedPath = filePath.trim();

    // 1. 絶対パスの拒否
    Path path = Paths.get(trimmedPath);
    if (path.isAbsolute()) {
      securityLogger.warn("Rejected absolute path: {}", trimmedPath);
      throw new SecurityException("Absolute paths are not allowed: " + trimmedPath);
    }

    // 2. ".."シーケンスの検出
    if (trimmedPath.contains("..")) {
      securityLogger.warn("Path traversal detected in path: {}", trimmedPath);
      throw new SecurityException("Path traversal sequence (..) detected: " + trimmedPath);
    }

    // 3. パスの正規化
    Path normalizedPath = path.normalize();
    String normalizedString = normalizedPath.toString();

    // 正規化後も".."が含まれている、または空になる場合は拒否
    if (normalizedString.isEmpty() || normalizedString.contains("..")) {
      securityLogger.warn("Invalid normalized path: {}", normalizedString);
      throw new SecurityException("Invalid path after normalization: " + trimmedPath);
    }

    securityLogger.debug("Relative path validation successful: {}", trimmedPath);
  }

  /**
   * パストラバーサル攻撃を防ぐためのパス検証（絶対パスも許可）
   *
   * <p>このメソッドは".."シーケンスを検出して拒否しますが、 絶対パスは許可します。ライブラリとして使用する場合、 ユーザーが正当な理由で絶対パスを使用する可能性があるためです。
   *
   * @param filePath 検証対象のファイルパス
   * @throws SecurityException パストラバーサル攻撃の試みが検出された場合
   * @throws IllegalArgumentException パスがnullまたは空の場合
   */
  public static void validatePath(String filePath) {
    if (filePath == null || filePath.trim().isEmpty()) {
      throw new IllegalArgumentException("File path cannot be null or empty");
    }

    String trimmedPath = filePath.trim();

    // ".."シーケンスの検出
    if (trimmedPath.contains("..")) {
      securityLogger.warn("Path traversal detected in path: {}", trimmedPath);
      throw new SecurityException("Path traversal sequence (..) detected: " + trimmedPath);
    }

    // パスの正規化
    Path path = Paths.get(trimmedPath);
    Path normalizedPath = path.normalize();
    String normalizedString = normalizedPath.toString();

    // 正規化後も".."が含まれている場合は拒否
    if (normalizedString.contains("..")) {
      securityLogger.warn("Invalid normalized path: {}", normalizedString);
      throw new SecurityException("Invalid path after normalization: " + trimmedPath);
    }

    securityLogger.debug("Path validation successful: {}", trimmedPath);
  }

  /**
   * ファイルが指定されたディレクトリ内に存在することを検証します
   *
   * @param file 検証対象のファイル
   * @param baseDirectory 許可されたベースディレクトリ
   * @throws SecurityException ファイルがベースディレクトリ外にある場合
   * @throws IOException パスの正規化に失敗した場合
   */
  public static void validateFileInDirectory(File file, File baseDirectory) throws IOException {
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null");
    }
    if (baseDirectory == null) {
      throw new IllegalArgumentException("Base directory cannot be null");
    }

    Path basePath = baseDirectory.toPath().toAbsolutePath().normalize();
    Path filePath = file.toPath().toAbsolutePath().normalize();

    if (!filePath.startsWith(basePath)) {
      securityLogger.warn("File {} is outside allowed directory {}", filePath, basePath);
      throw new SecurityException(
          String.format("File is outside allowed directory: %s (base: %s)", filePath, basePath));
    }

    // シンボリックリンクの検証
    if (Files.exists(filePath)) {
      Path realPath = filePath.toRealPath();
      if (!realPath.startsWith(basePath)) {
        securityLogger.warn(
            "Symlink target {} is outside allowed directory {}", realPath, basePath);
        throw new SecurityException(
            String.format(
                "Symbolic link target is outside allowed directory: %s (base: %s)",
                realPath, basePath));
      }
    }

    securityLogger.debug("File validation successful: {}", filePath);
  }
}
