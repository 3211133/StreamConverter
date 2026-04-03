package com.streamconverter.context;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class SignalChannelTest {

  @Test
  void peekReturnsEmptyWhenNoSignalSent() {
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");

    assertEquals(Optional.empty(), channel.peek());
  }

  @Test
  void peekReturnsSignalAfterSend() {
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");

    channel.send(new PipelineSignal.Skip("test reason"));

    Optional<PipelineSignal> result = channel.peek();
    assertTrue(result.isPresent());
    assertInstanceOf(PipelineSignal.Skip.class, result.get());
    assertEquals("test reason", ((PipelineSignal.Skip) result.get()).reason());
  }

  @Test
  void peekCanBeCalledMultipleTimes() {
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");
    channel.send(new PipelineSignal.Skip("some reason"));

    // シグナルは消費されず何度でも確認できる
    assertTrue(channel.peek().isPresent());
    assertTrue(channel.peek().isPresent());
    assertTrue(channel.peek().isPresent());
  }

  @Test
  void laterSignalOverwritesEarlierOne() {
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");

    channel.send(new PipelineSignal.Skip("first"));
    channel.send(new PipelineSignal.Skip("second"));

    Optional<PipelineSignal> result = channel.peek();
    assertTrue(result.isPresent());
    assertInstanceOf(PipelineSignal.Skip.class, result.get());
    assertEquals("second", ((PipelineSignal.Skip) result.get()).reason());
  }

  @Test
  void sendThrowsOnNullSignal() {
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");

    assertThrows(IllegalArgumentException.class, () -> channel.send(null));
  }

  @Test
  void skipWithNullReasonThrowsNullPointerException() {
    assertThrows(NullPointerException.class, () -> new PipelineSignal.Skip(null));
  }

  @Test
  void prepareSignalChannelReturnsSameInstanceForSameId() {
    PipelineContext ctx = new PipelineContext();
    SignalChannel ch1 = ctx.prepareSignalChannel("same-id");
    SignalChannel ch2 = ctx.prepareSignalChannel("same-id");

    assertSame(ch1, ch2);
  }

  @Test
  void prepareSignalChannelThrowsOnNullId() {
    PipelineContext ctx = new PipelineContext();

    assertThrows(IllegalArgumentException.class, () -> ctx.prepareSignalChannel(null));
  }

  @Test
  void getSignalChannelThrowsWhenContextNotSet() {
    PipelineContext.clear();

    assertThrows(IllegalStateException.class, () -> PipelineContext.getSignalChannel("any-id"));
  }

  @Test
  void getSignalChannelThrowsOnNullId() {
    assertThrows(IllegalArgumentException.class, () -> PipelineContext.getSignalChannel(null));
  }

  @Test
  void getSignalChannelReturnsNullForUnregisteredId() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);
    try {
      // コンテキストは設定済みだがチャネルが未登録 → null を返す
      assertNull(PipelineContext.getSignalChannel("not-registered"));
    } finally {
      PipelineContext.clear();
    }
  }

  @Test
  void getSignalChannelReturnsPreparedChannel() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);
    try {
      SignalChannel prepared = ctx.prepareSignalChannel("my-channel");
      assertSame(prepared, PipelineContext.getSignalChannel("my-channel"));
    } finally {
      PipelineContext.clear();
    }
  }

  @Test
  void channelIdIsCorrect() {
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("my-channel-id");

    assertEquals("my-channel-id", channel.channelId());
  }

  @Test
  void resetClearsSignal() {
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");
    channel.send(new PipelineSignal.Skip("some reason"));

    channel.reset();

    assertEquals(Optional.empty(), channel.peek());
  }

  @Test
  void resetOnEmptyChannelIsNoOp() {
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");

    channel.reset();

    assertEquals(Optional.empty(), channel.peek());
  }
}
