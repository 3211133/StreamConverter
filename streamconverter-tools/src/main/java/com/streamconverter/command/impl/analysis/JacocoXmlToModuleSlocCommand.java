package com.streamconverter.command.impl.analysis;

import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.security.SecureXmlConfiguration;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
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
 * XML として扱う。ラッパー要素名は JaCoCo の仕様に依存しない独自名を使用している。
 *
 * <p><b>YAGNI:</b> JaCoCo のルート要素名が変わった場合でも、ラッパー要素名は独立しているため影響を受けない。
 */
public class JacocoXmlToModuleSlocCommand extends AbstractStreamCommand {

  private static final byte[] WRAPPER_OPEN = "<jacoco-reports>".getBytes(StandardCharsets.UTF_8);
  private static final byte[] WRAPPER_CLOSE = "</jacoco-reports>".getBytes(StandardCharsets.UTF_8);

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    XMLInputFactory factory = SecureXmlConfiguration.createSecureXMLInputFactory();
    // XML宣言（<?xml ...?>）を除去してからラッパー要素で包み Well-formed XML として扱う。
    // XML宣言はドキュメント先頭にしか置けないため、ラッパーの後には置けない。
    InputStream wrapped =
        new SequenceInputStream(
            new SequenceInputStream(
                new ByteArrayInputStream(WRAPPER_OPEN), new XmlDeclarationStrippingStream(input)),
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

  /**
   * XML宣言（{@code <?xml ...?>}）をストリーム先頭から除去する {@link FilterInputStream}。
   *
   * <p>複数の {@code <report>} をラッパー要素で包む際に、内側の XML 宣言がパースエラーになるのを防ぐ。
   */
  private static final class XmlDeclarationStrippingStream extends FilterInputStream {

    private static final byte[] XML_DECL_START = "<?xml".getBytes(StandardCharsets.UTF_8);
    private boolean stripped = false;

    XmlDeclarationStrippingStream(InputStream in) {
      super(new BufferedInputStream(in));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (!stripped) {
        stripDeclarations();
      }
      return super.read(b, off, len);
    }

    @Override
    public int read() throws IOException {
      if (!stripped) {
        stripDeclarations();
      }
      return super.read();
    }

    private void stripDeclarations() throws IOException {
      stripped = true;
      // 先読みして <?xml で始まる行をスキップ（複数モジュール連結で複数ある可能性）
      while (true) {
        in.mark(XML_DECL_START.length + 1);
        byte[] peek = new byte[XML_DECL_START.length];
        int read = in.read(peek);
        if (read < XML_DECL_START.length) {
          if (read > 0) in.reset();
          break;
        }
        boolean isXmlDecl = true;
        for (int i = 0; i < XML_DECL_START.length; i++) {
          if (peek[i] != XML_DECL_START[i]) {
            isXmlDecl = false;
            break;
          }
        }
        if (!isXmlDecl) {
          in.reset();
          break;
        }
        // <?xml ... ?> の終端 ?> まで読み飛ばす
        int prev = -1;
        int cur;
        while ((cur = in.read()) != -1) {
          if (prev == '?' && cur == '>') {
            break;
          }
          prev = cur;
        }
        // 改行・空白をスキップ
        while (true) {
          in.mark(1);
          int c = in.read();
          if (c == -1 || (c != '\n' && c != '\r' && c != ' ' && c != '\t')) {
            if (c != -1) in.reset();
            break;
          }
        }
      }
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
