package com.streamconverter;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.IStreamCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("StreamConverter Test")
class StreamConverterTest {

  private IStreamCommand[] validCommands;
  private String testInput;

  @BeforeEach
  void setUp() {
    // テスト前の準備
    validCommands =
        new IStreamCommand[] {(in, out) -> in.transferTo(out), (in, out) -> in.transferTo(out)};
    testInput = "Hello, StreamConverter!";
  }

  @Test
  @DisplayName("Constructor Normal Case: Valid Command Array")
  void testConstructorWithValidCommandArray() {
    // 配列コンストラクタのテスト
    assertDoesNotThrow(
        () -> {
          StreamConverter.create(validCommands);
        });
  }

  @Test
  @DisplayName("Constructor Normal Case: Valid Command List")
  void testConstructorWithValidCommandList() {
    // リストコンストラクタのテスト
    List<IStreamCommand> commandList = new ArrayList<>();
    commandList.add((in, out) -> in.transferTo(out));

    assertDoesNotThrow(
        () -> {
          StreamConverter.create(commandList);
        });
  }

  @Test
  @DisplayName("Constructor Error Case: Null Command Array")
  void testConstructorWithNullCommandArray() {
    // nullコマンド配列でのコンストラクタテスト
    IStreamCommand[] nullCommands = null;

    assertThrows(
        NullPointerException.class,
        () -> {
          StreamConverter.create(nullCommands);
        });
  }

  @Test
  @DisplayName("Constructor Error Case: Null Command List")
  void testConstructorWithNullCommandList() {
    // nullコマンドリストでのコンストラクタテスト
    List<IStreamCommand> nullCommandList = null;

    assertThrows(
        NullPointerException.class,
        () -> {
          StreamConverter.create(nullCommandList);
        });
  }

  @Test
  @DisplayName("Constructor Error Case: Empty Command Array")
  void testConstructorWithEmptyCommandArray() {
    // 空のコマンド配列でのコンストラクタテスト
    IStreamCommand[] emptyCommands = new IStreamCommand[0];

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          StreamConverter.create(emptyCommands);
        });
  }

  @Test
  @DisplayName("Constructor Error Case: Empty Command List")
  void testConstructorWithEmptyCommandList() {
    // 空のコマンドリストでのコンストラクタテスト
    List<IStreamCommand> emptyCommandList = new ArrayList<>();

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          StreamConverter.create(emptyCommandList);
        });
  }

  @Test
  @DisplayName("Run Normal Case: Input/Output Stream Processing")
  void testRunWithValidStreams() throws IOException {
    // 正常系のrunメソッドテスト
    StreamConverter converter = StreamConverter.create(validCommands);

    try (InputStream inputStream =
            new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      converter.run(inputStream, outputStream);

      // 結果の検証
      assertEquals(testInput, outputStream.toString(StandardCharsets.UTF_8));
    }
  }

  @Test
  @DisplayName("Run Error Case: Null Input Stream")
  void testRunWithNullInputStream() {
    // null入力ストリームでのrunメソッドテスト
    StreamConverter converter = StreamConverter.create(validCommands);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(
        NullPointerException.class,
        () -> {
          converter.run(null, outputStream);
        });
  }

  @Test
  @DisplayName("Run Error Case: Null Output Stream")
  void testRunWithNullOutputStream() {
    // null出力ストリームでのrunメソッドテスト
    StreamConverter converter = StreamConverter.create(validCommands);
    InputStream inputStream = new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        NullPointerException.class,
        () -> {
          converter.run(inputStream, null);
        });
  }

  @Test
  @DisplayName("Multiple Commands Integration Test")
  void testMultipleCommands() throws IOException {
    // 複数コマンドを使用した場合のテスト
    IStreamCommand[] commands =
        new IStreamCommand[] {
          (in, out) -> in.transferTo(out),
          (in, out) -> in.transferTo(out),
          (in, out) -> in.transferTo(out)
        };

    StreamConverter converter = StreamConverter.create(commands);

    try (InputStream inputStream =
            new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      converter.run(inputStream, outputStream);

      // 結果の検証 - SampleStreamCommandは単純にコピーするだけなので、入力と同じ出力になるはず
      assertEquals(testInput, outputStream.toString(StandardCharsets.UTF_8));
    }
  }

  @Test
  @Timeout(10)
  @DisplayName("Downstream failure unblocks upstream within 10 seconds (no 60s timeout)")
  void testDownstreamFailureUnblocksUpstreamQuickly() {
    // 後段が即時失敗した場合、前段が 60 秒待たずに失敗例外を受け取る
    // （例外規定: RuntimeException は分類 C としてラップされずそのまま伝播する）
    IStreamCommand upstreamCommand =
        (in, out) -> {
          // 大量データを書き込もうとして、後段が失敗したときにブロックしないことを確認
          byte[] chunk = new byte[65536];
          Arrays.fill(chunk, (byte) 'A');
          for (int i = 0; i < 1000; i++) {
            out.write(chunk); // 後段失敗後は IOException で解放される
          }
        };
    IStreamCommand downstreamCommand =
        (in, out) -> {
          throw new RuntimeException("Downstream failed immediately");
        };

    StreamConverter converter = StreamConverter.create(upstreamCommand, downstreamCommand);
    InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    RuntimeException ex = assertThrows(RuntimeException.class, () -> converter.run(input, output));
    assertEquals("Downstream failed immediately", ex.getMessage(), "後段の失敗例外がラップされずそのまま伝播すること");
    assertFalse(
        StreamConverter.isPipeAbortedCause(ex),
        "Root cause should not be a pipe-aborted cause. Got: " + ex);
  }

  @Test
  @Timeout(10)
  @DisplayName("Middle-stage failure in 3-stage pipeline unblocks upstream quickly")
  void testMiddleStageFailureUnblocksUpstreamIn3StagePipeline() {
    // 3段パイプライン: 前段(大量書き込み) → 中段(即時失敗) → 後段(コピー)
    // 中段の失敗により前段のパイプ書き込みブロックが 60 秒タイムアウトを待たずに解放されることを確認
    IStreamCommand upstream =
        (in, out) -> {
          byte[] chunk = new byte[65536];
          Arrays.fill(chunk, (byte) 'A');
          for (int i = 0; i < 1000; i++) {
            out.write(chunk);
          }
        };
    IStreamCommand middle =
        (in, out) -> {
          throw new RuntimeException("Middle stage failed");
        };
    IStreamCommand downstream = (in, out) -> in.transferTo(out);

    StreamConverter converter = StreamConverter.create(upstream, middle, downstream);
    InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    RuntimeException ex = assertThrows(RuntimeException.class, () -> converter.run(input, output));
    assertEquals("Middle stage failed", ex.getMessage(), "中段の失敗例外がラップされずそのまま伝播すること");
    assertFalse(
        StreamConverter.isPipeAbortedCause(ex),
        "Root cause should not be a pipe-aborted cause. Got: " + ex);
  }

  @Test
  @Timeout(10)
  @DisplayName(
      "Pipe IOException from upstream is not reported as root cause when downstream fails first")
  void testPipeIOExceptionFromUpstreamIsNotRootCause() {
    // 後段が即時失敗 → 前段が出力ストリームへの書き込みで PipeAbortedException を受ける。
    // isPipeAbortedCause() が PipeAbortedException を secondary として除外し、
    // 後段の失敗が根本原因として返ることを確認する。
    IStreamCommand upstream =
        (in, out) -> {
          byte[] chunk = new byte[65536];
          Arrays.fill(chunk, (byte) 'A');
          for (int i = 0; i < 1000; i++) {
            out.write(chunk); // 後段失敗後に PipeAbortedException が来る
          }
        };
    IStreamCommand downstream =
        (in, out) -> {
          throw new RuntimeException("Root cause: downstream failed");
        };

    StreamConverter converter = StreamConverter.create(upstream, downstream);
    InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    RuntimeException ex = assertThrows(RuntimeException.class, () -> converter.run(input, output));

    // pipe 系の二次エラーではなく、後段の失敗が根本原因として伝播すること
    assertEquals("Root cause: downstream failed", ex.getMessage(), "後段の失敗例外がラップされずそのまま伝播すること");
    assertFalse(
        StreamConverter.isPipeAbortedCause(ex),
        "Root cause should not be a pipe-aborted cause. Got: " + ex);
  }

  @Test
  @DisplayName("AssertionError propagates as-is without being wrapped")
  void testAssertionErrorPropagatesUnwrapped() {
    IStreamCommand failingCommand =
        (in, out) -> {
          throw new AssertionError("assertion failed in command");
        };

    StreamConverter converter = StreamConverter.create(failingCommand);
    InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    AssertionError ex = assertThrows(AssertionError.class, () -> converter.run(input, output));
    assertEquals("assertion failed in command", ex.getMessage());
  }

  @Test
  @DisplayName("large data memory efficiency test - cross-platform adaptive")
  @EnabledOnOs(OS.LINUX)
  void testLargeDataMemoryEfficiency() throws IOException {
    // プラットフォーム適応型メモリ効率性テスト
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();

    // プラットフォーム/環境に応じたテストサイズ調整
    long testDataSize = Math.min(maxMemory / 10, 50 * 1024 * 1024); // ヒープの10%または50MB
    long maxMemoryThresholdMB = testDataSize / (1024 * 1024) * 2; // テストデータの2倍まで許可

    long initialMemory = runtime.totalMemory() - runtime.freeMemory();

    // 大容量データのストリーム生成（実際のファイルを作らずにメモリ効率的に）
    InputStream largeInputStream =
        new InputStream() {
          private long bytesRead = 0;
          private final byte[] pattern =
              "Large file test data pattern for memory efficiency testing.\n"
                  .getBytes(StandardCharsets.UTF_8);
          private int patternIndex = 0;

          @Override
          public int read() throws IOException {
            if (bytesRead >= testDataSize) {
              return -1; // EOF
            }
            int data = pattern[patternIndex] & 0xFF;
            patternIndex = (patternIndex + 1) % pattern.length;
            bytesRead++;
            return data;
          }
        };

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    StreamConverter converter =
        StreamConverter.create((in, out) -> in.transferTo(out), (in, out) -> in.transferTo(out));

    // メモリ使用量監視しながら実行
    long startTime = System.currentTimeMillis();
    converter.run(largeInputStream, outputStream);
    long endTime = System.currentTimeMillis();

    // メモリ使用量チェック
    runtime.gc(); // ガベージコレクション実行
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsedMB = (finalMemory - initialMemory) / (1024 * 1024);

    // プラットフォーム適応型アサーション
    assertTrue(
        memoryUsedMB <= maxMemoryThresholdMB,
        "Memory usage should be <= " + maxMemoryThresholdMB + "MB, but was " + memoryUsedMB + "MB");

    // 処理時間をデータサイズに比例して調整（1MBあたり1秒、最大30秒）
    long maxProcessingTimeMs = Math.min((testDataSize / (1024 * 1024)) * 1000, 30000);
    long processingTimeMs = endTime - startTime;
    assertTrue(
        processingTimeMs <= maxProcessingTimeMs,
        String.format(
            "Processing time should be <= %dms for %dMB data, but was %dms",
            maxProcessingTimeMs, testDataSize / (1024 * 1024), processingTimeMs));

    // 出力サイズが入力サイズと一致することを確認
    assertEquals(testDataSize, outputStream.size(), "Output size should match input size");
  }

  // -------------------------------------------------------------------------
  // 前段が read() でブロック中に後段が失敗するシナリオ（Codex指摘 P1）
  // -------------------------------------------------------------------------

  @Test
  @Timeout(10)
  @DisplayName(
      "Downstream failure unblocks upstream blocked in read() and reports correct root cause")
  void testDownstreamFailureUnblocksUpstreamBlockedInRead() {
    // シナリオ:
    //   前段: 入力を読み込んで次へ転送（小さい入力 → すぐに read() でブロック待ち）
    //   後段: 前段から読み込む前に即時失敗
    //
    // 問題の仮説: 前段が in.read() 内部でブロックしている間に abort() が呼ばれると、
    // checkAborted() は次回の read() 呼び出し前にしかチェックされないため、
    // closeResources() による "Pipe closed" IOException で解放される。
    // isPipeAbortedCause() がそれを secondary と認識できなければ、
    // "Pipe closed" が根本原因として誤報告される。

    CountDownLatch upstreamBlockingInRead = new CountDownLatch(1);

    // 前段: データをそのまま転送するが、後段が読まないので read() でブロックする
    IStreamCommand upstream =
        (in, out) -> {
          byte[] buf = new byte[1];
          // 1バイト読み出してから書き込み → 後段が読まないとここでブロック
          int b = in.read(buf);
          if (b > 0) {
            out.write(buf, 0, b);
            out.flush();
          }
          // 後段が何も読まなければここで次の read() がブロックする
          upstreamBlockingInRead.countDown();
          in.transferTo(out); // ここで read() ブロック待ちになる
        };

    // 後段: 前段が read() でブロックするまで少し待ってから失敗
    IStreamCommand downstream =
        (in, out) -> {
          // 前段が最初の書き込みを完了するまで待機
          try {
            upstreamBlockingInRead.await(5, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          // 前段が read() でブロックした後に失敗する
          throw new RuntimeException(
              "Root cause: downstream failed while upstream blocked in read");
        };

    StreamConverter converter = StreamConverter.create(upstream, downstream);
    // 前段が read() でブロックするよう、最低2バイト以上の入力を用意
    InputStream input = new ByteArrayInputStream("AB".getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    RuntimeException ex = assertThrows(RuntimeException.class, () -> converter.run(input, output));

    assertEquals(
        "Root cause: downstream failed while upstream blocked in read",
        ex.getMessage(),
        "後段の失敗例外がラップされずそのまま伝播すること");
    assertFalse(
        StreamConverter.isPipeAbortedCause(ex),
        "Root cause should not be a pipe-aborted cause. Got: " + ex);
  }

  // -------------------------------------------------------------------------
  // isPipeAbortedCause のカバレッジテスト
  // -------------------------------------------------------------------------

  // -------------------------------------------------------------------------
  // Phase 5: AggregatedStreamProcessingException aggregation tests
  // -------------------------------------------------------------------------

  @Test
  @Timeout(10)
  @DisplayName("UserInputException from command is wrapped in AggregatedStreamProcessingException")
  void testClassifiedFailureIsAggregated() {
    UserInputException original = new UserInputException("入力データが不正です");
    IStreamCommand failingCommand =
        (in, out) -> {
          throw original;
        };

    StreamConverter converter = StreamConverter.create(failingCommand);
    InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    AggregatedStreamProcessingException ex =
        assertThrows(AggregatedStreamProcessingException.class, () -> converter.run(input, output));
    assertEquals(1, ex.getAllFailures().size(), "単一失敗もリスト化される");
    assertSame(original, ex.getAllFailures().get(0), "元の例外がそのまま格納される");
  }

  @Test
  @Timeout(10)
  @DisplayName(
      "InternalSystemException from command is wrapped in AggregatedStreamProcessingException")
  void testInternalSystemExceptionIsAggregated() {
    IStreamCommand failingCommand =
        (in, out) -> {
          throw new InternalSystemException("内部障害が発生しました");
        };

    StreamConverter converter = StreamConverter.create(failingCommand);
    InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    AggregatedStreamProcessingException ex =
        assertThrows(AggregatedStreamProcessingException.class, () -> converter.run(input, output));
    assertEquals(1, ex.getAllFailures().size());
    assertInstanceOf(InternalSystemException.class, ex.getAllFailures().get(0));
  }

  @Test
  @Timeout(10)
  @DisplayName("Unclassified RuntimeException still propagates unwrapped (§4.1.1 open)")
  void testUnclassifiedRuntimeExceptionPropagatesUnwrapped() {
    RuntimeException original = new RuntimeException("unclassified failure");
    IStreamCommand failingCommand =
        (in, out) -> {
          throw original;
        };

    StreamConverter converter = StreamConverter.create(failingCommand);
    InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    RuntimeException ex = assertThrows(RuntimeException.class, () -> converter.run(input, output));
    assertSame(original, ex, "未分類 RuntimeException はラップされないこと（§4.1.1 未決）");
  }

  @Test
  @DisplayName("PipeAbortedException is recognized as pipe-aborted cause")
  void testPipeAbortedExceptionIsSecondaryCause() {
    PipeAbortedException ex = new PipeAbortedException();
    assertTrue(
        StreamConverter.isPipeAbortedCause(ex),
        "PipeAbortedException should be treated as pipe-aborted cause");
  }

  @Test
  @DisplayName(
      "StreamProcessingException wrapping PipeAbortedException is recognized as pipe-aborted cause")
  void testWrappedPipeAbortedExceptionIsSecondaryCause() {
    PipeAbortedException inner = new PipeAbortedException();
    StreamProcessingException ex = new StreamProcessingException("wrapped", inner);
    assertTrue(
        StreamConverter.isPipeAbortedCause(ex),
        "StreamProcessingException wrapping PipeAbortedException should be treated as pipe-aborted cause");
  }

  @Test
  @DisplayName("Unrelated IOException is NOT treated as pipe-aborted cause")
  void testUnrelatedIOExceptionIsNotPipeAbortedCause() {
    IOException ex = new IOException("File not found");
    assertFalse(
        StreamConverter.isPipeAbortedCause(ex),
        "Unrelated IOException should not be treated as pipe-aborted cause");
  }

  @Test
  @DisplayName("StreamProcessingException wrapping unrelated IOException is NOT pipe-aborted cause")
  void testWrappedUnrelatedIOExceptionIsNotPipeAbortedCause() {
    IOException inner = new IOException("File not found");
    StreamProcessingException ex = new StreamProcessingException("wrapped", inner);
    assertFalse(
        StreamConverter.isPipeAbortedCause(ex),
        "StreamProcessingException wrapping unrelated IOException should not be treated as pipe-aborted cause");
  }

  // -------------------------------------------------------------------------
  // コマンド名リグレッション防止テスト
  // -------------------------------------------------------------------------

  @Test
  @Timeout(10)
  @DisplayName("suppressed コンテキストにコマンド名が含まれる（命名リグレッション防止）")
  void testErrorMessageContainsConcreteCommandName() throws IOException {
    class FailingCommand implements IStreamCommand {
      @Override
      public void execute(InputStream in, OutputStream out) throws IOException {
        throw new RuntimeException("deliberate failure");
      }
    }

    StreamConverter converter = StreamConverter.create(new FailingCommand());
    InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    // 例外規定: 失敗例外はラップされず、コマンド名は suppressed コンテキストで報告される
    RuntimeException ex = assertThrows(RuntimeException.class, () -> converter.run(input, output));

    assertTrue(
        Arrays.stream(ex.getSuppressed())
            .anyMatch(s -> String.valueOf(s.getMessage()).contains("FailingCommand")),
        "suppressed コンテキストに 'FailingCommand' が含まれるべき: " + Arrays.toString(ex.getSuppressed()));
  }

  @Test
  @Timeout(10)
  @DisplayName("exceptionally コールバックがリソースクリーンアップ後も根本原因を維持する")
  void testExceptionallyDoesNotOverrideRootCause() throws IOException {
    RuntimeException originalCause = new RuntimeException("original failure");
    IStreamCommand failingCommand =
        (in, out) -> {
          throw originalCause;
        };

    StreamConverter converter = StreamConverter.create(failingCommand);
    InputStream input = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    OutputStream output = new ByteArrayOutputStream();

    // 例外規定: RuntimeException はラップされないため、根本原因の例外そのものが伝播する
    RuntimeException ex = assertThrows(RuntimeException.class, () -> converter.run(input, output));

    assertSame(originalCause, ex, "exceptionally コールバックが根本原因を上書きしていないこと");
  }
}
