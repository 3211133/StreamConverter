package com.streamConverter.command.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * DatabaseFetchRuleのテストクラス
 *
 * <p>このクラスは、DatabaseFetchRuleクラスの機能をテストします。 データベース接続はモック化されています。
 */
public class DatabaseFetchRuleTest {

  private Connection mockConnection;
  private PreparedStatement mockStatement;
  private ResultSet mockResultSet;
  private ResultSetMetaData mockMetaData;

  @BeforeEach
  public void setUp() throws SQLException {
    // モックオブジェクトの作成
    mockConnection = mock(Connection.class);
    mockStatement = mock(PreparedStatement.class);
    mockResultSet = mock(ResultSet.class);
    mockMetaData = mock(ResultSetMetaData.class);

    // モックの振る舞いを設定
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
    when(mockStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
  }

  @Test
  @DisplayName("単一の結果を返すクエリのテスト")
  // @Disabled("このテストは実装のみで、実行は無効化されています")
  public void testApplyWithSingleResult() throws SQLException {
    // テスト用のデータ
    String databaseUrl = "jdbc:mock:db";
    String query = "SELECT value FROM test_table WHERE key = ?";
    String input = "testKey";
    String expectedOutput = "testValue";

    // モックの振る舞いを設定
    try (MockedStatic<java.sql.DriverManager> mockedDriverManager =
        Mockito.mockStatic(java.sql.DriverManager.class)) {
      mockedDriverManager
          .when(() -> java.sql.DriverManager.getConnection(databaseUrl))
          .thenReturn(mockConnection);

      when(mockMetaData.getColumnCount()).thenReturn(1);
      when(mockResultSet.next()).thenReturn(true, false); // 最初はtrue（結果あり）、次はfalse（結果なし）
      when(mockResultSet.getString(1)).thenReturn(expectedOutput);

      // テスト対象のインスタンスを作成
      DatabaseFetchRule rule = new DatabaseFetchRule(databaseUrl, query);

      // テスト実行
      String result = rule.apply(input);

      // 結果の検証
      assertEquals(expectedOutput, result, "単一の結果が正しく取得できること");
    }
  }

  @Test
  @DisplayName("結果が空の場合のテスト")
  // @Disabled("このテストは実装のみで、実行は無効化されています")
  public void testApplyWithEmptyResult() throws SQLException {
    // テスト用のデータ
    String databaseUrl = "jdbc:mock:db";
    String query = "SELECT value FROM test_table WHERE key = ?";
    String input = "nonExistentKey";

    // モックの振る舞いを設定
    try (MockedStatic<java.sql.DriverManager> mockedDriverManager =
        Mockito.mockStatic(java.sql.DriverManager.class)) {
      mockedDriverManager
          .when(() -> java.sql.DriverManager.getConnection(databaseUrl))
          .thenReturn(mockConnection);

      when(mockResultSet.next()).thenReturn(false); // 結果なし

      // テスト対象のインスタンスを作成
      DatabaseFetchRule rule = new DatabaseFetchRule(databaseUrl, query);

      // テスト実行
      String result = rule.apply(input);

      // 結果の検証
      assertEquals("", result, "結果が空の場合は空文字列が返されること");
    }
  }

  @Test
  @DisplayName("複数列の結果を返すクエリのテスト")
  // @Disabled("このテストは実装のみで、実行は無効化されています")
  public void testApplyWithMultipleColumns() throws SQLException {
    // テスト用のデータ
    String databaseUrl = "jdbc:mock:db";
    String query = "SELECT col1, col2 FROM test_table WHERE key = ?";
    String input = "testKey";
    String expectedOutput = "col1Value";

    // モックの振る舞いを設定
    try (MockedStatic<java.sql.DriverManager> mockedDriverManager =
        Mockito.mockStatic(java.sql.DriverManager.class)) {
      mockedDriverManager
          .when(() -> java.sql.DriverManager.getConnection(databaseUrl))
          .thenReturn(mockConnection);

      when(mockMetaData.getColumnCount()).thenReturn(2); // 2列
      when(mockResultSet.next()).thenReturn(true, false);
      when(mockResultSet.getString(1)).thenReturn(expectedOutput);

      // テスト対象のインスタンスを作成
      DatabaseFetchRule rule = new DatabaseFetchRule(databaseUrl, query);

      // テスト実行
      String result = rule.apply(input);

      // 結果の検証
      assertEquals(expectedOutput, result, "複数列の場合は先頭列の値が返されること");
    }
  }

  @Test
  @DisplayName("複数行の結果を返すクエリのテスト")
  // @Disabled("このテストは実装のみで、実行は無効化されています")
  public void testApplyWithMultipleRows() throws SQLException {
    // テスト用のデータ
    String databaseUrl = "jdbc:mock:db";
    String query = "SELECT value FROM test_table WHERE category = ?";
    String input = "testCategory";
    String expectedOutput = "firstRowValue";

    // モックの振る舞いを設定
    try (MockedStatic<java.sql.DriverManager> mockedDriverManager =
        Mockito.mockStatic(java.sql.DriverManager.class)) {
      mockedDriverManager
          .when(() -> java.sql.DriverManager.getConnection(databaseUrl))
          .thenReturn(mockConnection);

      when(mockMetaData.getColumnCount()).thenReturn(1);
      when(mockResultSet.next()).thenReturn(true, true, false); // 2行の結果
      when(mockResultSet.getString(1)).thenReturn(expectedOutput, "secondRowValue");

      // テスト対象のインスタンスを作成
      DatabaseFetchRule rule = new DatabaseFetchRule(databaseUrl, query);

      // テスト実行
      String result = rule.apply(input);

      // 結果の検証
      assertEquals(expectedOutput, result, "複数行の場合は先頭行の値が返されること");
    }
  }

  @Test
  @DisplayName("SQLエラーが発生した場合のテスト")
  // @Disabled("このテストは実装のみで、実行は無効化されています")
  public void testApplyWithSQLException() throws SQLException {
    // テスト用のデータ
    String databaseUrl = "jdbc:mock:db";
    String query = "SELECT value FROM test_table WHERE key = ?";
    String input = "testKey";
    String errorMessage = "Database connection failed";

    // モックの振る舞いを設定
    try (MockedStatic<java.sql.DriverManager> mockedDriverManager =
        Mockito.mockStatic(java.sql.DriverManager.class)) {
      // 事前に SQLException を作成
      SQLException sqlException = Mockito.mock(SQLException.class);
      when(sqlException.getMessage()).thenReturn(errorMessage);

      // doThrow を使用して例外をスローするように設定
      mockedDriverManager
          .when(() -> java.sql.DriverManager.getConnection(databaseUrl))
          .thenThrow(sqlException);

      // テスト対象のインスタンスを作成
      DatabaseFetchRule rule = new DatabaseFetchRule(databaseUrl, query);

      // テスト実行
      String result = rule.apply(input);

      // 結果の検証
      assertEquals("ERROR: " + errorMessage, result, "SQLエラーが発生した場合はエラーメッセージが返されること");
    }
  }

  @Test
  @DisplayName("NULL値を返すクエリのテスト")
  // @Disabled("このテストは実装のみで、実行は無効化されています")
  public void testApplyWithNullResult() throws SQLException {
    // テスト用のデータ
    String databaseUrl = "jdbc:mock:db";
    String query = "SELECT value FROM test_table WHERE key = ?";
    String input = "nullValueKey";

    // モックの振る舞いを設定
    try (MockedStatic<java.sql.DriverManager> mockedDriverManager =
        Mockito.mockStatic(java.sql.DriverManager.class)) {
      mockedDriverManager
          .when(() -> java.sql.DriverManager.getConnection(databaseUrl))
          .thenReturn(mockConnection);

      when(mockMetaData.getColumnCount()).thenReturn(1);
      when(mockResultSet.next()).thenReturn(true, false);
      when(mockResultSet.getString(1)).thenReturn(null); // NULL値

      // テスト対象のインスタンスを作成
      DatabaseFetchRule rule = new DatabaseFetchRule(databaseUrl, query);

      // テスト実行
      String result = rule.apply(input);

      // 結果の検証
      assertEquals("", result, "NULL値の場合は空文字列が返されること");
    }
  }
}
