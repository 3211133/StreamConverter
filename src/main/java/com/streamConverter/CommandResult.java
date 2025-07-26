package com.streamConverter;

import java.time.Duration;
import java.time.Instant;

/**
 * コマンドの実行結果を表すクラス
 *
 * <p>各StreamCommandの実行結果、実行時間、エラー情報などを保持します。
 */
public class CommandResult {
  private final String commandName;
  private final boolean success;
  private final long executionTimeMillis;
  private final long inputBytes;
  private final long outputBytes;
  private final String errorMessage;
  private final Instant startTime;
  private final Instant endTime;

  private CommandResult(Builder builder) {
    this.commandName = builder.commandName;
    this.success = builder.success;
    this.executionTimeMillis = builder.executionTimeMillis;
    this.inputBytes = builder.inputBytes;
    this.outputBytes = builder.outputBytes;
    this.errorMessage = builder.errorMessage;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
  }

  /** コマンド名を取得 */
  public String getCommandName() {
    return commandName;
  }

  /** 実行成功フラグを取得 */
  public boolean isSuccess() {
    return success;
  }

  /** 実行時間（ミリ秒）を取得 */
  public long getExecutionTimeMillis() {
    return executionTimeMillis;
  }

  /** 入力バイト数を取得 */
  public long getInputBytes() {
    return inputBytes;
  }

  /** 出力バイト数を取得 */
  public long getOutputBytes() {
    return outputBytes;
  }

  /** エラーメッセージを取得（エラー時のみ） */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** 開始時刻を取得 */
  public Instant getStartTime() {
    return startTime;
  }

  /** 終了時刻を取得 */
  public Instant getEndTime() {
    return endTime;
  }

  /** 実行時間をDurationで取得 */
  public Duration getDuration() {
    return Duration.between(startTime, endTime);
  }

  @Override
  public String toString() {
    return String.format(
        "CommandResult{name='%s', success=%s, time=%dms, input=%db, output=%db%s}",
        commandName,
        success,
        executionTimeMillis,
        inputBytes,
        outputBytes,
        errorMessage != null ? ", error='" + errorMessage + "'" : "");
  }

  /** CommandResultのビルダークラス */
  public static class Builder {
    private String commandName;
    private boolean success;
    private long executionTimeMillis;
    private long inputBytes;
    private long outputBytes;
    private String errorMessage;
    private Instant startTime;
    private Instant endTime;

    public Builder commandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public Builder success(boolean success) {
      this.success = success;
      return this;
    }

    public Builder executionTime(long executionTimeMillis) {
      this.executionTimeMillis = executionTimeMillis;
      return this;
    }

    public Builder inputBytes(long inputBytes) {
      this.inputBytes = inputBytes;
      return this;
    }

    public Builder outputBytes(long outputBytes) {
      this.outputBytes = outputBytes;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder startTime(Instant startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder endTime(Instant endTime) {
      this.endTime = endTime;
      return this;
    }

    public CommandResult build() {
      return new CommandResult(this);
    }
  }

  /** 成功結果を作成するヘルパーメソッド */
  public static CommandResult success(
      String commandName,
      long executionTime,
      long inputBytes,
      long outputBytes,
      Instant startTime,
      Instant endTime) {
    return new Builder()
        .commandName(commandName)
        .success(true)
        .executionTime(executionTime)
        .inputBytes(inputBytes)
        .outputBytes(outputBytes)
        .startTime(startTime)
        .endTime(endTime)
        .build();
  }

  /** 失敗結果を作成するヘルパーメソッド */
  public static CommandResult failure(
      String commandName,
      long executionTime,
      String errorMessage,
      Instant startTime,
      Instant endTime) {
    return new Builder()
        .commandName(commandName)
        .success(false)
        .executionTime(executionTime)
        .errorMessage(errorMessage)
        .startTime(startTime)
        .endTime(endTime)
        .build();
  }
}
