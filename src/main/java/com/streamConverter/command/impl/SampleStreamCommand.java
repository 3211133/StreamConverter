package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * SampleStreamCommand is a concrete implementation of AbstractStreamCommand.
 *
 * <p>This command reads data from an input stream and writes it to an output stream.
 */
public class SampleStreamCommand extends AbstractStreamCommand {

  private String id;

  /** Default constructor that initializes the command with a default ID. */
  public SampleStreamCommand() {
    this.id = "default";
  }

  /**
   * Constructor that initializes the command with a specific ID.
   *
   * @param arg The ID to be assigned to this command.
   */
  public SampleStreamCommand(String arg) {
    this.id = arg;
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
    inputStream.transferTo(outputStream);
  }

  @Override
  public String toString() {
    return "SampleStreamCommand [id=" + id + "]";
  }
}
