package com.streamConverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for stream commands.
 *
 * <p>This class provides a template for executing commands on input streams and writing the results
 * to output streams.
 */
public abstract class AbstractStreamCommand implements IStreamCommand {
  private static final Logger log = LoggerFactory.getLogger(AbstractStreamCommand.class);

  /**
   * Default constructor.
   *
   * <p>Initializes the command with default settings.
   */
  public AbstractStreamCommand() {
    super();
  }

  /**
   * Executes the command on the provided input stream and writes the result to the output stream.
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    log.debug("execute start");
    _execute(inputStream, outputStream);
    log.debug("execute end");
  }

  /**
   * Abstract method to be implemented by subclasses for executing the command.
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  protected abstract void _execute(InputStream inputStream, OutputStream outputStream)
      throws IOException;
  // TODO execute以外の共通処理を実装する
}
