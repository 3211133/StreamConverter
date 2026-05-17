package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayName("HikariConnectionPoolConfig ログ出力テスト")
class HikariConnectionPoolConfigLogTest {

  // ---- #669: DB URL は INFO ログに含まれてはいけない ----

  @Test
  @DisplayName("コンストラクタの INFO ログにデータベース URL が含まれない（#669）")
  void testConstructorInfoLogDoesNotContainDatabaseUrl() {
    // 修正前: logger.info(..., databaseUrl, ...) で URL が INFO ログに出力される
    // 修正後: URL をログに含めないよう修正
    String testUrl = "jdbc:h2:mem:testdb-669-" + System.nanoTime();

    Logger hikariLogger = (Logger) LoggerFactory.getLogger(HikariConnectionPoolConfig.class);
    Level originalLevel = hikariLogger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    hikariLogger.addAppender(appender);
    hikariLogger.setLevel(Level.INFO);

    try (HikariConnectionPoolConfig pool = new HikariConnectionPoolConfig(testUrl)) {
      // do nothing — construction triggers the INFO log under test
    } finally {
      hikariLogger.detachAppender(appender);
      appender.stop();
      hikariLogger.setLevel(originalLevel);
    }

    assertTrue(
        appender.list.stream().anyMatch(e -> e.getLevel() == Level.INFO),
        "コンストラクタは少なくとも 1 件の INFO ログを出力するべき");
    boolean urlAppearsInInfoLog =
        appender.list.stream()
            .filter(e -> e.getLevel() == Level.INFO)
            .anyMatch(e -> e.getFormattedMessage().contains(testUrl));

    assertFalse(urlAppearsInInfoLog, "INFO ログにデータベース URL が含まれてはいけない");
  }

  // ---- #669: PooledDatabaseFetchRule の初期化 INFO ログも URL を含まない ----

  @Test
  @DisplayName("PooledDatabaseFetchRule の初期化 INFO ログにデータベース URL が含まれない（#669）")
  void testPooledRuleInitInfoLogDoesNotContainDatabaseUrl() {
    String testUrl = "jdbc:h2:mem:pooled-rule-669-" + System.nanoTime();

    Logger pooledLogger = (Logger) LoggerFactory.getLogger(PooledDatabaseFetchRule.class);
    Level originalLevel = pooledLogger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    pooledLogger.addAppender(appender);
    pooledLogger.setLevel(Level.INFO);

    try (HikariConnectionPoolConfig pool =
        new HikariConnectionPoolConfig(testUrl, 2, Duration.ofSeconds(5))) {
      new PooledDatabaseFetchRule(pool, "SELECT 1");
    } finally {
      pooledLogger.detachAppender(appender);
      appender.stop();
      pooledLogger.setLevel(originalLevel);
    }

    assertTrue(
        appender.list.stream().anyMatch(e -> e.getLevel() == Level.INFO),
        "PooledDatabaseFetchRule の初期化は少なくとも 1 件の INFO ログを出力するべき");
    boolean urlAppearsInInfoLog =
        appender.list.stream()
            .filter(e -> e.getLevel() == Level.INFO)
            .anyMatch(e -> e.getFormattedMessage().contains(testUrl));

    assertFalse(urlAppearsInInfoLog, "PooledDatabaseFetchRule の INFO ログにデータベース URL が含まれてはいけない");
  }
}
