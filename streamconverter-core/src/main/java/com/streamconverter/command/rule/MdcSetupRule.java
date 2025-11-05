package com.streamconverter.command.rule;

import com.streamconverter.context.ExecutionContext;
import java.util.Objects;

/**
 * MDC設定用のルール
 *
 * <p>XMLやJSONなどから抽出した値を、ExecutionContextの共有コンテキストに設定します。
 * 共有コンテキストに設定された値は、全ての並列実行中のコマンドから即座にアクセス可能になり、 applyToMDC()呼び出し時にMDCに反映されます。
 *
 * <p><b>使用例:</b>
 *
 * <pre>{@code
 * // ExecutionContextを作成
 * ExecutionContext context = ExecutionContext.create();
 *
 * // MdcSetupRuleを使用してXMLからuserIdを抽出してMDCに設定
 * IStreamCommand xmlNavigate = new XmlNavigateCommand(
 *     TreePath.of("request", "userId"),
 *     new MdcSetupRule(context, "userId")
 * );
 *
 * // パイプライン実行
 * StreamConverter.createWithContext(context, xmlNavigate, otherCommands...)
 *     .run(inputStream, outputStream);
 *
 * // 全てのコマンドのログに [userId:USER12345] が出力される
 * }</pre>
 *
 * <p><b>スレッドセーフ性:</b> このルールはスレッドセーフです。複数のスレッドから同時に呼び出されても、
 * ExecutionContextの共有コンテキスト（ConcurrentHashMap）により安全に動作します。
 */
public class MdcSetupRule implements IRule {

  private final ExecutionContext context;
  private final String mdcKey;

  /**
   * MdcSetupRuleを作成します
   *
   * @param context ExecutionContextインスタンス
   * @param mdcKey MDCキー名（共有コンテキストのキーとして使用される）
   * @throws NullPointerException contextまたはmdcKeyがnullの場合
   */
  public MdcSetupRule(ExecutionContext context, String mdcKey) {
    this.context = Objects.requireNonNull(context, "context cannot be null");
    this.mdcKey = Objects.requireNonNull(mdcKey, "mdcKey cannot be null");
  }

  /**
   * 抽出された値を共有コンテキストに設定します
   *
   * <p>このメソッドは値を変更せずそのまま返しますが、副作用として ExecutionContextの共有コンテキストに値を設定します。
   *
   * @param extractedValue 抽出された値（nullの場合は共有コンテキストから削除）
   * @return 入力値をそのまま返す
   */
  @Override
  public String apply(String extractedValue) {
    context.setSharedContext(mdcKey, extractedValue);
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
