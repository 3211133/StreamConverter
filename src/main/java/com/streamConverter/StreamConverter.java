package com.streamConverter;

import com.streamConverter.command.IStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * ストリーム変換クラス。
 *
 * <p>ストリームを変換するクラス。ストリームを変換するコマンドを指定して、ストリームを変換する。
 *
 * <p>ストリームを変換するコマンドは、IStreamCommandインターフェースを実装したクラスである必要がある。
 */
public class StreamConverter {
  private List<IStreamCommand> commands;

  StreamConverter(IStreamCommand[] commands) {
    Objects.requireNonNull(commands);
    if (commands.length == 0) {
      throw new IllegalArgumentException("commands is empty.");
    }
    this.commands = List.of(commands);
  }

  StreamConverter(List<IStreamCommand> commands) {
    Objects.requireNonNull(commands);
    if (commands.isEmpty()) {
      throw new IllegalArgumentException("commands is empty.");
    }
    this.commands = commands;
  }

  /**
   * 非同期並列処理でストリームを変換する。
   * メモリ効率を重視し、PipedStreamを使用して大容量ファイルに対応。
   *
   * @param inputStream
   * @param outputStream
   * @return TODO 各コマンドの実行結果(未実装)
   * @throws IOException
   */
  List<Object> run(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    if (this.commands.size() == 1) {
      // 単一コマンドの場合は直接実行（メモリ効率最優先）
      IStreamCommand command = this.commands.get(0);
      Logger.getGlobal().info("command: 0:" + command.toString());
      command.execute(inputStream, outputStream);
      List<Object> result = new ArrayList<>();
      result.add(null);
      return result;
    }

    // 複数コマンドの場合はPipedStreamで並行処理
    ExecutorService executor = Executors.newFixedThreadPool(this.commands.size());
    List<Future<?>> futures = new ArrayList<>();
    List<AutoCloseable> resources = new ArrayList<>();
    
    try {
      InputStream currentInput = inputStream;
      
      // パイプライン構築
      for (int i = 0; i < this.commands.size(); i++) {
        IStreamCommand command = this.commands.get(i);
        Logger.getGlobal().info("command: " + i + ":" + command.toString());

        final InputStream commandInput = currentInput;
        final OutputStream commandOutput;
        
        if (i == this.commands.size() - 1) {
          // 最後のコマンド
          commandOutput = outputStream;
        } else {
          // 中間コマンド: 次のコマンド用にPipedStreamペア作成
          PipedOutputStream pipedOut = new PipedOutputStream();
          PipedInputStream pipedIn = new PipedInputStream(pipedOut, 64 * 1024); // 64KBバッファ
          resources.add(pipedOut);
          resources.add(pipedIn);
          commandOutput = pipedOut;
          currentInput = pipedIn;
        }

        // 各コマンドを非同期実行
        futures.add(executor.submit(() -> {
          try {
            command.execute(commandInput, commandOutput);
            // 中間コマンドの場合、出力ストリームを閉じてEOFをシグナル
            if (commandOutput != outputStream) {
              commandOutput.close();
            }
          } catch (IOException e) {
            Logger.getGlobal().severe("Command execution failed: " + command.getClass().getSimpleName() + " - " + e.getMessage());
            throw new RuntimeException("Command execution failed: " + command.getClass().getSimpleName(), e);
          }
          return null;
        }));
      }

      // すべてのタスクの完了を待機
      List<Object> result = new ArrayList<>();
      for (Future<?> future : futures) {
        try {
          // タイムアウト付きで待機（デッドロック防止）
          future.get(60, TimeUnit.SECONDS);
          result.add(null);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof RuntimeException re) {
            Throwable rootCause = re.getCause();
            if (rootCause instanceof IOException ioe) {
              throw ioe;
            }
            throw re;
          }
          throw new RuntimeException("Unexpected error during command execution", cause);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Command execution was interrupted", e);
        } catch (java.util.concurrent.TimeoutException e) {
          throw new RuntimeException("Command execution timed out after 60 seconds", e);
        }
      }
      
      return result;
      
    } finally {
      // リソースクリーンアップ
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
      
      // PipedStreamのクリーンアップ
      for (AutoCloseable resource : resources) {
        try {
          resource.close();
        } catch (Exception e) {
          Logger.getGlobal().warning("Failed to close resource: " + e.getMessage());
        }
      }
    }
  }
}
