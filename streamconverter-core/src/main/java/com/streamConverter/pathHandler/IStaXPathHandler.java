package com.streamConverter.pathHandler;

import java.util.List;

/**
 * Xpathを指定して、Xpathの階層を判定するインターフェース。
 *
 * <p>Xpathは、XMLドキュメント内のノードを選択するための言語です。 このインターフェースは、上から読み下すことを想定して、Xpathの機能を一部省略して実装します。
 */
public interface IStaXPathHandler {
  /**
   * 対象階層かどうか判定する。
   *
   * @param xpathList 判定対象のXpathリスト
   * @throws IllegalArgumentException xpathListがnullの場合
   * @return 対象Xpathで指定した階層かどうか。
   */
  boolean isTarget(List<String> xpathList);
}
