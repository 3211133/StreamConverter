package com.streamconverter.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;
import org.slf4j.Marker;

/**
 * MDCContextからMDCに値を自動設定するLogback TurboFilter
 *
 * <p>このフィルターは、ログ出力の都度MDCContextから値を取得し、MDCに自動的に設定します。 これにより、アプリケーションコードはMDC同期を意識する必要がありません。
 *
 * <p><b>設定例（logback.xml）:</b>
 *
 * <pre>{@code
 * <configuration>
 *   <turboFilter class="com.streamconverter.logging.MDCTurboFilter"/>
 *
 *   <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
 *     <encoder>
 *       <pattern>%d{HH:mm:ss.SSS} [userId:%X{userId}] - %msg%n</pattern>
 *     </encoder>
 *   </appender>
 * </configuration>
 * }</pre>
 *
 * <p><b>動作フロー:</b>
 *
 * <ol>
 *   <li>ログ出力の都度、このフィルターのdecide()メソッドが呼ばれる
 *   <li>MDCContextから現在のスレッドのMDC値を取得
 *   <li>各キーについて、MDCと比較して変更がある場合のみ更新
 *   <li>削除されたキーはMDCからも削除
 * </ol>
 *
 * <p><b>パフォーマンス最適化:</b>
 *
 * <ul>
 *   <li>MDCの値と比較して、変更がある場合のみMDC.put()を呼ぶ
 *   <li>前回同期したキーをThreadLocalで追跡し、削除されたキーをクリーンアップ
 *   <li>任意の数のMDCキーに対応
 * </ul>
 *
 * <p><b>ExecutionContextTurboFilterとの違い:</b>
 *
 * <ul>
 *   <li>MDCTurboFilter: MDCContextから取得（シンプル）、依存1つ
 *   <li>ExecutionContextTurboFilter: ExecutionContextから取得（複雑）、依存2つ
 * </ul>
 *
 * <p><b>スレッドセーフ性:</b> ThreadLocalを使用しているため、スレッドごとに独立したMDC値が設定されます。
 *
 * @see MDCContext
 * @since 1.0
 */
public class MDCTurboFilter extends TurboFilter {

  /**
   * 各スレッドで管理しているMDCキーセット
   *
   * <p>前回同期時に存在していたキーを追跡し、削除されたキーをMDCからクリーンアップするために使用します。
   */
  private static final ThreadLocal<Set<String>> managedKeys = ThreadLocal.withInitial(HashSet::new);

  /** Default constructor. */
  public MDCTurboFilter() {
    super();
  }

  /**
   * ログイベント処理時に呼ばれ、MDCContextの値をMDCに同期します
   *
   * <p>MDCContextの全キーについて、現在のMDC値と比較し、変更がある場合のみ更新します。 前回存在していたが今回削除されたキーは、MDCからも削除されます。
   *
   * <p><b>パフォーマンス最適化:</b> MDC.get()で現在の値を取得し、equals()で比較することで、
   * 変更がない場合はMDC.put()をスキップします。これにより、頻繁なログ出力でもパフォーマンスへの影響を最小限に抑えます。
   *
   * @param marker マーカー
   * @param logger ロガー
   * @param level ログレベル
   * @param format メッセージフォーマット
   * @param params パラメータ
   * @param t 例外
   * @return FilterReply.NEUTRAL（フィルタリングは行わず、常にログを通過させる）
   */
  @Override
  public FilterReply decide(
      Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {

    // MDCContextから現在のスレッドの値を取得
    Map<String, String> mdcValues = MDCContext.get();

    if (mdcValues != null && !mdcValues.isEmpty()) {
      Set<String> currentKeys = new HashSet<>();

      // 各キーについて、MDCと比較して変更がある場合のみ更新
      for (Map.Entry<String, String> entry : mdcValues.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        currentKeys.add(key);

        String currentValue = MDC.get(key);
        if (!value.equals(currentValue)) {
          MDC.put(key, value);
        }
      }

      // 前回同期時に存在していたが、今回削除されたキーをMDCから削除
      Set<String> previousKeys = managedKeys.get();
      for (String key : previousKeys) {
        if (!currentKeys.contains(key)) {
          MDC.remove(key);
        }
      }

      // 今回のキーセットを保存
      managedKeys.set(currentKeys);

    } else {
      // MDCContextが空の場合、管理していた全てのキーをMDCから削除
      Set<String> previousKeys = managedKeys.get();
      for (String key : previousKeys) {
        MDC.remove(key);
      }
      // ThreadLocalをクリアしてメモリリークを防止
      managedKeys.remove();
    }

    // フィルタリングは行わず、常にログを通過させる
    return FilterReply.NEUTRAL;
  }
}
