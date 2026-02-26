package com.streamconverter.examples.docs;

import com.streamconverter.StreamConverter;
import com.streamconverter.StreamProcessingException;
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
   * CSV navigation pipeline example - demonstrates pipeline structure with CSV column navigation.
   * Note: Uses PassThroughRule (no transformation) and a lambda pass-through (placeholder for
   * actual processing).
   */
  public void csvNavigateBasic() throws Exception {
    // Sample CSV data
    String csvData = "id,productName,price\n" + "1,Apple,100\n" + "2,Banana,50\n";
    InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Create pipeline: navigate CSV + process
    IStreamCommand[] pipeline = {
      new CsvNavigateCommand(new CSVPath("productName"), new PassThroughRule()),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };

    StreamConverter converter = StreamConverter.create(pipeline);
    converter.run(inputStream, outputStream);
  }

  // [END csv-navigate-basic]

  // [START csv-filter-basic]
  /**
   * CSV filter pipeline example - demonstrates column extraction in a pipeline. Note: The lambda
   * pass-through is a placeholder for actual processing logic.
   */
  public void csvFilterBasic() throws Exception {
    String csvData = "id,name,price\n" + "1,Apple,100\n" + "2,Banana,50\n";
    InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Create pipeline: filter CSV + process
    IStreamCommand[] pipeline = {
      CsvFilterCommand.create(new CSVPath("price")),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };

    StreamConverter converter = StreamConverter.create(pipeline);
    converter.run(inputStream, outputStream);
  }

  // [END csv-filter-basic]

  // [START json-navigate-basic]
  /**
   * JSON navigation pipeline example - demonstrates pipeline structure with JSON path navigation.
   * Note: Uses PassThroughRule (no transformation) and a lambda pass-through (placeholder for
   * actual processing).
   */
  public void jsonNavigateBasic() throws Exception {
    String jsonData = "{\"user\": {\"name\": \"John\", \"age\": 30}}";
    InputStream inputStream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Create pipeline: navigate JSON + process
    IStreamCommand[] pipeline = {
      new JsonNavigateCommand(TreePath.fromJson("$.user.name"), new PassThroughRule()),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };

    StreamConverter converter = StreamConverter.create(pipeline);
    converter.run(inputStream, outputStream);
  }

  // [END json-navigate-basic]

  // [START xml-navigate-basic]
  /**
   * XML navigation pipeline example - demonstrates pipeline structure with XML path navigation.
   * Note: Uses PassThroughRule (no transformation) and a lambda pass-through (placeholder for
   * actual processing).
   */
  public void xmlNavigateBasic() throws Exception {
    String xmlData = "<?xml version=\"1.0\"?><root><user><name>John</name></user></root>";
    InputStream inputStream = new ByteArrayInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Create pipeline: navigate XML + process
    IStreamCommand[] pipeline = {
      new XmlNavigateCommand(TreePath.fromXml("root/user/name"), new PassThroughRule()),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };

    StreamConverter converter = StreamConverter.create(pipeline);
    converter.run(inputStream, outputStream);
  }

  // [END xml-navigate-basic]

  // [START character-convert-basic]
  /**
   * Character encoding conversion pipeline example - demonstrates charset conversion in a pipeline.
   * Note: The lambda pass-through is a placeholder for actual processing logic.
   */
  public void characterConvertBasic() throws Exception {
    String data = "Hello World";
    InputStream inputStream = new ByteArrayInputStream(data.getBytes("Shift_JIS"));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Create pipeline: convert encoding + process
    IStreamCommand[] pipeline = {
      new CharacterConvertCommand("Shift_JIS", "UTF-8"),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };

    StreamConverter converter = StreamConverter.create(pipeline);
    converter.run(inputStream, outputStream);
  }

  // [END character-convert-basic]

  // [START line-ending-normalize-basic]
  /**
   * Line ending normalization pipeline example - demonstrates line ending conversion in a pipeline.
   * Note: The lambda pass-through is a placeholder for actual processing logic.
   */
  public void lineEndingNormalizeBasic() throws Exception {
    String data = "Line 1\r\nLine 2\rLine 3\n";
    InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Create pipeline: normalize line endings + process
    IStreamCommand[] pipeline = {
      new LineEndingNormalizeCommand(LineEndingType.UNIX),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };

    StreamConverter converter = StreamConverter.create(pipeline);
    converter.run(inputStream, outputStream);
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
    converter.run(inputStream, outputStream);
  }

  // [END pipeline-simple]

  // [START error-handling-basic]
  /**
   * Error handling pipeline example - demonstrates exception-based error detection. Note: The
   * lambda pass-through is a placeholder for actual processing logic.
   */
  public void errorHandlingBasic() throws Exception {
    String csvData = "id,name\n1,Apple\n";
    InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Create pipeline for error handling demonstration
    IStreamCommand[] pipeline = {
      new CsvNavigateCommand(new CSVPath("name"), new PassThroughRule()),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };

    StreamConverter converter = StreamConverter.create(pipeline);
    try {
      converter.run(inputStream, outputStream);
    } catch (StreamProcessingException e) {
      System.err.println("Pipeline failed: " + e.getMessage());
      throw e;
    }
  }
  // [END error-handling-basic]
}
