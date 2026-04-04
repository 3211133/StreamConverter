package com.streamconverter.sloc.command;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.sloc.ModuleSloc;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlocAggregateCommandTest {

  @Test
  void aggregatesModulesAndAddsTotal() throws Exception {
    var input =
        serialize(List.of(new ModuleSloc("core", 100, 80, 20), new ModuleSloc("db", 50, 40, 10)));
    var output = new ByteArrayOutputStream();

    new SlocAggregateCommand().execute(new ByteArrayInputStream(input), output);

    List<ModuleSloc> results = deserialize(output);
    assertEquals(3, results.size());
    assertEquals("core", results.get(0).name());
    assertEquals("db", results.get(1).name());

    var total = results.get(2);
    assertEquals("Total", total.name());
    assertEquals(150, total.lines());
    assertEquals(120, total.covered());
    assertEquals(30, total.missed());
  }

  @Test
  void singleModule_totalEqualsModule() throws Exception {
    var input = serialize(List.of(new ModuleSloc("only", 200, 150, 50)));
    var output = new ByteArrayOutputStream();

    new SlocAggregateCommand().execute(new ByteArrayInputStream(input), output);

    List<ModuleSloc> results = deserialize(output);
    assertEquals(2, results.size());
    var total = results.get(1);
    assertEquals("Total", total.name());
    assertEquals(200, total.lines());
  }

  @Test
  void emptyInput_onlyTotalIsEmitted() throws Exception {
    var input = serialize(List.of());
    var output = new ByteArrayOutputStream();

    new SlocAggregateCommand().execute(new ByteArrayInputStream(input), output);

    List<ModuleSloc> results = deserialize(output);
    assertEquals(1, results.size());
    var total = results.get(0);
    assertEquals("Total", total.name());
    assertEquals(0, total.lines());
    assertEquals(0, total.covered());
    assertEquals(0, total.missed());
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
