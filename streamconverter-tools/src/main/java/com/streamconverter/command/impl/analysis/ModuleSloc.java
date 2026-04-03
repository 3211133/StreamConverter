package com.streamconverter.command.impl.analysis;

import java.io.Serial;
import java.io.Serializable;

/**
 * モジュールごとの SLOC（実行可能行数）情報。
 *
 * <p>パイプライン内コマンド間の中間データとして {@link java.io.ObjectOutputStream} / {@link java.io.ObjectInputStream}
 * で受け渡しされる。
 *
 * @param name モジュール名
 * @param lines 総行数（missed + covered）
 * @param covered カバー済み行数
 * @param missed 未カバー行数
 */
public record ModuleSloc(String name, int lines, int covered, int missed) implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
