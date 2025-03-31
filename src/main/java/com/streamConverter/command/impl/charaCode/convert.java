package com.streamConverter.command.impl.charaCode;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Objects;

public class convert extends AbstractStreamCommand {
  private String from;
  private String to;

  public convert(String from, String to) {
    Objects.requireNonNull(from);
    Objects.requireNonNull(to);
    if (Charset.isSupported(from) == false) {
      throw new IllegalArgumentException("変換元文字コードがサポートされていません");
    }
    if (Charset.isSupported(to) == false) {
      throw new IllegalArgumentException("変換先文字コードがサポートされていません");
    }
    this.from = from;
    this.to = to;
  }

  @Override
  public void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    // 文字コードを変換する
    try (InputStreamReader reader = new InputStreamReader(inputStream, this.from);
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, this.to); ) {
      reader.transferTo(writer);
    }
  }
}
