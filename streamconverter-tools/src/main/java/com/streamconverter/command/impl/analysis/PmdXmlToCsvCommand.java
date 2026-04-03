package com.streamconverter.command.impl.analysis;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.streamconverter.command.AbstractStreamCommand;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link PmdViolation} オブジェクトのストリームを CSV 形式に変換するコマンド。
 *
 * <p>入力: {@link PmdXmlToViolationsCommand} が出力する {@link java.io.ObjectOutputStream} ストリーム
 *
 * <p>出力: ヘッダー付き CSV（Jackson CsvMapper による型安全な生成）
 */
public class PmdXmlToCsvCommand extends AbstractStreamCommand {

  private final CsvMapper csvMapper;

  /** Creates a new command instance. */
  public PmdXmlToCsvCommand() {
    this.csvMapper = new CsvMapper();
  }

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    List<PmdViolation> violations = readAll(input);
    CsvSchema schema =
        csvMapper
            .schemaFor(PmdViolation.class)
            .withHeader()
            .withColumnSeparator(',')
            .withQuoteChar('"')
            .withEscapeChar('\\')
            .withLineSeparator("\n");
    csvMapper.writer(schema).writeValue(output, violations);
  }

  private List<PmdViolation> readAll(InputStream input) throws IOException {
    List<PmdViolation> result = new ArrayList<>();
    try (ObjectInputStream ois = new ObjectInputStream(input)) {
      while (true) {
        try {
          result.add((PmdViolation) ois.readObject());
        } catch (EOFException e) {
          break;
        } catch (ClassNotFoundException | ClassCastException e) {
          throw new IOException("Unexpected object type in stream: expected PmdViolation", e);
        }
      }
    }
    return result;
  }
}
