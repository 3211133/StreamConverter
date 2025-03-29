package com.streamConverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IStreamCommand {

  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException;
}
