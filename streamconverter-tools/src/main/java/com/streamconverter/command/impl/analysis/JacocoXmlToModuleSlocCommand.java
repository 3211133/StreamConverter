package com.streamconverter.command.impl.analysis;

import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.security.SecureXmlConfiguration;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * JaCoCo XML レポートから LINE カウンターを抽出して {@link ModuleSloc} オブジェクトとして出力するコマンド。
 *
 * <p>{@link ModuleXmlConcatCommand} が出力する連結ストリームを入力として受け取り、 モジュールごとに report 直下の {@code <counter
 * type="LINE">} を抽出して {@link ObjectOutputStream} で {@link ModuleSloc} を書き出す。
 *
 * <p>単一モジュール用途（モジュール名を直接指定）にも使用できる。その場合は {@link #JacocoXmlToModuleSlocCommand(String)} を使用する。
 */
public class JacocoXmlToModuleSlocCommand extends AbstractStreamCommand {

  private final String singleModuleName;

  /**
   * 複数モジュール対応コンストラクタ。
   *
   * <p>{@link ModuleXmlConcatCommand} の出力を入力として受け取る。
   */
  public JacocoXmlToModuleSlocCommand() {
    this.singleModuleName = null;
  }

  /**
   * 単一モジュール用コンストラクタ。モジュール名ヘッダーのない生の JaCoCo XML を入力として受け取る。
   *
   * @param moduleName {@link ModuleSloc} の name フィールドに使用するモジュール名
   */
  public JacocoXmlToModuleSlocCommand(String moduleName) {
    this.singleModuleName = moduleName;
  }

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    try (ObjectOutputStream oos = new ObjectOutputStream(output)) {
      if (singleModuleName != null) {
        ModuleSloc sloc = parseSingleXml(singleModuleName, input);
        oos.writeObject(sloc);
      } else {
        parseMultiXml(input, oos);
      }
    }
  }

  private void parseMultiXml(InputStream input, ObjectOutputStream oos) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      String currentModule = null;
      List<String> xmlLines = new ArrayList<>();

      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith(ModuleXmlConcatCommand.MODULE_HEADER_PREFIX)) {
          if (currentModule != null && !xmlLines.isEmpty()) {
            ModuleSloc sloc = flushXml(currentModule, xmlLines);
            oos.writeObject(sloc);
          }
          currentModule = line.substring(ModuleXmlConcatCommand.MODULE_HEADER_PREFIX.length());
          xmlLines = new ArrayList<>();
        } else if (currentModule != null) {
          xmlLines.add(line);
        }
      }
      if (currentModule != null && !xmlLines.isEmpty()) {
        ModuleSloc sloc = flushXml(currentModule, xmlLines);
        oos.writeObject(sloc);
      }
    }
  }

  private ModuleSloc flushXml(String moduleName, List<String> xmlLines) throws IOException {
    byte[] xmlBytes = String.join("\n", xmlLines).getBytes(StandardCharsets.UTF_8);
    return parseSingleXml(moduleName, new ByteArrayInputStream(xmlBytes));
  }

  private ModuleSloc parseSingleXml(String moduleName, InputStream input) throws IOException {
    XMLInputFactory factory = SecureXmlConfiguration.createSecureXMLInputFactory();
    try {
      XMLStreamReader reader = factory.createXMLStreamReader(input);
      int depth = 0;
      try {
        while (reader.hasNext()) {
          int event = reader.next();
          if (event == XMLStreamConstants.START_ELEMENT) {
            depth++;
            if (depth == 2
                && "counter".equals(reader.getLocalName())
                && "LINE".equals(reader.getAttributeValue(null, "type"))) {
              int missed = Integer.parseInt(reader.getAttributeValue(null, "missed"));
              int covered = Integer.parseInt(reader.getAttributeValue(null, "covered"));
              return new ModuleSloc(moduleName, missed + covered, covered, missed);
            }
          } else if (event == XMLStreamConstants.END_ELEMENT) {
            depth--;
          }
        }
      } finally {
        reader.close();
      }
    } catch (XMLStreamException e) {
      throw new IOException(
          "Failed to parse JaCoCo XML for module " + moduleName + ": " + e.getMessage(), e);
    } catch (NumberFormatException e) {
      throw new IOException(
          "Invalid LINE counter attribute in JaCoCo XML for module "
              + moduleName
              + ": "
              + e.getMessage(),
          e);
    }
    log.warn(
        "No LINE counter found in JaCoCo report for module '{}'. "
            + "Check JaCoCo XML format or coverage configuration.",
        moduleName);
    return new ModuleSloc(moduleName, 0, 0, 0);
  }
}
