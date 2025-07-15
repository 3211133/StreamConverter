package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * CSV変換コマンドクラス
 *
 * <p>このクラスは、CSV形式のデータを変換するためのコマンドを実装します。 ストリームを使用して、CSVデータを読み込み、変換後のデータを出力します。
 * 変換対象のXPathである箇所を特定したあとに、変換処理を実行することを想定しています。
 */
public class CsvNavigateCommand extends AbstractStreamCommand {

  private String columnSelector;
  private int columnIndex = -1;

  /**
   * Constructor for CSV navigation with column selector.
   *
   * @param columnSelector the column name or index to select (e.g., "name", "2")
   */
  public CsvNavigateCommand(String columnSelector) {
    this.columnSelector = columnSelector;
  }

  /**
   * Default constructor - processes all columns.
   */
  public CsvNavigateCommand() {
    this.columnSelector = null;
  }
  
  @Override
  protected String getCommandDetails() {
    if (columnSelector != null) {
      return String.format("CsvNavigateCommand(columnSelector='%s')", columnSelector);
    } else {
      return "CsvNavigateCommand(all columns)";
    }
  }

  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      String headerLine = reader.readLine();
      if (headerLine == null) {
        return; // Empty input
      }

      String[] headers = parseCSVLine(headerLine);
      
      // Determine column index if selector is provided
      if (columnSelector != null) {
        columnIndex = findColumnIndex(headers, columnSelector);
        if (columnIndex == -1) {
          throw new IllegalArgumentException("Column not found: " + columnSelector);
        }
      }

      // Write header
      if (columnIndex >= 0) {
        writer.write(headers[columnIndex]);
      } else {
        writer.write(headerLine);
      }
      writer.write(System.lineSeparator());

      // Process data rows
      String line;
      while ((line = reader.readLine()) != null) {
        String[] values = parseCSVLine(line);
        
        if (columnIndex >= 0 && columnIndex < values.length) {
          writer.write(values[columnIndex]);
        } else if (columnIndex < 0) {
          writer.write(line);
        }
        writer.write(System.lineSeparator());
      }
      writer.flush();
    }
  }

  private String[] parseCSVLine(String line) {
    return line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
  }

  private int findColumnIndex(String[] headers, String selector) {
    // Try to find by column name
    for (int i = 0; i < headers.length; i++) {
      if (headers[i].trim().equalsIgnoreCase(selector.trim())) {
        return i;
      }
    }
    
    // Try to parse as column index
    try {
      int index = Integer.parseInt(selector);
      if (index >= 0 && index < headers.length) {
        return index;
      }
    } catch (NumberFormatException e) {
      // Not a number, ignore
    }
    
    return -1;
  }
}
