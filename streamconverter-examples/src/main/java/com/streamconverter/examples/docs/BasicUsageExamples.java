package com.streamconverter.examples.docs;

import com.streamconverter.CommandResult;
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.LineEndingNormalizeCommand;
import com.streamconverter.command.impl.LineEndingNormalizeCommand.LineEndingType;
import com.streamconverter.command.impl.charcode.CharacterConvertCommand;
import com.streamconverter.command.impl.csv.CsvFilterCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.impl.xml.XmlNavigateCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Verifiable code examples for StreamConverter documentation.
 *
 * <p>This class contains actual, compilable, and runnable code examples that are referenced from
 * documentation. Each example is marked with START/END comments for potential extraction into
 * documentation.
 *
 * <p>All examples in this file are tested to ensure they compile and execute correctly.
 */
public class BasicUsageExamples {

  // [START csv-navigate-basic]
  /**
   * Basic CSV navigation example - applies transformation rule to specific column. Preserves entire
   * CSV structure.
   */
  public void csvNavigateBasic() throws Exception {
    // Sample CSV data
    String csvData = "id,productName,price\n" + "1,Apple,100\n" + "2,Banana,50\n";
    InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Create command to process 'productName' column
    IStreamCommand csvCommand =
        new CsvNavigateCommand(new CSVPath("productName"), new PassThroughRule());

    StreamConverter converter = StreamConverter.create(new IStreamCommand[] {csvCommand});
    List<CommandResult> results = converter.run(inputStream, outputStream);

    // Verify execution
    boolean success = results.stream().allMatch(CommandResult::isSuccess);
    if (!success) {
      throw new RuntimeException("CSV navigation failed");
    }
  }

  // [END csv-navigate-basic]

  // [START csv-filter-basic]
  /**
   * CSV filter example - extracts only specified columns. Outputs only selected columns, removes
   * others.
   */
  public void csvFilterBasic() throws Exception {
    String csvData = "id,name,price\n" + "1,Apple,100\n" + "2,Banana,50\n";
    InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Extract only 'price' column (assumes header exists)
    IStreamCommand filterCommand = CsvFilterCommand.create(new CSVPath("price"));

    StreamConverter converter = StreamConverter.create(new IStreamCommand[] {filterCommand});
    List<CommandResult> results = converter.run(inputStream, outputStream);

    boolean success = results.stream().allMatch(CommandResult::isSuccess);
    if (!success) {
      throw new RuntimeException("CSV filter failed");
    }
  }

  // [END csv-filter-basic]

  // [START json-navigate-basic]
  /**
   * JSON navigation example - applies transformation rule to specific elements. Preserves entire
   * JSON structure.
   */
  public void jsonNavigateBasic() throws Exception {
    String jsonData = "{\"user\": {\"name\": \"John\", \"age\": 30}}";
    InputStream inputStream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Process 'user.name' element
    IStreamCommand jsonCommand =
        new JsonNavigateCommand(TreePath.fromJson("$.user.name"), new PassThroughRule());

    StreamConverter converter = StreamConverter.create(new IStreamCommand[] {jsonCommand});
    List<CommandResult> results = converter.run(inputStream, outputStream);

    boolean success = results.stream().allMatch(CommandResult::isSuccess);
    if (!success) {
      throw new RuntimeException("JSON navigation failed");
    }
  }

  // [END json-navigate-basic]

  // [START xml-navigate-basic]
  /**
   * XML navigation example - applies transformation rule to specific elements. Preserves entire XML
   * structure.
   */
  public void xmlNavigateBasic() throws Exception {
    String xmlData = "<?xml version=\"1.0\"?><root><user><name>John</name></user></root>";
    InputStream inputStream = new ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Process 'root/user/name' element
    IStreamCommand xmlCommand =
        new XmlNavigateCommand(TreePath.fromXml("root/user/name"), new PassThroughRule());

    StreamConverter converter = StreamConverter.create(new IStreamCommand[] {xmlCommand});
    List<CommandResult> results = converter.run(inputStream, outputStream);

    boolean success = results.stream().allMatch(CommandResult::isSuccess);
    if (!success) {
      throw new RuntimeException("XML navigation failed");
    }
  }

  // [END xml-navigate-basic]

  // [START character-convert-basic]
  /** Character encoding conversion example. Converts from Shift_JIS to UTF-8. */
  public void characterConvertBasic() throws Exception {
    String data = "Hello World";
    InputStream inputStream = new ByteArrayInputStream(data.getBytes("Shift_JIS"));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Convert from Shift_JIS to UTF-8
    IStreamCommand convertCommand = new CharacterConvertCommand("Shift_JIS", "UTF-8");

    StreamConverter converter = StreamConverter.create(new IStreamCommand[] {convertCommand});
    List<CommandResult> results = converter.run(inputStream, outputStream);

    boolean success = results.stream().allMatch(CommandResult::isSuccess);
    if (!success) {
      throw new RuntimeException("Character conversion failed");
    }
  }

  // [END character-convert-basic]

  // [START line-ending-normalize-basic]
  /** Line ending normalization example. Normalizes line endings to Unix format (LF). */
  public void lineEndingNormalizeBasic() throws Exception {
    String data = "Line 1\r\nLine 2\rLine 3\n";
    InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Normalize to Unix line endings (LF)
    IStreamCommand normalizeCommand = new LineEndingNormalizeCommand(LineEndingType.UNIX);

    StreamConverter converter = StreamConverter.create(new IStreamCommand[] {normalizeCommand});
    List<CommandResult> results = converter.run(inputStream, outputStream);

    boolean success = results.stream().allMatch(CommandResult::isSuccess);
    if (!success) {
      throw new RuntimeException("Line ending normalization failed");
    }
  }

  // [END line-ending-normalize-basic]

  // [START pipeline-simple]
  /** Simple pipeline example - chaining multiple commands. */
  public void simplePipeline() throws Exception {
    String csvData = "id,name,price\n1,Apple,100\n";
    InputStream inputStream = new ByteArrayInputStream(csvData.getBytes("Shift_JIS"));
    OutputStream outputStream = new ByteArrayOutputStream();

    // CSV → Character conversion → Output
    IStreamCommand[] pipeline = {
      new CsvNavigateCommand(new CSVPath("name"), new PassThroughRule()),
      new CharacterConvertCommand("Shift_JIS", "UTF-8")
    };

    StreamConverter converter = StreamConverter.create(pipeline);
    List<CommandResult> results = converter.run(inputStream, outputStream);

    boolean success = results.stream().allMatch(CommandResult::isSuccess);
    if (!success) {
      throw new RuntimeException("Pipeline execution failed");
    }
  }

  // [END pipeline-simple]

  // [START error-handling-basic]
  /** Error handling example using CommandResult. */
  public void errorHandlingBasic() throws Exception {
    String csvData = "id,name\n1,Apple\n";
    InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    IStreamCommand csvCommand = new CsvNavigateCommand(new CSVPath("name"), new PassThroughRule());

    StreamConverter converter = StreamConverter.create(new IStreamCommand[] {csvCommand});
    List<CommandResult> results = converter.run(inputStream, outputStream);

    // Check results
    for (CommandResult result : results) {
      if (!result.isSuccess()) {
        System.err.println("Command failed: " + result.getCommandName());
        System.err.println("Error: " + result.getErrorMessage());
      } else {
        System.out.println(
            "Success: "
                + result.getCommandName()
                + " (duration: "
                + result.getExecutionTimeMillis()
                + "ms)");
      }
    }
  }
  // [END error-handling-basic]
}
