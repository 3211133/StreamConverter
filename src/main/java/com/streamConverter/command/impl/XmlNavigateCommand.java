package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * XML変換コマンドクラス
 *
 * <p>このクラスは、XML形式のデータを変換するためのコマンドを実装します。 ストリームを使用して、XMLデータを読み込み、変換後のデータを出力します。
 * 変換対象のXPathである箇所を特定したあとに、変換処理を実行することを想定しています。
 */
public class XmlNavigateCommand extends AbstractStreamCommand {

  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method '_execute'");
  }
}
