package com.streamconverter.sloc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SlocCounterTest {

  // 実際の JaCoCo 形式に合わせて <?xml?> + <!DOCTYPE> を含む
  private static final String JACOCO_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<!DOCTYPE report PUBLIC \"-//JACOCO//DTD Report 1.1//EN\" \"report.dtd\">"
          + "<report name=\"my-module\">"
          + "<counter type=\"LINE\" missed=\"30\" covered=\"70\"/>"
          + "</report>\n";

  @Test
  void run_producesReportForValidModule(@TempDir Path tempDir) throws Exception {
    // モジュールの JaCoCo XML を配置
    Path reportDir = tempDir.resolve("my-module/build/reports/jacoco/test");
    Files.createDirectories(reportDir);
    Files.writeString(reportDir.resolve("jacocoTestReport.xml"), JACOCO_XML);

    var output = new ByteArrayOutputStream();
    new SlocCounter().run(tempDir, List.of("my-module"), output);

    String report = output.toString(StandardCharsets.UTF_8);
    assertTrue(report.contains("=== SLOC Report ==="), "header should be present");
    assertTrue(report.contains("my-module"), "module name should appear");
    assertTrue(report.contains("100"), "total lines (30+70) should appear");
    assertTrue(report.contains("Total"), "Total row should appear");
  }

  @Test
  void run_skipsModuleWithMissingReport(@TempDir Path tempDir) throws Exception {
    // XML が存在しないモジュール
    var output = new ByteArrayOutputStream();
    new SlocCounter().run(tempDir, List.of("missing-module"), output);

    String report = output.toString(StandardCharsets.UTF_8);
    // レポートは生成されるが missing-module の行はない
    assertTrue(report.contains("=== SLOC Report ==="));
    assertFalse(report.contains("missing-module"));
  }
}
