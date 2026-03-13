package com.streamconverter.command.rule;

import com.streamconverter.context.PipelineContext;

/**
 * 変換結果をMDC共有値として伝搬するルール。
 *
 * <p>ストリームデータから抽出された値を、パイプライン内の全コマンドのログ出力に 自動反映するために使用する。値はパススルーされ、変換は行わない。
 *
 * <p>{@code MDC.put} だけのルールとの違い:
 *
 * <ol>
 *   <li><b>後続コマンドへの伝搬</b> — 値を {@link com.streamconverter.context.PipelineContext}
 *       の共有値として格納し、{@link com.streamconverter.logging.PipelineContextTurboFilter
 *       PipelineContextTurboFilter} が他コマンドのログ出力直前に {@code syncToMDC()} で自動反映する。{@code MDC.put}
 *       だけでは呼び出しスレッド内しか効かない。
 *   <li><b>実行タイミング非依存</b> — 子スレッド起動後に値を書いても {@code PipelineContext} 経由なら {@code TurboFilter}
 *       が毎ログ前に同期するため順序非依存。
 *   <li><b>スレッドからのコンテキスト切り離し</b> — {@code PipelineContext.clear()} により、このスレッドとの関連付けを解除できる。
 * </ol>
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // XMLの orderId を抽出してMDCに伝搬
 * XmlNavigateCommand command = XmlNavigateCommand.create(
 *     TreePath.fromXPath("/order/@id"),
 *     MdcPropagatingRule.create("orderId")
 * );
 * }</pre>
 */
public final class MdcPropagatingRule implements IRule {

  private final String mdcKey;

  private MdcPropagatingRule(String mdcKey) {
    this.mdcKey = mdcKey;
  }

  /**
   * 指定されたキーにストリーム値を伝搬するルールを作成します。
   *
   * <p>使用例:
   *
   * <pre>{@code
   * XmlNavigateCommand.create(
   *     TreePath.fromXml("request/userId"),
   *     MdcPropagatingRule.create("userId"));
   * }</pre>
   *
   * @param mdcKey MDC／PipelineContext に格納するキー名
   * @return 伝搬ルールのインスタンス
   * @throws IllegalArgumentException mdcKey が null または空の場合
   */
  public static MdcPropagatingRule create(String mdcKey) {
    if (mdcKey == null || mdcKey.isEmpty()) {
      throw new IllegalArgumentException("mdcKey cannot be null or empty");
    }
    return new MdcPropagatingRule(mdcKey);
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
