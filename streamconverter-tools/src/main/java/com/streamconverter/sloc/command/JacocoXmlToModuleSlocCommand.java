package com.streamconverter.sloc.command;

import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.security.SecureXmlConfiguration;
import com.streamconverter.sloc.ModuleSloc;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * JaCoCo XML レポートから LINE カウンターを抽出して {@link ModuleSloc} オブジェクトとして出力するコマンド。
 *
 * <p>{@link ModuleXmlConcatCommand} が出力する連結ストリームを入力として受け取り、 各 {@code <report>} 要素の name
 * 属性をモジュール名として、report 直下の {@code <counter type="LINE">} を抽出して {@link ObjectOutputStream} で {@link
 * ModuleSloc} を書き出す。
 *
 * <p>入力ストリームは複数の {@code <report>} 要素を持つため、パース前に {@code <jacoco-reports>} ラッパーで 包んで Well-formed な
 * XML として扱う。ラッパー要素名は JaCoCo の仕様に依存しない独自名を使用している。 {@link ModuleXmlConcatCommand} が XML宣言・DOCTYPE宣言を
 * 除去した上で {@code <report>} 要素だけを渡すため、ここでは宣言の除去は不要。
 *
 * <p>ラッパー要素名は JaCoCo の仕様に依存しない独自名のため、JaCoCo のルート要素名が変わっても影響を受けない。
 */
public class JacocoXmlToModuleSlocCommand extends AbstractStreamCommand {

  private static final byte[] WRAPPER_OPEN =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><jacoco-reports>".getBytes(StandardCharsets.UTF_8);
  private static final byte[] WRAPPER_CLOSE = "</jacoco-reports>".getBytes(StandardCharsets.UTF_8);

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    XMLInputFactory factory = SecureXmlConfiguration.createSecureXMLInputFactory();
    InputStream wrapped =
        new SequenceInputStream(
            new SequenceInputStream(new ByteArrayInputStream(WRAPPER_OPEN), input),
            new ByteArrayInputStream(WRAPPER_CLOSE));
    try (ObjectOutputStream oos = new ObjectOutputStream(output)) {
      XMLStreamReader reader = factory.createXMLStreamReader(wrapped);
      try {
        parseAll(reader, oos);
      } finally {
        reader.close();
      }
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Failed to parse JaCoCo XML: " + e.getMessage(), e);
    }
  }

  private void parseAll(XMLStreamReader reader, ObjectOutputStream oos)
      throws XMLStreamException, IOException {
    String currentModule = null;
    boolean lineCounterFound = false;
    int depth = 0;

    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        depth++;
        String localName = reader.getLocalName();
        if (depth == 2 && "report".equals(localName)) {
          currentModule = reader.getAttributeValue(null, "name");
          lineCounterFound = false;
        } else if (depth == 3
            && "counter".equals(localName)
            && "LINE".equals(reader.getAttributeValue(null, "type"))
            && currentModule != null) {
          try {
            int missed = Integer.parseInt(reader.getAttributeValue(null, "missed"));
            int covered = Integer.parseInt(reader.getAttributeValue(null, "covered"));
            oos.writeObject(new ModuleSloc(currentModule, missed + covered, covered, missed));
            lineCounterFound = true;
          } catch (NumberFormatException e) {
            throw new IOException(
                "Invalid LINE counter attribute in JaCoCo XML for module "
                    + currentModule
                    + ": "
                    + e.getMessage(),
                e);
          }
        }
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        if (depth == 2 && currentModule != null) {
          if (!lineCounterFound) {
            log.warn(
                "No LINE counter found in JaCoCo report for module '{}'. "
                    + "Check JaCoCo XML format or coverage configuration.",
                currentModule);
            oos.writeObject(new ModuleSloc(currentModule, 0, 0, 0));
          }
          currentModule = null;
        }
        depth--;
      }
    }
  }
}
