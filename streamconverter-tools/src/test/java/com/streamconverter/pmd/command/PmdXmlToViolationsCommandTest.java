package com.streamconverter.pmd.command;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.pmd.PmdViolation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PmdXmlToViolationsCommandTest {

  private static final String PMD_XML =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <pmd>
        <file name="/home/user/streamconverter-core/src/main/java/Foo.java">
          <violation beginline="10" rule="UnusedVariable" ruleset="Best Practices"
                     priority="3" class="Foo" method="bar" variable="x">
            Avoid unused variables.
          </violation>
          <violation beginline="20" rule="EmptyCatchBlock" ruleset="Error Prone"
                     priority="1" class="Foo" method="process" variable="">
            Empty catch block.
          </violation>
        </file>
        <file name="/home/user/streamconverter-db/src/main/java/Dao.java">
          <violation beginline="5" rule="LongMethod" ruleset="Design"
                     priority="2" class="Dao" method="find" variable="">
            Method too long.
          </violation>
        </file>
      </pmd>
      """;

  @Test
  void extractsAllViolations() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToViolationsCommand()
        .execute(new ByteArrayInputStream(PMD_XML.getBytes(StandardCharsets.UTF_8)), output);

    List<PmdViolation> violations = deserialize(output);
    assertEquals(3, violations.size());
  }

  @Test
  void mapsAttributesCorrectly() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToViolationsCommand()
        .execute(new ByteArrayInputStream(PMD_XML.getBytes(StandardCharsets.UTF_8)), output);

    List<PmdViolation> violations = deserialize(output);
    PmdViolation first = violations.get(0);
    assertEquals("streamconverter-core/src/main/java/Foo.java", first.file());
    assertEquals(10, first.line());
    assertEquals("UnusedVariable", first.rule());
    assertEquals("Best Practices", first.ruleset());
    assertEquals(3, first.priority());
    assertEquals("Foo", first.className());
    assertEquals("bar", first.method());
    assertEquals("x", first.variable());
  }

  @Test
  void extractsRelativePath() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToViolationsCommand()
        .execute(new ByteArrayInputStream(PMD_XML.getBytes(StandardCharsets.UTF_8)), output);

    List<PmdViolation> violations = deserialize(output);
    // フルパスではなく streamconverter- 以降の相対パスになること
    assertTrue(violations.get(0).file().startsWith("streamconverter-"));
    assertFalse(violations.get(0).file().contains("/home/"));
  }

  @Test
  void emptyPmdXml_producesNoViolations() throws Exception {
    String emptyXml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <pmd/>
        """;
    var output = new ByteArrayOutputStream();
    new PmdXmlToViolationsCommand()
        .execute(new ByteArrayInputStream(emptyXml.getBytes(StandardCharsets.UTF_8)), output);

    List<PmdViolation> violations = deserialize(output);
    assertEquals(0, violations.size());
  }

  private List<PmdViolation> deserialize(ByteArrayOutputStream output) throws Exception {
    var ois = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()));
    List<PmdViolation> result = new ArrayList<>();
    try {
      while (true) {
        result.add((PmdViolation) ois.readObject());
      }
    } catch (EOFException e) {
      // 終端
    }
    return result;
  }
}
