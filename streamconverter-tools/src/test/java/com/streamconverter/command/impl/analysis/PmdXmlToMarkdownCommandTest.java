package com.streamconverter.command.impl.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PmdXmlToMarkdownCommandTest {

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
  void outputContainsHeader() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToMarkdownCommand().execute(serialize(List.of(VIOLATION_A, VIOLATION_B)), output);

    String report = output.toString(StandardCharsets.UTF_8);
    assertTrue(report.contains("# PMD Code Quality Analysis Report"));
    assertTrue(report.contains("Total Violations**: 2"));
  }

  @Test
  void outputContainsRuleNames() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToMarkdownCommand().execute(serialize(List.of(VIOLATION_A, VIOLATION_B)), output);

    String report = output.toString(StandardCharsets.UTF_8);
    assertTrue(report.contains("UnusedVariable"));
    assertTrue(report.contains("LongMethod"));
  }

  @Test
  void emptyInput_producesEmptyReport() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToMarkdownCommand().execute(serialize(List.of()), output);

    String report = output.toString(StandardCharsets.UTF_8);
    assertTrue(report.contains("Total Violations**: 0"));
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
