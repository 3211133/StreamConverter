package com.streamconverter.execution;

import com.streamconverter.StreamProcessingException;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.context.PipelineContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 逐次実行戦略。
 *
 * <p>コマンドを順番に一つずつ実行する。前のコマンドの出力をバッファリングして 次のコマンドの入力として渡す。並列実行の複雑さが不要な場合や、 デバッグ・テスト用途に適している。
 *
 * <p>注意: 中間バッファリングのためメモリ使用量は {@link ParallelExecutionStrategy} より多くなる場合がある。 大容量データ処理には {@link
 * ParallelExecutionStrategy} の使用を推奨する。
 */
public final class SequentialExecutionStrategy implements ExecutionStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(SequentialExecutionStrategy.class);

  @Override
  public void execute(
      List<IStreamCommand> commands,
      List<String> commandNames,
      InputStream inputStream,
      OutputStream outputStream,
      BufferPolicy bufferPolicy)
      throws IOException {

    PipelineContext pipelineContext = new PipelineContext();
    PipelineContext.set(pipelineContext);

    try {
      InputStream currentInput = inputStream;

      for (int i = 0; i < commands.size(); i++) {
        IStreamCommand command = commands.get(i);
        String commandName = commandNames.get(i);

        if (i == commands.size() - 1) {
          // 最後のコマンドは最終出力ストリームに直接書き込む
          try {
            command.execute(currentInput, outputStream);
          } catch (StreamProcessingException e) {
            throw e;
          } catch (IOException | RuntimeException e) {
            throw new StreamProcessingException(
                "Command execution failed: " + commandName + " - " + e.getMessage(), e);
          }
        } else {
          // 中間コマンドはバッファに書き込む
          ByteArrayOutputStream buffer =
              new ByteArrayOutputStream(bufferPolicy.getBufferSizeBytes());
          try {
            command.execute(currentInput, buffer);
          } catch (StreamProcessingException e) {
            throw e;
          } catch (IOException | RuntimeException e) {
            throw new StreamProcessingException(
                "Command execution failed: " + commandName + " - " + e.getMessage(), e);
          }
          currentInput = new ByteArrayInputStream(buffer.toByteArray());

          if (LOG.isDebugEnabled()) {
            LOG.debug("Sequential command completed: {}", commandName);
          }
        }
      }

      if (LOG.isInfoEnabled()) {
        LOG.info("All commands completed successfully (sequential)");
      }

    } finally {
      PipelineContext.clear();
      MDC.clear();
    }
  }
}
