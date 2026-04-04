package com.streamconverter.sloc.command;

import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.sloc.ModuleSloc;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * {@link ModuleSloc} オブジェクトのストリームを集約して合計行を追加するコマンド。
 *
 * <p>入力: {@link JacocoXmlToModuleSlocCommand} が出力する {@link ObjectOutputStream} ストリーム
 *
 * <p>出力: 各 {@link ModuleSloc} + 合計を表す {@link ModuleSloc}("Total", ...) を {@link ObjectOutputStream}
 * で書き出す
 *
 * <p>1件ずつ読みながら即座に書き出し、カウントのみ累積するためモジュール数によらずメモリ使用量は O(1)。
 *
 * <p><b>セキュリティ前提:</b> 入力ストリームは同一 JVM 内の前段コマンド（{@link
 * JacocoXmlToModuleSlocCommand}）が書き出したものであり、外部入力を直接受け取らない。予期しない型が 含まれる場合は {@link
 * ClassNotFoundException} / {@link ClassCastException} を {@link java.io.IOException} に変換して伝播する。
 */
public class SlocAggregateCommand extends AbstractStreamCommand {

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    int totalLines = 0;
    int totalCovered = 0;
    int totalMissed = 0;

    try (ObjectInputStream ois = new ObjectInputStream(input);
        ObjectOutputStream oos = new ObjectOutputStream(output)) {
      while (true) {
        try {
          ModuleSloc sloc = (ModuleSloc) ois.readObject();
          oos.writeObject(sloc);
          totalLines += sloc.lines();
          totalCovered += sloc.covered();
          totalMissed += sloc.missed();
        } catch (EOFException e) {
          break;
        } catch (ClassNotFoundException | ClassCastException e) {
          throw new IOException("Unexpected object type in stream: expected ModuleSloc", e);
        }
      }
      oos.writeObject(new ModuleSloc("Total", totalLines, totalCovered, totalMissed));
    }
  }
}
