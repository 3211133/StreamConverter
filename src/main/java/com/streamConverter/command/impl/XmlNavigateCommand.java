package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.pathHandler.FixedStaXPathHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
public class XmlNavigateCommand extends AbstractStreamCommand {

  private String xpath;
  private FixedStaXPathHandler pathHandler;

  /**
   * Constructor for XML navigation with XPath selector.
   *
   * @param xpath the XPath expression to select elements (e.g., "users/user/name")
   */
  public XmlNavigateCommand(String xpath) {
    this.xpath = xpath;
    if (xpath != null) {
      this.pathHandler = new FixedStaXPathHandler(xpath);
    }
  }

  /** Default constructor - processes entire XML. */
  public XmlNavigateCommand() {
    this.xpath = null;
    this.pathHandler = null;
  }

  @Override
  protected String getCommandDetails() {
    if (xpath != null) {
      return String.format("XmlNavigateCommand(xpath='%s')", xpath);
    } else {
      return "XmlNavigateCommand(entire XML)";
    }
  }

  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      XMLInputFactory inputFactory = XMLInputFactory.newInstance();

      // XXE攻撃を防ぐためのセキュリティ設定
      inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

      XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);
      XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);

      if (pathHandler == null) {
        // Pass through entire XML
        while (eventReader.hasNext()) {
          XMLEvent event = eventReader.nextEvent();
          eventWriter.add(event);
        }
      } else {
        // Navigate using XPath
        navigateXml(eventReader, eventWriter);
      }

      eventWriter.flush();
      eventReader.close();
      eventWriter.close();
    } catch (XMLStreamException e) {
      throw new IOException("XML processing error", e);
    }
  }

  private void navigateXml(XMLEventReader eventReader, XMLEventWriter eventWriter)
      throws XMLStreamException {
    List<String> currentPath = new ArrayList<>();
    boolean inTargetElement = false;
    int targetDepth = 0;

    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();

      if (event.isStartElement()) {
        String elementName = event.asStartElement().getName().getLocalPart();
        currentPath.add(elementName);

        if (pathHandler.isTarget(currentPath)) {
          inTargetElement = true;
          targetDepth = currentPath.size();
          eventWriter.add(event);
        } else if (inTargetElement) {
          eventWriter.add(event);
        }
      } else if (event.isEndElement()) {
        if (inTargetElement) {
          eventWriter.add(event);
          if (currentPath.size() == targetDepth) {
            inTargetElement = false;
            // Add newline as simple characters (XMLEventFactory not available in all JVMs)
            eventWriter.add(javax.xml.stream.XMLEventFactory.newInstance().createCharacters("\n"));
          }
        }
        currentPath.remove(currentPath.size() - 1);
      } else if (event.isCharacters() && inTargetElement) {
        eventWriter.add(event);
      }
    }
  }

  private XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
}
