package com.streamConverter.command;

import com.streamConverter.context.ExecutionContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Unified interface for stream commands with optional execution context support.
 *
 * <p>This interface defines methods for executing commands on input streams and writing results to
 * output streams. Commands can optionally utilize ExecutionContext for enhanced logging,
 * traceability, and multi-threaded context propagation.
 *
 * <p>Implementation options:
 *
 * <ul>
 *   <li>Basic commands: Implement only the 2-parameter execute method
 *   <li>Context-aware commands: Override the 3-parameter execute method for enhanced functionality
 * </ul>
 */
public interface IStreamCommand {

  /**
   * Executes the command with ExecutionContext support for enhanced traceability.
   *
   * <p>This is the primary execution method that supports MDC context propagation, execution
   * tracking, and enhanced logging capabilities.
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @param context The execution context for traceability and logging enhancement.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  default void execute(InputStream inputStream, OutputStream outputStream, ExecutionContext context)
      throws IOException {
    // Default implementation delegates to the basic execute method for backward compatibility
    execute(inputStream, outputStream);
  }

  /**
   * Executes the command on the provided input stream and writes the result to the output stream.
   *
   * <p>This method maintains backward compatibility for existing implementations. When called
   * without ExecutionContext, a new context will be automatically created by the StreamConverter
   * pipeline.
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  void execute(InputStream inputStream, OutputStream outputStream) throws IOException;
}
