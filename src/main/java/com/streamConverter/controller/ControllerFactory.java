package com.streamConverter.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating and managing stream controllers.
 *
 * <p>This factory provides centralized controller creation and management, supporting:
 *
 * <ul>
 *   <li>Controller creation based on data types and processing requirements
 *   <li>Controller registry for reuse and management
 *   <li>Automatic controller selection based on input/output types
 *   <li>Configuration validation and optimization
 * </ul>
 *
 * <p>The factory pattern further enhances the Controller architecture by providing a single entry
 * point for external systems to obtain appropriate controllers without needing to know the specific
 * implementation details.
 *
 * <p>Usage examples:
 *
 * <pre>
 * // Get controller by data types
 * IStreamController controller = ControllerFactory.getController("CSV", "JSON_PROPERTY");
 *
 * // Get controller with specific configuration
 * IStreamController controller = ControllerFactory.getCsvController()
 *     .forColumnExtraction("name");
 *
 * // Register custom controller
 * ControllerFactory.registerController("CUSTOM_CSV", customController);
 * </pre>
 *
 * @author StreamConverter Team
 * @version 1.0
 * @since 1.0
 */
public class ControllerFactory {

  private static final Logger log = LoggerFactory.getLogger(ControllerFactory.class);

  /** Registry of controllers by type combination */
  private static final Map<String, IStreamController> controllerRegistry = new HashMap<>();

  /** Registry of controller builders by input type */
  private static final Map<String, ControllerBuilder> builderRegistry = new HashMap<>();

  static {
    // Initialize default builders
    builderRegistry.put("CSV", new CsvControllerBuilder());
    builderRegistry.put("JSON", new JsonControllerBuilder());
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
    IStreamController cachedController = controllerRegistry.get(key);
    if (cachedController != null) {
      log.debug("Found cached controller for {} → {}", inputType, outputType);
      return cachedController;
    }

    // Try to create using builders
    ControllerBuilder builder = builderRegistry.get(inputType);
    if (builder != null) {
      IStreamController controller = builder.createForOutputType(outputType);
      if (controller != null) {
        log.info("Created controller for {} → {} using builder", inputType, outputType);
        // Cache for future use
        controllerRegistry.put(key, controller);
        return controller;
      }
    }

    log.warn("No controller available for {} → {}", inputType, outputType);
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
    controllerRegistry.put(key, controller);

    log.info(
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

    builderRegistry.put(inputType, builder);

    log.info(
        "Registered custom builder for input type {}: {}",
        inputType,
        builder.getClass().getSimpleName());
  }

  /**
   * Gets all registered controller types.
   *
   * @return array of registered input → output type combinations
   */
  public static String[] getRegisteredTypes() {
    return controllerRegistry.keySet().toArray(new String[0]);
  }

  /**
   * Clears all registered controllers and builders.
   *
   * <p>This method is primarily for testing purposes.
   */
  public static void clearRegistry() {
    controllerRegistry.clear();
    builderRegistry.clear();
    log.info("Controller registry cleared");
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

  /** Builder for CSV controllers. */
  public static class CsvControllerBuilder implements ControllerBuilder {

    @Override
    public IStreamController createForOutputType(String outputType) {
      switch (outputType) {
        case "CSV_COLUMN":
          return CsvProcessingController.forColumnExtraction("default");
        case "PROCESSED_DATA":
          return CsvProcessingController.forComplexProcessing("default", "default-processor");
        case "CSV":
          return CsvProcessingController.forPassThrough();
        default:
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
  }

  /** Builder for JSON controllers. */
  public static class JsonControllerBuilder implements ControllerBuilder {

    @Override
    public IStreamController createForOutputType(String outputType) {
      switch (outputType) {
        case "JSON_PROPERTY":
          return JsonProcessingController.forPropertyExtraction("default", false);
        case "VALIDATED_JSON_PROPERTY":
          return JsonProcessingController.forPropertyExtraction("default", true);
        case "JSON_FORMATTED":
          return JsonProcessingController.forFormatting();
        case "TRANSFORMED_DATA":
          return JsonProcessingController.forTransformation("default", "default-processor");
        default:
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
  }
}
