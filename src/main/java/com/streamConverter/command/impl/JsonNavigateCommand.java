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
 * JSON変換コマンドクラス
 *
 * <p>このクラスは、JSON形式のデータを変換するためのコマンドを実装します。 ストリームを使用して、JSONデータを読み込み、変換後のデータを出力します。
 * 変換対象のXPathである箇所を特定したあとに、変換処理を実行することを想定しています。
 */
public class JsonNavigateCommand extends AbstractStreamCommand {

  private String jsonPath;

  /**
   * Constructor for JSON navigation with JSONPath selector.
   *
   * @param jsonPath the JSONPath expression to select data (e.g., "$.users[*].name")
   */
  public JsonNavigateCommand(String jsonPath) {
    this.jsonPath = jsonPath;
  }

  /**
   * Default constructor - processes entire JSON.
   */
  public JsonNavigateCommand() {
    this.jsonPath = null;
  }
  
  @Override
  protected String getCommandDetails() {
    if (jsonPath != null) {
      return String.format("JsonNavigateCommand(jsonPath='%s')", jsonPath);
    } else {
      return "JsonNavigateCommand(entire JSON)";
    }
  }

  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      StringBuilder jsonBuilder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        jsonBuilder.append(line);
      }

      String jsonContent = jsonBuilder.toString();
      if (jsonContent.trim().isEmpty()) {
        return;
      }

      String result;
      if (jsonPath == null) {
        // Return formatted JSON
        result = formatJson(jsonContent);
      } else {
        // Apply JSONPath navigation
        result = navigateJson(jsonContent, jsonPath);
      }

      writer.write(result);
      writer.flush();
    }
  }

  private String formatJson(String json) {
    // Simple JSON formatting - add newlines and indentation
    StringBuilder formatted = new StringBuilder();
    int indent = 0;
    boolean inString = false;
    boolean escaped = false;

    for (char c : json.toCharArray()) {
      if (escaped) {
        formatted.append(c);
        escaped = false;
        continue;
      }

      if (c == '\\') {
        escaped = true;
        formatted.append(c);
        continue;
      }

      if (c == '"') {
        inString = !inString;
        formatted.append(c);
        continue;
      }

      if (inString) {
        formatted.append(c);
        continue;
      }

      switch (c) {
        case '{':
        case '[':
          formatted.append(c).append('\n');
          indent++;
          addIndentation(formatted, indent);
          break;
        case '}':
        case ']':
          formatted.append('\n');
          indent--;
          addIndentation(formatted, indent);
          formatted.append(c);
          break;
        case ',':
          formatted.append(c).append('\n');
          addIndentation(formatted, indent);
          break;
        default:
          formatted.append(c);
          break;
      }
    }

    return formatted.toString();
  }

  private void addIndentation(StringBuilder sb, int indent) {
    for (int i = 0; i < indent; i++) {
      sb.append("  ");
    }
  }

  private String navigateJson(String json, String path) {
    // Simple JSONPath implementation for basic navigation
    try {
      if (path.startsWith("$.")) {
        path = path.substring(2);
      }

      // Handle array access like "users[0].name"
      if (path.contains("[") && path.contains("]")) {
        return navigateWithArray(json, path);
      }

      // Handle simple property access like "users.name"
      return navigateWithProperty(json, path);
    } catch (Exception e) {
      return "Error navigating JSON: " + e.getMessage();
    }
  }

  private String navigateWithProperty(String json, String path) {
    String[] parts = path.split("\\.");
    String current = json;

    for (String part : parts) {
      current = extractProperty(current, part);
      if (current == null) {
        return "null";
      }
    }

    return current;
  }

  private String navigateWithArray(String json, String path) {
    // Simple array navigation - just return the first element for demo
    if (path.contains("[*]")) {
      String propertyPath = path.replace("[*]", "");
      return navigateWithProperty(json, propertyPath);
    }
    return navigateWithProperty(json, path.replaceAll("\\[\\d+\\]", ""));
  }

  private String extractProperty(String json, String property) {
    String searchKey = "\"" + property + "\"";
    int keyIndex = json.indexOf(searchKey);
    if (keyIndex == -1) {
      return null;
    }

    int colonIndex = json.indexOf(":", keyIndex);
    if (colonIndex == -1) {
      return null;
    }

    int start = colonIndex + 1;
    while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
      start++;
    }

    if (start >= json.length()) {
      return null;
    }

    // Handle string values
    if (json.charAt(start) == '"') {
      int end = json.indexOf('"', start + 1);
      return json.substring(start + 1, end);
    }

    // Handle numeric/boolean values
    int end = start;
    while (end < json.length() && 
           json.charAt(end) != ',' && 
           json.charAt(end) != '}' && 
           json.charAt(end) != ']' && 
           !Character.isWhitespace(json.charAt(end))) {
      end++;
    }

    return json.substring(start, end);
  }
}
