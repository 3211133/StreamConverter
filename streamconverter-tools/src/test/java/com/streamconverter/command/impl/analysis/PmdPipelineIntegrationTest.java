package com.streamconverter.command.impl.analysis;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.StreamConverter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PmdPipelineIntegrationTest {

  private static final String PMD_XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<pmd version=\"7.0.0\">\n"
          + "  <file name=\"/project/streamconverter-core/src/Foo.java\">\n"
          + "    <violation beginline=\"10\" rule=\"UnusedVariable\" ruleset=\"Best Practices\""
          + " priority=\"3\" class=\"Foo\" method=\"bar\" variable=\"x\">Avoid unused variables.</violation>\n"
          + "    <violation beginline=\"20\" rule=\"UnusedVariable\" ruleset=\"Best Practices\""
          + " priority=\"3\" class=\"Foo\" method=\"baz\" variable=\"y\">Avoid unused variables.</violation>\n"
          + "  </file>\n"
          + "  <file name=\"/project/streamconverter-core/src/Bar.java\">\n"
          + "    <violation beginline=\"5\" rule=\"LongMethod\" ruleset=\"Design\""
          + " priority=\"2\" class=\"Bar\" method=\"find\" variable=\"\">Method too long.</violation>\n"
          + "  </file>\n"
          + "</pmd>\n";

  @Test
  void pipeline_producesMarkdownReport() throws Exception {
    StreamConverter converter =
        StreamConverter.create(new PmdXmlToViolationsCommand(), new PmdXmlToMarkdownCommand());

    InputStream input = new java.io.ByteArrayInputStream(PMD_XML.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    converter.run(input, output);

    String report = output.toString(StandardCharsets.UTF_8);
    assertTrue(report.contains("# PMD Code Quality Analysis Report"));
    assertTrue(report.contains("Total Violations**: 3"));
    assertTrue(report.contains("UnusedVariable"));
    assertTrue(report.contains("LongMethod"));
    assertTrue(report.contains("Foo.java"));
    assertTrue(report.contains("Bar.java"));
  }
}
