package com.streamConverter.command;

import com.streamConverter.context.ExecutionContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 既存のIStreamCommandをコンテキスト対応にするデコレータ
 *
 * <p>このデコレータは既存のコマンドを変更することなく、 ExecutionContextの適用とMDCの設定を自動化します。
 */
public class ContextPropagatingDecorator implements IContextAwareStreamCommand {

  private static final Logger logger = LoggerFactory.getLogger(ContextPropagatingDecorator.class);

  private final IStreamCommand wrappedCommand;
  private final String commandName;

  /**
   * デコレータを作成
   *
   * @param command ラップするコマンド
   */
  public ContextPropagatingDecorator(IStreamCommand command) {
    this.wrappedCommand = Objects.requireNonNull(command, "command cannot be null");
    this.commandName = command.getClass().getSimpleName();
  }

  /**
   * デコレータを作成（カスタムコマンド名付き）
   *
   * @param command ラップするコマンド
   * @param commandName ログ用のコマンド名
   */
  public ContextPropagatingDecorator(IStreamCommand command, String commandName) {
    this.wrappedCommand = Objects.requireNonNull(command, "command cannot be null");
    this.commandName = Objects.requireNonNull(commandName, "commandName cannot be null");
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream, ExecutionContext context)
      throws IOException {

    // 実行前の準備
    int sequence = context.getNextCommandSequence();
    String stageName = commandName + "-" + sequence;

    // 既存のMDCコンテキストを保存
    String previousExecutionId = MDC.get(ExecutionContext.EXECUTION_ID_KEY);
    String previousStage = MDC.get(ExecutionContext.STAGE_KEY);

    try {
      // ExecutionContextをMDCに適用
      context.applyToMDCWithStage(stageName);

      // コンテキスト情報をユーザーコンテキストに保存（必要に応じて）
      context.setUserContext("currentCommand", commandName);
      context.setUserContext("currentSequence", String.valueOf(sequence));

      logger.info("Starting command execution: {} (sequence: {})", commandName, sequence);

      // ラップしたコマンドの実行
      if (wrappedCommand instanceof IContextAwareStreamCommand) {
        // コンテキスト対応コマンドの場合はコンテキストを渡す
        ((IContextAwareStreamCommand) wrappedCommand).execute(inputStream, outputStream, context);
      } else {
        // 従来のコマンドの場合はMDCで対応
        wrappedCommand.execute(inputStream, outputStream);
      }

      logger.info("Completed command execution: {} (sequence: {})", commandName, sequence);

    } catch (IOException e) {
      logger.error(
          "Command execution failed: {} (sequence: {}) - {}",
          commandName,
          sequence,
          e.getMessage(),
          e);
      throw e;
    } catch (Exception e) {
      logger.error(
          "Unexpected error in command execution: {} (sequence: {}) - {}",
          commandName,
          sequence,
          e.getMessage(),
          e);
      throw new IOException("Command execution failed: " + commandName, e);
    } finally {
      // MDCの復元（必要に応じて）
      restoreMDCContext(previousExecutionId, previousStage);

      // ユーザーコンテキストのクリーンアップ
      context.setUserContext("currentCommand", null);
      context.setUserContext("currentSequence", null);
    }
  }

  /** MDCコンテキストを復元 */
  private void restoreMDCContext(String previousExecutionId, String previousStage) {
    if (previousExecutionId != null) {
      MDC.put(ExecutionContext.EXECUTION_ID_KEY, previousExecutionId);
    } else {
      MDC.remove(ExecutionContext.EXECUTION_ID_KEY);
    }

    if (previousStage != null) {
      MDC.put(ExecutionContext.STAGE_KEY, previousStage);
    } else {
      MDC.remove(ExecutionContext.STAGE_KEY);
    }
  }

  /**
   * ラップされたコマンドを取得
   *
   * @return ラップされたコマンド
   */
  public IStreamCommand getWrappedCommand() {
    return wrappedCommand;
  }

  /**
   * コマンド名を取得
   *
   * @return コマンド名
   */
  public String getCommandName() {
    return commandName;
  }

  @Override
  public String toString() {
    return String.format(
        "ContextPropagatingDecorator{commandName='%s', wrappedCommand=%s}",
        commandName, wrappedCommand.getClass().getSimpleName());
  }
}
