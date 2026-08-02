package com.streamconverter.web;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvWalker;
import com.streamconverter.command.impl.json.JsonWalker;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * {@code "csv:name,json:$.result,process"} 形式のパイプライン設定文字列を {@link IStreamCommand} 配列へ変換するパーサ。
 *
 * <p>HTTPリクエスト由来の文字列を解釈するため、DoS を避ける目的で設定長・コマンド数・パラメータ長に上限を設けている。 不正な設定はすべて {@link
 * IllegalArgumentException} として報告し、呼び出し側（コントローラ）が 400 応答へ変換する。
 */
final class PipelineConfigParser {

  private static final int MAX_PIPELINE_CONFIG_LENGTH = 1000;
  private static final int MAX_PIPELINE_COMMANDS = 10;
  private static final int MAX_PARAMETER_LENGTH = 500;

  private PipelineConfigParser() {
    // ユーティリティクラスのためインスタンス化しない
  }

  /**
   * 設定文字列からコマンド配列を構築する。
   *
   * @param config パイプライン設定（例: {@code "csv:name,json:$.result,process"}）
   * @return 生成されたコマンド配列
   * @throws IllegalArgumentException config が null/空/長すぎる、またはコマンド数・パラメータが不正な場合
   */
  static IStreamCommand[] parse(String config) {
    validatePipelineConfig(config);

    String[] commandConfigs = config.split(",", -1);
    if (commandConfigs.length > MAX_PIPELINE_COMMANDS) {
      throw new IllegalArgumentException(
          "Pipeline config exceeds maximum command count of " + MAX_PIPELINE_COMMANDS);
    }

    IStreamCommand[] commands = new IStreamCommand[commandConfigs.length];
    for (int i = 0; i < commandConfigs.length; i++) {
      commands[i] = buildCommand(commandConfigs[i], i);
    }
    return commands;
  }

  /** パイプライン設定文字列そのものの妥当性（null/空/長さ）を検証する。 */
  private static void validatePipelineConfig(String config) {
    if (config == null || config.isBlank()) {
      throw new IllegalArgumentException("Pipeline config must not be null or blank");
    }
    if (config.length() > MAX_PIPELINE_CONFIG_LENGTH) {
      throw new IllegalArgumentException(
          "Pipeline config exceeds maximum length of " + MAX_PIPELINE_CONFIG_LENGTH);
    }
  }

  /**
   * {@code "csv:name"} 形式の1コマンド分の設定からコマンドを生成する。
   *
   * @param commandConfig 1コマンド分の設定文字列
   * @param index パイプライン内の位置（エラーメッセージ用）
   * @return 生成されたコマンド
   * @throws IllegalArgumentException コマンド種別／パラメータが不正な場合
   */
  private static IStreamCommand buildCommand(String commandConfig, int index) {
    String[] parts = commandConfig.split(":", 2);
    String commandType = parts[0].trim();
    if (commandType.isEmpty()) {
      throw new IllegalArgumentException("Command type must not be empty at index " + index);
    }
    String parameter = parts.length > 1 ? parts[1].trim() : "";
    if (parameter.length() > MAX_PARAMETER_LENGTH) {
      throw new IllegalArgumentException(
          "Parameter exceeds maximum length of " + MAX_PARAMETER_LENGTH + " at index " + index);
    }

    return createCommand(commandType, parameter, index);
  }

  /**
   * 検証済みのコマンド種別とパラメータからコマンド実体を生成する。
   *
   * @param commandType コマンド種別（csv / json / process）
   * @param parameter コマンドのパラメータ
   * @param index パイプライン内の位置（エラーメッセージ用）
   * @return 生成されたコマンド
   * @throws IllegalArgumentException コマンド種別が未知、または必須パラメータが空の場合
   */
  private static IStreamCommand createCommand(String commandType, String parameter, int index) {
    return switch (commandType.toLowerCase(Locale.ROOT)) {
      case "csv" ->
          CsvWalker.create(
              CSVPath.of(
                  requireParameter(
                      parameter, "csv command requires a column name at index " + index)),
              new PassThroughRule());
      case "json" ->
          JsonWalker.create(
              TreePath.fromJson(
                  requireParameter(parameter, "json command requires a path at index " + index)),
              new PassThroughRule());
      case "process" -> createPassThroughCommand();
      default -> throw new IllegalArgumentException("Unknown command type: " + commandType);
    };
  }

  /** パラメータ必須のコマンドに対し、空パラメータを拒否する。 */
  private static String requireParameter(String parameter, String message) {
    if (parameter.isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return parameter;
  }

  /** 入力をそのまま出力へ流す {@code process} コマンドを生成する。 */
  private static IStreamCommand createPassThroughCommand() {
    return new IStreamCommand() {
      @Override
      public void execute(InputStream in, java.io.OutputStream out) throws IOException {
        in.transferTo(out);
      }

      @Override
      public String commandName() {
        return "process";
      }
    };
  }
}
