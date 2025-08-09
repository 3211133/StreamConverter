package com.streamConverter.controller;

import com.streamConverter.command.CommandConfig;
import com.streamConverter.command.impl.CsvNavigateCommand;
import com.streamConverter.command.impl.SampleStreamCommand;

/**
 * Controller for CSV data processing operations.
 *
 * <p>This controller demonstrates how to configure command pipelines for CSV processing. It
 * provides several processing modes:
 *
 * <ul>
 *   <li>Column extraction - Extract specific columns by name or index
 *   <li>Row filtering - Filter rows based on criteria
 *   <li>Data transformation - Transform CSV data with additional processing
 *   <li>Validation - Validate CSV structure and content
 * </ul>
 *
 * <p>Usage examples:
 *
 * <pre>
 * // Extract specific column
 * CsvProcessingController controller = CsvProcessingController.forColumnExtraction("name");
 * controller.process(inputStream, outputStream);
 *
 * // Complex processing pipeline
 * CsvProcessingController controller = CsvProcessingController.forComplexProcessing("data", "processor-id");
 * controller.process(inputStream, outputStream);
 * </pre>
 *
 * @author StreamConverter Team
 * @version 1.0
 * @since 1.0
 */
public class CsvProcessingController extends AbstractStreamController {

  /** Processing mode enumeration */
  public enum ProcessingMode {
    /** Extract specific column data from CSV */
    COLUMN_EXTRACTION,
    /** Perform complex multi-step processing */
    COMPLEX_PROCESSING,
    /** Validate CSV format only */
    VALIDATION_ONLY,
    /** Pass through CSV data unchanged */
    PASS_THROUGH
  }

  /** The processing mode for this controller */
  private final ProcessingMode mode;

  /** Column name or index for extraction */
  private final String columnSelector;

  /** Additional processor ID for complex processing */
  private final String processorId;

  /**
   * Creates a controller for column extraction.
   *
   * @param columnSelector the column name or index to extract
   */
  private CsvProcessingController(String columnSelector) {
    this.mode = ProcessingMode.COLUMN_EXTRACTION;
    this.columnSelector = columnSelector;
    this.processorId = null;
  }

  /**
   * Creates a controller for complex processing.
   *
   * @param columnSelector the column name or index to extract
   * @param processorId the processor ID for additional processing
   */
  private CsvProcessingController(String columnSelector, String processorId) {
    this.mode = ProcessingMode.COMPLEX_PROCESSING;
    this.columnSelector = columnSelector;
    this.processorId = processorId;
  }

  /** Creates a controller for pass-through processing. */
  private CsvProcessingController() {
    this.mode = ProcessingMode.PASS_THROUGH;
    this.columnSelector = null;
    this.processorId = null;
  }

  /**
   * Factory method for creating a column extraction controller.
   *
   * @param columnSelector column name or index to extract
   * @return configured controller
   */
  public static CsvProcessingController forColumnExtraction(String columnSelector) {
    if (columnSelector == null || columnSelector.trim().isEmpty()) {
      throw new IllegalArgumentException("Column selector cannot be null or empty");
    }
    return new CsvProcessingController(columnSelector.trim());
  }

  /**
   * Factory method for creating a complex processing controller.
   *
   * @param columnSelector column name or index to extract
   * @param processorId processor ID for additional processing
   * @return configured controller
   */
  public static CsvProcessingController forComplexProcessing(
      String columnSelector, String processorId) {
    if (columnSelector == null || columnSelector.trim().isEmpty()) {
      throw new IllegalArgumentException("Column selector cannot be null or empty");
    }
    if (processorId == null || processorId.trim().isEmpty()) {
      throw new IllegalArgumentException("Processor ID cannot be null or empty");
    }
    return new CsvProcessingController(columnSelector.trim(), processorId.trim());
  }

  /**
   * Factory method for creating a pass-through controller.
   *
   * @return configured controller that processes CSV without extraction
   */
  public static CsvProcessingController forPassThrough() {
    return new CsvProcessingController();
  }

  /**
   * Configures the command pipeline based on the processing mode.
   *
   * @return array of command configurations
   */
  @Override
  protected CommandConfig[] configureCommands() {
    switch (mode) {
      case COLUMN_EXTRACTION:
        return new CommandConfig[] {
          new CommandConfig(
              CsvNavigateCommand.class, "Extract column: " + columnSelector, columnSelector)
        };

      case COMPLEX_PROCESSING:
        return new CommandConfig[] {
          new CommandConfig(
              CsvNavigateCommand.class, "Extract column: " + columnSelector, columnSelector),
          new CommandConfig(SampleStreamCommand.class, "Process with: " + processorId, processorId)
        };

      case VALIDATION_ONLY:
        return new CommandConfig[] {
          new CommandConfig(CsvNavigateCommand.class, "Validate CSV structure")
        };

      case PASS_THROUGH:
        return new CommandConfig[] {
          new CommandConfig(CsvNavigateCommand.class, "Process entire CSV")
        };

      default:
        throw new IllegalStateException("Unknown processing mode: " + mode);
    }
  }

  @Override
  public String getInputDataType() {
    return "CSV";
  }

  @Override
  public String getOutputDataType() {
    switch (mode) {
      case COLUMN_EXTRACTION:
        return "CSV_COLUMN";
      case COMPLEX_PROCESSING:
        return "PROCESSED_DATA";
      case VALIDATION_ONLY:
        return "VALIDATION_RESULT";
      case PASS_THROUGH:
        return "CSV";
      default:
        return "UNKNOWN";
    }
  }

  /**
   * Gets the processing mode of this controller.
   *
   * @return the processing mode
   */
  public ProcessingMode getProcessingMode() {
    return mode;
  }

  /**
   * Gets the column selector if applicable.
   *
   * @return the column selector, or null if not applicable
   */
  public String getColumnSelector() {
    return columnSelector;
  }

  /**
   * Gets the processor ID if applicable.
   *
   * @return the processor ID, or null if not applicable
   */
  public String getProcessorId() {
    return processorId;
  }

  @Override
  public String getConfigurationDescription() {
    StringBuilder desc = new StringBuilder(super.getConfigurationDescription());
    desc.append(" - Mode: ").append(mode);

    if (columnSelector != null) {
      desc.append(", Column: ").append(columnSelector);
    }

    if (processorId != null) {
      desc.append(", Processor: ").append(processorId);
    }

    return desc.toString();
  }
}
