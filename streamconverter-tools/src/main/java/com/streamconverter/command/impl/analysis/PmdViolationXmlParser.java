package com.streamconverter.command.impl.analysis;

import com.streamconverter.security.SecureXmlConfiguration;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * PMD XML レポートを {@link PmdViolation} リストに変換する共有パーサー。
 *
 * <p>StreamConverter パイプラインとスタンドアロン CLI の両方から利用される、パイプライン非依存のユーティリティ。
 */
public class PmdViolationXmlParser {

  private PmdViolationXmlParser() {}

  /**
   * PMD XML 入力ストリームを解析して {@link PmdViolation} リストを返す。
   *
   * @param input PMD XML レポートの入力ストリーム
   * @return 解析された違反情報のリスト
   * @throws Exception XML 解析エラーの場合
   */
  public static List<PmdViolation> parse(InputStream input) throws Exception {
    DocumentBuilder builder = SecureXmlConfiguration.createSecureDocumentBuilderForStream(input);
    Document doc = builder.parse(input);

    NodeList fileNodes = doc.getElementsByTagName("file");
    List<PmdViolation> violations = new ArrayList<>();

    for (int i = 0; i < fileNodes.getLength(); i++) {
      Element fileElement = (Element) fileNodes.item(i);
      String fileName = fileElement.getAttribute("name");

      NodeList violationNodes = fileElement.getElementsByTagName("violation");
      for (int j = 0; j < violationNodes.getLength(); j++) {
        Element v = (Element) violationNodes.item(j);
        violations.add(
            new PmdViolation(
                extractRelativePath(fileName),
                Integer.parseInt(v.getAttribute("beginline")),
                v.getAttribute("rule"),
                v.getAttribute("ruleset"),
                Integer.parseInt(v.getAttribute("priority")),
                v.getTextContent().trim(),
                v.getAttribute("class"),
                v.getAttribute("method"),
                v.getAttribute("variable")));
      }
    }
    return violations;
  }

  /**
   * フルパスから StreamConverter プロジェクト内の相対パスを抽出する。
   *
   * @param fullPath 絶対パス
   * @return {@code streamconverter-} 以降の相対パス、見つからない場合は元のパスをそのまま返す
   */
  static String extractRelativePath(String fullPath) {
    int index = fullPath.indexOf("streamconverter-");
    return index != -1 ? fullPath.substring(index) : fullPath;
  }
}
