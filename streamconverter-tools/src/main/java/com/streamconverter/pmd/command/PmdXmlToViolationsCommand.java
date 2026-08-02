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

  private void parseAll(XMLStreamReader reader, ObjectOutputStream oos)
      throws XMLStreamException, IOException {
    ParseState state = new ParseState();

    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        handleStartElement(reader, state);
      } else if (isDescriptionText(event) && state.description != null) {
        state.description.append(reader.getText());
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        handleEndElement(reader, oos, state);
      }
    }
  }

  /** violation 要素の本文（説明文）として蓄積すべきイベントかどうかを判定する。 */
  private static boolean isDescriptionText(int event) {
    return event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA;
  }

  /** {@code <file>} でファイル名を、{@code <violation>} で違反の属性を取り込む。 */
  private void handleStartElement(XMLStreamReader reader, ParseState state) {
    String localName = reader.getLocalName();
    if ("file".equals(localName)) {
      state.file = extractRelativePath(reader.getAttributeValue(null, "name"));
    } else if ("violation".equals(localName) && state.file != null) {
      state.rule = reader.getAttributeValue(null, "rule");
      state.ruleset = reader.getAttributeValue(null, "ruleset");
      state.line =
          parseIntOrZero(reader.getAttributeValue(null, "beginline"), "beginline", state.file);
      state.priority =
          parseIntOrZero(reader.getAttributeValue(null, "priority"), "priority", state.file);
      state.violationClass = nullToEmpty(reader.getAttributeValue(null, "class"));
      state.method = nullToEmpty(reader.getAttributeValue(null, "method"));
      state.variable = nullToEmpty(reader.getAttributeValue(null, "variable"));
      state.description = new StringBuilder();
    }
  }

  /** {@code </violation>} で1件を書き出し、{@code </file>} でファイル状態をリセットする。 */
  // NullAssignment: description / file への null 代入はストリーミングパーサの状態リセットであり、
  // 「violation・file 要素の外にいる」ことを表す意図的なセンチネル。
  @SuppressWarnings("PMD.NullAssignment")
  private void handleEndElement(XMLStreamReader reader, ObjectOutputStream oos, ParseState state)
      throws IOException {
    String localName = reader.getLocalName();
    if ("violation".equals(localName) && state.description != null) {
      oos.writeObject(
          new PmdViolation(
              state.file,
              state.line,
              state.rule,
              state.ruleset,
              state.priority,
              state.description.toString().strip(),
              state.violationClass,
              state.method,
              state.variable));
      state.description = null;
    } else if ("file".equals(localName)) {
      state.file = null;
    }
  }

  /** ストリーミングパース中の可変状態。抽出したハンドラ間で受け渡すための入れ物。 */
  // AvoidStringBufferField: ParseState は parseAll() 1回分の寿命しか持たず、
  // description は violation 1件ごとに作り直されるため、長期滞留による肥大化は起きない。
  @SuppressWarnings("PMD.AvoidStringBufferField")
  private static final class ParseState {
    private String file;
    private String rule;
    private String ruleset;
    private int line;
    private int priority;
    private String violationClass;
    private String method;
    private String variable;
    private StringBuilder description;
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
