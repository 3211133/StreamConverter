package com.streamConverter.command.impl.xml;

import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.command.rule.IRule;
import com.streamConverter.pathHandler.FixedStaXPathHandler;
import com.streamConverter.pathHandler.IStaXPathHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * XML変換コマンドクラス
 *
 * <p>このクラスは、XML形式のデータを変換するためのコマンドを実装します。 ストリームを使用して、XMLデータを読み込み、変換後のデータを出力します。
 * 変換対象のXPathである箇所を特定したあとに、変換処理を実行することを想定しています。
 */
public class ConvertCommand extends AbstractStreamCommand {

  private IRule rule;
  private IStaXPathHandler pathHandler;

  /**
   * デフォルトコンストラクタ
   *
   * @param rule 変換ルール
   * @param path 変換対象のXPath
   */
  public ConvertCommand(IRule rule, String path) {
    super();
    Objects.requireNonNull(rule, "rule must not be null");
    Objects.requireNonNull(path, "path must not be null");
    this.rule = rule;

    this.pathHandler = new FixedStaXPathHandler(path);
  }

  /**
   * XML変換コマンドの実行
   *
   * @param inputStream 入力ストリーム
   * @param outputStream 出力ストリーム
   * @throws IOException 入出力エラーが発生した場合
   */
  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    // StaXを使用したXML変換処理を実装する
    // ここでは、IRuleを使用して変換処理を行うことを想定しています。
    // 例: XMLを読み込み、IRuleを適用して変換し、出力ストリームに書き込む処理を実装する
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    XMLEventReader xmlEventReader = null;
    XMLEventWriter xmlEventWriter = null;
    try {
      xmlEventReader = xmlInputFactory.createXMLEventReader(inputStream);
      xmlEventWriter = xmlOutputFactory.createXMLEventWriter(outputStream);

      List<String> currentDirectory = new ArrayList<>();
      while (xmlEventReader.hasNext()) {
        XMLEvent event = xmlEventReader.nextEvent();
        int eventType = event.getEventType();
        switch (eventType) {
          // 現在地点を保持するための処理
          case XMLEvent.START_ELEMENT:
            // 現在のXpathを保持する
            currentDirectory.add(event.asStartElement().getName().getLocalPart());
            break;
          case XMLEvent.END_ELEMENT:
            // 現在のXpathを保持する
            currentDirectory.remove(currentDirectory.size() - 1);
            break;
          // 変換対象の箇所なら変換処理を実行する
          case XMLEvent.CHARACTERS:
            if (this.pathHandler.isTarget(currentDirectory)) {
              String transformedData = rule.apply(event.asCharacters().getData());
              event = XMLEventFactory.newDefaultFactory().createCharacters(transformedData);
            }
            break;
          default:
            // Handle other events if necessary
            break;
        }
        // Write the event to the output stream
        xmlEventWriter.add(event);
      }
    } catch (XMLStreamException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (xmlEventReader != null) {
        try {
          xmlEventReader.close();
        } catch (XMLStreamException e) {
          e.printStackTrace();
        }
      }
      if (xmlEventWriter != null) {
        try {
          xmlEventWriter.close();
        } catch (XMLStreamException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
