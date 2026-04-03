package com.streamconverter.command.impl.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.streamconverter.command.AbstractStreamCommand;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link PmdViolation} オブジェクトのストリームを JSON 形式に変換するコマンド。
 *
 * <p>入力: {@link PmdXmlToViolationsCommand} が出力する {@link java.io.ObjectOutputStream} ストリーム
 *
 * <p>出力: Jackson によるプリティプリント JSON（サマリー + 違反一覧）
 */
public class PmdXmlToJsonCommand extends AbstractStreamCommand {

  private final ObjectMapper objectMapper;

  /** Creates a new command instance. */
  public PmdXmlToJsonCommand() {
    this.objectMapper =
        new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    List<PmdViolation> violations = readAll(input);
    PmdJsonReport report =
        new PmdJsonReport(
            new PmdJsonSummary(violations.size(), Instant.now().toString()), violations);
    objectMapper.writeValue(output, report);
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

  /**
   * JSON レポートのルートオブジェクト。
   *
   * @param summary サマリー情報
   * @param violations 違反一覧
   */
  public record PmdJsonReport(PmdJsonSummary summary, List<PmdViolation> violations) {
    public PmdJsonReport {
      violations = List.copyOf(violations);
    }
  }

  /**
   * JSON レポートのサマリー情報。
   *
   * @param totalViolations 総違反数
   * @param generatedAt 生成日時（ISO-8601）
   */
  public record PmdJsonSummary(int totalViolations, String generatedAt) {}
}
