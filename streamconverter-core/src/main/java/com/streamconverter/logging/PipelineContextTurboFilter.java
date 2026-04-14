package com.streamconverter.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.streamconverter.context.PipelineContext;
import org.slf4j.Marker;

/**
 * ログ出力直前にPipelineContextの共有値をMDCへ同期するTurboFilter。
 *
 * <p>パイプライン内のあるコマンドが {@link PipelineContext#putShared(String, String)} で設定した値を、
 * 他のコマンドスレッドのログ出力時に自動的にMDCへ反映する。
 *
 * <p>logback.xmlに以下を追加して有効化する:
 *
 * <pre>{@code
 * <turboFilter class="com.streamconverter.logging.PipelineContextTurboFilter"/>
 * }</pre>
 */
public class PipelineContextTurboFilter extends TurboFilter {

  /** Creates a new filter instance. */
  public PipelineContextTurboFilter() {}

  /**
   * ログイベント発生直前に {@link PipelineContext#syncToMDC()} を呼び出し、 パイプライン共有値を呼び出しスレッドのMDCへ反映する。
   *
   * <p>フィルタリングは行わず、常に {@link FilterReply#NEUTRAL} を返す。
   *
   * <p>このフィルタはログを発生させたスレッドと同一スレッドで実行されるため、 {@link PipelineContext#syncToMDC()}
   * によりそのスレッドのMDCのみが更新される。 これは {@link PipelineContext} が {@link ThreadLocal} ベースであるための前提条件であり、
   * TurboFilter の仕様（ログ発生スレッドでの同期実行）により保証されている。
   *
   * @param marker ログマーカー（未使用）
   * @param logger ログ出力元ロガー（未使用）
   * @param level ログレベル（未使用）
   * @param format メッセージフォーマット（未使用）
   * @param params メッセージパラメータ（未使用）
   * @param t 例外（未使用）
   * @return 常に {@link FilterReply#NEUTRAL}
   */
  @Override
  public FilterReply decide(
      Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
    PipelineContext.syncToMDC();
    return FilterReply.NEUTRAL;
  }
}
