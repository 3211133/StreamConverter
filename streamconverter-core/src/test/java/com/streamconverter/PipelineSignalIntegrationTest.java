package com.streamconverter;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.context.PipelineContext;
import com.streamconverter.context.PipelineSignal;
import com.streamconverter.context.SignalChannel;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * SignalChannelを使ったパイプライン連携の統合テスト。
 *
 * <p>実際のStreamConverterパイプラインでSignalChannelが正しく動作することを確認する。
 */
class PipelineSignalIntegrationTest {

  /** 各行を出力しながら、2行目でSkipシグナルを送る前段コマンド */
  static class SkipSendingCommand extends AbstractStreamCommand {
    private final SignalChannel channel;

    SkipSendingCommand(SignalChannel channel) {
      this.channel = channel;
    }

    @Override
    public void execute(InputStream in, OutputStream out) throws IOException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      PrintWriter writer =
          new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
      String line;
      int lineNum = 0;
      while ((line = reader.readLine()) != null) {
        lineNum++;
        if (lineNum == 2) {
          channel.send(new PipelineSignal.Skip("line 2 is problematic"));
        }
        writer.println(line);
      }
      writer.flush();
    }
  }

  /** Skipシグナルを受け取ったら後続の行をパススルーする後段コマンド */
  static class SkipAwareTransformCommand extends AbstractStreamCommand {
    private final SignalChannel channel;

    SkipAwareTransformCommand(SignalChannel channel) {
      this.channel = channel;
    }

    @Override
    public void execute(InputStream in, OutputStream out) throws IOException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      PrintWriter writer =
          new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
      String line;
      while ((line = reader.readLine()) != null) {
        boolean shouldSkip =
            channel.poll().map(s -> s instanceof PipelineSignal.Skip).orElse(false);
        if (shouldSkip) {
          writer.println("[skipped] " + line);
        } else {
          writer.println("[transformed] " + line);
        }
      }
      writer.flush();
    }
  }

  @Test
  void skipSignalCausesDownstreamBehaviorChange() throws IOException {
    // シグナルを最初から設定済みにして（事前設定パターン）、後段が確実にSkipを受け取るテスト
    String input = "line1\nline2\nline3\n";
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");
    // 事前にSkipシグナルを設定しておく
    channel.send(new PipelineSignal.Skip("pre-set signal"));

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    StreamConverter.create(
            // 前段: シグナルを送らずそのまま転送
            (in, out) -> in.transferTo(out), new SkipAwareTransformCommand(channel))
        .run(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output, ctx);

    String result = output.toString(StandardCharsets.UTF_8);
    // 全行にSkipシグナルが届いている → すべてskipped
    assertTrue(result.contains("[skipped] line1"), "line1 should be skipped due to pre-set signal");
    assertTrue(result.contains("[skipped] line2"), "line2 should be skipped");
    assertTrue(result.contains("[skipped] line3"), "line3 should be skipped");
  }

  @Test
  void skipSignalSentDuringProcessingEventuallyTakesEffect() throws IOException {
    // 前段が処理途中でシグナルを送る場合: タイミングにより前後の行に影響する（割り込み型の特性）
    // シグナル送信後の行は確実にskippedになることを検証する
    String input = "line1\nline2\nline3\n";
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    StreamConverter.create(
            new SkipSendingCommand(channel), // 2行目でSkip送信
            new SkipAwareTransformCommand(channel))
        .run(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output, ctx);

    String result = output.toString(StandardCharsets.UTF_8);
    // シグナルが届いた後のline3は必ずskippedになる
    assertTrue(result.contains("[skipped] line3"), "line3 should be skipped after signal is sent");
    // line1, line2はタイミング次第（どちらになってもよい）
    assertTrue(result.contains("line1"), "line1 should appear in output");
    assertTrue(result.contains("line2"), "line2 should appear in output");
  }

  @Test
  void noSignalMeansDefaultBehavior() throws IOException {
    // シグナルを一切送らないパイプライン
    String input = "line1\nline2\n";
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    StreamConverter.create(
            // 前段: シグナルを送らず転送のみ
            (in, out) -> in.transferTo(out), new SkipAwareTransformCommand(channel))
        .run(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output, ctx);

    String result = output.toString(StandardCharsets.UTF_8);
    // シグナルなし → すべてtransformed
    assertTrue(result.contains("[transformed] line1"));
    assertTrue(result.contains("[transformed] line2"));
  }

  @Test
  void laterSignalOverwritesEarlier() throws IOException {
    // 前段が2つのSkipを送った場合、後のシグナルで上書きされる
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("test");
    channel.send(new PipelineSignal.Skip("first"));
    channel.send(new PipelineSignal.Skip("second"));

    PipelineSignal signal = channel.poll().orElse(null);
    assertNotNull(signal);
    assertInstanceOf(PipelineSignal.Skip.class, signal);
    assertEquals("second", ((PipelineSignal.Skip) signal).reason());
  }

  @Test
  void runWithContextOverloadPassesChannelToCommands() throws IOException {
    String input = "hello\n";
    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("channel-test");
    channel.send(new PipelineSignal.Skip("preset"));

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    StreamConverter.create((in, out) -> in.transferTo(out), new SkipAwareTransformCommand(channel))
        .run(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output, ctx);

    String result = output.toString(StandardCharsets.UTF_8);
    assertTrue(
        result.contains("[skipped] hello"), "preset Skip signal should be visible to downstream");
  }
}
