package com.streamconverter.command.rule;

import com.streamconverter.logging.MDCContext;
import java.util.Objects;
import org.slf4j.MDC;

/**
 * MDC設定用のルール
 *
 * <p>XMLやJSONなどから抽出した値を、MDCContextに設定します。 MDCContextに設定された値は、現在のスレッドのログに自動的に反映されます。
 *
 * <p><b>使用例:</b>
 *
 * <pre>{@code
 * // MdcSetupRuleを使用してXMLからuserIdを抽出してMDCに設定
 * IStreamCommand xmlNavigate = new XmlNavigateCommand(
 *     TreePath.of("request", "userId"),
 *     new MdcSetupRule("userId")
 * );
 *
 * // パイプライン実行
 * StreamConverter.create(xmlNavigate, otherCommands...)
 *     .run(inputStream, outputStream);
 *
 * // コマンドのログに [userId:USER12345] が出力される
 * }</pre>
 *
 * <p><b>スレッドセーフ性:</b> このルールはスレッドセーフです。複数のスレッドから同時に呼び出されても、 MDCContextのThreadLocalにより安全に動作します。
 */
public class MdcSetupRule implements IRule {

  private final String mdcKey;

  /**
   * MdcSetupRuleを作成します
   *
   * @param mdcKey MDCキー名
   * @throws NullPointerException mdcKeyがnullの場合
   */
  public MdcSetupRule(String mdcKey) {
    this.mdcKey = Objects.requireNonNull(mdcKey, "mdcKey cannot be null");
  }

  /**
   * 抽出された値をMDCに設定します
   *
   * <p>このメソッドは値を変更せずそのまま返しますが、副作用としてMDCに値を設定します。
   *
   * <p><b>マルチスレッド対応:</b>
   *
   * <ul>
   *   <li>MDC.put() - 現在のスレッドのMDCに即座に設定（InheritableThreadLocalにより子スレッドに伝播）
   *   <li>MDCContext.putShared() - 共有コンテキストに設定（TurboFilterを経由して他のスレッドでも参照可能）
   * </ul>
   *
   * @param extractedValue 抽出された値（nullの場合はMDCから削除）
   * @return 入力値をそのまま返す
   */
  @Override
  public String apply(String extractedValue) {
    if (extractedValue == null) {
      MDC.remove(mdcKey);
      MDCContext.removeShared(mdcKey);
    } else {
      MDC.put(mdcKey, extractedValue);
      MDCContext.putShared(mdcKey, extractedValue);
    }
    return extractedValue;
  }

  /**
   * このルールが使用するMDCキー名を取得します
   *
   * @return MDCキー名
   */
  public String getMdcKey() {
    return mdcKey;
  }

  @Override
  public String toString() {
    return String.format("MdcSetupRule{mdcKey='%s'}", mdcKey);
  }
}
