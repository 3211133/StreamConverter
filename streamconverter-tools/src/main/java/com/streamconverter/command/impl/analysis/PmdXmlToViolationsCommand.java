package com.streamconverter.command.impl.analysis;

import com.streamconverter.command.AbstractStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * PMD XML レポートを {@link PmdViolation} オブジェクトのストリームに変換するコマンド。
 *
 * <p>入力: PMD XML レポートの {@link InputStream}
 *
 * <p>出力: {@link ObjectOutputStream} で {@link PmdViolation} を順次 writeObject
 *
 * <p>後続コマンド（{@link PmdXmlToMarkdownCommand}・{@link PmdXmlToJsonCommand}・{@link
 * PmdXmlToCsvCommand}）は {@link java.io.ObjectInputStream} で受け取る。
 */
public class PmdXmlToViolationsCommand extends AbstractStreamCommand {

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    try (ObjectOutputStream oos = new ObjectOutputStream(output)) {
      for (PmdViolation violation : PmdViolationXmlParser.parse(input)) {
        oos.writeObject(violation);
      }
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Failed to parse PMD XML: " + e.getMessage(), e);
    }
  }
}
