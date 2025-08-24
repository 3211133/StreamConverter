package com.streamConverter.controller;

import com.streamConverter.command.CommandConfig;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.impl.json.JsonValidateCommand;

/**
 * Controller for JSON data processing operations.
 *
 * <p>This controller demonstrates advanced command pipeline configuration for JSON processing. It
 * supports various JSON processing scenarios:
 *
 * <ul>
 *   <li>Property extraction using JSONPath-like expressions
 *   <li>JSON formatting and pretty-printing
 *   <li>Data validation with custom rules
 *   <li>Transformation pipelines with multiple stages
 * </ul>
 *
 * <p>Usage examples:
 *
 * <pre>
 * // Extract property with validation
 * JsonProcessingController controller = JsonProcessingController.forPropertyExtraction("user.name", true);
 * controller.process(inputStream, outputStream);
 *
 * // Multi-stage transformation
 * JsonProcessingController controller = JsonProcessingController.forTransformation(
 *     "data.items", "item-processor", "final-formatter"
 * );
 * controller.process(inputStream, outputStream);
 * </pre>
 *
 * @author StreamConverter Team
 * @version 1.0
 * @since 1.0
 */
public class JsonProcessingController extends AbstractStreamController {

  /** Default JSON schema path for validation */
  private static final String DEFAULT_SCHEMA_PATH = "schemas/default.json";

  /** Processing scenarios for JSON */
  public enum ProcessingScenario {
    /** Extract specific JSON property values */
    PROPERTY_EXTRACTION,
    /** Format JSON for readability only */
    FORMAT_ONLY,
    /** Validate JSON and extract properties */
    VALIDATION_WITH_EXTRACTION,
    /** Apply multiple transformation stages */
    MULTI_STAGE_TRANSFORMATION
  }

  /** The processing scenario */
  private final ProcessingScenario scenario;

  /** JSONPath-like expression for property extraction */
  private final String propertyPath;

  /** Whether to enable validation */
  private final boolean enableValidation;

  /** Processing stages for multi-stage transformation */
  private final String[] processingStages;

  /** Private constructor for property extraction. */
  private JsonProcessingController(String propertyPath, boolean enableValidation) {
    this.scenario =
        enableValidation
            ? ProcessingScenario.VALIDATION_WITH_EXTRACTION
            : ProcessingScenario.PROPERTY_EXTRACTION;
    this.propertyPath = propertyPath;
    this.enableValidation = enableValidation;
    this.processingStages = null;
  }

  /** Private constructor for format-only. */
  private JsonProcessingController() {
    this.scenario = ProcessingScenario.FORMAT_ONLY;
    this.propertyPath = null;
    this.enableValidation = false;
    this.processingStages = null;
  }

  /** Private constructor for multi-stage transformation. */
  private JsonProcessingController(String propertyPath, String... processingStages) {
    this.scenario = ProcessingScenario.MULTI_STAGE_TRANSFORMATION;
    this.propertyPath = propertyPath;
    this.enableValidation = false;
    this.processingStages = processingStages.clone();
  }

  /**
   * Factory method for property extraction.
   *
   * @param propertyPath JSONPath-like expression (e.g., "user.name", "data[0].id")
   * @param enableValidation whether to enable validation
   * @return configured controller
   */
  public static JsonProcessingController forPropertyExtraction(
      String propertyPath, boolean enableValidation) {
    if (propertyPath == null || propertyPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Property path cannot be null or empty");
    }
    return new JsonProcessingController(propertyPath.trim(), enableValidation);
  }

  /**
   * Factory method for JSON formatting only.
   *
   * @return configured controller that formats JSON without extraction
   */
  public static JsonProcessingController forFormatting() {
    return new JsonProcessingController();
  }

  /**
   * Factory method for multi-stage transformation.
   *
   * @param propertyPath JSONPath-like expression for initial extraction
   * @param processingStages array of processor IDs for transformation stages
   * @return configured controller
   */
  public static JsonProcessingController forTransformation(
      String propertyPath, String... processingStages) {
    if (propertyPath == null || propertyPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Property path cannot be null or empty");
    }
    if (processingStages == null || processingStages.length == 0) {
      throw new IllegalArgumentException("At least one processing stage must be specified");
    }

    // Validate processing stages
    for (int i = 0; i < processingStages.length; i++) {
      if (processingStages[i] == null || processingStages[i].trim().isEmpty()) {
        throw new IllegalArgumentException("Processing stage " + i + " cannot be null or empty");
      }
      processingStages[i] = processingStages[i].trim();
    }

    return new JsonProcessingController(propertyPath.trim(), processingStages);
  }

  @Override
  protected CommandConfig[] configureCommands() {
    switch (scenario) {
      case PROPERTY_EXTRACTION:
        return new CommandConfig[] {
          new CommandConfig(
              JsonNavigateCommand.class, "Extract JSON property: " + propertyPath, propertyPath)
        };

      case FORMAT_ONLY:
        return new CommandConfig[] {new CommandConfig(JsonNavigateCommand.class, "Format JSON")};

      case VALIDATION_WITH_EXTRACTION:
        return new CommandConfig[] {
          new CommandConfig(
              JsonValidateCommand.class, "Validate JSON structure", DEFAULT_SCHEMA_PATH),
          new CommandConfig(
              JsonNavigateCommand.class, "Extract JSON property: " + propertyPath, propertyPath)
        };

      case MULTI_STAGE_TRANSFORMATION:
        // Build dynamic pipeline based on processing stages
        CommandConfig[] configs = new CommandConfig[1 + processingStages.length];

        // First stage: JSON extraction
        configs[0] =
            new CommandConfig(
                JsonNavigateCommand.class, "Extract JSON property: " + propertyPath, propertyPath);

        // Additional processing stages
        for (int i = 0; i < processingStages.length; i++) {
          configs[i + 1] =
              new CommandConfig(
                  SampleStreamCommand.class,
                  "Processing stage " + (i + 1) + ": " + processingStages[i],
                  processingStages[i]);
        }

        return configs;

      default:
        throw new IllegalStateException("Unknown processing scenario: " + scenario);
    }
  }

  @Override
  public String getInputDataType() {
    return "JSON";
  }

  @Override
  public String getOutputDataType() {
    switch (scenario) {
      case PROPERTY_EXTRACTION:
        return "JSON_PROPERTY";
      case FORMAT_ONLY:
        return "JSON_FORMATTED";
      case VALIDATION_WITH_EXTRACTION:
        return "VALIDATED_JSON_PROPERTY";
      case MULTI_STAGE_TRANSFORMATION:
        return "TRANSFORMED_DATA";
      default:
        return "UNKNOWN";
    }
  }

  /**
   * Gets the processing scenario.
   *
   * @return the processing scenario
   */
  public ProcessingScenario getProcessingScenario() {
    return scenario;
  }

  /**
   * Gets the property path for extraction.
   *
   * @return the property path, or null if not applicable
   */
  public String getPropertyPath() {
    return propertyPath;
  }

  /**
   * Checks if validation is enabled.
   *
   * @return true if validation is enabled
   */
  public boolean isValidationEnabled() {
    return enableValidation;
  }

  /**
   * Gets the processing stages for multi-stage transformation.
   *
   * @return array of processing stage IDs, or null if not applicable
   */
  public String[] getProcessingStages() {
    return processingStages == null ? null : processingStages.clone();
  }

  @Override
  public String getConfigurationDescription() {
    StringBuilder desc = new StringBuilder(super.getConfigurationDescription());
    desc.append(" - Scenario: ").append(scenario);

    if (propertyPath != null) {
      desc.append(", Property: ").append(propertyPath);
    }

    if (enableValidation) {
      desc.append(", Validation: enabled");
    }

    if (processingStages != null) {
      desc.append(", Stages: ").append(processingStages.length);
    }

    return desc.toString();
  }

  @Override
  protected void validateConfiguration() {
    super.validateConfiguration();

    // Custom validation for JSON processing
    if (scenario == ProcessingScenario.VALIDATION_WITH_EXTRACTION && propertyPath == null) {
      throw new IllegalStateException("Property path is required for validation with extraction");
    }

    if (scenario == ProcessingScenario.MULTI_STAGE_TRANSFORMATION) {
      if (propertyPath == null || processingStages == null || processingStages.length == 0) {
        throw new IllegalStateException(
            "Multi-stage transformation requires property path and processing stages");
      }
    }
  }
}
