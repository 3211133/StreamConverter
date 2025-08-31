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

  private final List<String> selectors;

  /**
   * 単一セレクターでCSVPathを作成
   *
   * @param selector 列選択子（列名または数値インデックス）
   * @throws IllegalArgumentException セレクターが不正な場合
   */
  public CSVPath(String selector) {
    super(selector);
    this.selectors = Collections.singletonList(selector.trim());
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
    List<String> temp = new ArrayList<>();
    for (String sel : selectorList) {
      temp.add(sel.trim());
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
    if (columnIndex == null || columnIndex < 0) {
      return false;
    }

    // OR条件：いずれかのセレクターがマッチすればtrue
    for (String selector : selectors) {
      if (matchesSingleSelector(selector, columnIndex)) {
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
    if (headers == null || targetIndex < 0 || targetIndex >= headers.length) {
      return false;
    }

    // OR条件：いずれかのセレクターがマッチすればtrue
    for (String selector : selectors) {
      if (matchesSingleSelector(selector, headers, targetIndex)) {
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

  /** 単一セレクターの列インデックス一致判定 */
  private boolean matchesSingleSelector(String selector, Integer columnIndex) {
    int parsedIndex = parseAsIndex(selector);
    if (parsedIndex >= 0) {
      return parsedIndex == columnIndex;
    }
    return false; // 列名指定はヘッダー情報が必要
  }

  /** 単一セレクターのヘッダー一致判定 */
  private boolean matchesSingleSelector(String selector, String[] headers, int targetIndex) {
    int parsedIndex = parseAsIndex(selector);
    if (parsedIndex >= 0) {
      // インデックス指定の場合
      return parsedIndex == targetIndex;
    } else {
      // 列名指定の場合
      return headers[targetIndex].trim().equalsIgnoreCase(selector.trim());
    }
  }

  /** 文字列が数値インデックスかどうかを判定 */
  private static int parseAsIndex(String selector) {
    if (selector == null || selector.isEmpty()) {
      return -1;
    }
    try {
      int index = Integer.parseInt(selector.trim());
      return index >= 0 ? index : -1;
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  @Override
  public String toString() {
    if (selectors.size() == 1) {
      return selectors.get(0);
    } else {
      return String.join(",", selectors);
    }
  }
}
