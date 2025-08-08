package com.streamConverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import org.apache.commons.io.input.TeeInputStream;

/**
 * Abstract class for commands that consume an input stream and produce an output stream.
 *
 * <p>This class provides a template method pattern for executing the command, ensuring that the
 * input and output streams are not null. It also provides a method to consume the input stream,
 * which must be implemented by subclasses.
 */
public abstract class ConsumerCommand extends AbstractStreamCommand {

  /**
   * Default constructor.
   *
   * <p>Initializes the command with default settings.
   */
  public ConsumerCommand() {
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
  public void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    try (InputStream teeInputStream = new TeeInputStream(inputStream, outputStream); ) {
      this.consume(teeInputStream);
    } catch (IOException e) {
      throw new IOException("Error while consuming input stream", e);
    }
  }

  /**
   * Abstract method to be implemented by subclasses for consuming the input stream.
   *
   * @param inputStream The input stream to read data from.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  public abstract void consume(InputStream inputStream) throws IOException;
}
