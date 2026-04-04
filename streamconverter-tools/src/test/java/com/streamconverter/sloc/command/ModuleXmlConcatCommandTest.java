package com.streamconverter.sloc.command;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModuleXmlConcatCommandTest {

  // 実際の JaCoCo XML 形式: <?xml?> + <!DOCTYPE> + <report> が1行に連結
  private static final String JACOCO_XML_WITH_PROLOGUE =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<!DOCTYPE report PUBLIC \"-//JACOCO//DTD Report 1.1//EN\" \"report.dtd\">"
          + "<report name=\"my-module\">"
          + "<counter type=\"LINE\" missed=\"10\" covered=\"90\"/>"
          + "</report>\n";

  @Test
  void stripsXmlAndDoctypeDeclarations(@TempDir Path tempDir) throws Exception {
    Path reportDir =
        tempDir.resolve("my-module/" + ModuleXmlConcatCommand.JACOCO_XML_PATH).getParent();
    Files.createDirectories(reportDir);
    Files.writeString(reportDir.resolve("jacocoTestReport.xml"), JACOCO_XML_WITH_PROLOGUE);

    var output = new ByteArrayOutputStream();
    new ModuleXmlConcatCommand(tempDir)
        .execute(new ByteArrayInputStream("my-module\n".getBytes(StandardCharsets.UTF_8)), output);

    String result = output.toString(StandardCharsets.UTF_8);
    // XML ヘッダー・DOCTYPE 宣言は除去され、<report> 要素のみ出力される
    assertFalse(result.contains("<?xml"));
    assertFalse(result.contains("<!DOCTYPE"));
    assertTrue(result.contains("<report name=\"my-module\">"));
  }

  @Test
  void multipleModules_onlyOneXmlHeader(@TempDir Path tempDir) throws Exception {
    for (String mod : new String[] {"module-a", "module-b"}) {
      Path dir = tempDir.resolve(mod + "/" + ModuleXmlConcatCommand.JACOCO_XML_PATH).getParent();
      Files.createDirectories(dir);
      Files.writeString(
          dir.resolve("jacocoTestReport.xml"), JACOCO_XML_WITH_PROLOGUE.replace("my-module", mod));
    }

    var output = new ByteArrayOutputStream();
    new ModuleXmlConcatCommand(tempDir)
        .execute(
            new ByteArrayInputStream("module-a\nmodule-b\n".getBytes(StandardCharsets.UTF_8)),
            output);

    String result = output.toString(StandardCharsets.UTF_8);
    // XML 宣言・DOCTYPE は除去される
    assertFalse(result.contains("<?xml"));
    assertFalse(result.contains("<!DOCTYPE"));
    // 両モジュールの <report> が含まれる
    assertTrue(result.contains("<report name=\"module-a\">"));
    assertTrue(result.contains("<report name=\"module-b\">"));
  }

  @Test
  void missingReport_isSkipped(@TempDir Path tempDir) throws Exception {
    var output = new ByteArrayOutputStream();
    new ModuleXmlConcatCommand(tempDir)
        .execute(
            new ByteArrayInputStream("missing-module\n".getBytes(StandardCharsets.UTF_8)), output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertFalse(result.contains("<report"));
  }
}
