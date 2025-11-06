package com.streamconverter.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.streamconverter.context.ExecutionContext;
import com.streamconverter.context.ExecutionContextHolder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;
import org.slf4j.Marker;

/**
 * ExecutionContextからMDCに値を自動設定するLogback TurboFilter
 *
 * <p>このフィルターは、ログ出力の都度ExecutionContextHolderから値を取得し、 MDCに自動的に設定します。これにより、アプリケーションコードは
 * MDC同期を意識する必要がありません。
 *
 * <p><b>設定例（logback.xml）:</b>
 *
 * <pre>{@code
 * <configuration>
 *   <turboFilter class="com.streamconverter.logging.ExecutionContextTurboFilter"/>
 *
 *   <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
 *     <encoder>
 *       <pattern>%d{HH:mm:ss.SSS} [userId:%X{userId}] - %msg%n</pattern>
 *     </encoder>
 *   </appender>
 * </configuration>
 * }</pre>
 *
 * <p><b>動作:</b>
 *
 * <ol>
 *   <li>ログ出力の都度、このフィルターのdecide()メソッドが呼ばれる
 *   <li>ExecutionContextHolderから現在のExecutionContextを取得
 *   <li>共有コンテキストの全キーについて、MDCと比較して変更がある場合のみ更新
 *   <li>削除されたキーはMDCからも削除
 * </ol>
 *
 * <p><b>パフォーマンス最適化:</b>
 *
 * <ul>
 *   <li>MDCの値と比較して、変更がある場合のみMDC.put()を呼ぶ
 *   <li>前回同期したキーをThreadLocalで追跡し、削除されたキーをクリーンアップ
 *   <li>任意の数の共有コンテキストキーに対応
 * </ul>
 *
 * <p><b>スレッドセーフ性:</b> ThreadLocalを使用しているため、スレッドごとに独立したMDC値が設定されます。
 */
public class ExecutionContextTurboFilter extends TurboFilter {

  /**
   * 各スレッドで管理している共有コンテキストのキーセット
   *
   * <p>前回同期時に存在していたキーを追跡し、削除されたキーをMDCからクリーンアップするために使用します。
   */
  private static final ThreadLocal<Set<String>> managedKeys = ThreadLocal.withInitial(HashSet::new);

  /**
   * ログイベント処理時に呼ばれ、ExecutionContextの共有コンテキストをMDCに同期します
   *
   * <p>共有コンテキストの全キーについて、現在のMDC値と比較し、変更がある場合のみ更新します。 前回存在していたが今回削除されたキーは、MDCからも削除されます。
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

    ExecutionContext context = ExecutionContextHolder.get();

    if (context != null) {
      // 共有コンテキストの全キーと値を取得
      Map<String, String> sharedContext = context.getAllSharedContext();
      Set<String> currentKeys = new HashSet<>();

      // 各キーについて、MDCと比較して変更がある場合のみ更新
      for (Map.Entry<String, String> entry : sharedContext.entrySet()) {
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
      // ExecutionContextが無い場合、管理していた全てのキーをMDCから削除
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
