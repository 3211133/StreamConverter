package com.streamConverter.controller;

import com.streamConverter.CommandResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Controller interface for stream processing operations.
 *
 * <p>This interface defines the contract for controllers that manage the complete lifecycle of
 * stream processing operations. Controllers are responsible for:
 *
 * <ul>
 *   <li>Configuring and creating appropriate command pipelines based on use cases
 *   <li>Managing external I/O connections and stream lifecycle
 *   <li>Orchestrating StreamConverter execution
 *   <li>Handling errors and providing feedback
 * </ul>
 *
 * <p>The Controller layer sits between external systems and the StreamConverter core, implementing
 * the ideal architecture:
 *
 * <pre>
 * External Systems → Controller → StreamConverter → Commands
 * </pre>
 *
 * <p>This separation ensures that:
 *
 * <ul>
 *   <li>External systems don't need to know about StreamConverter internals
 *   <li>Complex command configuration logic is centralized in controllers
 *   <li>StreamConverter focuses purely on command orchestration
 *   <li>Commands remain focused on stream transformation logic
 * </ul>
 *
 * @author StreamConverter Team
 * @version 1.0
 * @since 1.0
 */
public interface IStreamController {

  /**
   * Processes data from the input stream and writes results to the output stream.
   *
   * <p>This method represents the main entry point for stream processing. The controller is
   * responsible for:
   *
   * <ul>
   *   <li>Analyzing the processing requirements
   *   <li>Configuring appropriate command pipelines
   *   <li>Managing StreamConverter lifecycle
   *   <li>Handling any processing errors
   * </ul>
   *
   * @param inputStream the input stream to read data from
   * @param outputStream the output stream to write results to
   * @return list of command execution results for monitoring and debugging
   * @throws IOException if an I/O error occurs during processing
   * @throws IllegalArgumentException if input parameters are invalid
   * @throws IllegalStateException if the controller is not properly configured
   */
  List<CommandResult> process(InputStream inputStream, OutputStream outputStream)
      throws IOException;

  /**
   * Validates that the controller is properly configured and ready for processing.
   *
   * <p>This method should be called before attempting to process streams to ensure all required
   * configuration is in place.
   *
   * @return true if the controller is ready for processing, false otherwise
   */
  boolean isConfigured();

  /**
   * Gets a human-readable description of the controller's configuration.
   *
   * <p>This method provides information about the controller's current state, configured commands,
   * and processing capabilities. Useful for debugging and monitoring.
   *
   * @return description of the controller configuration
   */
  String getConfigurationDescription();

  /**
   * Gets the expected input data type that this controller can handle.
   *
   * <p>This information can be used by external systems to route data to appropriate controllers.
   *
   * @return the expected input data type (e.g., "CSV", "JSON", "XML", "BINARY")
   */
  String getInputDataType();

  /**
   * Gets the output data type that this controller produces.
   *
   * <p>This information helps external systems understand what type of data to expect from the
   * processing.
   *
   * @return the output data type (e.g., "CSV", "JSON", "XML", "BINARY")
   */
  String getOutputDataType();
}
