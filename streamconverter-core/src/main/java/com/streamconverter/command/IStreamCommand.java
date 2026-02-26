package com.streamconverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Unified interface for stream commands.
 *
 * <p>This interface defines methods for executing commands on input streams and writing results to
 * output streams.
 *
 * <p>This is a functional interface and can be implemented using lambda expressions or method
 * references for simple stream processing operations.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Simple copy operation (IOException is propagated from execute method signature)
 * IStreamCommand copyCommand = (in, out) -> in.transferTo(out);
 *
 * // Buffered stream processing with custom logic
 * IStreamCommand bufferCommand = (in, out) -> {
 *     byte[] buffer = new byte[8192];
 *     int len;
 *     while ((len = in.read(buffer)) != -1) {
 *         out.write(buffer, 0, len);
 *     }
 * };
 *
 * // Using in StreamConverter pipeline
 * StreamConverter converter = StreamConverter.create(copyCommand);
 * converter.run(inputStream, outputStream);
 * }</pre>
 */
@FunctionalInterface
public interface IStreamCommand {

  /**
   * Executes the command on the provided input stream and writes the result to the output stream.
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  void execute(InputStream inputStream, OutputStream outputStream) throws IOException;
}
