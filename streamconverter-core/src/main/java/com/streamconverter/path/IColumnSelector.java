package com.streamconverter.path;

import java.util.List;

/**
 * 表構造における列選択の契約。
 *
 * <p>ヘッダー行または列数を受け取り、対象列のインデックスリストを返す一括解決型。 木構造の反復マッチ型（{@link ITreeMatcher}）とは呼び出し契約が異なる。
 *
 * <p>実装はセル単位処理も行単位処理も許容する。現状の {@link com.streamconverter.command.impl.csv.CsvWalker} および {@link
 * com.streamconverter.command.impl.csv.CsvFilterCommand} は opencsv の制約により
 * 行単位処理を行っているが、セル単位処理が必要な場合は別ライブラリ（jackson-dataformat-csv 等） による実装に切り替えることができる。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * IColumnSelector selector = CSVPath.of("name");
 * List<Integer> indices = selector.resolve(headers);       // ヘッダーありCSV
 * List<Integer> indices = selector.resolve(totalColumns);  // ヘッダーなしCSV
 * }</pre>
 */
public interface IColumnSelector {

  /**
   * ヘッダー配列から対象列のインデックスリストを解決する。
   *
   * @param headers CSV列ヘッダー配列
   * @return 対象列インデックスのリスト（空の場合は該当列なし）
   */
  List<Integer> resolve(String[] headers);

  /**
   * ヘッダーなしCSVで列数から対象列のインデックスリストを解決する。
   *
   * @param totalColumns 総列数
   * @return 対象列インデックスのリスト（空の場合は該当列なし）
   */
  List<Integer> resolve(int totalColumns);
}
