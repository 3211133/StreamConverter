package com.streamConverter.command;

import com.streamConverter.util.MeasuredInputStream;
import com.streamConverter.util.MeasuredOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for stream commands.
 *
 * <p>This class provides a template for executing commands on input streams and writing the results
 * to output streams.
 */
public abstract class AbstractStreamCommand implements IStreamCommand {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractStreamCommand.class);

  /**
   * Default constructor.
   *
   * <p>Initializes the command with default settings.
   */
  public AbstractStreamCommand() {
    super();
  }

  /**
   * Executes the command on the provided input stream and writes the result to the output stream.
   *
   * <p>This method automatically LOGs execution details including: - Command name and execution
   * time - Input/output data sizes - Exception details if execution fails - Performance metrics
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  @Override
  public final void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    String commandName = this.getClass().getSimpleName();
    long startTime = System.currentTimeMillis();
    long startMemory = getUsedMemory();

    // 実行開始ログ
    LOG.info("Starting command execution: {}", commandName);
    LOG.debug("Command details: {}", getCommandDetails());

    // データサイズ測定用のストリームでラップ
    MeasuredInputStream measuredInput = null;
    MeasuredOutputStream measuredOutput = null;
    try {
      measuredInput = new MeasuredInputStream(inputStream);
      measuredOutput = new MeasuredOutputStream(outputStream);
      // 実際の処理実行
      executeInternal(measuredInput, measuredOutput);

      // 成功時のログ出力
      long duration = System.currentTimeMillis() - startTime;
      long memoryUsed = getUsedMemory() - startMemory;

      LOG.info(
          "Command execution completed: {} ({}ms, input: {}bytes, output: {}bytes, memory: {}MB)",
          commandName,
          duration,
          measuredInput.getBytesRead(),
          measuredOutput.getBytesWritten(),
          memoryUsed / 1024 / 1024);

      // パフォーマンス警告
      if (duration > 5000) { // 5秒以上
        LOG.warn("Command {} took longer than expected: {}ms", commandName, duration);
      }

    } catch (Exception e) {
      // 例外発生時のログ出力
      long duration = System.currentTimeMillis() - startTime;
      long memoryUsed = getUsedMemory() - startMemory;

      // measuredInput/measuredOutputがnullの場合は0を使用
      long inputBytes = (measuredInput != null) ? measuredInput.getBytesRead() : 0;
      long outputBytes = (measuredOutput != null) ? measuredOutput.getBytesWritten() : 0;

      LOG.error(
          "Command execution failed: {} ({}ms, input: {}bytes, output: {}bytes, memory: {}MB) - {}",
          commandName,
          duration,
          inputBytes,
          outputBytes,
          memoryUsed / 1024 / 1024,
          e.getMessage(),
          e);

      // NullPointerExceptionをIOExceptionでラップ
      if (e instanceof NullPointerException) {
        throw new IOException("Invalid null stream parameter", e);
      }

      throw e;
    }
  }

  /**
   * Abstract method to be implemented by subclasses for executing the command.
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  protected abstract void executeInternal(InputStream inputStream, OutputStream outputStream)
      throws IOException;

  /**
   * コマンドの詳細情報を取得します。
   *
   * <p>サブクラスでオーバーライドして、コマンド固有の設定や状態を返すことができます。 この情報はデバッグログに出力され、問題の診断に役立ちます。
   *
   * @return コマンドの詳細情報
   */
  protected String getCommandDetails() {
    return this.getClass().getSimpleName() + " (default implementation)";
  }

  /**
   * 現在の使用メモリ量を取得します。
   *
   * @return 使用メモリ量（バイト）
   */
  private long getUsedMemory() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }
}
