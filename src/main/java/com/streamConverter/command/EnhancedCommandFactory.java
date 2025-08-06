package com.streamConverter.command;

import com.streamConverter.factory.AbstractFactory;
import com.streamConverter.factory.FactoryConfiguration;
import com.streamConverter.factory.FactoryException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced Command Factory with optimized integration capabilities.
 *
 * <p>This factory extends AbstractFactory to provide enhanced command creation with:
 *
 * <ul>
 *   <li>Intelligent caching with configurable cache policies
 *   <li>Unified error handling and logging integration
 *   <li>Performance optimizations for high-throughput scenarios
 *   <li>Seamless integration with ControllerFactory
 *   <li>Backward compatibility with existing CommandFactory API
 * </ul>
 *
 * <p>This class addresses Issue #124 by providing optimized integration between CommandFactory and
 * ControllerFactory, reducing redundancy while maintaining backward compatibility.
 *
 * <p>Usage examples:
 *
 * <pre>
 * // Static methods (backward compatible)
 * IStreamCommand command = EnhancedCommandFactory.createWithLogging(CsvNavigateCommand.class, "name");
 *
 * // Instance methods with configuration
 * EnhancedCommandFactory factory = new EnhancedCommandFactory(FactoryConfiguration.productionConfig());
 * IStreamCommand cached = factory.createCached(JsonNavigateCommand.class, "$.user");
 *
 * // Pipeline creation with optimization
 * IStreamCommand[] pipeline = factory.createPipelineOptimized(
 *     CommandConfig.of(CsvNavigateCommand.class, "name"),
 *     CommandConfig.of(JsonNavigateCommand.class, "$.user.name")
 * );
 * </pre>
 *
 * @since 1.0
 */
public class EnhancedCommandFactory extends AbstractFactory<IStreamCommand> {
  private static final Logger log = LoggerFactory.getLogger(EnhancedCommandFactory.class);

  /** Default singleton instance for static methods */
  private static final EnhancedCommandFactory DEFAULT_INSTANCE = new EnhancedCommandFactory();

  /** Creates factory with default configuration. */
  public EnhancedCommandFactory() {
    super();
  }

  /**
   * Creates factory with custom configuration.
   *
   * @param config factory configuration
   */
  public EnhancedCommandFactory(FactoryConfiguration config) {
    super(config);
  }

  // ========== Static Methods (Backward Compatibility) ==========

  /**
   * Creates command with logging using default instance. Maintains backward compatibility with
   * existing CommandFactory.
   *
   * @param <T> command type
   * @param commandClass command class
   * @param args constructor arguments
   * @return command with logging enabled
   */
  public static <T extends IStreamCommand> T createWithLogging(
      Class<T> commandClass, Object... args) {
    try {
      return DEFAULT_INSTANCE.createCommandWithLogging(commandClass, false, args);
    } catch (FactoryException e) {
      throw new RuntimeException("Command creation failed: " + commandClass.getSimpleName(), e);
    }
  }

  /**
   * Creates command with detailed logging using default instance.
   *
   * @param <T> command type
   * @param commandClass command class
   * @param enableDetailedLogging whether to enable detailed logging
   * @param args constructor arguments
   * @return command with configured logging
   */
  public static <T extends IStreamCommand> T createWithLogging(
      Class<T> commandClass, boolean enableDetailedLogging, Object... args) {
    try {
      return DEFAULT_INSTANCE.createCommandWithLogging(commandClass, enableDetailedLogging, args);
    } catch (FactoryException e) {
      throw new RuntimeException("Command creation failed: " + commandClass.getSimpleName(), e);
    }
  }

  /**
   * Creates pipeline with logging using default instance.
   *
   * @param configs command configurations
   * @return array of commands with logging
   */
  public static IStreamCommand[] createPipelineWithLogging(CommandConfig... configs) {
    try {
      return DEFAULT_INSTANCE.createPipeline(configs, false);
    } catch (FactoryException e) {
      throw new RuntimeException("Pipeline creation failed", e);
    }
  }

  /**
   * Creates pipeline with detailed logging using default instance.
   *
   * @param configs command configurations
   * @return array of commands with detailed logging
   */
  public static IStreamCommand[] createPipelineWithDetailedLogging(CommandConfig... configs) {
    try {
      return DEFAULT_INSTANCE.createPipeline(configs, true);
    } catch (FactoryException e) {
      throw new RuntimeException("Pipeline creation failed", e);
    }
  }

  // ========== Enhanced Instance Methods ==========

  /**
   * Creates command with caching support.
   *
   * @param <T> command type
   * @param commandClass command class
   * @param args constructor arguments
   * @return cached or newly created command
   * @throws FactoryException if creation fails
   */
  public <T extends IStreamCommand> T createCached(Class<T> commandClass, Object... args)
      throws FactoryException {
    String cacheKey = createCacheKey(commandClass.getName(), args);

    @SuppressWarnings("unchecked")
    T cached = (T) getCachedInstance(cacheKey);
    if (cached != null) {
      log.debug("Retrieved cached command: {} (key: {})", commandClass.getSimpleName(), cacheKey);
      return cached;
    }

    T command = createCommandWithLogging(commandClass, config.isDetailedLoggingEnabled(), args);
    cacheInstance(cacheKey, command);
    return command;
  }

  /**
   * Creates optimized pipeline with intelligent caching.
   *
   * @param configs command configurations
   * @return optimized command pipeline
   * @throws FactoryException if creation fails
   */
  public IStreamCommand[] createPipelineOptimized(CommandConfig... configs)
      throws FactoryException {
    return createPipeline(configs, config.isDetailedLoggingEnabled());
  }

  /**
   * Creates command specifically for ControllerFactory integration. This method is optimized for
   * ControllerFactory use cases.
   *
   * @param <T> command type
   * @param commandClass command class
   * @param enableDetailedLogging whether to enable detailed logging
   * @param args constructor arguments
   * @return command optimized for controller integration
   * @throws FactoryException if creation fails
   */
  public <T extends IStreamCommand> T createForControllerIntegration(
      Class<T> commandClass, boolean enableDetailedLogging, Object... args)
      throws FactoryException {
    return createCommandWithLogging(commandClass, enableDetailedLogging, args);
  }

  // ========== Private Implementation Methods ==========

  /** Core command creation method with logging integration. */
  @SuppressWarnings("unchecked")
  private <T extends IStreamCommand> T createCommandWithLogging(
      Class<T> commandClass, boolean enableDetailedLogging, Object... args)
      throws FactoryException {

    T command = createInstance(commandClass, args);

    if (config.isDetailedLoggingEnabled() || enableDetailedLogging) {
      log.info(
          "Created command instance: {} with {} args (detailed logging: {})",
          commandClass.getSimpleName(),
          args.length,
          enableDetailedLogging);
    }

    // Handle logging decoration
    if (command instanceof AbstractStreamCommand) {
      // AbstractStreamCommand already has integrated logging
      return command;
    } else if (enableDetailedLogging) {
      // Wrap with LoggingDecorator for non-AbstractStreamCommand instances
      return (T) new LoggingDecorator(command);
    } else {
      return command;
    }
  }

  /** Creates pipeline with specified logging configuration. */
  private IStreamCommand[] createPipeline(CommandConfig[] configs, boolean enableDetailedLogging)
      throws FactoryException {
    List<IStreamCommand> commands = new ArrayList<>();

    log.info(
        "Creating pipeline with {} commands (detailed logging: {})",
        configs.length,
        enableDetailedLogging);

    for (int i = 0; i < configs.length; i++) {
      CommandConfig config = configs[i];
      try {
        IStreamCommand command =
            createCommandWithLogging(
                config.getCommandClass(), enableDetailedLogging, config.getArgs());
        commands.add(command);

        if (log.isDebugEnabled()) {
          log.debug("Added command {}/{}: {}", i + 1, configs.length, config.getDescription());
        }
      } catch (Exception e) {
        String message =
            String.format(
                "Failed to create command %d/%d: %s",
                i + 1, configs.length, config.getDescription());
        log.error(message, e);

        if (this.config.isFailFastOnError()) {
          throw new FactoryException(message, e);
        }
        // Continue with next command if fail-fast is disabled
      }
    }

    log.info("Created pipeline with {}/{} commands successfully", commands.size(), configs.length);
    return commands.toArray(new IStreamCommand[0]);
  }

  // ========== Integration Support Methods ==========

  /**
   * Gets the default factory instance. Useful for ControllerFactory integration.
   *
   * @return default factory instance
   */
  public static EnhancedCommandFactory getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  /**
   * Creates a production-optimized factory instance. Recommended for ControllerFactory integration
   * in production environments.
   *
   * @return production-optimized factory
   */
  public static EnhancedCommandFactory createProductionInstance() {
    return new EnhancedCommandFactory(FactoryConfiguration.productionConfig());
  }

  /**
   * Creates a development-friendly factory instance. Useful for debugging and development
   * scenarios.
   *
   * @return development-optimized factory
   */
  public static EnhancedCommandFactory createDevelopmentInstance() {
    return new EnhancedCommandFactory(FactoryConfiguration.developmentConfig());
  }
}
