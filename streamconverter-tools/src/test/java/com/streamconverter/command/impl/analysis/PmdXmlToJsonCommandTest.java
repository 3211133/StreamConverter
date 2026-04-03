package com.streamconverter.command.impl.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PmdXmlToJsonCommandTest {

  private static final PmdViolation VIOLATION =
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

  @Test
  void outputIsValidJson() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToJsonCommand().execute(serialize(List.of(VIOLATION)), output);

    String json = output.toString(StandardCharsets.UTF_8);
    assertTrue(json.contains("\"totalViolations\""));
    assertTrue(json.contains("\"violations\""));
    assertTrue(json.contains("\"UnusedVariable\""));
    assertTrue(json.contains("\"generatedAt\""));
  }

  @Test
  void totalViolationsCountIsCorrect() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToJsonCommand().execute(serialize(List.of(VIOLATION, VIOLATION)), output);

    String json = output.toString(StandardCharsets.UTF_8);
    assertTrue(json.contains("\"totalViolations\" : 2"));
  }

  @Test
  void emptyInput_producesZeroViolations() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToJsonCommand().execute(serialize(List.of()), output);

    String json = output.toString(StandardCharsets.UTF_8);
    assertTrue(json.contains("\"totalViolations\" : 0"));
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
