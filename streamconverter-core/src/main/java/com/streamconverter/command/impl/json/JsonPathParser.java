package com.streamconverter.command.impl.json;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a JSON path expression string into an ordered list of {@link PathSegment}s.
 *
 * <p>Supported syntax:
 *
 * <ul>
 *   <li>{@code $} – root (empty list)
 *   <li>{@code $.name} – top-level field
 *   <li>{@code $[*].name} – root-array wildcard then field
 *   <li>{@code $.users[*].profile.department} – field, wildcard, nested fields
 * </ul>
 */
final class JsonPathParser {

  private JsonPathParser() {}

  /**
   * Parse a path string into an ordered list of {@link PathSegment}s.
   *
   * @param path the path expression
   * @return ordered list of path segments (empty = root)
   */
  static List<PathSegment> parse(String path) {
    if ("$".equals(path)) {
      return new ArrayList<>();
    }
    int start = path.startsWith("$") ? 1 : 0;
    return parseSegments(path.substring(start));
  }

  private static List<PathSegment> parseSegments(String rest) {
    List<PathSegment> result = new ArrayList<>();
    int i = 0;
    while (i < rest.length()) {
      char c = rest.charAt(i);
      if (c == '.') {
        i++;
      } else if (c == '[') {
        int advance = parseArraySegment(rest, i, result);
        if (advance < 0) {
          break;
        }
        i = advance;
      } else {
        i = parseFieldSegment(rest, i, result);
      }
    }
    return result;
  }

  private static int parseArraySegment(String rest, int i, List<PathSegment> result) {
    int close = rest.indexOf(']', i);
    if (close == -1) {
      return -1;
    }
    String inner = rest.substring(i + 1, close);
    if ("*".equals(inner)) {
      result.add(PathSegment.wildcard());
    } else {
      try {
        result.add(PathSegment.index(Integer.parseInt(inner)));
      } catch (NumberFormatException e) {
        result.add(PathSegment.wildcard());
      }
    }
    return close + 1;
  }

  private static int parseFieldSegment(String rest, int i, List<PathSegment> result) {
    int end = i;
    while (end < rest.length() && rest.charAt(end) != '.' && rest.charAt(end) != '[') {
      end++;
    }
    String fieldName = rest.substring(i, end);
    if (!fieldName.isEmpty()) {
      result.add(PathSegment.field(fieldName));
    }
    return end;
  }
}
