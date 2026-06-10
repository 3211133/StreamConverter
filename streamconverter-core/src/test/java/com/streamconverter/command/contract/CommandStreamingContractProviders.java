package com.streamconverter.command.contract;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.FileBufferCommand;
import com.streamconverter.command.impl.LineEndingNormalizeCommand;
import com.streamconverter.command.impl.LineEndingNormalizeCommand.LineEndingType;
import com.streamconverter.command.impl.SendHttpCommand;
import com.streamconverter.command.impl.charcode.CharacterConvertCommand;
import com.streamconverter.command.impl.csv.CsvFilterCommand;
import com.streamconverter.command.impl.csv.CsvValidateCommand;
import com.streamconverter.command.impl.csv.CsvWalker;
import com.streamconverter.command.impl.json.JsonExtractCommand;
import com.streamconverter.command.impl.json.JsonWalker;
import com.streamconverter.command.impl.xml.ValidateCommand;
import com.streamconverter.command.impl.xml.XmlExtractCommand;
import com.streamconverter.command.impl.xml.XmlWalker;
import com.streamconverter.command.rule.TestRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import com.streamconverter.pmd.command.PmdXmlToViolationsCommand;
import com.streamconverter.sloc.ModuleSloc;
import com.streamconverter.sloc.command.JacocoXmlToModuleSlocCommand;
import com.streamconverter.sloc.command.ModuleXmlConcatCommand;
import com.streamconverter.sloc.command.SlocAggregateCommand;
import com.streamconverter.sloc.command.SlocReportFormatCommand;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

  static byte[] serializeObjects(List<?> objects) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try (ObjectOutputStream objectOutput = new ObjectOutputStream(output)) {
        for (Object object : objects) {
          objectOutput.writeObject(object);
        }
      }
      return output.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to serialize contract test input", e);
    }
  }

  static Path createTempDirectory(String prefix) {
    try {
      return Files.createTempDirectory(prefix);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temp directory for contract test", e);
    }
  }

  static void writeString(Path path, String value) {
    try {
      Path parent = path.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(path, value, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to prepare contract test input file: " + path, e);
    }
  }

  static WebClient sendHttpEchoWebClient(String responsePrefix) {
    DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    ExchangeFunction exchangeFunction =
        request -> {
          AtomicReference<String> requestBody = new AtomicReference<>("");
          MockClientHttpRequest mockRequest =
              new MockClientHttpRequest(HttpMethod.POST, URI.create(request.url().toString()));
          mockRequest.setWriteHandler(
              body ->
                  DataBufferUtils.join(body)
                      .doOnNext(
                          dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            requestBody.set(new String(bytes, StandardCharsets.UTF_8));
                            DataBufferUtils.release(dataBuffer);
                          })
                      .then());
          return request
              .writeTo(mockRequest, ExchangeStrategies.withDefaults())
              .then(Mono.fromSupplier(requestBody::get))
              .map(
                  body ->
                      ClientResponse.create(HttpStatus.OK)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                          .body(
                              Flux.just(
                                  bufferFactory.wrap(
                                      (responsePrefix + body).getBytes(StandardCharsets.UTF_8))))
                          .build());
        };
    return WebClient.builder().exchangeFunction(exchangeFunction).build();
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
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
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
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class CsvFilterCommandStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return CsvFilterCommand.create(CSVPath.of("*"), false);
  }

  @Override
  public byte[] sampleInput() {
    String largeFirstRow = "selected-value-".repeat(900);
    String largeTail = "tail-value-".repeat(900);
    return CommandStreamingContractProviders.utf8(
        "1," + largeFirstRow + ",30\n2," + largeTail + ",40\n3,tail,50\n");
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class CsvWalkerStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return CsvWalker.create(CSVPath.of("1"), new TestRule("original", "transformed"));
  }

  @Override
  public byte[] sampleInput() {
    String largeHeader = "header-name-".repeat(900);
    return CommandStreamingContractProviders.utf8(
        "id," + largeHeader + ",age\n1,original,30\n2,Bob,40\n3,Carol,50\n");
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
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
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class JsonWalkerStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return JsonWalker.create(
        TreePath.fromJson("$.user.name"), new TestRule("original", "transformed"));
  }

  @Override
  public byte[] sampleInput() {
    String largeValue = "original".repeat(1400);
    return CommandStreamingContractProviders.utf8(
        "{\"user\":{\"name\":\"" + largeValue + "\",\"role\":\"admin\"},\"tail\":\"value\"}");
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class JsonExtractCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return JsonExtractCommand.create(TreePath.fromJson("$.user"));
  }

  @Override
  public byte[] sampleInput() {
    String largeValue = "AliceValue".repeat(1400);
    return CommandStreamingContractProviders.utf8(
        "{\"user\":{\"name\":\""
            + largeValue
            + "\",\"role\":\"admin\"},\"tail\":\""
            + "tail-value-".repeat(1400)
            + "\"}");
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class ValidateCommandStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() throws IOException {
    return ValidateCommand.create("test-schema.xsd");
  }

  @Override
  public byte[] sampleInput() {
    String largeContent = "content-".repeat(1500);
    return CommandStreamingContractProviders.utf8(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<root id=\"test\">"
            + "<element>"
            + largeContent
            + "</element>"
            + "<element>tail</element>"
            + "</root>");
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class XmlExtractCommandStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return XmlExtractCommand.create(TreePath.fromXml("root/item"));
  }

  @Override
  public byte[] sampleInput() {
    String largeFirstItem = "value-".repeat(1200);
    String largeTail = "tail-".repeat(1200);
    return CommandStreamingContractProviders.utf8(
        "<root><item>"
            + largeFirstItem
            + "</item><item>second</item><item>third</item><tail>"
            + largeTail
            + "</tail></root>");
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class XmlWalkerStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return XmlWalker.create(TreePath.fromXml("root/item"), new TestRule("original", "transformed"));
  }

  @Override
  public byte[] sampleInput() {
    String largeText = "original".repeat(1600);
    return CommandStreamingContractProviders.utf8(
        "<root><item>" + largeText + "</item><item>second</item><tail>done</tail></root>");
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
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
  public StreamingExpectation expectation() {
    return StreamingExpectation.ALLOWED_FULL_BUFFERING;
  }

  @Override
  public String exemptionReason() {
    return "FileBufferCommand intentionally writes the entire input to a temp file before emitting output.";
  }
}

final class SendHttpCommandStreamingContractProvider implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return new SendHttpCommand(
        "https://example.com/post",
        CommandStreamingContractProviders.sendHttpEchoWebClient("ack:"));
  }

  @Override
  public byte[] sampleInput() {
    return CommandStreamingContractProviders.utf8("{\"message\":\"probe\"}");
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.KNOWN_STREAMING_VIOLATION;
  }

  @Override
  public String exemptionReason() {
    return "Current SendHttpCommand behavior starts processing the HTTP response after the request body upload completes.";
  }
}

final class PmdXmlToViolationsCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return new PmdXmlToViolationsCommand();
  }

  @Override
  public byte[] sampleInput() {
    return CommandStreamingContractProviders.utf8(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <pmd>
          <file name="/home/user/streamconverter-core/src/main/java/Foo.java">
            <violation beginline="10" rule="UnusedVariable" ruleset="Best Practices"
                       priority="3" class="Foo" method="bar" variable="x">
              Avoid unused variables.
            </violation>
          </file>
          <file name="/home/user/streamconverter-tools/src/main/java/Bar.java">
            <violation beginline="20" rule="LongMethod" ruleset="Design"
                       priority="2" class="Bar" method="baz" variable="">
              Method too long.
            </violation>
          </file>
        </pmd>
        """);
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class JacocoXmlToModuleSlocCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return new JacocoXmlToModuleSlocCommand();
  }

  @Override
  public byte[] sampleInput() {
    return CommandStreamingContractProviders.utf8(
        "<report name=\"streamconverter-core\">"
            + "<counter type=\"LINE\" missed=\"50\" covered=\"100\"/>"
            + "</report>\n"
            + "<report name=\"streamconverter-tools\">"
            + "<counter type=\"LINE\" missed=\"5\" covered=\"30\"/>"
            + "</report>\n");
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class ModuleXmlConcatCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  private static final String JACOCO_XML_PATH = "build/reports/jacoco/test/jacocoTestReport.xml";

  @Override
  public IStreamCommand createCommand() {
    Path projectRoot = CommandStreamingContractProviders.createTempDirectory("module-xml-concat");
    CommandStreamingContractProviders.writeString(
        projectRoot.resolve("module-a").resolve(JACOCO_XML_PATH),
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<!DOCTYPE report PUBLIC \"-//JACOCO//DTD Report 1.1//EN\" \"report.dtd\">"
            + "<report name=\"module-a\"><counter type=\"LINE\" missed=\"10\" covered=\"90\"/></report>\n");
    CommandStreamingContractProviders.writeString(
        projectRoot.resolve("module-b").resolve(JACOCO_XML_PATH),
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<!DOCTYPE report PUBLIC \"-//JACOCO//DTD Report 1.1//EN\" \"report.dtd\">"
            + "<report name=\"module-b\"><counter type=\"LINE\" missed=\"5\" covered=\"35\"/></report>\n");
    return new ModuleXmlConcatCommand(projectRoot);
  }

  @Override
  public byte[] sampleInput() {
    return CommandStreamingContractProviders.utf8("module-a\nmodule-b\n");
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class SlocAggregateCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return new SlocAggregateCommand();
  }

  @Override
  public byte[] sampleInput() {
    return CommandStreamingContractProviders.serializeObjects(
        List.of(
            new ModuleSloc("streamconverter-core", 1869, 1500, 369),
            new ModuleSloc("streamconverter-tools", 1038, 900, 138)));
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}

final class SlocReportFormatCommandStreamingContractProvider
    implements CommandStreamingContractProvider {
  @Override
  public IStreamCommand createCommand() {
    return new SlocReportFormatCommand();
  }

  @Override
  public byte[] sampleInput() {
    String largeModuleName = "streamconverter-core-segment-".repeat(80);
    return CommandStreamingContractProviders.serializeObjects(
        List.of(
            new ModuleSloc(largeModuleName, 1869, 1500, 369),
            new ModuleSloc("streamconverter-tools", 1038, 900, 138),
            new ModuleSloc(ModuleSloc.TOTAL_NAME, 2907, 2400, 507)));
  }

  @Override
  public StreamingExpectation expectation() {
    return StreamingExpectation.STREAMING_COMPLIANT;
  }
}
