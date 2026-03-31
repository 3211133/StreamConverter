package com.streamconverter.command.rule;

import java.util.regex.Pattern;
import org.slf4j.Logger;

/**
 * SQLクエリの検証・サニタイズユーティリティ。
 *
 * <p>{@link DatabaseFetchRule} と {@link PooledDatabaseFetchRule} で共有される
 * ロジックを提供します。
 */
final class SqlQueryUtils {

  /** SQLインジェクション攻撃を検出するパターン（SELECT以外の危険なSQL文） */
  private static final Pattern SQL_INJECTION_PATTERN =
      Pattern.compile(
          ".*(union|insert|update|delete|drop|create|alter|exec|execute|sp_|xp_).*",
          Pattern.CASE_INSENSITIVE);

  private SqlQueryUtils() {}

  /**
   * SELECTクエリの妥当性を検証します。
   *
   * @param queryString 検証対象のクエリ
   * @param logger ロガー
   * @return 検証済みクエリ
   * @throws IllegalArgumentException クエリが空の場合
   * @throws SecurityException 危険なクエリパターンが検出された場合
   */
  static String validateQuery(String queryString, Logger logger) {
    if (queryString.isEmpty()) {
      throw new IllegalArgumentException("Query cannot be empty");
    }

    // SELECTクエリのみ許可
    if (!queryString.trim().toLowerCase().startsWith("select")) {
      throw new SecurityException("Only SELECT queries are allowed: " + queryString);
    }

    // SQLインジェクション攻撃の検出（パターンマッチング使用）
    if (SQL_INJECTION_PATTERN.matcher(queryString).matches()) {
      throw new SecurityException(
          "Query contains potentially dangerous SQL commands: " + queryString);
    }

    // セミコロンによる複数文の実行を防止（末尾の1個のみ許可）
    String stripped = queryString.trim();
    String withoutTrailingSemicolon = stripped.endsWith(";") ? stripped.substring(0, stripped.length() - 1) : stripped;
    if (withoutTrailingSemicolon.contains(";")) {
      throw new SecurityException("Multiple SQL statements are not allowed: " + queryString);
    }

    logger.debug("Query validation passed, length: {}", queryString.length());
    return queryString;
  }

  /**
   * 入力パラメータをサニタイズします。
   *
   * @param input サニタイズ対象の入力
   * @param logger ロガー
   * @return サニタイズされた入力（非null）
   * @throws IllegalArgumentException inputがnullの場合
   */
  static String sanitizeInput(String input, Logger logger) {
    if (input == null) {
      throw new IllegalArgumentException("Input parameter cannot be null");
    }

    // PreparedStatement がパラメータバインディングを担うため、シングルクォートエスケープは行わない。
    // ここでは PreparedStatement を通じないコンテキスト向けに残存する危険パターンのみ除去する。
    String sanitized =
        input
            .replace("--", "") // SQLコメントの除去
            .replace("/*", "") // ブロックコメント開始の除去
            .replace("*/", ""); // ブロックコメント終了の除去

    // 極端に長い入力の制限
    if (sanitized.length() > 1000) {
      logger.warn("Input parameter is extremely long, truncating: length={}", sanitized.length());
      sanitized = sanitized.substring(0, 1000);
    }

    return sanitized;
  }
}
