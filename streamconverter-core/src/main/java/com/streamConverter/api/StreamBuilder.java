package com.streamConverter.api;

import com.streamConverter.CommandResult;
import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.SendHttpCommand;
import com.streamConverter.command.impl.charaCode.CharacterConvertCommand;
import com.streamConverter.command.impl.csv.CsvNavigateCommand;
import com.streamConverter.command.impl.csv.CsvValidateCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.impl.json.JsonValidateCommand;
import com.streamConverter.command.impl.xml.ValidateCommand;
import com.streamConverter.command.impl.xml.XmlNavigateCommand;
import com.streamConverter.command.rule.PassThroughRule;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 流暢なAPI（Fluent API）を提供するStreamBuilderクラス
 *
 * <p>このクラスはメソッドチェーンを使用してストリーム処理パイプラインを 直感的に構築できるビルダーパターンを実装しています。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * List<CommandResult> results = StreamBuilder.create()
 *     .fromString("Hello, World!")
 *     .validateJson("schema.json")
 *     .extractJson("$.message")
 *     .convertEncoding("UTF-8", "UTF-16")
 *     .sendHttp("https://api.example.com/process")
 *     .toStream(outputStream);
 * }</pre>
 */
public final class StreamBuilder {

  private final List<IStreamCommand> commands;
  private InputStream inputStream;
  private DataFormat dataFormat = DataFormat.GENERIC;

  /** プライベートコンストラクタ - ファクトリメソッドを使用してインスタンス化 */
  private StreamBuilder() {
    this.commands = new ArrayList<>();
  }

  /**
   * 新しいStreamBuilderインスタンスを作成します
   *
   * @return 新しいStreamBuilderインスタンス
   */
  public static StreamBuilder create() {
    return new StreamBuilder();
  }

  // === Input Sources ===

  /**
   * 文字列から入力ストリームを作成
   *
   * @param input 入力文字列（UTF-8エンコーディング）
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder fromString(final String input) {
    Objects.requireNonNull(input, "Input string cannot be null");
    this.inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    return this;
  }

  /**
   * InputStreamから入力を設定
   *
   * @param inputStream 入力ストリーム
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder fromStream(final InputStream inputStream) {
    Objects.requireNonNull(inputStream, "InputStream cannot be null");
    this.inputStream = inputStream;
    return this;
  }

  /**
   * ファイルから入力ストリームを作成
   *
   * @param filePath ファイルパス
   * @return このビルダーインスタンス（メソッドチェーン用）
   * @throws IOException ファイル読み込みエラー
   */
  public StreamBuilder fromFile(final String filePath) throws IOException {
    Objects.requireNonNull(filePath, "File path cannot be null");
    this.inputStream = Files.newInputStream(Paths.get(filePath));
    return this;
  }

  // === Data Format Configuration ===

  /**
   * データ形式をJSONに設定
   *
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder asJson() {
    this.dataFormat = DataFormat.JSON;
    return this;
  }

  /**
   * データ形式をCSVに設定
   *
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder asCsv() {
    this.dataFormat = DataFormat.CSV;
    return this;
  }

  /**
   * データ形式をXMLに設定
   *
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder asXml() {
    this.dataFormat = DataFormat.XML;
    return this;
  }

  /**
   * 現在のデータ形式を取得
   *
   * @return 現在のデータ形式
   */
  public DataFormat getFormat() {
    return dataFormat;
  }

  // === Validation Commands ===

  /**
   * JSONスキーマバリデーションを追加
   *
   * @param schemaPath JSONスキーマファイルのパス
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder validateJson(String schemaPath) {
    Objects.requireNonNull(schemaPath, "Schema path cannot be null");
    this.commands.add(new JsonValidateCommand(schemaPath));
    return this;
  }

  /**
   * XMLスキーマバリデーションを追加
   *
   * @param schemaPath XMLスキーマファイルのパス
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder validateXml(String schemaPath) {
    Objects.requireNonNull(schemaPath, "Schema path cannot be null");
    this.commands.add(new ValidateCommand(schemaPath));
    return this;
  }

  /**
   * CSVスキーマバリデーションを追加
   *
   * @param requiredColumns 必須列名の配列
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder validateCsv(final String... requiredColumns) {
    Objects.requireNonNull(requiredColumns, "Required columns cannot be null");
    this.commands.add(new CsvValidateCommand(requiredColumns));
    return this;
  }

  // === Unified Format-Aware Commands ===

  /**
   * 現在のデータ形式に応じたバリデーションを追加
   *
   * @param schemaPath スキーマファイルのパス
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder validate(final String schemaPath) {
    Objects.requireNonNull(schemaPath, "Schema path cannot be null");

    switch (dataFormat) {
      case JSON:
        this.commands.add(new JsonValidateCommand(schemaPath));
        break;
      case XML:
        this.commands.add(new ValidateCommand(schemaPath));
        break;
      case CSV:
        // CSVの場合はschemaPathを必須カラム配列として解釈
        // TODO: より良いCSVスキーマ指定方法を検討
        throw new UnsupportedOperationException(
            "CSV validation with schema path not supported. Use validateCsv(String[]) instead.");
      case GENERIC:
      default:
        throw new IllegalStateException(
            "Data format must be specified before validation. Use asJson(), asCsv(), or asXml() first.");
    }

    return this;
  }

  /**
   * 現在のデータ形式に応じたデータ抽出を追加
   *
   * @param path 抽出パス（JSONPath、XPath、CSV列名など）
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder extract(final String path) {
    Objects.requireNonNull(path, "Extraction path cannot be null");

    switch (dataFormat) {
      case JSON:
        this.commands.add(
            JsonNavigateCommand.create(
                new com.streamConverter.path.JSONPath(path), new PassThroughRule()));
        break;
      case XML:
        this.commands.add(
            XmlNavigateCommand.create(
                new com.streamConverter.path.XPath(path), new PassThroughRule()));
        break;
      case CSV:
        this.commands.add(CsvNavigateCommand.create(path, new PassThroughRule()));
        break;
      case GENERIC:
      default:
        throw new IllegalStateException(
            "Data format must be specified before extraction. Use asJson(), asCsv(), or asXml() first.");
    }

    return this;
  }

  /**
   * JSONデータのフォーマット処理を追加（JSON形式のみ）
   *
   * @return このビルダーインスタンス（メソッドチェーン用）
   * @deprecated Use a dedicated formatting command instead of NavigateCommand with null path
   */
  @Deprecated
  public StreamBuilder format() {
    throw new UnsupportedOperationException(
        "format() method removed - NavigateCommand requires a specific path. Use a dedicated formatting command instead.");
  }

  // === Extraction Commands ===

  /**
   * JSONパスによるデータ抽出を追加
   *
   * @param jsonPath JSONPath式（例: "$.user.name"）
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder extractJson(final String jsonPath) {
    Objects.requireNonNull(jsonPath, "JSONPath cannot be null");
    this.commands.add(
        JsonNavigateCommand.create(
            new com.streamConverter.path.JSONPath(jsonPath), new PassThroughRule()));
    return this;
  }

  /**
   * JSONデータのフォーマット処理を追加（JSONPath指定なし）
   *
   * @return このビルダーインスタンス（メソッドチェーン用）
   * @deprecated Use a dedicated formatting command instead of NavigateCommand with null path
   */
  @Deprecated
  public StreamBuilder formatJson() {
    throw new UnsupportedOperationException(
        "formatJson() method removed - NavigateCommand requires a specific path. Use a dedicated formatting command instead.");
  }

  /**
   * XPathによるXMLデータ抽出を追加
   *
   * @param xpath XPath式（例: "//user/name"）
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder extractXml(final String xpath) {
    Objects.requireNonNull(xpath, "XPath cannot be null");
    this.commands.add(
        XmlNavigateCommand.create(
            new com.streamConverter.path.XPath(xpath), new PassThroughRule()));
    return this;
  }

  /**
   * CSV列による抽出を追加
   *
   * @param columnName 列名
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder extractCsv(final String columnName) {
    Objects.requireNonNull(columnName, "Column name cannot be null");
    this.commands.add(CsvNavigateCommand.create(columnName, new PassThroughRule()));
    return this;
  }

  // === Transformation Commands ===

  /**
   * 文字エンコーディング変換を追加
   *
   * @param fromEncoding 変換元エンコーディング（例: "UTF-8"）
   * @param toEncoding 変換先エンコーディング（例: "UTF-16"）
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder convertEncoding(final String fromEncoding, final String toEncoding) {
    Objects.requireNonNull(fromEncoding, "From encoding cannot be null");
    Objects.requireNonNull(toEncoding, "To encoding cannot be null");
    this.commands.add(new CharacterConvertCommand(fromEncoding, toEncoding));
    return this;
  }

  /**
   * カスタム処理コマンドを追加
   *
   * @param commandName コマンド名（ログ用）
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder process(final String commandName) {
    Objects.requireNonNull(commandName, "Command name cannot be null");
    this.commands.add(new SampleStreamCommand(commandName));
    return this;
  }

  /**
   * カスタムコマンドを追加
   *
   * @param command カスタムIStreamCommandの実装
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder addCommand(final IStreamCommand command) {
    Objects.requireNonNull(command, "Command cannot be null");
    this.commands.add(command);
    return this;
  }

  // === Network Commands ===

  /**
   * HTTP送信コマンドを追加
   *
   * @param url 送信先URL
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder sendHttp(final String url) {
    Objects.requireNonNull(url, "URL cannot be null");
    this.commands.add(new SendHttpCommand(url));
    return this;
  }

  /**
   * HTTP送信コマンドを追加（基本実装のみサポート）
   *
   * @param url 送信先URL
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder sendHttpPost(final String url) {
    Objects.requireNonNull(url, "URL cannot be null");
    this.commands.add(new SendHttpCommand(url));
    return this;
  }

  // === Configuration and Conditional Operations ===

  /**
   * 条件付きでコマンドを追加
   *
   * @param condition 条件
   * @param configurer 条件がtrueの場合に実行する設定ラムダ
   * @return このビルダーインスタンス（メソッドチェーン用）
   */
  public StreamBuilder when(final boolean condition, final Consumer<StreamBuilder> configurer) {
    Objects.requireNonNull(configurer, "Configurer cannot be null");
    if (condition) {
      configurer.accept(this);
    }
    return this;
  }

  // === Terminal Operations ===

  /**
   * パイプラインを実行して結果を文字列として取得
   *
   * @return 処理結果の文字列
   * @throws IOException 処理中にI/Oエラーが発生した場合
   */
  public String asString() throws IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      execute(outputStream);
      return outputStream.toString(StandardCharsets.UTF_8);
    }
  }

  /**
   * パイプラインを実行して指定されたOutputStreamに結果を書き込み
   *
   * @param outputStream 出力先ストリーム
   * @return コマンド実行結果のリスト
   * @throws IOException 処理中にI/Oエラーが発生した場合
   */
  public List<CommandResult> toStream(final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(outputStream, "OutputStream cannot be null");
    return execute(outputStream);
  }

  /**
   * パイプラインを実行してファイルに結果を出力
   *
   * @param filePath 出力ファイルパス
   * @return コマンド実行結果のリスト
   * @throws IOException 処理中にI/Oエラーが発生した場合
   */
  public List<CommandResult> toFile(final String filePath) throws IOException {
    Objects.requireNonNull(filePath, "File path cannot be null");
    try (OutputStream outputStream = Files.newOutputStream(Paths.get(filePath))) {
      return execute(outputStream);
    }
  }

  /**
   * StreamConverterを構築（再利用可能）
   *
   * @return 構築されたStreamConverterインスタンス
   * @throws IllegalStateException コマンドが設定されていない場合
   */
  public StreamConverter build() {
    if (commands.isEmpty()) {
      throw new IllegalStateException("No commands have been added to the pipeline");
    }
    return new StreamConverter(new ArrayList<>(commands));
  }

  // === Utility Methods ===

  /**
   * 現在のパイプライン情報を取得
   *
   * @return パイプライン情報の文字列
   */
  public String getPipelineInfo() {
    final StringBuilder info = new StringBuilder(64);
    info.append("StreamBuilder Pipeline (").append(commands.size()).append(" commands):\n");
    for (int i = 0; i < commands.size(); i++) {
      final IStreamCommand command = commands.get(i);
      final String commandInfo = formatCommandInfo(command);
      info.append("  ").append(i + 1).append(". ").append(commandInfo).append('\n');
    }
    return info.toString();
  }

  /**
   * コマンド情報を適切にフォーマットする
   *
   * @param command フォーマット対象のコマンド
   * @return フォーマットされたコマンド情報
   */
  private String formatCommandInfo(final IStreamCommand command) {
    String className = command.getClass().getSimpleName();

    // SampleStreamCommandの場合、IDも含める
    if (command instanceof SampleStreamCommand) {
      String commandString = command.toString();
      if (commandString.contains("[id=")) {
        String id =
            commandString.substring(commandString.indexOf("[id=") + 4, commandString.indexOf("]"));
        return className + " [" + id + "]";
      }
    }

    // その他のコマンドは従来通りクラス名のみ
    return className;
  }

  /**
   * コマンド数を取得
   *
   * @return 現在設定されているコマンド数
   */
  public int getCommandCount() {
    return commands.size();
  }

  /**
   * パイプラインが空かどうかを確認
   *
   * @return コマンドが設定されていない場合true
   */
  public boolean isEmpty() {
    return commands.isEmpty();
  }

  // === Private Helper Methods ===

  /** 内部実行メソッド */
  private List<CommandResult> execute(OutputStream outputStream) throws IOException {
    if (inputStream == null) {
      throw new IllegalStateException("No input source has been specified");
    }
    if (commands.isEmpty()) {
      throw new IllegalStateException("No commands have been added to the pipeline");
    }

    StreamConverter converter = new StreamConverter(new ArrayList<>(commands));
    return converter.run(inputStream, outputStream);
  }
}
