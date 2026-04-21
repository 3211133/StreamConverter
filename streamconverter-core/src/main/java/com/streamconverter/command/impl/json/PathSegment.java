package com.streamconverter.command.impl.json;

/** A single segment in a parsed JSON path. */
final class PathSegment {

  final String fieldName;
  final boolean isWildcard;
  final int arrayIndex;

  static PathSegment field(String name) {
    return new PathSegment(name, false, -1);
  }

  static PathSegment wildcard() {
    return new PathSegment(null, true, -1);
  }

  static PathSegment index(int i) {
    return new PathSegment(null, false, i);
  }

  private PathSegment(String fieldName, boolean isWildcard, int arrayIndex) {
    this.fieldName = fieldName;
    this.isWildcard = isWildcard;
    this.arrayIndex = arrayIndex;
  }

  boolean isField() {
    return fieldName != null;
  }
}
