package com.streamconverter.execution;

import com.streamconverter.command.IStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * パイプライン実行戦略を定義するインターフェース。
 *
 * <p>StreamConverter のコマンド実行方式を抽象化し、並列・逐次など異なる実行モデルを 差し替え可能にする。デフォルト実装は {@link
 * ParallelExecutionStrategy}（仮想スレッドによる並列実行）。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // 並列実行（デフォルト）
 * StreamConverter converter = StreamConverter.create(commands);
 *
 * // 逐次実行
 * StreamConverter converter = StreamConverter.create(new SequentialExecutionStrategy(), commands);
 * }</pre>
 */
public interface ExecutionStrategy {

  /**
   * コマンドリストを指定された入出力ストリームに対して実行する。
   *
   * @param commands 実行するコマンドのリスト
   * @param commandNames コマンド名のリスト（ログ・エラーメッセージ用）
   * @param inputStream 処理対象の入力ストリーム
   * @param outputStream 処理結果を書き込む出力ストリーム
   * @param bufferPolicy パイプバッファのポリシー
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   */
  void execute(
      List<IStreamCommand> commands,
      List<String> commandNames,
      InputStream inputStream,
      OutputStream outputStream,
      BufferPolicy bufferPolicy)
      throws IOException;
}
