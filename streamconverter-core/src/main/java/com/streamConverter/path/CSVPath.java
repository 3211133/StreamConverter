package com.streamConverter.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 最小限のCSVPath実装
 *
 * <p>CSV列選択のパス一致判定のみに特化したシンプルな設計
 *
 * <p>複数の列セレクターをOR条件で判定する機能を提供
 */
public class CSVPath extends AbstractPath<Integer> {

  private final List<SingleSelector> selectors;

  /** 内部セレクター表現 */
  private static class SingleSelector {
    private final String selector;
    private final boolean isIndex;
    private final int columnIndex;

    SingleSelector(String selector) {
      this.selector = selector.trim();
      int parsedIndex = parseAsIndex(this.selector);
      if (parsedIndex >= 0) {
        this.isIndex = true;
        this.columnIndex = parsedIndex;
      } else {
        this.isIndex = false;
        this.columnIndex = -1;
      }
    }

    private static int parseAsIndex(String selector) {
      if (selector == null || selector.isEmpty()) {
        return -1;
      }
      try {
        int index = Integer.parseInt(selector);
        return index >= 0 ? index : -1;
      } catch (NumberFormatException e) {
        return -1;
      }
    }

    boolean matches(Integer columnIndex) {
      if (columnIndex == null || columnIndex < 0) {
        return false;
      }
      if (isIndex) {
        return this.columnIndex == columnIndex;
      }
      return false;
    }

    boolean matches(String[] headers, int targetIndex) {
      if (headers == null || targetIndex < 0 || targetIndex >= headers.length) {
        return false;
      }
      if (isIndex) {
        return this.columnIndex == targetIndex;
      } else {
        return headers[targetIndex].trim().equalsIgnoreCase(selector.trim());
      }
    }

    @Override
    public String toString() {
      return selector;
    }
  }

  /**
   * 単一セレクターでCSVPathを作成
   *
   * @param selector 列選択子（列名または数値インデックス）
   * @throws IllegalArgumentException セレクターが不正な場合
   */
  public CSVPath(String selector) {
    super(selector);
    this.selectors = Collections.singletonList(new SingleSelector(selector));
  }

  /**
   * 複数セレクターでCSVPathを作成（OR条件）
   *
   * @param selectorList 列選択子のリスト
   * @throws IllegalArgumentException セレクターが不正な場合
   */
  public CSVPath(List<String> selectorList) {
    super(String.join(",", selectorList));
    if (selectorList == null || selectorList.isEmpty()) {
      throw new IllegalArgumentException("Selector list cannot be null or empty");
    }
    List<SingleSelector> temp = new ArrayList<>();
    for (String sel : selectorList) {
      temp.add(new SingleSelector(sel));
    }
    this.selectors = Collections.unmodifiableList(temp);
  }

  @Override
  protected void validateAndNormalize(String rawSelector) {
    if (isNullOrEmpty(rawSelector)) {
      throw new IllegalArgumentException("CSV column selector cannot be null or empty");
    }
  }

  @Override
  public boolean matches(Integer columnIndex) {
    // OR条件：いずれかのセレクターがマッチすればtrue
    for (SingleSelector selector : selectors) {
      if (selector.matches(columnIndex)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 列ヘッダー配列との一致判定（OR条件）
   *
   * @param headers CSV列ヘッダー配列
   * @param targetIndex 対象列のインデックス
   * @return いずれかのセレクターが一致する場合true
   */
  public boolean matches(String[] headers, int targetIndex) {
    // OR条件：いずれかのセレクターがマッチすればtrue
    for (SingleSelector selector : selectors) {
      if (selector.matches(headers, targetIndex)) {
        return true;
      }
    }
    return false;
  }

  /**
   * マッチするすべての列インデックスを取得（Don't Ask Tell準拠）
   *
   * @param headers CSV列ヘッダー配列
   * @return マッチした列インデックスのリスト
   */
  public List<Integer> findMatchingIndices(String[] headers) {
    List<Integer> matchingIndices = new ArrayList<>();
    if (headers == null) {
      return matchingIndices;
    }

    for (int i = 0; i < headers.length; i++) {
      if (matches(headers, i)) {
        matchingIndices.add(i);
      }
    }
    return matchingIndices;
  }

  /**
   * マッチするすべての列インデックスを取得（ヘッダーなしの場合）
   *
   * @param totalColumns 総列数
   * @return マッチした列インデックスのリスト
   */
  public List<Integer> findMatchingIndices(int totalColumns) {
    List<Integer> matchingIndices = new ArrayList<>();

    for (int i = 0; i < totalColumns; i++) {
      if (matches(i)) {
        matchingIndices.add(i);
      }
    }
    return matchingIndices;
  }

  @Override
  public String toString() {
    if (selectors.size() == 1) {
      return selectors.get(0).toString();
    } else {
      return selectors.stream()
          .map(SingleSelector::toString)
          .collect(java.util.stream.Collectors.joining(","));
    }
  }
}
