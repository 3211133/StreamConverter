package com.streamconverter.command.impl.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JacocoXmlToModuleSlocCommandTest {

  private static final String JACOCO_XML =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <report name="test">
        <counter type="LINE" missed="50" covered="100"/>
        <counter type="BRANCH" missed="10" covered="20"/>
      </report>
      """;

  private static final String JACOCO_XML_NO_LINE_COUNTER =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <report name="test">
        <counter type="BRANCH" missed="10" covered="20"/>
      </report>
      """;

  @Test
  void parseSingleXml_extractsLineCounter() throws Exception {
    var cmd = new JacocoXmlToModuleSlocCommand("core");
    var input = new ByteArrayInputStream(JACOCO_XML.getBytes(StandardCharsets.UTF_8));
    var output = new ByteArrayOutputStream();

    cmd.execute(input, output);

    List<ModuleSloc> results = deserialize(output);
    assertEquals(1, results.size());
    var sloc = results.get(0);
    assertEquals("core", sloc.name());
    assertEquals(150, sloc.lines());
    assertEquals(100, sloc.covered());
    assertEquals(50, sloc.missed());
  }

  @Test
  void parseSingleXml_noLineCounter_returnsFallback() throws Exception {
    var cmd = new JacocoXmlToModuleSlocCommand("empty-module");
    var input =
        new ByteArrayInputStream(JACOCO_XML_NO_LINE_COUNTER.getBytes(StandardCharsets.UTF_8));
    var output = new ByteArrayOutputStream();

    cmd.execute(input, output);

    List<ModuleSloc> results = deserialize(output);
    assertEquals(1, results.size());
    var sloc = results.get(0);
    assertEquals("empty-module", sloc.name());
    assertEquals(0, sloc.lines());
  }

  @Test
  void parseMultiXml_processesAllModules() throws Exception {
    String multiXml =
        ModuleXmlConcatCommand.MODULE_HEADER_PREFIX
            + "module-a\n"
            + JACOCO_XML
            + ModuleXmlConcatCommand.MODULE_HEADER_PREFIX
            + "module-b\n"
            + JACOCO_XML_NO_LINE_COUNTER;

    var cmd = new JacocoXmlToModuleSlocCommand();
    var input = new ByteArrayInputStream(multiXml.getBytes(StandardCharsets.UTF_8));
    var output = new ByteArrayOutputStream();

    cmd.execute(input, output);

    List<ModuleSloc> results = deserialize(output);
    assertEquals(2, results.size());
    assertEquals("module-a", results.get(0).name());
    assertEquals(150, results.get(0).lines());
    assertEquals("module-b", results.get(1).name());
    assertEquals(0, results.get(1).lines());
  }

  @Test
  void parseMultiXml_lastModuleIsFlushed() throws Exception {
    // 最後のモジュールが正しく出力に含まれることを確認
    String multiXml = ModuleXmlConcatCommand.MODULE_HEADER_PREFIX + "only-module\n" + JACOCO_XML;

    var cmd = new JacocoXmlToModuleSlocCommand();
    var input = new ByteArrayInputStream(multiXml.getBytes(StandardCharsets.UTF_8));
    var output = new ByteArrayOutputStream();

    cmd.execute(input, output);

    List<ModuleSloc> results = deserialize(output);
    assertEquals(1, results.size());
    assertEquals("only-module", results.get(0).name());
  }

  @Test
  void parseSingleXml_invalidXml_throwsIOException() {
    var cmd = new JacocoXmlToModuleSlocCommand("bad-module");
    var input = new ByteArrayInputStream("not xml at all <<<".getBytes(StandardCharsets.UTF_8));
    var output = new ByteArrayOutputStream();

    assertThrows(IOException.class, () -> cmd.execute(input, output));
  }

  private List<ModuleSloc> deserialize(ByteArrayOutputStream output) throws Exception {
    var ois = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()));
    List<ModuleSloc> result = new ArrayList<>();
    try {
      while (true) {
        result.add((ModuleSloc) ois.readObject());
      }
    } catch (java.io.EOFException e) {
      // 終端
    }
    return result;
  }
}
