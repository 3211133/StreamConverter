package com.streamconverter;

import java.time.Duration;
import java.time.Instant;

/**
 * コマンドの実行結果を表すクラス
 *
 * <p>各StreamCommandの実行結果、実行時間、エラー情報などを保持します。
 */
public final class CommandResult {
  private final String commandName;
  private final boolean success;
  private final long execMillis;
  private final long inputBytes;
  private final long outputBytes;
  private final String errorMessage;
  private final Instant startTime;
  private final Instant endTime;

  private CommandResult(final Builder builder) {
    this.commandName = builder.commandName;
    this.success = builder.success;
    this.execMillis = builder.execMillis;
    this.inputBytes = builder.inputBytes;
    this.outputBytes = builder.outputBytes;
    this.errorMessage = builder.errorMessage;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
  }

  /**
   * コマンド名を取得
   *
   * @return コマンド名
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * 実行成功フラグを取得
   *
   * @return 実行が成功した場合true
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * 実行時間（ミリ秒）を取得
   *
   * @return 実行時間（ミリ秒）
   */
  public long getExecutionTimeMillis() {
    return execMillis;
  }

  /**
   * 入力バイト数を取得
   *
   * @return 入力バイト数
   */
  public long getInputBytes() {
    return inputBytes;
  }

  /**
   * 出力バイト数を取得
   *
   * @return 出力バイト数
   */
  public long getOutputBytes() {
    return outputBytes;
  }

  /**
   * エラーメッセージを取得（エラー時のみ）
   *
   * @return エラーメッセージ（エラーがない場合はnull）
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * 開始時刻を取得
   *
   * @return 開始時刻
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * 終了時刻を取得
   *
   * @return 終了時刻
   */
  public Instant getEndTime() {
    return endTime;
  }

  /**
   * 実行時間をDurationで取得
   *
   * @return 実行時間
   */
  public Duration getDuration() {
    return Duration.between(startTime, endTime);
  }

  @Override
  public String toString() {
    return String.format(
        "CommandResult{name='%s', success=%s, time=%dms, input=%db, output=%db%s}",
        commandName,
        success,
        execMillis,
        inputBytes,
        outputBytes,
        errorMessage != null ? ", error='" + errorMessage + "'" : "");
  }

  /** CommandResultのビルダークラス */
  public static final class Builder {
    /** Default constructor for CommandResult Builder. */
    public Builder() {}

    private String commandName;
    private boolean success;
    private long execMillis;
    private long inputBytes;
    private long outputBytes;
    private String errorMessage;
    private Instant startTime;
    private Instant endTime;

    /**
     * コマンド名を設定
     *
     * @param commandName コマンド名
     * @return Builderインスタンス
     */
    public Builder withCommandName(final String commandName) {
      this.commandName = commandName;
      return this;
    }

    /**
     * 成功フラグを設定
     *
     * @param success 成功フラグ
     * @return Builderインスタンス
     */
    public Builder withSuccess(final boolean success) {
      this.success = success;
      return this;
    }

    /**
     * 実行時間を設定
     *
     * @param execMillis 実行時間（ミリ秒）
     * @return Builderインスタンス
     */
    public Builder withExecMillis(final long execMillis) {
      this.execMillis = execMillis;
      return this;
    }

    /**
     * 入力バイト数を設定
     *
     * @param inputBytes 入力バイト数
     * @return Builderインスタンス
     */
    public Builder withInputBytes(final long inputBytes) {
      this.inputBytes = inputBytes;
      return this;
    }

    /**
     * 出力バイト数を設定
     *
     * @param outputBytes 出力バイト数
     * @return Builderインスタンス
     */
    public Builder withOutputBytes(final long outputBytes) {
      this.outputBytes = outputBytes;
      return this;
    }

    /**
     * エラーメッセージを設定
     *
     * @param errorMessage エラーメッセージ
     * @return Builderインスタンス
     */
    public Builder withErrorMessage(final String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    /**
     * 開始時刻を設定
     *
     * @param startTime 開始時刻
     * @return Builderインスタンス
     */
    public Builder withStartTime(final Instant startTime) {
      this.startTime = startTime;
      return this;
    }

    /**
     * 終了時刻を設定
     *
     * @param endTime 終了時刻
     * @return Builderインスタンス
     */
    public Builder withEndTime(final Instant endTime) {
      this.endTime = endTime;
      return this;
    }

    /**
     * CommandResultインスタンスを生成
     *
     * @return CommandResultインスタンス
     */
    public CommandResult build() {
      return new CommandResult(this);
    }
  }

  /**
   * 成功結果を作成するヘルパーメソッド
   *
   * @param commandName コマンド名
   * @param executionTime 実行時間（ミリ秒）
   * @param inputBytes 入力バイト数
   * @param outputBytes 出力バイト数
   * @param startTime 開始時刻
   * @param endTime 終了時刻
   * @return 成功結果のCommandResult
   */
  public static CommandResult success(
      final String commandName,
      final long executionTime,
      final long inputBytes,
      final long outputBytes,
      final Instant startTime,
      final Instant endTime) {
    return new Builder()
        .withCommandName(commandName)
        .withSuccess(true)
        .withExecMillis(executionTime)
        .withInputBytes(inputBytes)
        .withOutputBytes(outputBytes)
        .withStartTime(startTime)
        .withEndTime(endTime)
        .build();
  }

  /**
   * 失敗結果を作成するヘルパーメソッド
   *
   * @param commandName コマンド名
   * @param executionTime 実行時間（ミリ秒）
   * @param errorMessage エラーメッセージ
   * @param startTime 開始時刻
   * @param endTime 終了時刻
   * @return 失敗結果のCommandResult
   */
  public static CommandResult failure(
      final String commandName,
      final long executionTime,
      final String errorMessage,
      final Instant startTime,
      final Instant endTime) {
    return new Builder()
        .withCommandName(commandName)
        .withSuccess(false)
        .withExecMillis(executionTime)
        .withErrorMessage(errorMessage)
        .withStartTime(startTime)
        .withEndTime(endTime)
        .build();
  }
}
