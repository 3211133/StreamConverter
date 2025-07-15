package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * JSON変換コマンドクラス
 *
 * <p>このクラスは、JSON形式のデータを変換するためのコマンドを実装します。 ストリームを使用して、JSONデータを読み込み、変換後のデータを出力します。
 * 変換対象のXPathである箇所を特定したあとに、変換処理を実行することを想定しています。
 */
public class JsonNavigateCommand extends AbstractStreamCommand {

  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
         Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      
      StringBuilder jsonBuilder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        jsonBuilder.append(line);
      }
      
      // Basic JSON processing - pass through for now
      // TODO: Implement JSON parsing and navigation logic based on JSONPath selectors
      String jsonContent = jsonBuilder.toString();
      writer.write(jsonContent);
      writer.flush();
    }
  }
}
