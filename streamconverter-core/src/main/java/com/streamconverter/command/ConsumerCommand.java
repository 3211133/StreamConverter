package com.streamconverter.command;

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
   * <p>Subclasses typically expose their own factory methods or constructors.
   */
  protected ConsumerCommand() {
    super();
  }

  /**
   * Executes the command on the provided input stream and writes the result to the output stream.
   *
   * <p><strong>Note on data integrity:</strong> This method uses {@link TeeInputStream} to copy
   * input bytes to {@code outputStream} as they are read by {@link #consume(InputStream)}. If
   * {@code consume()} throws an exception, partial data may already have been written to {@code
   * outputStream}. To prevent this, place a {@link
   * com.streamconverter.command.impl.FileBufferCommand} immediately before this command in the
   * pipeline:
   *
   * <pre>{@code
   * StreamConverter.create(
   *     new MyValidateCommand(),
   *     FileBufferCommand.create(),
   *     new MyConsumerCommand()  // ConsumerCommand subclass
   * );
   * }</pre>
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    try (InputStream teeInputStream = new TeeInputStream(inputStream, outputStream)) {
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
