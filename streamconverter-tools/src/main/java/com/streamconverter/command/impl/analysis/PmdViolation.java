package com.streamconverter.command.impl.analysis;

import java.io.Serial;
import java.io.Serializable;

/**
 * PMD 違反情報。
 *
 * <p>パイプライン内コマンド間の中間データとして {@link java.io.ObjectOutputStream} / {@link java.io.ObjectInputStream}
 * で受け渡しされる。
 *
 * @param file ファイルパス（プロジェクトルートからの相対パス）
 * @param line 違反行番号
 * @param rule ルール名
 * @param ruleset ルールセット名
 * @param priority 優先度（1=高〜5=低）
 * @param description 違反の説明
 * @param className クラス名
 * @param method メソッド名
 * @param variable 変数名
 */
public record PmdViolation(
    String file,
    int line,
    String rule,
    String ruleset,
    int priority,
    String description,
    String className,
    String method,
    String variable)
    implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
