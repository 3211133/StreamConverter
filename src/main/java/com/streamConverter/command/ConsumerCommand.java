package com.streamConverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import org.apache.commons.io.input.TeeInputStream;

public abstract class ConsumerCommand extends AbstractStreamCommand {

  public ConsumerCommand() {
    super();
  }

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

  public abstract void consume(InputStream inputStream) throws IOException;
}
