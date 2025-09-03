package com.streamConverter.path;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class TreePathTest {

  @Test
  void testJsonPathParsing() {
    TreePath path = TreePath.fromJson("$.user.name");
    assertEquals("$.user.name", path.toString());

    List<String> expectedPath = List.of("user", "name");
    assertTrue(path.match(expectedPath));
  }

  @Test
  void testXmlPathParsing() {
    TreePath path = TreePath.fromXml("user/profile/name");
    assertEquals("user/profile/name", path.toString());

    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(path.match(expectedPath));
  }

  @Test
  void testSingleSegmentPath() {
    TreePath path = TreePath.fromXml("user");
    assertEquals("user", path.toString());

    List<String> expectedPath = List.of("user");
    assertTrue(path.match(expectedPath));
  }

  @Test
  void testRootJsonPath() {
    TreePath path = TreePath.fromJson("$.");
    assertEquals("$.", path.toString());

    List<String> emptyPath = List.of();
    assertTrue(path.match(emptyPath));
  }

  @Test
  void testRootXmlPath() {
    TreePath path = TreePath.fromXml("/");
    assertEquals("/", path.toString());

    List<String> emptyPath = List.of();
    assertTrue(path.match(emptyPath));
  }

  @Test
  void testPathMatching() {
    TreePath jsonPath = TreePath.fromJson("$.user.profile.name");
    TreePath xmlPath = TreePath.fromXml("user/profile/name");

    List<String> targetPath = List.of("user", "profile", "name");
    List<String> differentPath = List.of("user", "profile", "email");
    List<String> shortPath = List.of("user", "profile");

    // Both JSON and XML paths should match the same hierarchical path
    assertTrue(jsonPath.match(targetPath));
    assertTrue(xmlPath.match(targetPath));

    assertFalse(jsonPath.match(differentPath));
    assertFalse(xmlPath.match(differentPath));

    assertFalse(jsonPath.match(shortPath));
    assertFalse(xmlPath.match(shortPath));
  }

  @Test
  void testNullHandling() {
    TreePath path = TreePath.fromJson("$.user.name");
    assertFalse(path.match(null));
  }

  @Test
  void testEqualsAndHashCode() {
    TreePath jsonPath = TreePath.fromJson("$.user.name");
    TreePath xmlPath = TreePath.fromXml("user/name");
    TreePath differentPath = TreePath.fromJson("$.user.age");

    // Paths with same segments should be equal
    assertEquals(jsonPath, xmlPath);
    assertEquals(jsonPath.hashCode(), xmlPath.hashCode());

    assertNotEquals(jsonPath, differentPath);
    assertNotEquals(jsonPath.hashCode(), differentPath.hashCode());
  }

  @Test
  void testValidationErrors() {
    assertThrows(IllegalArgumentException.class, () -> TreePath.fromJson(null));
    assertThrows(IllegalArgumentException.class, () -> TreePath.fromJson(""));
    assertThrows(IllegalArgumentException.class, () -> TreePath.fromJson("   "));
    assertThrows(IllegalArgumentException.class, () -> TreePath.fromXml(null));
    assertThrows(IllegalArgumentException.class, () -> TreePath.fromXml(""));
    assertThrows(IllegalArgumentException.class, () -> TreePath.fromXml("   "));
  }

  @Test
  void testComplexPaths() {
    // Test path with leading/trailing slashes
    TreePath xmlPath = TreePath.fromXml("/user/profile/name/");
    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(xmlPath.match(expectedPath));

    // Test multiple slashes normalization
    TreePath multiSlashPath = TreePath.fromXml("//user///profile//name//");
    assertTrue(multiSlashPath.match(expectedPath));
  }

  @Test
  void testToString() {
    TreePath path = TreePath.fromJson("$.user.profile.name");
    assertEquals("$.user.profile.name", path.toString());
  }

  @Test
  void testFromJsonPath() {
    TreePath path = TreePath.fromJson("$.user.profile.name");
    assertEquals("$.user.profile.name", path.toString());

    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(path.match(expectedPath));
  }

  @Test
  void testFromXmlPath() {
    TreePath path = TreePath.fromXml("user/profile/name");
    assertEquals("user/profile/name", path.toString());

    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(path.match(expectedPath));
  }

  @Test
  void testFactoryMethodsEquivalent() {
    TreePath jsonPath = TreePath.fromJson("$.user.name");
    TreePath xmlPath = TreePath.fromXml("user/name");
    TreePath directJsonPath = TreePath.fromJson("$.user.name");
    TreePath directXmlPath = TreePath.fromXml("user/name");

    // All should match the same hierarchical path
    List<String> expectedPath = List.of("user", "name");
    assertTrue(jsonPath.match(expectedPath));
    assertTrue(xmlPath.match(expectedPath));
    assertTrue(directJsonPath.match(expectedPath));
    assertTrue(directXmlPath.match(expectedPath));

    // Factory methods should be equivalent to direct construction
    assertEquals(jsonPath, directJsonPath);
    assertEquals(xmlPath, directXmlPath);
    assertEquals(jsonPath, xmlPath); // Same internal representation
  }
}
