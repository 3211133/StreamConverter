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
   *
   * @param inputStream
   * @param outputStream
   * @return TODO 各コマンドの実行結果(未実装)
   * @throws IOException
   */
  List<Object> run(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    // スレッドを生成
    ExecutorService executor = Executors.newFixedThreadPool(this.commands.size());
    List<Future<?>> futures = new ArrayList<>();
    // N+1回目のInputStreamの引数。N回目のOutputStream生成時に都度更新される。
    PipedOutputStream pipedOut = new PipedOutputStream();
    // 各コマンドを非同期で実行
    for (int i = 0; i < this.commands.size(); i++) {
      IStreamCommand command = this.commands.get(i);
      Logger.getGlobal().info("command: " + i + ":" + command.toString());

      /*
       * PipedInputStreamとPipedOutputStreamは、一組にする必要がある。
       * PipedInXとPipedOutX-1で一組にする
       *
       * i InputStream OutputStream
       * 0 (引数の)in PipedOut1
       * 1 PipedIn1 PipedOut2
       * 2 PipedIn2 PipedOut3
       * ...
       * N-1 PipedInN1 PipedOutN
       * N PipedInN (引数の)out
       */

      InputStream currentIn = (i == 0) ? inputStream : new PipedInputStream(pipedOut);
      OutputStream currentOut =
          (i == this.commands.size() - 1) ? outputStream : (pipedOut = new PipedOutputStream());

      // Thread実行。
      futures.add(
          executor.submit(
              () -> {
                try (OutputStream currentOut1 = currentOut;
                    InputStream currentIn1 = currentIn; ) {
                  // コマンド実行
                  command.execute(inputStream, outputStream);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }));
    }
    executor.shutdown();

    // Thread内で例外が投げられていたら投げる。
    List<Object> result = new ArrayList<>();
    for (Future<?> future : futures) {
      try {
        result.add(future.get());
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof IOException ioe) {
          throw ioe;
        } else { // 発生しないと思われる。
          throw new RuntimeException(cause);
        }
      } catch (InterruptedException e) { // 発生しないと思われる。
        throw new RuntimeException(e);
      }
    }
    return result;
  }
}
