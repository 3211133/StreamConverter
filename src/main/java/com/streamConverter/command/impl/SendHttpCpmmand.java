/**
 * Copyright (c) 2023, Stream Converter Project All rights reserved.
 * 指定された通信先にOutputStreamを送信するコマンドクラス。
 *
 * <p>このクラスは、指定された通信先にOutputStreamを送信するためのコマンドを実装します。 ストリームを使用して、データを送信します。 送信先のURLはコンストラクタで指定されます。
 * 送信先のURLは、HTTP POSTリクエストを使用してデータを送信します。 送信先のURLは、HTTPまたはHTTPSで始まる必要があります。
 * 送信先のURLは、コンストラクタで指定されたURLに基づいて決定されます。
 *
 * <p>このクラスは、ストリーム変換のコマンドを実装するための抽象クラスを拡張しています。 ストリーム変換のコマンドは、ストリームを使用してデータを変換するためのものです。
 *
 * <p>レスポンスを受信してOutputStreamに書き込むことができます。
 */
package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SendHttpCpmmand extends AbstractStreamCommand {

  private String url;

  /**
   * デフォルトコンストラクタ
   *
   * @param url 送信先のURL
   */
  public SendHttpCpmmand(String url) {
    super();
    this.url = url;
  }

  /**
   * ストリームを指定されたURLに送信します。
   *
   * @param inputStream 入力ストリーム
   * @param outputStream 出力ストリーム
   * @throws IOException 入出力エラーが発生した場合
   */
  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    // HTTP POSTリクエストを使用してデータを送信する処理を実装する
    // ここでは、指定されたURLにデータを送信するためのロジックを実装します。

    // 通信を確立する

  }
}
