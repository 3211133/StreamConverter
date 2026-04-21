package com.streamconverter.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 最小限のCSVPath実装
 *
 * <p>CSV列選択のパス一致判定のみに特化したシンプルな設計
 *
 * <p>複数の列セレクターをOR条件で判定する機能を提供
 *
 * <p>セレクターには次を指定できる。
 *
 * <ul>
 *   <li>列名
 *   <li>0始まりの数値インデックス
 *   <li>{@code "*"} による全列選択
 * </ul>
 *
 * <p><b>注意:</b> {@code "*"} はワイルドカードとして解釈されるため、ヘッダー名そのものが {@code "*"} の列を個別指定する用途には使えない。
 */
@SuppressWarnings("PMD.TooManyMethods")
// セレクター API は列インデックス一致・列名一致・ヘッダー配列一致の各バリアントを
// 型安全に分けて提供する必要があり、オーバーロードを統合すると呼び出し元の型安全性が失われる。
// ヘルパーメソッド（matchesSingleSelector × 2, parseAsIndex, isAllColumnsSelector 等）を
// 別クラスに抽出するとパッケージ外に公開せざるを得ず、内部実装が漏洩する。
public class CSVPath extends AbstractPath<Integer> {

  private final List<String> selectors;

  /**
   * 単一セレクターでCSVPathを作成
   *
   * @param selector 列選択子（列名、数値インデックス、または {@code "*"} による全列選択）
   * @throws IllegalArgumentException セレクターが不正な場合
   */
  private CSVPath(String selector) {
    super(selector);
    this.selectors = Collections.singletonList(selector.trim());
  }

  /**
   * 複数セレクターでCSVPathを作成（OR条件）
   *
   * @param selectorList 列選択子のリスト
   * @throws IllegalArgumentException セレクターが不正な場合
   */
  private CSVPath(List<String> selectorList) {
    super(String.join(",", selectorList));
    List<String> temp = new ArrayList<>();
    for (String sel : selectorList) {
      temp.add(sel.trim());
    }
    this.selectors = Collections.unmodifiableList(temp);
  }

  /**
   * 単一セレクターでCSVPathを作成
   *
   * @param selector 列選択子（列名、数値インデックス、または {@code "*"} による全列選択）
   * @return CSVPath instance
   * @throws IllegalArgumentException セレクターが不正な場合
   */
  public static CSVPath of(String selector) {
    if (selector == null || selector.isBlank()) {
      throw new IllegalArgumentException("CSV column selector cannot be null or empty");
    }
    return new CSVPath(selector);
  }

  /**
   * 複数セレクターでCSVPathを作成（OR条件）
   *
   * @param selectorList 列選択子のリスト
   * @return CSVPath instance
   * @throws IllegalArgumentException セレクターが不正な場合
   */
  public static CSVPath of(List<String> selectorList) {
    if (selectorList == null || selectorList.isEmpty()) {
      throw new IllegalArgumentException("Selector list cannot be null or empty");
    }
    return new CSVPath(selectorList);
  }

  /**
   * 検証・正規化処理のフック。
   *
   * <p>{@link AbstractPath#AbstractPath(String)} コンストラクタから呼び出されるが、CSVPathでは検証を ファクトリメソッド（{@link
   * #of(String)} / {@link #of(java.util.List)}）側で行うため、
   * コンストラクタ内スロー（CT_CONSTRUCTOR_THROW）を避けるためにここでは何もしない。
   *
   * @param rawSelector 生のセレクター文字列（未使用）
   */
  @Override
  protected void validateAndNormalize(String rawSelector) {
    // Validation is performed in factory methods (of(...)) to avoid CT_CONSTRUCTOR_THROW
  }

  /**
   * 指定された列インデックスがこのパスにマッチするかどうかを判定する（OR条件）。
   *
   * <p>いずれかのセレクターがインデックスに一致すれば {@code true} を返す。
   *
   * <p><b>注意:</b> このメソッドでインデックス一致として扱えるのは、数値インデックス指定セレクター（例: {@code "0"}）と全列選択 {@code "*"} のみ。
   * 列名指定セレクター （例: {@code "name"}, {@code "userId"}）は常に {@code false} を返す。 列名での一致判定には {@link
   * #matches(String[], int)} を使用すること。
   *
   * @param columnIndex 判定対象の列インデックス（0始まり）。nullまたは負値の場合はfalse
   * @return いずれかのセレクターが一致する場合true
   */
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
    if (isAllColumnsSelector(selector)) {
      return true;
    }
    int parsedIndex = parseAsIndex(selector);
    return parsedIndex >= 0 && parsedIndex == columnIndex; // 列名指定はヘッダー情報が必要
  }

  /** 単一セレクターのヘッダー一致判定 */
  private boolean matchesSingleSelector(String selector, String[] headers, int targetIndex) {
    if (isAllColumnsSelector(selector)) {
      return true;
    }
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

  private static boolean isAllColumnsSelector(String selector) {
    return "*".equals(selector);
  }

  /**
   * このパスの文字列表現を返す。
   *
   * <p>セレクターが1つの場合はその値をそのまま返し、複数の場合はカンマ区切りで結合する。
   *
   * @return セレクターの文字列表現
   */
  @Override
  public String toString() {
    if (selectors.size() == 1) {
      return selectors.get(0);
    } else {
      return String.join(",", selectors);
    }
  }
}
