package com.streamconverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;

/**
 * Unified interface for stream commands.
 *
 * <p>This interface defines methods for executing commands on input streams and writing results to
 * output streams.
 *
 * <p>This is a functional interface and can be implemented using lambda expressions or method
 * references for simple stream processing operations.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Simple copy operation (IOException is propagated from execute method signature)
 * IStreamCommand copyCommand = (in, out) -> in.transferTo(out);
 *
 * // Buffered stream processing with custom logic
 * IStreamCommand bufferCommand = (in, out) -> {
 *     byte[] buffer = new byte[8192];
 *     int len;
 *     while ((len = in.read(buffer)) != -1) {
 *         out.write(buffer, 0, len);
 *     }
 * };
 *
 * // Using in StreamConverter pipeline
 * StreamConverter converter = StreamConverter.create(copyCommand);
 * converter.run(inputStream, outputStream);
 * }</pre>
 *
 * <p><b>MDC への値の伝搬について:</b> {@code execute()} 内で {@code MDC.put()} を直接呼び出しても、
 * 他コマンドのスレッドには伝搬されません。ストリームから抽出した値を全コマンドのログに反映させるには {@link
 * com.streamconverter.command.rule.MdcPropagatingRule} を使用してください。子スレッドへの MDC 自動継承が必要な場合は {@link
 * com.streamconverter.logging.MDCInitializer} を参照してください。
 */
@FunctionalInterface
public interface IStreamCommand {

  /**
   * Executes the command on the provided input stream and writes the result to the output stream.
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  void execute(InputStream inputStream, OutputStream outputStream) throws IOException;

  /**
   * Returns a best-effort human-readable name for this command.
   *
   * <p>Synthetic and anonymous implementations fall back to {@code IStreamCommand} so diagnostics
   * remain stable even when the concrete class name is not meaningful.
   *
   * @return command label suitable for logging and error messages
   */
  default String commandName() {
    Class<?> implClass = this.getClass();
    if (implClass.isSynthetic()) {
      return "IStreamCommand";
    }
    String simpleName = implClass.getSimpleName();
    if (!simpleName.isEmpty()) {
      return simpleName;
    }
    Class<?> superClass = implClass.getSuperclass();
    if (superClass != null && !Object.class.equals(superClass)) {
      String superName = superClass.getSimpleName();
      if (!superName.isEmpty()) {
        return superName;
      }
    }
    return "IStreamCommand";
  }

  /**
   * Wraps this command with logging. The returned command logs start, completion, and failure using
   * the provided logger.
   *
   * <p>The command name is derived by {@link #commandName()}. When a caller wants a different label
   * in logs, prefer {@link #withLogging(Logger, String)}.
   *
   * @param logger the logger to write messages to
   * @return a new {@link IStreamCommand} that delegates to this command and emits log records
   */
  default IStreamCommand withLogging(Logger logger) {
    return withLogging(logger, commandName());
  }

  /**
   * Wraps this command with logging using an explicit command name.
   *
   * <p>This overload is recommended when the command is implemented as a lambda expression or
   * method reference, where the underlying class name might be synthetic and not meaningful in
   * logs.
   *
   * @param logger the logger to write messages to
   * @param commandName the human-readable name of this command to appear in log messages
   * @return a new {@link IStreamCommand} that delegates to this command and emits log records
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  // PMD 誤検知: 実際は IOException | RuntimeException と Error を個別にキャッチ済み。
  // PMD が RuntimeException を「汎用 Exception キャッチ」と誤判断する。
  default IStreamCommand withLogging(Logger logger, String commandName) {
    return (in, out) -> {
      logger.info("Starting command: {}", commandName);
      long start = System.currentTimeMillis();
      try {
        this.execute(in, out);
        logger.info(
            "Completed command: {} ({}ms)", commandName, System.currentTimeMillis() - start);
      } catch (IOException | RuntimeException e) {
        logger.error("Failed command: {} - {}", commandName, e.getMessage(), e);
        throw e;
      } catch (Error e) {
        logger.error("Fatal error in command: {} - {}", commandName, e.getMessage(), e);
        throw e;
      }
    };
  }
}
