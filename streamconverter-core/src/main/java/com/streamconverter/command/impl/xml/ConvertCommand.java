package com.streamconverter.command.impl.xml;

import com.streamconverter.StreamProcessingException;
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.command.rule.IRule;
import com.streamconverter.path.TreePath;
import com.streamconverter.security.SecureXmlConfiguration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML変換コマンドクラス
 *
 * <p>このクラスは、XML形式のデータを変換するためのコマンドを実装します。 ストリームを使用して、XMLデータを読み込み、変換後のデータを出力します。
 * 変換対象のXPathである箇所を特定したあとに、変換処理を実行することを想定しています。
 */
public class ConvertCommand extends AbstractStreamCommand {

  private static final Logger logger = LoggerFactory.getLogger(ConvertCommand.class);
  private IRule rule;
  private TreePath treePath;

  /**
   * デフォルトコンストラクタ
   *
   * @param rule 変換ルール
   * @param treePath 変換対象のTreePath
   */
  private ConvertCommand(IRule rule, TreePath treePath) {
    super();
    Objects.requireNonNull(rule, "rule must not be null");
    Objects.requireNonNull(treePath, "treePath must not be null");
    this.rule = rule;
    this.treePath = treePath;
  }

  /**
   * Factory method for creating a ConvertCommand with explicit rule and TreePath.
   *
   * @param rule 変換ルール
   * @param treePath 変換対象のTreePath
   * @return a ConvertCommand instance
   * @throws NullPointerException if rule or treePath is null
   */
  public static ConvertCommand create(IRule rule, TreePath treePath) {
    Objects.requireNonNull(rule, "rule must not be null");
    Objects.requireNonNull(treePath, "treePath must not be null");
    return new ConvertCommand(rule, treePath);
  }

  /**
   * XML変換コマンドの実行
   *
   * @param inputStream 入力ストリーム
   * @param outputStream 出力ストリーム
   * @throws IOException 入出力エラーが発生した場合
   * @throws StreamProcessingException XML処理エラーが発生した場合
   */
  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    // StaXを使用したXML変換処理を実装する
    // ここでは、IRuleを使用して変換処理を行うことを想定しています。
    // 例: XMLを読み込み、IRuleを適用して変換し、出力ストリームに書き込む処理を実装する
    XMLInputFactory xmlInputFactory = SecureXmlConfiguration.createSecureXMLInputFactory();
    XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

    try {
      XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(inputStream);
      XMLEventWriter xmlEventWriter = xmlOutputFactory.createXMLEventWriter(outputStream);

      try {
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
              if (this.treePath.matches(currentDirectory)) {
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
      } finally {
        // リソースのクリーンアップ
        try {
          if (xmlEventWriter != null) {
            xmlEventWriter.close();
          }
        } catch (XMLStreamException e) {
          logger.warn("XMLEventWriterのクローズ中にエラーが発生しました: {}", e.getMessage(), e);
        }
        try {
          if (xmlEventReader != null) {
            xmlEventReader.close();
          }
        } catch (XMLStreamException e) {
          logger.warn("XMLEventReaderのクローズ中にエラーが発生しました: {}", e.getMessage(), e);
        }
      }
    } catch (XMLStreamException e) {
      logger.error("XML処理中にエラーが発生しました: {}", e.getMessage(), e);
      throw new StreamProcessingException("XML変換処理に失敗しました", e);
    }
  }
}
