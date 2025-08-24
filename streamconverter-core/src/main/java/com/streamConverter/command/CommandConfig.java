package com.streamConverter.command;

/**
 * コマンド設定クラス
 *
 * <p>CommandFactoryで使用するコマンドの設定情報を保持します。 コマンドクラスとコンストラクタ引数を含みます。
 */
public class CommandConfig {
  private final Class<? extends IStreamCommand> commandClass;
  private final Object[] args;
  private final String description;

  /**
   * コンストラクタ
   *
   * @param commandClass コマンドクラス
   * @param args コンストラクタ引数
   */
  public CommandConfig(Class<? extends IStreamCommand> commandClass, Object... args) {
    this.commandClass = commandClass;
    this.args = args;
    this.description = commandClass.getSimpleName();
  }

  /**
   * 説明付きコンストラクタ
   *
   * @param commandClass コマンドクラス
   * @param description コマンドの説明
   * @param args コンストラクタ引数
   */
  public CommandConfig(
      Class<? extends IStreamCommand> commandClass, String description, Object... args) {
    this.commandClass = commandClass;
    this.args = args;
    this.description = description;
  }

  /**
   * コマンドクラスを取得
   *
   * @return コマンドクラス
   */
  public Class<? extends IStreamCommand> getCommandClass() {
    return commandClass;
  }

  /**
   * コンストラクタ引数を取得
   *
   * @return コンストラクタ引数のコピー
   */
  public Object[] getArgs() {
    return args.clone();
  }

  /**
   * コマンドの説明を取得
   *
   * @return コマンドの説明
   */
  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return String.format(
        "CommandConfig{class=%s, description='%s', args=%d}",
        commandClass.getSimpleName(), description, args.length);
  }
}
