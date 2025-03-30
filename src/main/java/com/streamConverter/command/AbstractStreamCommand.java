package com.streamConverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStreamCommand implements IStreamCommand {
  private static final Logger log = LoggerFactory.getLogger(AbstractStreamCommand.class);

  public AbstractStreamCommand() {
    super();
  }

  // TODO execute以外の共通処理を実装する
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    log.debug("execute start");
    _execute(inputStream, outputStream);
    log.debug("execute end");
  }

  protected abstract void _execute(InputStream inputStream, OutputStream outputStream)
      throws IOException;
}
