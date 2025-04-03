package com.streamConverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for stream commands.
 *
 * <p>This interface defines a method for executing a command on an input stream and writing the
 * result to an output stream.
 */
public interface IStreamCommand {

  /**
   * Executes the command on the provided input stream and writes the result to the output stream.
   *
   * @param inputStream The input stream to read data from.
   * @param outputStream The output stream to write data to.
   * @throws IOException If an I/O error occurs during the execution of the command.
   */
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException;
}
