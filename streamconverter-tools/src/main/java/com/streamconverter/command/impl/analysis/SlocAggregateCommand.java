package com.streamconverter.command.impl.analysis;

import com.streamconverter.command.AbstractStreamCommand;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ModuleSloc} オブジェクトのストリームを集約して合計行を追加するコマンド。
 *
 * <p>入力: {@link JacocoXmlToModuleSlocCommand} が出力する {@link ObjectOutputStream} ストリーム
 *
 * <p>出力: 各 {@link ModuleSloc} + 合計を表す {@link ModuleSloc}("Total", ...) を {@link ObjectOutputStream}
 * で書き出す
 */
public class SlocAggregateCommand extends AbstractStreamCommand {

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    List<ModuleSloc> modules = readAll(input);

    int totalLines = modules.stream().mapToInt(ModuleSloc::lines).sum();
    int totalCovered = modules.stream().mapToInt(ModuleSloc::covered).sum();
    int totalMissed = modules.stream().mapToInt(ModuleSloc::missed).sum();
    ModuleSloc total = new ModuleSloc("Total", totalLines, totalCovered, totalMissed);

    try (ObjectOutputStream oos = new ObjectOutputStream(output)) {
      for (ModuleSloc sloc : modules) {
        oos.writeObject(sloc);
      }
      oos.writeObject(total);
    }
  }

  private List<ModuleSloc> readAll(InputStream input) throws IOException {
    List<ModuleSloc> result = new ArrayList<>();
    try (ObjectInputStream ois = new ObjectInputStream(input)) {
      while (true) {
        try {
          result.add((ModuleSloc) ois.readObject());
        } catch (EOFException e) {
          break;
        } catch (ClassNotFoundException | ClassCastException e) {
          throw new IOException("Unexpected object type in stream: expected ModuleSloc", e);
        }
      }
    }
    return result;
  }
}
