package com.streamconverter.command.rule;

import java.util.Locale;
import org.slf4j.Logger;

/**
 * SQLクエリの検証ユーティリティ。
 *
 * <p>{@link DatabaseFetchRule} と {@link PooledDatabaseFetchRule} で共有される ロジックを提供します。
 */
final class SqlQueryUtils {

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
    if (!queryString.trim().toLowerCase(Locale.ROOT).startsWith("select")) {
      throw new SecurityException("Only SELECT queries are allowed: " + queryString);
    }

    // セミコロンによる複数文の実行を防止（末尾の1個のみ許可）
    String stripped = queryString.trim();
    String withoutTrailingSemicolon =
        stripped.endsWith(";") ? stripped.substring(0, stripped.length() - 1) : stripped;
    if (withoutTrailingSemicolon.contains(";")) {
      throw new SecurityException("Multiple SQL statements are not allowed: " + queryString);
    }

    logger.debug("Query validation passed, length: {}", queryString.length());
    return queryString;
  }
}
