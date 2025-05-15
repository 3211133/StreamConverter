package com.streamConverter.pathHandler;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xpathを指定して、Xpathの階層を判定するクラス。
 *
 * <p>Xpathは、XMLドキュメント内のノードを選択するための言語です。 このクラスは、上から読み下すことを想定して、Xpathの機能を一部省略して実装します。
 *
 * <p>フルパスで指定するクラス。
 *
 * <p>例: root/child/grandchild のように、全ての階層を指定する必要があります。
 *
 * <p>両端に/がある場合は、エラーをスローします。
 *
 * <p>hoge//fugaのように空要素がある場合は、エラーをスローします。
 */
public class FixedStaXPathHandler implements IStaXPathHandler {
  // log
  private static final Logger log = LoggerFactory.getLogger(FixedStaXPathHandler.class);

  private List<String> targetXpath;

  public FixedStaXPathHandler(String xpath) {
    if (xpath == null) {
      throw new IllegalArgumentException("xpath must not be null");
    }
    this.targetXpath = List.of(xpath.split("/"));
    // 空要素がある場合は、エラーをスローする
    if (this.targetXpath.contains("")) {
      throw new IllegalArgumentException("xpath must not contain empty segments");
    }
  }

  /**
   * 対象階層かどうか判定する。 引数のXpathが、対象Xpathと同じ階層かどうかを完全一致で判定します。
   *
   * @param xpathList 判定対象のXpathリスト
   * @throws IllegalArgumentException xpathListがnullの場合
   * @return 対象Xpathで指定した階層かどうか。
   */
  @Override
  public boolean isTarget(List<String> xpathList) {
    if (xpathList == null) {
      throw new IllegalArgumentException("xpathList must not be null");
    }
    if (xpathList.size() != targetXpath.size()) {
      return false;
    }
    for (int i = 0; i < xpathList.size(); i++) {
      if (!xpathList.get(i).equals(targetXpath.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Xpathを取得します。
   *
   * @return Xpathの文字列
   */
  public List<String> getTargetXpath() {
    return this.targetXpath;
  }
}
