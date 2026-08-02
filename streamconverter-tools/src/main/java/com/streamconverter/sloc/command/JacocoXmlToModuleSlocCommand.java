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
 *
 * <p>パース処理は要素の入れ子構造をそのままメソッドの入れ子で写している。{@code <report>} に入ったら {@link #readReport}
 * がその要素を閉じるまでを担当するため、モジュール名や LINE カウンターの検出有無はローカル変数で表現でき、 イベントを跨いで持ち回る可変状態を持たない。
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

  /**
   * {@code <report>} 要素を探し、見つけるたびにその要素全体の読み取りを {@link #readReport} に委ねる。
   *
   * <p>{@link #readReport} が report のサブツリーを消費しきるため、ここで見える {@code <report>} は
   * 常にラッパー直下のものになる。旧実装が絶対深度 {@code depth == 2} で行っていた絞り込みは、 この構造によって自然に満たされる。
   */
  private void parseAll(XMLStreamReader reader, ObjectOutputStream oos)
      throws XMLStreamException, IOException {
    while (reader.hasNext()) {
      if (reader.next() == XMLStreamConstants.START_ELEMENT
          && "report".equals(reader.getLocalName())) {
        readReport(reader, oos);
      }
    }
  }

  /**
   * {@code </report>} に到達するまで読み進め、直下の {@code <counter type="LINE">} を書き出す。
   *
   * <p>LINE カウンターが1つも無かった場合はゼロ件として記録する。{@code depth} は report 直下を 0 とした相対深度で、 深い階層（package / class
   * / method）にある同名のカウンターを除外するために用いる。
   *
   * @param reader カーソルが {@code <report>} の START_ELEMENT にあるリーダー
   */
  private void readReport(XMLStreamReader reader, ObjectOutputStream oos)
      throws XMLStreamException, IOException {
    String module = reader.getAttributeValue(null, "name");
    boolean lineCounterFound = false;
    int depth = 0;

    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        depth++;
        if (depth == 1 && isLineCounter(reader)) {
          writeLineCounter(reader, oos, module);
          lineCounterFound = true;
        }
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        if (depth == 0) {
          finishReport(oos, module, lineCounterFound);
          return;
        }
        depth--;
      }
    }
  }

  /** カーソル位置の要素が {@code <counter type="LINE">} かどうかを判定する。 */
  private static boolean isLineCounter(XMLStreamReader reader) {
    return "counter".equals(reader.getLocalName())
        && "LINE".equals(reader.getAttributeValue(null, "type"));
  }

  /** LINE カウンターの missed/covered を {@link ModuleSloc} として書き出す。 */
  private void writeLineCounter(XMLStreamReader reader, ObjectOutputStream oos, String module)
      throws IOException {
    try {
      int missed = Integer.parseInt(reader.getAttributeValue(null, "missed"));
      int covered = Integer.parseInt(reader.getAttributeValue(null, "covered"));
      oos.writeObject(new ModuleSloc(module, missed + covered, covered, missed));
    } catch (NumberFormatException e) {
      throw new IOException(
          "Invalid LINE counter attribute in JaCoCo XML for module "
              + module
              + ": "
              + e.getMessage(),
          e);
    }
  }

  /** report を閉じる際に、LINE カウンターが見つからなかったモジュールをゼロ件として記録する。 */
  private void finishReport(ObjectOutputStream oos, String module, boolean lineCounterFound)
      throws IOException {
    if (lineCounterFound) {
      return;
    }
    log.warn(
        "No LINE counter found in JaCoCo report for module '{}'. "
            + "Check JaCoCo XML format or coverage configuration.",
        module);
    oos.writeObject(new ModuleSloc(module, 0, 0, 0));
  }
}
