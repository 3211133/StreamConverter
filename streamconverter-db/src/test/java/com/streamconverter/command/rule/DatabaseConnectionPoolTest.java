package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
  @Tag("known-bug") // #754
  @org.junit.jupiter.api.DisplayName("プール内の接続が無効化された後でも新しい接続を取得できる")
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
