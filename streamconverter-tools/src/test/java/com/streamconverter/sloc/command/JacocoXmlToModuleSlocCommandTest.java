package com.streamconverter.sloc.command;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.sloc.ModuleSloc;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JacocoXmlToModuleSlocCommandTest {

  // ModuleXmlConcatCommand は XML宣言・DOCTYPE宣言を除去して <report> 要素のみ渡す
  private static final String JACOCO_XML_CORE =
      "<report name=\"streamconverter-core\">"
          + "<counter type=\"LINE\" missed=\"50\" covered=\"100\"/>"
          + "<counter type=\"BRANCH\" missed=\"10\" covered=\"20\"/>"
          + "</report>\n";

  private static final String JACOCO_XML_DB =
      "<report name=\"streamconverter-db\">"
          + "<counter type=\"LINE\" missed=\"5\" covered=\"30\"/>"
          + "</report>\n";

  private static final String JACOCO_XML_NO_LINE_COUNTER =
      "<report name=\"no-line-module\">"
          + "<counter type=\"BRANCH\" missed=\"10\" covered=\"20\"/>"
          + "</report>\n";

  @Test
  void singleReport_extractsModuleNameAndLineCounter() throws Exception {
    var output = new ByteArrayOutputStream();
    new JacocoXmlToModuleSlocCommand()
        .execute(
            new ByteArrayInputStream(JACOCO_XML_CORE.getBytes(StandardCharsets.UTF_8)), output);

    List<ModuleSloc> results = deserialize(output);
    assertEquals(1, results.size());
    var sloc = results.get(0);
    assertEquals("streamconverter-core", sloc.name());
    assertEquals(150, sloc.lines());
    assertEquals(100, sloc.covered());
    assertEquals(50, sloc.missed());
  }

  @Test
  void multipleReports_processesAllModules() throws Exception {
    // ModuleXmlConcatCommand の出力形式：最初のXML宣言のみ保持し、2つ目以降は除去して連結
    String multiXml = JACOCO_XML_CORE + JACOCO_XML_DB;

    var output = new ByteArrayOutputStream();
    new JacocoXmlToModuleSlocCommand()
        .execute(new ByteArrayInputStream(multiXml.getBytes(StandardCharsets.UTF_8)), output);

    List<ModuleSloc> results = deserialize(output);
    assertEquals(2, results.size());
    assertEquals("streamconverter-core", results.get(0).name());
    assertEquals(150, results.get(0).lines());
    assertEquals("streamconverter-db", results.get(1).name());
    assertEquals(35, results.get(1).lines());
  }

  @Test
  void noLineCounter_emitsZeroSloc() throws Exception {
    var output = new ByteArrayOutputStream();
    new JacocoXmlToModuleSlocCommand()
        .execute(
            new ByteArrayInputStream(JACOCO_XML_NO_LINE_COUNTER.getBytes(StandardCharsets.UTF_8)),
            output);

    List<ModuleSloc> results = deserialize(output);
    assertEquals(1, results.size());
    assertEquals("no-line-module", results.get(0).name());
    assertEquals(0, results.get(0).lines());
  }

  @Test
  void invalidXml_throwsIOException() {
    var output = new ByteArrayOutputStream();
    assertThrows(
        IOException.class,
        () ->
            new JacocoXmlToModuleSlocCommand()
                .execute(
                    new ByteArrayInputStream("not xml <<<".getBytes(StandardCharsets.UTF_8)),
                    output));
  }

  private List<ModuleSloc> deserialize(ByteArrayOutputStream output) throws Exception {
    var ois = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()));
    List<ModuleSloc> result = new ArrayList<>();
    try {
      while (true) {
        result.add((ModuleSloc) ois.readObject());
      }
    } catch (EOFException e) {
      // 終端
    }
    return result;
  }
}
