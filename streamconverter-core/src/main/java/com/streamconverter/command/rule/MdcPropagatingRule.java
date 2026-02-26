package com.streamconverter.command.rule;

import com.streamconverter.context.PipelineContext;

/**
 * 変換結果をMDC共有値として伝搬するルール。
 *
 * <p>ストリームデータから抽出された値を、パイプライン内の全コマンドのログ出力に 自動反映するために使用する。値はパススルーされ、変換は行わない。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // XMLの orderId を抽出してMDCに伝搬
 * XmlNavigateCommand command = XmlNavigateCommand.create(
 *     TreePath.fromXPath("/order/@id"),
 *     new MdcPropagatingRule("orderId")
 * );
 * }</pre>
 */
public final class MdcPropagatingRule implements IRule {

  private final String mdcKey;

  /**
   * 指定されたMDCキーに値を伝搬するルールを作成する。
   *
   * @param mdcKey MDCに設定するキー名
   * @throws IllegalArgumentException mdcKeyがnullの場合
   */
  public MdcPropagatingRule(String mdcKey) {
    if (mdcKey == null) {
      throw new IllegalArgumentException("mdcKey must not be null");
    }
    this.mdcKey = mdcKey;
  }

  /**
   * 入力値をMDC共有値として設定し、そのまま返す。
   *
   * @param input 変換対象の文字列
   * @return 入力値をそのまま返す
   * @throws IllegalArgumentException inputがnullの場合
   */
  @Override
  public String apply(String input) {
    if (input == null) {
      throw new IllegalArgumentException("Input cannot be null");
    }
    PipelineContext.putShared(mdcKey, input);
    return input;
  }

  @Override
  public String toString() {
    return "MdcPropagatingRule{mdcKey='" + mdcKey + "'}";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof MdcPropagatingRule other)) return false;
    return mdcKey.equals(other.mdcKey);
  }

  @Override
  public int hashCode() {
    return mdcKey.hashCode();
  }
}
