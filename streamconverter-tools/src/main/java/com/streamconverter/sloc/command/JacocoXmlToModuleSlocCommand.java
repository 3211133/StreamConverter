package com.streamconverter.sloc.command;

import com.streamconverter.command.IStreamCommand;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class JacocoXmlToModuleSlocCommand implements IStreamCommand {

  private static final Logger log = LoggerFactory.getLogger(JacocoXmlToModuleSlocCommand.class);

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
    } catch (XMLStreamException e) {
      throw new IOException(
          "Failed to parse JaCoCo XML at " + e.getLocation() + ": " + e.getMessage(), e);
    }
  }

  private void parseAll(XMLStreamReader reader, ObjectOutputStream oos)
      throws XMLStreamException, IOException {
    ParseState state = new ParseState();

    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        handleStartElement(reader, oos, state);
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        handleEndElement(oos, state);
      }
    }
  }

  /** {@code <report>} でモジュール名を確定し、その直下の LINE カウンターを書き出す。 */
  private void handleStartElement(XMLStreamReader reader, ObjectOutputStream oos, ParseState state)
      throws IOException {
    state.depth++;
    String localName = reader.getLocalName();
    if (state.depth == 2 && "report".equals(localName)) {
      state.currentModule = reader.getAttributeValue(null, "name");
      state.lineCounterFound = false;
    } else if (isLineCounter(reader, localName, state)) {
      writeLineCounter(reader, oos, state);
    }
  }

  /** report 直下の {@code <counter type="LINE">} かどうかを判定する。 */
  private static boolean isLineCounter(XMLStreamReader reader, String localName, ParseState state) {
    return state.depth == 3
        && "counter".equals(localName)
        && "LINE".equals(reader.getAttributeValue(null, "type"))
        && state.currentModule != null;
  }

  /** LINE カウンターの missed/covered を {@link ModuleSloc} として書き出す。 */
  private void writeLineCounter(XMLStreamReader reader, ObjectOutputStream oos, ParseState state)
      throws IOException {
    try {
      int missed = Integer.parseInt(reader.getAttributeValue(null, "missed"));
      int covered = Integer.parseInt(reader.getAttributeValue(null, "covered"));
      oos.writeObject(new ModuleSloc(state.currentModule, missed + covered, covered, missed));
      state.lineCounterFound = true;
    } catch (NumberFormatException e) {
      throw new IOException(
          "Invalid LINE counter attribute in JaCoCo XML for module "
              + state.currentModule
              + ": "
              + e.getMessage(),
          e);
    }
  }

  /** {@code </report>} でモジュールを閉じる。LINE カウンターが無かった場合はゼロ件として記録する。 */
  // NullAssignment: currentModule への null 代入はストリーミングパーサの状態リセットであり、
  // 「モジュール外にいる」ことを表す意図的なセンチネル。
  @SuppressWarnings("PMD.NullAssignment")
  private void handleEndElement(ObjectOutputStream oos, ParseState state) throws IOException {
    if (state.depth == 2 && state.currentModule != null) {
      if (!state.lineCounterFound) {
        log.warn(
            "No LINE counter found in JaCoCo report for module '{}'. "
                + "Check JaCoCo XML format or coverage configuration.",
            state.currentModule);
        oos.writeObject(new ModuleSloc(state.currentModule, 0, 0, 0));
      }
      state.currentModule = null;
    }
    state.depth--;
  }

  /** ストリーミングパース中の可変状態。抽出したハンドラ間で受け渡すための入れ物。 */
  private static final class ParseState {
    private String currentModule;
    private boolean lineCounterFound;
    private int depth;
  }
}
