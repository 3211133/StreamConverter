package com.streamconverter.pmd.command;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.pmd.PmdViolation;
import com.streamconverter.security.SecureXmlConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PMD XML レポートを {@link PmdViolation} オブジェクトのストリームに変換するコマンド。
 *
 * <p>入力: PMD XML レポートの {@link InputStream}
 *
 * <p>出力: {@link ObjectOutputStream} で {@link PmdViolation} を順次 writeObject
 *
 * <p>StAX（{@link XMLStreamReader}）による1件ずつのストリーム処理を行うため、大規模リポジトリでも OOM が発生しない。
 *
 * <p>PMD XML 構造: {@code <pmd><file name="..."><violation ...>テキスト</violation></file></pmd>}
 *
 * <p>パース処理は要素の入れ子構造をそのままメソッドの入れ子で写している。{@code <file>} に入ったら {@link #readFile} がその要素を閉じるまでを担当するため、
 * 「今どのファイルの中にいるか」はローカル変数で表現でき、イベントを跨いで持ち回る可変状態を持たない。
 */
public class PmdXmlToViolationsCommand implements IStreamCommand {

  private static final Logger log = LoggerFactory.getLogger(PmdXmlToViolationsCommand.class);

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    XMLInputFactory factory = SecureXmlConfiguration.createSecureXMLInputFactory();
    try (ObjectOutputStream oos = new ObjectOutputStream(output)) {
      XMLStreamReader reader = factory.createXMLStreamReader(input);
      try {
        parseAll(reader, oos);
      } finally {
        reader.close();
      }
    } catch (XMLStreamException e) {
      throw new IOException(
          "Failed to parse PMD XML at " + e.getLocation() + ": " + e.getMessage(), e);
    }
  }

  /** {@code <file>} 要素を探し、見つけるたびにその要素全体の読み取りを {@link #readFile} に委ねる。 */
  private void parseAll(XMLStreamReader reader, ObjectOutputStream oos)
      throws XMLStreamException, IOException {
    while (reader.hasNext()) {
      if (reader.next() == XMLStreamConstants.START_ELEMENT
          && "file".equals(reader.getLocalName())) {
        readFile(reader, oos);
      }
    }
  }

  /**
   * {@code </file>} に到達するまで、配下の {@code <violation>} を1件ずつ書き出す。
   *
   * <p>呼び出し時点でカーソルは {@code <file>} の START_ELEMENT にあり、復帰時点で対応する END_ELEMENT にある。
   */
  private void readFile(XMLStreamReader reader, ObjectOutputStream oos)
      throws XMLStreamException, IOException {
    String file = extractRelativePath(reader.getAttributeValue(null, "name"));

    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT && "violation".equals(reader.getLocalName())) {
        oos.writeObject(readViolation(reader, file));
      } else if (event == XMLStreamConstants.END_ELEMENT && "file".equals(reader.getLocalName())) {
        return;
      }
    }
  }

  /**
   * {@code <violation>} 要素を属性と本文まとめて1件読み取る。
   *
   * <p>属性は本文の読み取りでカーソルが進む前に取得する必要がある。{@link XMLStreamReader#getElementText()} が CHARACTERS/CDATA
   * の連結と END_ELEMENT までの前進を行うため、説明文を蓄積する {@code StringBuilder} を フィールドとして保持する必要がない。
   */
  private PmdViolation readViolation(XMLStreamReader reader, String file)
      throws XMLStreamException {
    int line = parseIntOrZero(reader.getAttributeValue(null, "beginline"), "beginline", file);
    int priority = parseIntOrZero(reader.getAttributeValue(null, "priority"), "priority", file);
    String rule = reader.getAttributeValue(null, "rule");
    String ruleset = reader.getAttributeValue(null, "ruleset");
    String violationClass = nullToEmpty(reader.getAttributeValue(null, "class"));
    String method = nullToEmpty(reader.getAttributeValue(null, "method"));
    String variable = nullToEmpty(reader.getAttributeValue(null, "variable"));
    String description = reader.getElementText().strip();

    return new PmdViolation(
        file, line, rule, ruleset, priority, description, violationClass, method, variable);
  }

  /**
   * フルパスから {@code streamconverter-} 以降の相対パスを抽出する。
   *
   * <p>例: {@code /home/user/streamconverter-core/src/Foo.java} → {@code
   * streamconverter-core/src/Foo.java}
   */
  private static String extractRelativePath(String fullPath) {
    if (fullPath == null) {
      return "";
    }
    int idx = fullPath.indexOf("streamconverter-");
    return idx >= 0 ? fullPath.substring(idx) : fullPath;
  }

  private int parseIntOrZero(String value, String attributeName, String context) {
    if (value == null || value.isEmpty()) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      log.warn(
          "Invalid integer for attribute '{}' in '{}': '{}' — defaulting to 0",
          attributeName,
          context,
          value);
      return 0;
    }
  }

  private static String nullToEmpty(String value) {
    return value != null ? value : "";
  }
}
