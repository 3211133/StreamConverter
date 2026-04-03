package com.streamconverter.command.impl.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PmdXmlToCsvCommandTest {

  private static final PmdViolation VIOLATION_A =
      new PmdViolation(
          "streamconverter-core/Foo.java",
          10,
          "UnusedVariable",
          "Best Practices",
          3,
          "Avoid unused variables.",
          "Foo",
          "bar",
          "x");
  private static final PmdViolation VIOLATION_B =
      new PmdViolation(
          "streamconverter-db/Dao.java",
          5,
          "LongMethod",
          "Design",
          2,
          "Method too long.",
          "Dao",
          "find",
          "");

  @Test
  void outputContainsCsvHeader() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToCsvCommand().execute(serialize(List.of(VIOLATION_A)), output);

    String csv = output.toString(StandardCharsets.UTF_8);
    // Jackson CsvMapper はフィールド名をヘッダーとして出力する
    assertTrue(csv.contains("file") || csv.contains("rule"), "CSV should have headers");
  }

  @Test
  void outputContainsViolationData() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToCsvCommand().execute(serialize(List.of(VIOLATION_A, VIOLATION_B)), output);

    String csv = output.toString(StandardCharsets.UTF_8);
    assertTrue(csv.contains("UnusedVariable"));
    assertTrue(csv.contains("LongMethod"));
  }

  @Test
  void emptyInput_producesOnlyHeader() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToCsvCommand().execute(serialize(List.of()), output);

    String csv = output.toString(StandardCharsets.UTF_8);
    // ヘッダー行のみ（データ行なし）
    String[] lines = csv.strip().split("\n");
    assertEquals(1, lines.length, "should have only header line");
  }

  private ByteArrayInputStream serialize(List<PmdViolation> violations) throws Exception {
    var baos = new ByteArrayOutputStream();
    try (var oos = new ObjectOutputStream(baos)) {
      for (var v : violations) {
        oos.writeObject(v);
      }
    }
    return new ByteArrayInputStream(baos.toByteArray());
  }
}
