package com.streamconverter.examples;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.context.PipelineContext;
import com.streamconverter.context.PipelineSignal;
import com.streamconverter.context.SignalChannel;
import com.streamconverter.security.SecureXmlConfiguration;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SignalChannelを使ったコマンド間連携のデモ。
 *
 * <p>XML明細の非表示フィルタ（Skip）シナリオを示す：
 *
 * <ul>
 *   <li>前段が各 {@code <明細>} 要素を1件バッファし、表示フラグを確認して非表示明細を除去した後に {@link PipelineSignal.Skip} を送信する。
 *   <li>後段はチャンク単位で {@link SignalChannel#poll()} を確認し、 Skipシグナルがあれば「フィルタが発生した」という事実を監査ログに記録する。
 * </ul>
 */
public class SignalPipelineDemo {

  private static final Logger logger = LoggerFactory.getLogger(SignalPipelineDemo.class);

  /**
   * メインメソッド
   *
   * @param args コマンドライン引数
   */
  public static void main(String[] args) {
    logger.info("SignalChannel Pipeline Demo");
    logger.info("==========================");

    try {
      demonstrateXmlVisibilityFilter();
    } catch (Exception e) {
      logger.error("Demo failed: {}", e.getMessage(), e);
    }
  }

  // ---------------------------------------------------------------------------
  // XML明細の非表示フィルタ（Skip）
  // ---------------------------------------------------------------------------

  /**
   * XML明細の非表示フィルタデモ。
   *
   * <p>前段コマンドがXMLから非表示（{@code <表示フラグ>false}）の明細を除去して後段に流す。 除去を実施した後に {@link PipelineSignal.Skip}
   * シグナルを送信することで、 後段コマンドは「フィルタが発生した」という事実を割り込みで検知できる。
   *
   * <p>後段コマンドはシグナルを確認して、フィルタが発生した場合に追加の処理 （ログ出力、監査レコード生成など）を行う。
   *
   * <p><b>設計上のポイント:</b><br>
   * ストリームデータ（XMLバイト列）とシグナルは独立したチャネルを使って伝達される。 XMLデータはPipedStreamを通じて流れ、シグナルはAtomicReferenceを通じて伝わる。
   * 前段がフィルタ完了後にシグナルを送ることで、後段がその事実を次の処理単位で検知できる。
   */
  static void demonstrateXmlVisibilityFilter() throws IOException {
    logger.info("\n--- XML明細の非表示フィルタ（Skip） ---");

    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<明細一覧>"
            + "<明細><表示フラグ>true</表示フラグ><商品名>りんご</商品名><金額>100</金額></明細>"
            + "<明細><表示フラグ>false</表示フラグ><商品名>バナナ</商品名><金額>200</金額></明細>"
            + "<明細><表示フラグ>true</表示フラグ><商品名>みかん</商品名><金額>150</金額></明細>"
            + "</明細一覧>";

    PipelineContext ctx = new PipelineContext();
    SignalChannel channel = ctx.prepareSignalChannel("xml-visibility");

    ByteArrayOutputStream output = new ByteArrayOutputStream();

    StreamConverter.create(
            new XmlVisibilityFilterCommand(channel, "明細", "表示フラグ"),
            new FilterAwareAuditCommand(channel))
        .run(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), output, ctx);

    logger.info("出力結果:\n{}", output.toString(StandardCharsets.UTF_8));
  }

  /**
   * 前段コマンド: XML明細を1件ずつバッファし、表示フラグを確認して非表示の明細を除去する。
   *
   * <p>非表示明細を除去した後に {@link PipelineSignal.Skip} シグナルを送信する。
   * 後段コマンドはこのシグナルを受け取ることで「フィルタが発生した」という事実を検知できる。
   *
   * <p>明細1件ずつバッファしてから表示フラグを確認することで、 「先頭から読んで途中でフラグが判明する」問題を回避している。
   */
  static class XmlVisibilityFilterCommand extends AbstractStreamCommand {

    private final SignalChannel channel;
    private final String itemElement;
    private final String visibilityElement;

    XmlVisibilityFilterCommand(
        SignalChannel channel, String itemElement, String visibilityElement) {
      this.channel = channel;
      this.itemElement = itemElement;
      this.visibilityElement = visibilityElement;
    }

    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
      try {
        XMLInputFactory inputFactory = SecureXmlConfiguration.createSecureXMLInputFactory();
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

        XMLStreamReader reader = inputFactory.createXMLStreamReader(inputStream, "UTF-8");
        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(outputStream, "UTF-8");

        boolean insideItem = false;
        int itemDepth = 0;
        StringBuilder itemBuffer = null;
        boolean anyItemSkipped = false;

        while (reader.hasNext()) {
          int event = reader.next();

          if (!insideItem) {
            switch (event) {
              case XMLStreamConstants.START_DOCUMENT -> writer.writeStartDocument("UTF-8", "1.0");
              case XMLStreamConstants.END_DOCUMENT -> writer.writeEndDocument();
              case XMLStreamConstants.START_ELEMENT -> {
                String localName = reader.getLocalName();
                if (localName.equals(itemElement)) {
                  insideItem = true;
                  itemDepth = 1;
                  itemBuffer = new StringBuilder();
                  appendStartElementToBuffer(itemBuffer, reader);
                } else {
                  writer.writeStartElement(localName);
                  copyAttributes(reader, writer);
                }
              }
              case XMLStreamConstants.END_ELEMENT -> writer.writeEndElement();
              default -> {
                /* その他は無視 */
              }
            }
          } else {
            switch (event) {
              case XMLStreamConstants.START_ELEMENT -> {
                itemDepth++;
                appendStartElementToBuffer(itemBuffer, reader);
              }
              case XMLStreamConstants.END_ELEMENT -> {
                itemDepth--;
                itemBuffer.append("</").append(reader.getLocalName()).append(">");
                if (itemDepth == 0) {
                  insideItem = false;
                  String itemXml = itemBuffer.toString();
                  if (isVisible(itemXml)) {
                    // 表示対象: XMLをパースして出力ストリームに書き出す
                    writeItemXml(itemXml, writer);
                  } else {
                    log.info("Filtering out non-visible item");
                    anyItemSkipped = true;
                    channel.send(new PipelineSignal.Skip("non-visible item filtered"));
                  }
                }
              }
              case XMLStreamConstants.CHARACTERS -> itemBuffer.append(escapeXml(reader.getText()));
              default -> {
                /* その他は無視 */
              }
            }
          }
        }

        if (!anyItemSkipped) {
          channel.reset();
        }

        writer.flush();
        writer.close();
        reader.close();
      } catch (XMLStreamException e) {
        throw new IOException("XML processing failed in filter", e);
      }
    }

    private void appendStartElementToBuffer(StringBuilder buf, XMLStreamReader reader) {
      buf.append("<").append(reader.getLocalName());
      for (int i = 0; i < reader.getAttributeCount(); i++) {
        buf.append(" ")
            .append(reader.getAttributeLocalName(i))
            .append("=\"")
            .append(escapeXml(reader.getAttributeValue(i)))
            .append("\"");
      }
      buf.append(">");
    }

    private String escapeXml(String text) {
      return text.replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("\"", "&quot;");
    }

    private boolean isVisible(String itemXml) {
      try {
        XMLStreamReader r =
            SecureXmlConfiguration.createSecureXMLInputFactory()
                .createXMLStreamReader(new StringReader(itemXml));
        boolean insideVisibility = false;
        while (r.hasNext()) {
          int ev = r.next();
          if (ev == XMLStreamConstants.START_ELEMENT
              && r.getLocalName().equals(visibilityElement)) {
            insideVisibility = true;
          } else if (ev == XMLStreamConstants.CHARACTERS && insideVisibility) {
            boolean result = Boolean.parseBoolean(r.getText().trim());
            r.close();
            return result;
          } else if (ev == XMLStreamConstants.END_ELEMENT && insideVisibility) {
            break;
          }
        }
        r.close();
      } catch (XMLStreamException e) {
        log.warn("Failed to extract visibility, treating as visible", e);
      }
      return true;
    }

    private void writeItemXml(String itemXml, XMLStreamWriter writer) throws XMLStreamException {
      XMLStreamReader r =
          SecureXmlConfiguration.createSecureXMLInputFactory()
              .createXMLStreamReader(new StringReader(itemXml));
      while (r.hasNext()) {
        int ev = r.next();
        switch (ev) {
          case XMLStreamConstants.START_ELEMENT -> {
            writer.writeStartElement(r.getLocalName());
            copyAttributes(r, writer);
          }
          case XMLStreamConstants.END_ELEMENT -> writer.writeEndElement();
          case XMLStreamConstants.CHARACTERS -> writer.writeCharacters(r.getText());
          default -> {
            /* その他は無視 */
          }
        }
      }
      r.close();
    }

    private void copyAttributes(XMLStreamReader reader, XMLStreamWriter writer)
        throws XMLStreamException {
      for (int i = 0; i < reader.getAttributeCount(); i++) {
        writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
      }
    }
  }

  /**
   * 後段コマンド: XMLをそのまま出力しながら、{@link PipelineSignal.Skip} シグナルを確認する。
   *
   * <p>前段がフィルタを適用した場合（非表示明細を除去した場合）、 Skipシグナルを受け取ることで「フィルタが発生した」という事実を検知し、
   * 監査ログを出力する。シグナルは消費されないため、最初に確認したタイミングで一度だけ処理する。
   */
  static class FilterAwareAuditCommand extends AbstractStreamCommand {

    private final SignalChannel channel;

    FilterAwareAuditCommand(SignalChannel channel) {
      this.channel = channel;
    }

    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
      byte[] buf = new byte[8192];
      int len;
      boolean auditLogged = false;
      while ((len = inputStream.read(buf)) != -1) {
        if (!auditLogged) {
          channel
              .poll()
              .ifPresent(
                  signal -> {
                    switch (signal) {
                      case PipelineSignal.Skip s ->
                          log.info("[AUDIT] Filter was applied upstream: {}", s.reason());
                    }
                  });
          if (channel.poll().isPresent()) {
            auditLogged = true;
          }
        }
        outputStream.write(buf, 0, len);
      }
    }
  }
}
