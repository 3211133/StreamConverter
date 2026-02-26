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

  @Override
  public FilterReply decide(
      Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
    PipelineContext.syncToMDC();
    return FilterReply.NEUTRAL;
  }
}
