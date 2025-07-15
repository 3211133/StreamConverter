package com.streamConverter;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayName("Mainクラスのテスト")
class MainTest {

  private ListAppender<ILoggingEvent> listAppender;
  private Logger mainLogger;

  @BeforeEach
  void setUp() {
    // Logbackのルートロガーを取得し、テスト用のアペンダを追加
    mainLogger = (Logger) LoggerFactory.getLogger(Main.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    mainLogger.addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    // テスト用アペンダを削除
    mainLogger.detachAppender(listAppender);
  }

  @Test
  @DisplayName("main メソッドの実行テスト")
  void testMainMethod() throws Exception {
    // mainメソッドを実行
    Main.main(new String[] {});

    // ログイベントをキャプチャ
    List<String> logMessages =
        listAppender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());

    // 期待されるログメッセージが含まれていることを確認
    assertTrue(logMessages.stream().anyMatch(s -> s.contains("Starting StreamConverter application")));
    assertTrue(logMessages.stream().anyMatch(s -> s.contains("Processing result: any message")));
    assertTrue(logMessages.stream().anyMatch(s -> s.contains("StreamConverter application completed")));
  }

  @Test
  @DisplayName("引数付きmainメソッドの実行テスト")
  void testMainMethodWithArguments() throws Exception {
    // 引数付きでmainメソッドを実行
    String[] args = {"arg1", "arg2"};
    Main.main(args);

    // ログイベントをキャプチャ
    List<String> logMessages =
        listAppender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());

    // 期待されるログメッセージが含まれていることを確認
    assertTrue(logMessages.stream().anyMatch(s -> s.contains("Starting StreamConverter application")));
    assertTrue(logMessages.stream().anyMatch(s -> s.contains("Processing result: any message")));
    assertTrue(logMessages.stream().anyMatch(s -> s.contains("StreamConverter application completed")));
  }
}