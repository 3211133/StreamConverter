package com.streamconverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for stream commands.
 *
 * <p>This class provides a base for implementing {@link IStreamCommand} with a pre-configured
 * logger. Subclasses implement {@link #execute(InputStream, OutputStream)} directly.
 */
public abstract class AbstractStreamCommand implements IStreamCommand {
  /** Logger instance for this command. Uses the actual subclass name for better traceability. */
  protected final Logger log;

  /**
   * Default constructor.
   *
   * <p>Initializes the command with default settings and creates a logger using the actual command
   * class name.
   */
  public AbstractStreamCommand() {
    super();
    this.log = LoggerFactory.getLogger(getClass());
  }

  /**
   * Executes the command on the provided input stream and writes the result to the output stream.
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  @Override
  public abstract void execute(InputStream inputStream, OutputStream outputStream)
      throws IOException;
}
