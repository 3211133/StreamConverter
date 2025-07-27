package com.streamConverter.command;

import com.streamConverter.context.ExecutionContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * コンテキスト対応のストリームコマンドインターフェース
 *
 * <p>このインターフェースを実装することで、ExecutionContextを受け取り、 マルチスレッド環境でのMDCコンテキスト伝播を実現できます。
 */
public interface IContextAwareStreamCommand extends IStreamCommand {

  /**
   * ExecutionContextを使用してコマンドを実行します
   *
   * @param inputStream 入力ストリーム
   * @param outputStream 出力ストリーム
   * @param context 実行コンテキスト
   * @throws IOException I/Oエラーが発生した場合
   */
  void execute(InputStream inputStream, OutputStream outputStream, ExecutionContext context)
      throws IOException;

  /** 既存のIStreamCommandインターフェースとの互換性のためのデフォルト実装 コンテキストなしで呼び出された場合は新しいコンテキストを作成します */
  @Override
  default void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    ExecutionContext context = ExecutionContext.create();
    execute(inputStream, outputStream, context);
  }
}
