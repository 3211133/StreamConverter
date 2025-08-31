package com.streamConverter.command;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * コマンドファクトリークラス
 *
 * <p>このクラスは、ログ機能付きのIStreamCommandインスタンスを生成するためのファクトリーです。
 * 自動的にLoggingDecoratorでラップされたコマンドを作成し、統一されたログ出力を提供します。
 *
 * <p>使用例:
 *
 * <pre>
 * // 単一コマンドの生成
 * IStreamCommand command = CommandFactory.createWithLogging(CsvNavigateCommand.class, "name");
 *
 * // パイプライン全体の生成
 * IStreamCommand[] pipeline = CommandFactory.createPipelineWithLogging(
 *     new CommandConfig(CsvNavigateCommand.class, "name"),
 *     new CommandConfig(JsonNavigateCommand.class, "$.user.name")
 * );
 * </pre>
 */
public class CommandFactory {
  private static final Logger log = LoggerFactory.getLogger(CommandFactory.class);

  /** Prevent instantiation. */
  private CommandFactory() {}

  /**
   * ログ機能付きコマンドを生成
   *
   * @param <T> コマンドタイプ
   * @param commandClass コマンドクラス
   * @param args コンストラクタ引数
   * @return ログ機能付きコマンド
   */
  @SuppressWarnings("unchecked")
  public static <T extends IStreamCommand> T createWithLogging(
      Class<T> commandClass, Object... args) {
    try {
      T command = createNavigateCommandIfApplicable(commandClass, args);
      if (command == null) {
        command = createInstance(commandClass, args);
      }
      log.info(
          "Created command instance: {} with {} args", commandClass.getSimpleName(), args.length);

      // AbstractStreamCommandの場合は既にログ機能があるため、重複を避ける
      if (command instanceof AbstractStreamCommand) {
        return command;
      } else {
        // LoggingDecoratorでラップして返す
        return (T) new LoggingDecorator(command);
      }
    } catch (Exception e) {
      log.error("Failed to create command instance: {}", commandClass.getSimpleName(), e);
      throw new RuntimeException("Command creation failed: " + commandClass.getSimpleName(), e);
    }
  }

  /**
   * ログ機能付きコマンドを生成（詳細ログ付き）
   *
   * @param <T> コマンドタイプ
   * @param commandClass コマンドクラス
   * @param enableDetailedLogging 詳細ログを有効にするかどうか
   * @param args コンストラクタ引数
   * @return ログ機能付きコマンド
   */
  @SuppressWarnings("unchecked")
  public static <T extends IStreamCommand> T createWithLogging(
      Class<T> commandClass, boolean enableDetailedLogging, Object... args) {
    try {
      T command = createNavigateCommandIfApplicable(commandClass, args);
      if (command == null) {
        command = createInstance(commandClass, args);
      }
      log.info(
          "Created command instance: {} with {} args (detailed logging: {})",
          commandClass.getSimpleName(),
          args.length,
          enableDetailedLogging);

      if (enableDetailedLogging) {
        // 常にLoggingDecoratorでラップ
        return (T) new LoggingDecorator(command);
      } else {
        // AbstractStreamCommandの場合は既にログ機能があるため、そのまま返す
        return command;
      }
    } catch (Exception e) {
      log.error("Failed to create command instance: {}", commandClass.getSimpleName(), e);
      throw new RuntimeException("Command creation failed: " + commandClass.getSimpleName(), e);
    }
  }

  /**
   * ログ機能付きパイプラインを生成
   *
   * @param configs コマンド設定配列
   * @return ログ機能付きコマンド配列
   */
  public static IStreamCommand[] createPipelineWithLogging(CommandConfig... configs) {
    List<IStreamCommand> commands = new ArrayList<>();

    log.info("Creating pipeline with {} commands", configs.length);

    for (int i = 0; i < configs.length; i++) {
      CommandConfig config = configs[i];
      try {
        IStreamCommand command = createWithLogging(config.getCommandClass(), config.getArgs());
        commands.add(command);
        log.debug("Added command {}/{}: {}", i + 1, configs.length, config.getDescription());
      } catch (Exception e) {
        log.error(
            "Failed to create command {}/{}: {}",
            i + 1,
            configs.length,
            config.getDescription(),
            e);
        throw new RuntimeException("Pipeline creation failed at command " + (i + 1), e);
      }
    }

    log.info("Created pipeline with {} commands successfully", commands.size());
    return commands.toArray(new IStreamCommand[0]);
  }

  /**
   * パイプライン全体を詳細ログ付きで生成
   *
   * @param configs コマンド設定配列
   * @return 詳細ログ機能付きコマンド配列
   */
  public static IStreamCommand[] createPipelineWithDetailedLogging(CommandConfig... configs) {
    List<IStreamCommand> commands = new ArrayList<>();

    log.info("Creating pipeline with detailed logging for {} commands", configs.length);

    for (int i = 0; i < configs.length; i++) {
      CommandConfig config = configs[i];
      try {
        IStreamCommand command =
            createWithLogging(config.getCommandClass(), true, config.getArgs());
        commands.add(command);
        log.debug(
            "Added command {}/{} with detailed logging: {}",
            i + 1,
            configs.length,
            config.getDescription());
      } catch (Exception e) {
        log.error(
            "Failed to create command {}/{}: {}",
            i + 1,
            configs.length,
            config.getDescription(),
            e);
        throw new RuntimeException("Pipeline creation failed at command " + (i + 1), e);
      }
    }

    log.info(
        "Created pipeline with detailed logging for {} commands successfully", commands.size());
    return commands.toArray(new IStreamCommand[0]);
  }

  /**
   * Creates Navigate command using factory methods if applicable. This method handles the new API
   * for Navigate commands that no longer have deprecated constructors.
   *
   * @param <T> command type
   * @param commandClass the command class
   * @param args constructor arguments
   * @return created command or null if not applicable
   */
  @SuppressWarnings("unchecked")
  private static <T extends IStreamCommand> T createNavigateCommandIfApplicable(
      Class<T> commandClass, Object... args) {

    try {
      String className = commandClass.getSimpleName();

      if ("JsonNavigateCommand".equals(className)) {
        return createJsonNavigateCommand(commandClass, args);
      } else if ("CsvNavigateCommand".equals(className)) {
        return createCsvNavigateCommand(commandClass, args);
      } else if ("XmlNavigateCommand".equals(className)) {
        return createXmlNavigateCommand(commandClass, args);
      }

      return null; // Not a Navigate command

    } catch (Exception e) {
      log.error("Failed to create Navigate command: " + commandClass.getSimpleName(), e);
      return null; // Fall back to regular constructor
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends IStreamCommand> T createJsonNavigateCommand(
      Class<T> commandClass, Object... args) {
    try {
      com.streamConverter.command.rule.PassThroughRule defaultRule =
          new com.streamConverter.command.rule.PassThroughRule();

      if (args.length == 1) {
        Object path = args[0];
        return (T)
            com.streamConverter.command.impl.json.JsonNavigateCommand.create(
                new com.streamConverter.path.JSONPath((String) path), defaultRule);
      } else if (args.length == 0) {
        throw new IllegalArgumentException(
            "JsonNavigateCommand requires a JSONPath - use a dedicated transform command for entire JSON processing");
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends IStreamCommand> T createCsvNavigateCommand(
      Class<T> commandClass, Object... args) {
    try {
      com.streamConverter.command.rule.PassThroughRule defaultRule =
          new com.streamConverter.command.rule.PassThroughRule();

      if (args.length == 1) {
        Object path = args[0];
        return (T)
            com.streamConverter.command.impl.csv.CsvNavigateCommand.create(
                (String) path, defaultRule);
      } else if (args.length == 0) {
        throw new IllegalArgumentException(
            "CsvNavigateCommand requires a column selector - use a dedicated transform command for entire CSV processing");
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends IStreamCommand> T createXmlNavigateCommand(
      Class<T> commandClass, Object... args) {
    try {
      com.streamConverter.command.rule.PassThroughRule defaultRule =
          new com.streamConverter.command.rule.PassThroughRule();

      if (args.length == 1) {
        Object path = args[0];
        return (T)
            com.streamConverter.command.impl.xml.XmlNavigateCommand.create(
                new com.streamConverter.path.XPath((String) path), defaultRule);
      } else if (args.length == 0) {
        throw new IllegalArgumentException(
            "XmlNavigateCommand requires an XPath - use a dedicated transform command for entire XML processing");
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * コマンドインスタンスを生成
   *
   * @param <T> コマンドタイプ
   * @param commandClass コマンドクラス
   * @param args コンストラクタ引数
   * @return コマンドインスタンス
   * @throws Exception インスタンス生成失敗時
   */
  private static <T extends IStreamCommand> T createInstance(Class<T> commandClass, Object... args)
      throws Exception {
    // デフォルトコンストラクタを試す
    if (args.length == 0) {
      Constructor<T> constructor = commandClass.getDeclaredConstructor();
      return constructor.newInstance();
    }

    // 引数の型を取得
    Class<?>[] argTypes = new Class<?>[args.length];
    for (int i = 0; i < args.length; i++) {
      argTypes[i] = args[i].getClass();
    }

    // まず完全一致するコンストラクタを探す
    try {
      Constructor<T> constructor = commandClass.getDeclaredConstructor(argTypes);
      return constructor.newInstance(args);
    } catch (NoSuchMethodException e) {
      // 完全一致しない場合は、利用可能なコンストラクタを探す
      return createInstanceWithBestMatch(commandClass, args, argTypes);
    }
  }

  /**
   * 最適なコンストラクタを探してインスタンスを生成
   *
   * @param <T> コマンドタイプ
   * @param commandClass コマンドクラス
   * @param args コンストラクタ引数
   * @param argTypes 引数の型
   * @return コマンドインスタンス
   * @throws Exception インスタンス生成失敗時
   */
  @SuppressWarnings("unchecked")
  private static <T extends IStreamCommand> T createInstanceWithBestMatch(
      Class<T> commandClass, Object[] args, Class<?>[] argTypes) throws Exception {
    Constructor<?>[] constructors = commandClass.getDeclaredConstructors();

    for (Constructor<?> constructor : constructors) {
      Class<?>[] paramTypes = constructor.getParameterTypes();

      if (paramTypes.length == args.length) {
        boolean compatible = true;

        for (int i = 0; i < paramTypes.length; i++) {
          if (!isCompatible(paramTypes[i], argTypes[i])) {
            compatible = false;
            break;
          }
        }

        if (compatible) {
          return (T) constructor.newInstance(args);
        }
      }
    }

    throw new NoSuchMethodException(
        "No compatible constructor found for " + commandClass.getSimpleName());
  }

  /**
   * 型の互換性をチェック
   *
   * @param paramType パラメータ型
   * @param argType 引数型
   * @return 互換性があるかどうか
   */
  private static boolean isCompatible(Class<?> paramType, Class<?> argType) {
    if (paramType.equals(argType)) {
      return true;
    }

    // プリミティブ型とラッパー型の対応
    if (paramType.isPrimitive()) {
      if (paramType == int.class && argType == Integer.class) return true;
      if (paramType == long.class && argType == Long.class) return true;
      if (paramType == boolean.class && argType == Boolean.class) return true;
      if (paramType == double.class && argType == Double.class) return true;
      if (paramType == float.class && argType == Float.class) return true;
      if (paramType == char.class && argType == Character.class) return true;
      if (paramType == byte.class && argType == Byte.class) return true;
      if (paramType == short.class && argType == Short.class) return true;
    }

    // 継承関係をチェック
    return paramType.isAssignableFrom(argType);
  }
}
