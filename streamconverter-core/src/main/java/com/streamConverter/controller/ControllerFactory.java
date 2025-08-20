package com.streamConverter.controller;

import com.streamConverter.command.EnhancedCommandFactory;
import com.streamConverter.factory.FactoryConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating and managing stream controllers with CommandFactory integration.
 *
 * <p>This factory provides centralized controller creation and management, supporting:
 *
 * <ul>
 *   <li>Controller creation based on data types and processing requirements
 *   <li>CommandFactory integration for unified command and controller creation
 *   <li>Type-safe OutputType enum for eliminating hardcoded string literals
 *   <li>Controller registry for reuse and management
 *   <li>Automatic controller selection based on input/output types
 *   <li>Configuration validation and optimization
 *   <li>Detailed logging control through CommandFactory integration
 * </ul>
 *
 * <p>The factory pattern enhances the Controller architecture by providing a single entry point for
 * external systems to obtain appropriate controllers while leveraging the existing CommandFactory
 * infrastructure for consistent command creation and logging.
 *
 * <p>Usage examples:
 *
 * <pre>
 * // Get controller by data types (string-based)
 * IStreamController controller = ControllerFactory.getController("CSV", "JSON_PROPERTY");
 *
 * // Get controller with OutputType enum (type-safe)
 * IStreamController controller = ControllerFactory.getController("CSV", OutputType.JSON_PROPERTY);
 *
 * // Get controller with CommandFactory integration and OutputType enum
 * IStreamController controller = ControllerFactory.createWithCommandFactory("CSV", OutputType.JSON_FORMATTED, true);
 *
 * // Get controller with specific configuration
 * IStreamController controller = ControllerFactory.getCsvController()
 *     .forColumnExtraction("name");
 *
 * // Register custom controller
 * ControllerFactory.registerController("CUSTOM_CSV", "CUSTOM_OUTPUT", customController);
 * </pre>
 *
 * @author StreamConverter Team
 * @version 1.0
 * @since 1.0
 */
public class ControllerFactory {

  private static final Logger LOG = LoggerFactory.getLogger(ControllerFactory.class);

  /** Registry of controllers by type combination */
  private static final Map<String, IStreamController> CONTROLLER_REGISTRY = new HashMap<>();

  /** Registry of controller builders by input type */
  private static final Map<String, ControllerBuilder> BUILDER_REGISTRY = new HashMap<>();

  /** Enhanced command factory for optimized command creation */
  private static final EnhancedCommandFactory COMMAND_FACTORY =
      EnhancedCommandFactory.createProductionInstance();

  static {
    // Initialize default builders with CommandFactory integration
    BUILDER_REGISTRY.put("CSV", new CsvControllerBuilder());
    BUILDER_REGISTRY.put("JSON", new JsonControllerBuilder());

    LOG.info(
        "Initialized ControllerFactory with enhanced CommandFactory integration for optimized performance");
  }

  /**
   * Gets a controller for the specified input and output data types using OutputType enum.
   *
   * <p>This method provides type-safe controller creation by using the OutputType enum instead of
   * string literals.
   *
   * @param inputType the expected input data type (e.g., "CSV", "JSON", "XML")
   * @param outputType the expected output data type as enum
   * @return appropriate controller, or null if none can be created
   * @throws IllegalArgumentException if input parameters are invalid
   */
  public static IStreamController getController(String inputType, OutputType outputType) {
    Objects.requireNonNull(outputType, "Output type cannot be null");
    return getController(inputType, outputType.getValue());
  }

  /**
   * Gets a controller for the specified input and output data types.
   *
   * <p>This method attempts to find an appropriate controller from the registry or create a new one
   * using the available builders.
   *
   * @param inputType the expected input data type (e.g., "CSV", "JSON", "XML")
   * @param outputType the expected output data type (e.g., "CSV_COLUMN", "JSON_PROPERTY")
   * @return appropriate controller, or null if none can be created
   * @throws IllegalArgumentException if input parameters are invalid
   */
  public static IStreamController getController(String inputType, String outputType) {
    Objects.requireNonNull(inputType, "Input type cannot be null");
    Objects.requireNonNull(outputType, "Output type cannot be null");

    String key = createRegistryKey(inputType, outputType);

    // Check registry first
    IStreamController cachedController = CONTROLLER_REGISTRY.get(key);
    if (cachedController != null) {
      LOG.debug("Found cached controller for {} → {}", inputType, outputType);
      return cachedController;
    }

    // Try to create using builders
    ControllerBuilder builder = BUILDER_REGISTRY.get(inputType);
    if (builder != null) {
      IStreamController controller = builder.createForOutputType(outputType);
      if (controller != null) {
        LOG.info("Created controller for {} → {} using builder", inputType, outputType);
        // Cache for future use
        CONTROLLER_REGISTRY.put(key, controller);
        return controller;
      }
    }

    LOG.warn("No controller available for {} → {}", inputType, outputType);
    return null;
  }

  /**
   * Gets a CSV controller builder for advanced configuration.
   *
   * @return CSV controller builder
   */
  public static CsvControllerBuilder getCsvController() {
    return new CsvControllerBuilder();
  }

  /**
   * Gets a JSON controller builder for advanced configuration.
   *
   * @return JSON controller builder
   */
  public static JsonControllerBuilder getJsonController() {
    return new JsonControllerBuilder();
  }

  /**
   * Creates a controller using CommandFactory-style configuration with OutputType enum.
   *
   * <p>This method provides type-safe controller creation while leveraging the existing
   * CommandFactory infrastructure for unified command and controller creation.
   *
   * @param inputType the expected input data type
   * @param outputType the expected output data type as enum
   * @param enableDetailedLogging whether to enable detailed logging for created commands
   * @return appropriate controller with CommandFactory integration, or null if none can be created
   */
  public static IStreamController createWithCommandFactory(
      String inputType, OutputType outputType, boolean enableDetailedLogging) {
    Objects.requireNonNull(outputType, "Output type cannot be null");
    return createWithCommandFactory(inputType, outputType.getValue(), enableDetailedLogging);
  }

  /**
   * Creates a controller using CommandFactory-style configuration.
   *
   * <p>This method leverages the existing CommandFactory infrastructure to provide a unified
   * approach to both controller and command creation.
   *
   * @param inputType the expected input data type
   * @param outputType the expected output data type
   * @param enableDetailedLogging whether to enable detailed logging for created commands
   * @return appropriate controller with CommandFactory integration, or null if none can be created
   */
  public static IStreamController createWithCommandFactory(
      String inputType, String outputType, boolean enableDetailedLogging) {
    Objects.requireNonNull(inputType, "Input type cannot be null");
    Objects.requireNonNull(outputType, "Output type cannot be null");

    String key = createRegistryKey(inputType, outputType);

    // Check registry first
    IStreamController cachedController = CONTROLLER_REGISTRY.get(key);
    if (cachedController != null) {
      LOG.debug(
          "Found cached CommandFactory-integrated controller for {} → {}", inputType, outputType);
      return cachedController;
    }

    // Create using enhanced builders with CommandFactory integration
    ControllerBuilder builder = BUILDER_REGISTRY.get(inputType);
    if (builder instanceof CommandFactoryAwareBuilder) {
      CommandFactoryAwareBuilder enhancedBuilder = (CommandFactoryAwareBuilder) builder;
      IStreamController controller =
          enhancedBuilder.createWithCommandFactory(outputType, enableDetailedLogging);
      if (controller != null) {
        LOG.info(
            "Created CommandFactory-integrated controller for {} → {} with detailed logging: {}",
            inputType,
            outputType,
            enableDetailedLogging);
        CONTROLLER_REGISTRY.put(key, controller);
        return controller;
      }
    }

    // Fallback to standard creation
    return getController(inputType, outputType);
  }

  /**
   * Registers a custom controller for specific input/output type combination.
   *
   * @param inputType the input data type
   * @param outputType the output data type
   * @param controller the controller to register
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static void registerController(
      String inputType, String outputType, IStreamController controller) {
    Objects.requireNonNull(inputType, "Input type cannot be null");
    Objects.requireNonNull(outputType, "Output type cannot be null");
    Objects.requireNonNull(controller, "Controller cannot be null");

    String key = createRegistryKey(inputType, outputType);
    CONTROLLER_REGISTRY.put(key, controller);

    LOG.info(
        "Registered custom controller for {} → {}: {}",
        inputType,
        outputType,
        controller.getClass().getSimpleName());
  }

  /**
   * Registers a custom controller builder for a specific input type.
   *
   * @param inputType the input data type
   * @param builder the controller builder to register
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static void registerBuilder(String inputType, ControllerBuilder builder) {
    Objects.requireNonNull(inputType, "Input type cannot be null");
    Objects.requireNonNull(builder, "Builder cannot be null");

    BUILDER_REGISTRY.put(inputType, builder);

    LOG.info(
        "Registered custom builder for input type {}: {}",
        inputType,
        builder.getClass().getSimpleName());
  }

  /**
   * Creates a controller with optimized CommandFactory integration. This method addresses Issue
   * #124 by reducing redundancy and improving integration.
   *
   * @param inputType the expected input data type
   * @param outputType the expected output data type as enum
   * @param config factory configuration for optimization
   * @return optimized controller with enhanced integration
   */
  public static IStreamController createOptimized(
      String inputType, OutputType outputType, FactoryConfiguration config) {
    Objects.requireNonNull(outputType, "Output type cannot be null");
    Objects.requireNonNull(config, "Factory configuration cannot be null");

    String key = createRegistryKey(inputType, outputType.getValue());

    // Check cache first
    IStreamController cached = CONTROLLER_REGISTRY.get(key);
    if (cached != null && config.isCachingEnabled()) {
      LOG.debug("Retrieved cached optimized controller for {} → {}", inputType, outputType);
      return cached;
    }

    // Create using optimized command factory
    ControllerBuilder builder = BUILDER_REGISTRY.get(inputType);
    if (builder instanceof CommandFactoryAwareBuilder) {
      CommandFactoryAwareBuilder enhancedBuilder = (CommandFactoryAwareBuilder) builder;

      // Use enhanced command factory for better performance
      IStreamController controller =
          enhancedBuilder.createWithCommandFactory(
              outputType.getValue(), config.isDetailedLoggingEnabled());

      if (controller != null) {
        LOG.info(
            "Created optimized controller for {} → {} with enhanced CommandFactory integration",
            inputType,
            outputType);

        if (config.isCachingEnabled()) {
          CONTROLLER_REGISTRY.put(key, controller);
        }
        return controller;
      }
    }

    // Fallback to standard creation
    return getController(inputType, outputType);
  }

  /**
   * Gets the enhanced command factory instance used by this controller factory. This provides
   * access to the optimized command creation infrastructure.
   *
   * @return enhanced command factory instance
   */
  public static EnhancedCommandFactory getCommandFactory() {
    return COMMAND_FACTORY;
  }

  /**
   * Gets all registered controller types.
   *
   * @return array of registered input → output type combinations
   */
  public static String[] getRegisteredTypes() {
    return CONTROLLER_REGISTRY.keySet().toArray(new String[0]);
  }

  /**
   * Clears all registered controllers and builders.
   *
   * <p>This method is primarily for testing purposes.
   */
  public static void clearRegistry() {
    CONTROLLER_REGISTRY.clear();
    BUILDER_REGISTRY.clear();
    LOG.info("Controller registry cleared");
  }

  /** Creates a registry key from input and output types. */
  private static String createRegistryKey(String inputType, String outputType) {
    return inputType + " → " + outputType;
  }

  /** Interface for controller builders. */
  public interface ControllerBuilder {
    /**
     * Creates a controller for the specified output type.
     *
     * @param outputType the desired output type
     * @return appropriate controller, or null if not supported
     */
    IStreamController createForOutputType(String outputType);
  }

  /** Interface for builders that can integrate with CommandFactory. */
  public interface CommandFactoryAwareBuilder extends ControllerBuilder {
    /**
     * Creates a controller with CommandFactory integration.
     *
     * @param outputType the desired output type
     * @param enableDetailedLogging whether to enable detailed logging
     * @return appropriate controller with CommandFactory integration, or null if not supported
     */
    IStreamController createWithCommandFactory(String outputType, boolean enableDetailedLogging);
  }

  /** Builder for CSV controllers. */
  public static class CsvControllerBuilder implements CommandFactoryAwareBuilder {

    @Override
    public IStreamController createForOutputType(String outputType) {
      OutputType type = OutputType.fromString(outputType);
      if (type != null) {
        return createForOutputType(type);
      }

      // Fallback for unknown string types
      LOG.warn("Unknown output type for CSV controller: {}", outputType);
      return null;
    }

    /**
     * Creates a controller for the specified OutputType enum.
     *
     * @param outputType the desired output type as enum
     * @return appropriate controller, or null if not supported
     */
    public IStreamController createForOutputType(OutputType outputType) {
      switch (outputType) {
        case CSV_COLUMN:
          return CsvProcessingController.forColumnExtraction("default");
        case PROCESSED_DATA:
          return CsvProcessingController.forComplexProcessing("default", "default-processor");
        case CSV:
          return CsvProcessingController.forPassThrough();
        default:
          LOG.warn("Unsupported output type for CSV controller: {}", outputType);
          return null;
      }
    }

    /**
     * Creates a controller for column extraction.
     *
     * @param columnSelector column name or index to extract
     * @return configured controller
     */
    public CsvProcessingController forColumnExtraction(String columnSelector) {
      return CsvProcessingController.forColumnExtraction(columnSelector);
    }

    /**
     * Creates a controller for complex processing.
     *
     * @param columnSelector column name or index
     * @param processorId processor ID
     * @return configured controller
     */
    public CsvProcessingController forComplexProcessing(String columnSelector, String processorId) {
      return CsvProcessingController.forComplexProcessing(columnSelector, processorId);
    }

    /**
     * Creates a controller for pass-through processing.
     *
     * @return configured controller
     */
    public CsvProcessingController forPassThrough() {
      return CsvProcessingController.forPassThrough();
    }

    @Override
    public IStreamController createWithCommandFactory(
        String outputType, boolean enableDetailedLogging) {
      // Create controller using standard method, as AbstractStreamController
      // already integrates with CommandFactory for command creation
      IStreamController controller = createForOutputType(outputType);

      if (controller != null) {
        LOG.debug(
            "Created CSV controller with CommandFactory integration for output type: {}, detailed logging: {}",
            outputType,
            enableDetailedLogging);
      }

      return controller;
    }
  }

  /** Builder for JSON controllers. */
  public static class JsonControllerBuilder implements CommandFactoryAwareBuilder {

    @Override
    public IStreamController createForOutputType(String outputType) {
      OutputType type = OutputType.fromString(outputType);
      if (type != null) {
        return createForOutputType(type);
      }

      // Fallback for unknown string types
      LOG.warn("Unknown output type for JSON controller: {}", outputType);
      return null;
    }

    /**
     * Creates a controller for the specified OutputType enum.
     *
     * @param outputType the desired output type as enum
     * @return appropriate controller, or null if not supported
     */
    public IStreamController createForOutputType(OutputType outputType) {
      switch (outputType) {
        case JSON_PROPERTY:
          return JsonProcessingController.forPropertyExtraction("default", false);
        case VALIDATED_JSON_PROPERTY:
          return JsonProcessingController.forPropertyExtraction("default", true);
        case JSON_FORMATTED:
          return JsonProcessingController.forFormatting();
        case TRANSFORMED_DATA:
          return JsonProcessingController.forTransformation("default", "default-processor");
        default:
          LOG.warn("Unsupported output type for JSON controller: {}", outputType);
          return null;
      }
    }

    /**
     * Creates a controller for property extraction.
     *
     * @param propertyPath JSON property path
     * @param enableValidation whether to enable validation
     * @return configured controller
     */
    public JsonProcessingController forPropertyExtraction(
        String propertyPath, boolean enableValidation) {
      return JsonProcessingController.forPropertyExtraction(propertyPath, enableValidation);
    }

    /**
     * Creates a controller for JSON formatting.
     *
     * @return configured controller
     */
    public JsonProcessingController forFormatting() {
      return JsonProcessingController.forFormatting();
    }

    /**
     * Creates a controller for multi-stage transformation.
     *
     * @param propertyPath JSON property path
     * @param processingStages processing stage IDs
     * @return configured controller
     */
    public JsonProcessingController forTransformation(
        String propertyPath, String... processingStages) {
      return JsonProcessingController.forTransformation(propertyPath, processingStages);
    }

    @Override
    public IStreamController createWithCommandFactory(
        String outputType, boolean enableDetailedLogging) {
      // Create controller using standard method, as AbstractStreamController
      // already integrates with CommandFactory for command creation
      IStreamController controller = createForOutputType(outputType);

      if (controller != null) {
        LOG.debug(
            "Created JSON controller with CommandFactory integration for output type: {}, detailed logging: {}",
            outputType,
            enableDetailedLogging);
      }

      return controller;
    }
  }
}
