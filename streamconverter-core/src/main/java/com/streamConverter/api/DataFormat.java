package com.streamConverter.api;

/**
 * データ形式を表すenum
 *
 * <p>StreamBuilderで処理するデータの形式を指定し、 適切なコマンドの選択とバリデーションを行います。
 */
public enum DataFormat {
  /** 汎用データ形式（形式指定なし） */
  GENERIC("generic"),

  /** JSON形式 */
  JSON("json"),

  /** CSV形式 */
  CSV("csv"),

  /** XML形式 */
  XML("xml");

  private final String name;

  DataFormat(String name) {
    this.name = name;
  }

  /**
   * データ形式名を取得
   *
   * @return データ形式名
   */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
