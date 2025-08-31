package com.streamConverter.path;

/**
 * 最小限のCSVPath実装
 *
 * <p>CSV列選択のパス一致判定のみに特化したシンプルな設計
 */
public class CSVPath extends AbstractPath<Integer> {

  private final String selector;
  private final boolean isIndex;
  private final int columnIndex;

  /**
   * CSVPathを作成
   *
   * @param selector 列選択子（列名または数値インデックス）
   * @throws IllegalArgumentException セレクターが不正な場合
   */
  public CSVPath(String selector) {
    super(selector);
    this.selector = selector.trim();

    // インデックスかどうかを判定
    int parsedIndex = parseAsIndex(this.selector);
    if (parsedIndex >= 0) {
      this.isIndex = true;
      this.columnIndex = parsedIndex;
    } else {
      this.isIndex = false;
      this.columnIndex = -1;
    }
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

    // インデックス指定の場合は直接比較
    if (isIndex) {
      return this.columnIndex == columnIndex;
    }

    // 列名指定の場合は実際の列ヘッダーと比較が必要
    // この簡素化版では列名での一致判定は NavigateCommand側で行う
    return false;
  }

  /** 文字列が数値インデックスかどうかを判定 */
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

  @Override
  public String toString() {
    return selector;
  }
}
