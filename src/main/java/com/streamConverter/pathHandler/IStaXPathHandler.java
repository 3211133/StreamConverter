package com.streamConverter.pathHandler;

import java.util.List;

public interface IStaXPathHandler {
  /**
   * 対象階層かどうか判定する。
   *
   * @return 対象Xpathで指定した階層かどうか。
   */
  boolean isTarget(List<String> xpathList);
}
