package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.assertFalse;

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

    Logger hikariLogger =
        (Logger) LoggerFactory.getLogger(HikariConnectionPoolConfig.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    hikariLogger.addAppender(appender);
    hikariLogger.setLevel(Level.INFO);

    try (HikariConnectionPoolConfig pool = new HikariConnectionPoolConfig(testUrl)) {
      // URL を含むINFOログが出力されているかを確認するため構築するだけでよい
      // AutoCloseable なのでリソースは自動解放される
    } catch (Exception e) {
      // URL ログの確認が目的なので接続失敗は無視
    } finally {
      hikariLogger.detachAppender(appender);
    }

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

    Logger pooledLogger =
        (Logger) LoggerFactory.getLogger(PooledDatabaseFetchRule.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    pooledLogger.addAppender(appender);
    pooledLogger.setLevel(Level.INFO);

    try (HikariConnectionPoolConfig pool =
            new HikariConnectionPoolConfig(testUrl, 2, Duration.ofSeconds(5))) {
      new PooledDatabaseFetchRule(pool, "SELECT 1");
    } catch (Exception e) {
      // 接続失敗は無視 — URL ログ不在の確認が目的
    } finally {
      pooledLogger.detachAppender(appender);
    }

    boolean urlAppearsInInfoLog =
        appender.list.stream()
            .filter(e -> e.getLevel() == Level.INFO)
            .anyMatch(e -> e.getFormattedMessage().contains(testUrl));

    assertFalse(urlAppearsInInfoLog, "PooledDatabaseFetchRule の INFO ログにデータベース URL が含まれてはいけない");
  }
}
