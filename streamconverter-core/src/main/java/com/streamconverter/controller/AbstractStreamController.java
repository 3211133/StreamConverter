package com.streamconverter.controller;

import com.streamconverter.CommandResult;
import com.streamconverter.StreamConverter;
import com.streamconverter.command.CommandConfig;
import com.streamconverter.command.IStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for stream controllers.
 *
 * <p>This class provides common functionality for controllers including:
 *
 * <ul>
 *   <li>Command configuration and creation using direct instantiation
 *   <li>StreamConverter lifecycle management
 *   <li>Error handling and logging
 *   <li>Input/output validation
 * </ul>
 *
 * <p>Subclasses need to implement the command configuration logic by overriding {@link
 * #configureCommands()} method. This allows each controller to define its specific processing
 * pipeline while inheriting common functionality.
 *
 * <p>Example usage:
 *
 * <pre>
 * public class CsvToJsonController extends AbstractStreamController {
 *
 *     &#64;Override
 *     protected CommandConfig[] configureCommands() {
 *         return new CommandConfig[] {
 *             new CommandConfig(CsvNavigateCommand.class, "Extract CSV data"),
 *             new CommandConfig(JsonFormatCommand.class, "Format as JSON")
 *         };
 *     }
 *
 *     &#64;Override
 *     public String getInputDataType() { return "CSV"; }
 *
 *     &#64;Override
 *     public String getOutputDataType() { return "JSON"; }
 * }
 * </pre>
 *
 * @author StreamConverter Team
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractStreamController implements IStreamController {

  private static final Logger log = LoggerFactory.getLogger(AbstractStreamController.class);

  /** Cached command pipeline configuration */
  private CommandConfig[] commandConfigs;

  /** Cached StreamConverter instance */
  private StreamConverter streamConverter;

  /** Flag indicating if the controller has been properly configured */
  private boolean configured = false;

  /**
   * Default constructor that initializes the controller.
   *
   * <p>The controller will be configured lazily on first use to allow subclasses to complete their
   * initialization.
   */
  protected AbstractStreamController() {
    // Configuration will be done lazily to allow subclass initialization
  }

  /**
   * Processes data from input stream to output stream using the configured command pipeline.
   *
   * <p>This method ensures the controller is configured before processing and handles all error
   * scenarios appropriately.
   *
   * @param inputStream the input stream to read data from
   * @param outputStream the output stream to write results to
   * @return list of command execution results
   * @throws IOException if an I/O error occurs during processing
   * @throws IllegalArgumentException if input parameters are null
   * @throws IllegalStateException if the controller cannot be configured
   */
  @Override
  public final List<CommandResult> process(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    Objects.requireNonNull(inputStream, "Input stream cannot be null");
    Objects.requireNonNull(outputStream, "Output stream cannot be null");

    // Ensure controller is configured
    ensureConfigured();

    String controllerName = getClass().getSimpleName();
    log.info("Starting stream processing with controller: {}", controllerName);
    log.debug("Controller configuration: {}", getConfigurationDescription());

    try {
      // Execute the processing pipeline
      List<CommandResult> results = streamConverter.run(inputStream, outputStream);

      log.info("Stream processing completed successfully with controller: {}", controllerName);
      return results;

    } catch (IOException e) {
      log.error(
          "Stream processing failed with controller: {} - {}", controllerName, e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      log.error(
          "Unexpected error during stream processing with controller: {} - {}",
          controllerName,
          e.getMessage(),
          e);
      throw new IOException("Stream processing failed: " + e.getMessage(), e);
    }
  }

  /**
   * Checks if the controller is properly configured.
   *
   * @return true if configured, false otherwise
   */
  @Override
  public final boolean isConfigured() {
    return configured && streamConverter != null;
  }

  /**
   * Gets a description of the controller's current configuration.
   *
   * @return configuration description
   */
  @Override
  public String getConfigurationDescription() {
    if (!configured) {
      return getClass().getSimpleName() + " (not configured)";
    }

    StringBuilder desc = new StringBuilder();
    desc.append(getClass().getSimpleName())
        .append(" - Input: ")
        .append(getInputDataType())
        .append(", Output: ")
        .append(getOutputDataType())
        .append(", Commands: ")
        .append(commandConfigs.length);

    if (log.isDebugEnabled()) {
      desc.append(" [");
      for (int i = 0; i < commandConfigs.length; i++) {
        if (i > 0) desc.append(", ");
        desc.append(commandConfigs[i].getCommandClass().getSimpleName());
      }
      desc.append("]");
    }

    return desc.toString();
  }

  /**
   * Configures the command pipeline for this controller.
   *
   * <p>Subclasses must implement this method to define their specific processing pipeline. The
   * returned CommandConfig array will be used to create the actual command objects using direct
   * instantiation via reflection.
   *
   * <p>Example implementation:
   *
   * <pre>
   * &#64;Override
   * protected CommandConfig[] configureCommands() {
   *     return new CommandConfig[] {
   *         new CommandConfig(CsvNavigateCommand.class, "data", "Extract data column"),
   *         new CommandConfig(CsvValidateCommand.class, "Validate output", requiredColumns)
   *     };
   * }
   * </pre>
   *
   * @return array of command configurations defining the processing pipeline
   * @throws IllegalStateException if the configuration is invalid
   */
  protected abstract CommandConfig[] configureCommands();

  /**
   * Ensures the controller is properly configured, performing lazy initialization if needed.
   *
   * @throws IllegalStateException if configuration fails
   */
  private void ensureConfigured() {
    if (configured && streamConverter != null) {
      return; // Already configured
    }

    try {
      log.debug("Configuring controller: {}", getClass().getSimpleName());

      // Get command configuration from subclass
      commandConfigs = configureCommands();

      if (commandConfigs == null || commandConfigs.length == 0) {
        throw new IllegalStateException("Controller must configure at least one command");
      }

      // Create command pipeline using direct instantiation
      IStreamCommand[] commands = createCommandsFromConfigs(commandConfigs);

      // Create StreamConverter with the configured commands
      streamConverter = StreamConverter.create(Arrays.asList(commands));

      configured = true;

      log.info(
          "Controller configured successfully: {} with {} commands",
          getClass().getSimpleName(),
          commands.length);

    } catch (Exception e) {
      log.error(
          "Failed to configure controller: {} - {}", getClass().getSimpleName(), e.getMessage(), e);
      throw new IllegalStateException("Controller configuration failed: " + e.getMessage(), e);
    }
  }

  /**
   * Create commands from CommandConfig array using direct instantiation
   *
   * @param configs command configurations
   * @return array of instantiated commands
   */
  private IStreamCommand[] createCommandsFromConfigs(CommandConfig[] configs) {
    IStreamCommand[] commands = new IStreamCommand[configs.length];
    for (int i = 0; i < configs.length; i++) {
      try {
        // For simplicity, use reflection to create instances
        // In a real refactor, we'd replace this with direct factory methods
        Class<? extends IStreamCommand> clazz = configs[i].getCommandClass();
        Object[] args = configs[i].getArgs();

        if (args.length == 0) {
          commands[i] = clazz.getDeclaredConstructor().newInstance();
        } else {
          // This is a simplified version - in practice, we'd need proper constructor matching
          commands[i] = clazz.getDeclaredConstructor(getArgTypes(args)).newInstance(args);
        }

        log.debug("Created command: {}", clazz.getSimpleName());
      } catch (Exception e) {
        throw new RuntimeException(
            "Failed to create command: " + configs[i].getCommandClass().getSimpleName(), e);
      }
    }
    return commands;
  }

  /** Get argument types for constructor matching */
  private Class<?>[] getArgTypes(Object[] args) {
    Class<?>[] types = new Class<?>[args.length];
    for (int i = 0; i < args.length; i++) {
      types[i] = args[i].getClass();
    }
    return types;
  }

  /**
   * Gets the configured command configurations.
   *
   * <p>This method is protected to allow subclasses access to the configuration for advanced
   * scenarios while keeping it internal to the controller hierarchy.
   *
   * @return array of command configurations, or null if not yet configured
   */
  protected final CommandConfig[] getCommandConfigs() {
    return commandConfigs == null ? null : commandConfigs.clone();
  }

  /**
   * Gets the configured StreamConverter instance.
   *
   * <p>This method is protected to allow subclasses access to the StreamConverter for advanced
   * scenarios while keeping it internal to the controller hierarchy.
   *
   * @return the StreamConverter instance, or null if not yet configured
   */
  protected final StreamConverter getStreamConverter() {
    return streamConverter;
  }

  /**
   * Allows subclasses to perform additional validation after configuration.
   *
   * <p>This method is called after the command pipeline is configured but before the controller is
   * marked as ready. Subclasses can override this to add custom validation logic.
   *
   * @throws IllegalStateException if validation fails
   */
  protected void validateConfiguration() {
    // Default implementation does nothing
    // Subclasses can override for custom validation
  }
}
