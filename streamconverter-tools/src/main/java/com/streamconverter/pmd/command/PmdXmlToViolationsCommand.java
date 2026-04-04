package com.streamconverter.pmd.command;

import com.streamconverter.command.AbstractStreamCommand;
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
public class PmdXmlToViolationsCommand extends AbstractStreamCommand {

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
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Failed to parse PMD XML: " + e.getMessage(), e);
    }
  }

  private void parseAll(XMLStreamReader reader, ObjectOutputStream oos)
      throws XMLStreamException, IOException {
    String currentFile = null;
    String currentRule = null;
    String currentRuleset = null;
    int currentLine = 0;
    int currentPriority = 0;
    String currentClass = null;
    String currentMethod = null;
    String currentVariable = null;
    StringBuilder currentDescription = null;

    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.START_ELEMENT) {
        String localName = reader.getLocalName();
        if ("file".equals(localName)) {
          currentFile = extractRelativePath(reader.getAttributeValue(null, "name"));
        } else if ("violation".equals(localName) && currentFile != null) {
          currentRule = reader.getAttributeValue(null, "rule");
          currentRuleset = reader.getAttributeValue(null, "ruleset");
          currentLine = parseIntOrZero(reader.getAttributeValue(null, "beginline"));
          currentPriority = parseIntOrZero(reader.getAttributeValue(null, "priority"));
          currentClass = nullToEmpty(reader.getAttributeValue(null, "class"));
          currentMethod = nullToEmpty(reader.getAttributeValue(null, "method"));
          currentVariable = nullToEmpty(reader.getAttributeValue(null, "variable"));
          currentDescription = new StringBuilder();
        }
      } else if (event == XMLStreamConstants.CHARACTERS && currentDescription != null) {
        currentDescription.append(reader.getText());
      } else if (event == XMLStreamConstants.END_ELEMENT) {
        String localName = reader.getLocalName();
        if ("violation".equals(localName) && currentDescription != null) {
          oos.writeObject(
              new PmdViolation(
                  currentFile,
                  currentLine,
                  currentRule,
                  currentRuleset,
                  currentPriority,
                  currentDescription.toString().strip(),
                  currentClass,
                  currentMethod,
                  currentVariable));
          currentDescription = null;
        } else if ("file".equals(localName)) {
          currentFile = null;
        }
      }
    }
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

  private static int parseIntOrZero(String value) {
    if (value == null || value.isEmpty()) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static String nullToEmpty(String value) {
    return value != null ? value : "";
  }
}
