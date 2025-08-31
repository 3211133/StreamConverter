package com.streamConverter.path;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamConverter.path.TreePath.PathFormat;
import com.streamConverter.path.TreePath.PathSegment;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TreePathTest {

  @Test
  void testFromJsonPath_SimpleProperty() {
    TreePath path = TreePath.fromJsonPath("$.user");

    assertEquals("$.user", path.toString());
    assertEquals(PathFormat.JSON, path.getSourceFormat());
    assertEquals(1, path.getDepth());

    List<PathSegment> segments = path.getSegments();
    assertEquals(1, segments.size());
    assertEquals("user", segments.get(0).getName());
    assertFalse(segments.get(0).hasArrayAccess());
  }

  @Test
  void testFromJsonPath_NestedProperty() {
    TreePath path = TreePath.fromJsonPath("$.user.profile.name");

    assertEquals(3, path.getDepth());

    List<PathSegment> segments = path.getSegments();
    assertEquals("user", segments.get(0).getName());
    assertEquals("profile", segments.get(1).getName());
    assertEquals("name", segments.get(2).getName());
  }

  @Test
  void testFromJsonPath_ArrayAccess() {
    TreePath path = TreePath.fromJsonPath("$.items[0].title");

    List<PathSegment> segments = path.getSegments();
    assertEquals(2, segments.size());

    PathSegment itemsSegment = segments.get(0);
    assertEquals("items", itemsSegment.getName());
    assertTrue(itemsSegment.hasArrayAccess());
    assertEquals(Optional.of(0), itemsSegment.getArrayIndex());
    assertFalse(itemsSegment.isWildcard());

    PathSegment titleSegment = segments.get(1);
    assertEquals("title", titleSegment.getName());
    assertFalse(titleSegment.hasArrayAccess());
  }

  @Test
  void testFromJsonPath_WildcardAccess() {
    TreePath path = TreePath.fromJsonPath("$.users[*].name");

    List<PathSegment> segments = path.getSegments();
    assertEquals(2, segments.size());

    PathSegment usersSegment = segments.get(0);
    assertEquals("users", usersSegment.getName());
    assertTrue(usersSegment.hasArrayAccess());
    assertTrue(usersSegment.isWildcard());
    assertEquals(Optional.empty(), usersSegment.getArrayIndex());
  }

  @Test
  void testFromXmlPath_Simple() {
    TreePath path = TreePath.fromXmlPath("user/name");

    assertEquals("user/name", path.toString());
    assertEquals(PathFormat.XML, path.getSourceFormat());
    assertEquals(2, path.getDepth());

    List<PathSegment> segments = path.getSegments();
    assertEquals("user", segments.get(0).getName());
    assertEquals("name", segments.get(1).getName());
  }

  @Test
  void testFromXmlPath_WithArrayAccess() {
    TreePath path = TreePath.fromXmlPath("items/item[0]/title");

    List<PathSegment> segments = path.getSegments();
    assertEquals(3, segments.size());

    assertEquals("items", segments.get(0).getName());
    assertFalse(segments.get(0).hasArrayAccess());

    assertEquals("item", segments.get(1).getName());
    assertTrue(segments.get(1).hasArrayAccess());
    assertEquals(Optional.of(0), segments.get(1).getArrayIndex());

    assertEquals("title", segments.get(2).getName());
    assertFalse(segments.get(2).hasArrayAccess());
  }

  @Test
  void testPathFormatConversion_JsonToXml() {
    TreePath jsonPath = TreePath.fromJsonPath("$.user.profile.name");

    assertEquals("$.user.profile.name", jsonPath.toJsonPath());
    assertEquals("user/profile/name", jsonPath.toXmlPath());
    assertEquals("$.user.profile.name", jsonPath.toOriginalFormat());
  }

  @Test
  void testPathFormatConversion_XmlToJson() {
    TreePath xmlPath = TreePath.fromXmlPath("user/profile/name");

    assertEquals("$.user.profile.name", xmlPath.toJsonPath());
    assertEquals("user/profile/name", xmlPath.toXmlPath());
    assertEquals("user/profile/name", xmlPath.toOriginalFormat());
  }

  @Test
  void testPathFormatConversion_WithArrayAccess() {
    TreePath jsonPath = TreePath.fromJsonPath("$.items[0].title");
    TreePath xmlPath = TreePath.fromXmlPath("items/item[0]/title");

    assertEquals("$.items[0].title", jsonPath.toJsonPath());
    assertEquals("items[0]/title", jsonPath.toXmlPath());

    assertEquals("$.items.item[0].title", xmlPath.toJsonPath());
    assertEquals("items/item[0]/title", xmlPath.toXmlPath());
  }

  @Test
  void testFromSegments() {
    List<PathSegment> segments =
        List.of(
            new PathSegment("user"), new PathSegment("items", 0, false), new PathSegment("title"));

    TreePath path = TreePath.fromSegments(segments);

    assertEquals("$.user.items[0].title", path.toJsonPath());
    assertEquals("user/items[0]/title", path.toXmlPath());
    assertEquals(3, path.getDepth());
  }

  @Test
  void testChildMethods() {
    TreePath basePath = TreePath.fromJsonPath("$.user");

    TreePath childPath = basePath.child("name");
    assertEquals("$.user.name", childPath.toJsonPath());
    assertEquals("user/name", childPath.toXmlPath());

    TreePath arrayChildPath = basePath.childWithArray("items", 0);
    assertEquals("$.user.items[0]", arrayChildPath.toJsonPath());
    assertEquals("user/items[0]", arrayChildPath.toXmlPath());

    TreePath wildcardChildPath = basePath.childWithWildcard("tags");
    assertEquals("$.user.tags[*]", wildcardChildPath.toJsonPath());
    assertEquals("user/tags[*]", wildcardChildPath.toXmlPath());
  }

  @Test
  void testIsDescendantOf() {
    TreePath parent = TreePath.fromJsonPath("$.user");
    TreePath child = TreePath.fromJsonPath("$.user.profile");
    TreePath grandchild = TreePath.fromJsonPath("$.user.profile.name");
    TreePath unrelated = TreePath.fromJsonPath("$.admin");

    assertTrue(child.isDescendantOf(parent));
    assertTrue(grandchild.isDescendantOf(parent));
    assertTrue(grandchild.isDescendantOf(child));
    assertFalse(parent.isDescendantOf(child));
    assertFalse(unrelated.isDescendantOf(parent));
  }

  @Test
  void testJsonExtraction() throws Exception {
    TreePath path = TreePath.fromJsonPath("$.user.name");

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonData = mapper.readTree("{\"user\": {\"name\": \"John\", \"age\": 30}}");

    // Test simplified to check matching only
    boolean matches = path.matches(jsonData);

    // Extract functionality removed in simplified design
    assertTrue(matches);
  }

  @Test
  void testJsonMatching() throws Exception {
    TreePath path = TreePath.fromJsonPath("$.user.name");

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonData = mapper.readTree("{\"user\": {\"name\": \"John\", \"age\": 30}}");
    JsonNode jsonDataWithoutName = mapper.readTree("{\"user\": {\"age\": 30}}");

    assertTrue(path.matches(jsonData));
    assertFalse(path.matches(jsonDataWithoutName));
  }

  @Test
  void testXmlMatching() {
    TreePath path = TreePath.fromXmlPath("user/profile/name");

    List<String> matchingXmlPath = List.of("user", "profile", "name");
    List<String> nonMatchingXmlPath = List.of("user", "profile", "email");
    List<String> shortXmlPath = List.of("user", "profile");

    assertTrue(path.matches(matchingXmlPath));
    assertFalse(path.matches(nonMatchingXmlPath));
    assertFalse(path.matches(shortXmlPath));
  }

  @Test
  void testRootPath() {
    TreePath jsonRoot = TreePath.fromJsonPath("$");
    TreePath xmlRoot = TreePath.fromXmlPath("");

    assertEquals(0, jsonRoot.getDepth());
    assertEquals(0, xmlRoot.getDepth());
    assertEquals("$", jsonRoot.toJsonPath());
    assertEquals("", xmlRoot.toXmlPath());
  }

  @Test
  void testEqualsAndHashCode() {
    TreePath path1 = TreePath.fromJsonPath("$.user.name");
    TreePath path2 = TreePath.fromXmlPath("user/name");
    TreePath path3 = TreePath.fromJsonPath("$.user.age");

    // 内部セグメントが同じなら等価
    assertEquals(path1, path2);
    assertEquals(path1.hashCode(), path2.hashCode());

    assertNotEquals(path1, path3);
    assertNotEquals(path1.hashCode(), path3.hashCode());
  }

  @Test
  void testValidationErrors() {
    assertThrows(IllegalArgumentException.class, () -> TreePath.fromJsonPath(null));

    assertThrows(IllegalArgumentException.class, () -> TreePath.fromJsonPath(""));

    assertThrows(
        IllegalArgumentException.class, () -> TreePath.fromJsonPath("user.name")); // $. で始まらない

    assertThrows(
        IllegalArgumentException.class,
        () -> TreePath.fromJsonPath("$.items[invalid]")); // 不正な配列インデックス
  }

  @Test
  void testPathSegmentToString() {
    PathSegment simple = new PathSegment("user");
    PathSegment indexed = new PathSegment("items", 0, false);
    PathSegment wildcard = new PathSegment("tags", null, true);

    assertEquals("user", simple.toString());
    assertEquals("items[0]", indexed.toString());
    assertEquals("tags[*]", wildcard.toString());
  }

  @Test
  void testToString() {
    TreePath jsonPath = TreePath.fromJsonPath("$.user.name");
    TreePath xmlPath = TreePath.fromXmlPath("user/name");

    // Simplified toString() returns original path only
    assertEquals("$.user.name", jsonPath.toString());
    assertEquals("user/name", xmlPath.toString());
  }
}
