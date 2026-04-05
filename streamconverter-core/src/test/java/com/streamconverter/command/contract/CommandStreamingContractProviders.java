package com.streamconverter.command.contract;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.FileBufferCommand;
import com.streamconverter.command.impl.LineEndingNormalizeCommand;
import com.streamconverter.command.impl.LineEndingNormalizeCommand.LineEndingType;
import com.streamconverter.command.impl.charcode.CharacterConvertCommand;
import com.streamconverter.command.impl.csv.CsvFilterCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.csv.CsvValidateCommand;
import com.streamconverter.command.impl.json.JsonFilterCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.impl.xml.ConvertCommand;
import com.streamconverter.command.impl.xml.ValidateCommand;
import com.streamconverter.command.impl.xml.XmlFilterCommand;
import com.streamconverter.command.impl.xml.XmlNavigateCommand;
import com.streamconverter.command.rule.TestRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class CommandStreamingContractProviders {
  private CommandStreamingContractProviders() {}

  static byte[] utf8(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  static byte[] resourceBytes(String resourceName) {
    try (InputStream inputStream =
        CommandStreamingContractProviders.class
            .getClassLoader()
            .getResourceAsStream(resourceName)) {
      if (inputStream == null) {
        throw new IllegalStateException("Missing test resource: " + resourceName);
      }
      return inputStream.readAllBytes();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read test resource: " + resourceName, e);
    }
  }
}

final class CharacterConvertCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return CharacterConvertCommand.create("UTF-8", "UTF-8");
  }

  @Override
  public byte[] sampleInput() {
    return CommandStreamingContractProviders.utf8(
        "alpha,original,one\nbeta,original,two\ngamma,original,three\n");
  }

  @Override
  public int firstChunkSize() {
    return 24;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.MUST_WRITE_BEFORE_INPUT_COMPLETE;
  }
}

final class LineEndingNormalizeCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return new LineEndingNormalizeCommand(LineEndingType.UNIX);
  }

  @Override
  public byte[] sampleInput() {
    String largeBlock = "line-content-".repeat(1200);
    return CommandStreamingContractProviders.utf8(
        largeBlock + "\r\n" + largeBlock + "\r\n" + "tail-line\r\n");
  }

  @Override
  public int firstChunkSize() {
    return 12_000;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.MUST_WRITE_BEFORE_INPUT_COMPLETE;
  }
}

final class CsvFilterCommandStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return CsvFilterCommand.create(CSVPath.of("1"), false);
  }

  @Override
  public byte[] sampleInput() {
    String largeCell = "selected-value-".repeat(900);
    return CommandStreamingContractProviders.utf8("1," + largeCell + ",30\n2,tail,40\n3,tail,50\n");
  }

  @Override
  public int firstChunkSize() {
    return 12_000;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.EXEMPT_FROM_STREAMING_CONTRACT;
  }

  @Override
  public String exemptionReason() {
    return "Current implementation does not emit raw output before input completion under the strict streaming probe.";
  }
}

final class CsvNavigateCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return CsvNavigateCommand.create(CSVPath.of("1"), new TestRule("original", "transformed"));
  }

  @Override
  public byte[] sampleInput() {
    String largeHeader = "header-name-".repeat(900);
    return CommandStreamingContractProviders.utf8(
        "id," + largeHeader + ",age\n1,original,30\n2,Bob,40\n3,Carol,50\n");
  }

  @Override
  public int firstChunkSize() {
    return 12_000;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.MUST_WRITE_BEFORE_INPUT_COMPLETE;
  }
}

final class CsvValidateCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return CsvValidateCommand.create(true, 5, "id", "name");
  }

  @Override
  public byte[] sampleInput() {
    return CommandStreamingContractProviders.utf8(
        "id,name,email\n1,Alice,a@example.com\n2,Bob,b@example.com\n3,Carol,c@example.com\n");
  }

  @Override
  public int firstChunkSize() {
    return 20;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.MUST_WRITE_BEFORE_INPUT_COMPLETE;
  }
}

final class JsonNavigateCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return JsonNavigateCommand.create(
        TreePath.fromJson("$.user.name"), new TestRule("original", "transformed"));
  }

  @Override
  public byte[] sampleInput() {
    String largeValue = "original".repeat(1400);
    return CommandStreamingContractProviders.utf8(
        "{\"user\":{\"name\":\"" + largeValue + "\",\"role\":\"admin\"},\"tail\":\"value\"}");
  }

  @Override
  public int firstChunkSize() {
    return 12_500;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.MUST_WRITE_BEFORE_INPUT_COMPLETE;
  }
}

final class JsonFilterCommandStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return JsonFilterCommand.create(TreePath.fromJson("$.user.name"));
  }

  @Override
  public byte[] sampleInput() {
    String largeValue = "AliceValue".repeat(1400);
    return CommandStreamingContractProviders.utf8(
        "{\"user\":{\"name\":\"" + largeValue + "\",\"role\":\"admin\"},\"tail\":\"value\"}");
  }

  @Override
  public int firstChunkSize() {
    return 12_500;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.EXEMPT_FROM_STREAMING_CONTRACT;
  }

  @Override
  public String exemptionReason() {
    return "Current implementation does not emit raw output before input completion under the strict streaming probe.";
  }
}

final class ConvertCommandStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return ConvertCommand.create(
        new TestRule("original", "transformed"), TreePath.fromXml("root/item"));
  }

  @Override
  public byte[] sampleInput() {
    String largeText = "original".repeat(1600);
    return CommandStreamingContractProviders.utf8(
        "<root><item>" + largeText + "</item><item>second</item><tail>z</tail></root>");
  }

  @Override
  public int firstChunkSize() {
    return 13_000;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.MUST_WRITE_BEFORE_INPUT_COMPLETE;
  }
}

final class ValidateCommandStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return ValidateCommand.create("test-schema.xsd");
  }

  @Override
  public byte[] sampleInput() {
    return CommandStreamingContractProviders.resourceBytes("valid-test.xml");
  }

  @Override
  public int firstChunkSize() {
    return 64;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.MUST_WRITE_BEFORE_INPUT_COMPLETE;
  }
}

final class XmlFilterCommandStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return XmlFilterCommand.create(TreePath.fromXml("root/item"));
  }

  @Override
  public byte[] sampleInput() {
    return CommandStreamingContractProviders.utf8(
        "<root><item>first</item><item>second</item><tail>done</tail></root>");
  }

  @Override
  public int firstChunkSize() {
    return 24;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.EXEMPT_FROM_STREAMING_CONTRACT;
  }

  @Override
  public String exemptionReason() {
    return "Current implementation accumulates extracted XML fragments before writing them.";
  }
}

final class XmlNavigateCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return XmlNavigateCommand.create(
        TreePath.fromXml("root/item"), new TestRule("original", "transformed"));
  }

  @Override
  public byte[] sampleInput() {
    String largeText = "original".repeat(1600);
    return CommandStreamingContractProviders.utf8(
        "<root><item>" + largeText + "</item><item>second</item><tail>done</tail></root>");
  }

  @Override
  public int firstChunkSize() {
    return 13_000;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.MUST_WRITE_BEFORE_INPUT_COMPLETE;
  }
}

final class FileBufferCommandStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return FileBufferCommand.create();
  }

  @Override
  public byte[] sampleInput() {
    return CommandStreamingContractProviders.utf8(
        "buffered-1\nbuffered-2\nbuffered-3\nbuffered-4\nbuffered-5\n");
  }

  @Override
  public int firstChunkSize() {
    return 20;
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.EXEMPT_FROM_STREAMING_CONTRACT;
  }

  @Override
  public String exemptionReason() {
    return "FileBufferCommand intentionally writes the entire input to a temp file before emitting output.";
  }
}
