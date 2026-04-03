package com.streamconverter.command.impl.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlocReportFormatCommandTest {

  @Test
  void formatsReportWithSeparatorBeforeTotal() throws Exception {
    var input =
        serialize(
            List.of(
                new ModuleSloc("streamconverter-core", 1869, 1500, 369),
                new ModuleSloc("streamconverter-db", 349, 300, 49),
                new ModuleSloc("Total", 2218, 1800, 418)));
    var output = new ByteArrayOutputStream();

    new SlocReportFormatCommand().execute(new ByteArrayInputStream(input), output);

    String report = output.toString();
    assertTrue(report.contains("=== SLOC Report ==="));
    assertTrue(report.contains("streamconverter-core"));
    assertTrue(report.contains("1,869"));
    assertTrue(report.contains("streamconverter-db"));
    assertTrue(report.contains("349"));
    assertTrue(report.contains("Total"));
    assertTrue(report.contains("2,218"));
    // セパレーター行が Total の前に存在する
    int separatorIndex = report.indexOf("─");
    int totalIndex = report.indexOf("Total");
    assertTrue(separatorIndex < totalIndex, "separator must appear before Total line");
  }

  @Test
  void numberFormatUsesCommaThousandSeparator() throws Exception {
    var input = serialize(List.of(new ModuleSloc("Total", 1000, 800, 200)));
    var output = new ByteArrayOutputStream();

    new SlocReportFormatCommand().execute(new ByteArrayInputStream(input), output);

    assertTrue(output.toString().contains("1,000"));
  }

  private byte[] serialize(List<ModuleSloc> modules) throws Exception {
    var baos = new ByteArrayOutputStream();
    try (var oos = new ObjectOutputStream(baos)) {
      for (var m : modules) {
        oos.writeObject(m);
      }
    }
    return baos.toByteArray();
  }
}
