package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * DatabaseConnectionPoolのテストクラス
 *
 * <p>データベース接続はモック化されています。
 */
class DatabaseConnectionPoolTest {

  @Test
  @Tag("known-bug") // #817
  @DisplayName("returnConnection が close() 失敗後も activeConnections をデクリメントして次の接続取得を可能にする")
  void returnConnection_decrementsActiveConnectionsEvenWhenCloseFails() throws SQLException {
    // returnConnection() は close() が SQLException をスローしても activeConnections を減算すべき。
    // close() 失敗後にデクリメントされないと、以降の getConnection() で
    // activeConnections >= maxPoolSize と判定されて新規接続を作れなくなる。
    Connection closeFailingConnection = mock(Connection.class);
    Connection nextConnection = mock(Connection.class);

    // closeFailingConnection: 返却時に isConnectionValid() == false を返し、close() で例外をスロー
    // returnConnection() は isConnectionValid()==false の場合に close() を試みるが、
    // close() が SQLException をスローするとデクリメントが行われない
    when(closeFailingConnection.isValid(1)).thenReturn(false);
    when(closeFailingConnection.isClosed()).thenReturn(false);
    Mockito.doThrow(new SQLException("simulated close failure"))
        .when(closeFailingConnection)
        .close();

    when(nextConnection.isValid(1)).thenReturn(true);
    when(nextConnection.isClosed()).thenReturn(false);
    Mockito.doNothing().when(nextConnection).close();

    try (MockedStatic<DriverManager> mockedDriverManager =
        Mockito.mockStatic(DriverManager.class)) {
      mockedDriverManager
          .when(() -> DriverManager.getConnection(anyString()))
          .thenReturn(closeFailingConnection, nextConnection);

      DatabaseConnectionPool pool = new DatabaseConnectionPool("jdbc:mock:db", 1, 200);

      // 1回目: 新規接続(closeFailingConnection)を取得
      Connection c1 = pool.getConnection();
      // 返却時に isConnectionValid() は false (isValid が false を返す) → close() が呼ばれ SQLException
      // activeConnections がデクリメントされれば空きスロットが生まれ、次の getConnection() で新規接続を作れる
      c1.close();

      // バグがある場合: activeConnections == 1 のまま → getConnection() がタイムアウトして SQLException
      // 修正後: activeConnections == 0 → 新規接続(nextConnection)が作成されて返却される
      assertDoesNotThrow(
          pool::getConnection,
          "returnConnection で close() が失敗した後も activeConnections がデクリメントされ、新規接続を取得できるべき");
    }
  }

  @Test
  @DisplayName("プール内の接続が無効化された後でも新しい接続を取得できる")
  void getConnection_createsNewConnectionAfterPooledConnectionBecomesInvalid() throws SQLException {
    Connection mockConnection1 = mock(Connection.class);
    Connection mockConnection2 = mock(Connection.class);

    // 1本目は最初は有効だが、プールに返却された後にDB切断などで無効になる
    when(mockConnection1.isValid(1)).thenReturn(true, false);
    when(mockConnection1.isClosed()).thenReturn(false);
    Mockito.doNothing().when(mockConnection1).close();

    when(mockConnection2.isValid(1)).thenReturn(true);
    when(mockConnection2.isClosed()).thenReturn(false);

    try (MockedStatic<DriverManager> mockedDriverManager =
        Mockito.mockStatic(DriverManager.class)) {
      mockedDriverManager
          .when(() -> DriverManager.getConnection(anyString()))
          .thenReturn(mockConnection1, mockConnection2);

      DatabaseConnectionPool pool = new DatabaseConnectionPool("jdbc:mock:db", 1, 200);

      // 1回目: 新規接続(mockConnection1)を取得して返却 -> プールに格納される
      Connection c1 = pool.getConnection();
      c1.close();

      // この時点でmockConnection1はプール内に存在するが、isValid()がfalseを返すようになる
      // (DB切断などを想定)。プールに空きがあるため、新規接続(mockConnection2)が
      // 作成されて返却されるべき。
      Connection c2 =
          assertDoesNotThrow(pool::getConnection, "プール内の接続が無効化された後でも、プールには空きがあるため新規接続が作成されるべき");
      c2.close();
    }
  }
}
