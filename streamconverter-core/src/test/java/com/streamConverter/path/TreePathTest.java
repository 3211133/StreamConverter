package com.streamConverter.path;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class TreePathTest {

  @Test
  void testJsonPathParsing() {
    TreePath path = TreePath.fromJsonPath("$.user.name");
    assertEquals("$.user.name", path.toString());

    List<String> expectedPath = List.of("user", "name");
    assertTrue(path.matches(expectedPath));
  }

  @Test
  void testXmlPathParsing() {
    TreePath path = TreePath.fromXmlPath("user/profile/name");
    assertEquals("user/profile/name", path.toString());

    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(path.matches(expectedPath));
  }

  @Test
  void testSingleSegmentPath() {
    TreePath path = TreePath.fromXmlPath("user");
    assertEquals("user", path.toString());

    List<String> expectedPath = List.of("user");
    assertTrue(path.matches(expectedPath));
  }

  @Test
  void testRootJsonPath() {
    TreePath path = TreePath.fromJsonPath("$.");
    assertEquals("$.", path.toString());

    List<String> emptyPath = List.of();
    assertTrue(path.matches(emptyPath));
  }

  @Test
  void testRootXmlPath() {
    TreePath path = TreePath.fromXmlPath("/");
    assertEquals("/", path.toString());

    List<String> emptyPath = List.of();
    assertTrue(path.matches(emptyPath));
  }

  @Test
  void testPathMatching() {
    TreePath jsonPath = TreePath.fromJsonPath("$.user.profile.name");
    TreePath xmlPath = TreePath.fromXmlPath("user/profile/name");

    List<String> targetPath = List.of("user", "profile", "name");
    List<String> differentPath = List.of("user", "profile", "email");
    List<String> shortPath = List.of("user", "profile");

    // Both JSON and XML paths should match the same hierarchical path
    assertTrue(jsonPath.matches(targetPath));
    assertTrue(xmlPath.matches(targetPath));

    assertFalse(jsonPath.matches(differentPath));
    assertFalse(xmlPath.matches(differentPath));

    assertFalse(jsonPath.matches(shortPath));
    assertFalse(xmlPath.matches(shortPath));
  }

  @Test
  void testNullHandling() {
    TreePath path = TreePath.fromJsonPath("$.user.name");
    assertFalse(path.matches(null));
  }

  @Test
  void testEqualsAndHashCode() {
    TreePath jsonPath = TreePath.fromJsonPath("$.user.name");
    TreePath xmlPath = TreePath.fromXmlPath("user/name");
    TreePath differentPath = TreePath.fromJsonPath("$.user.age");

    // Paths with same segments should be equal
    assertEquals(jsonPath, xmlPath);
    assertEquals(jsonPath.hashCode(), xmlPath.hashCode());

    assertNotEquals(jsonPath, differentPath);
    assertNotEquals(jsonPath.hashCode(), differentPath.hashCode());
  }

  @Test
  void testValidationErrors() {
    assertThrows(IllegalArgumentException.class, () -> TreePath.fromJsonPath(null));
    assertThrows(IllegalArgumentException.class, () -> TreePath.fromJsonPath(""));
    assertThrows(IllegalArgumentException.class, () -> TreePath.fromJsonPath("   "));
  }

  @Test
  void testComplexPaths() {
    // Test path with leading/trailing slashes
    TreePath xmlPath = TreePath.fromXmlPath("/user/profile/name/");
    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(xmlPath.matches(expectedPath));

    // Test multiple slashes normalization
    TreePath multiSlashPath = TreePath.fromXmlPath("//user///profile//name//");
    assertTrue(multiSlashPath.matches(expectedPath));
  }

  @Test
  void testToString() {
    TreePath path = TreePath.fromJsonPath("$.user.profile.name");
    assertEquals("$.user.profile.name", path.toString());
  }

  @Test
  void testFromJsonPath() {
    TreePath path = TreePath.fromJsonPath("$.user.profile.name");
    assertEquals("$.user.profile.name", path.toString());

    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(path.matches(expectedPath));
  }

  @Test
  void testFromXmlPath() {
    TreePath path = TreePath.fromXmlPath("user/profile/name");
    assertEquals("user/profile/name", path.toString());

    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(path.matches(expectedPath));
  }

  @Test
  void testFactoryMethodsEquivalent() {
    TreePath jsonPath = TreePath.fromJsonPath("$.user.name");
    TreePath xmlPath = TreePath.fromXmlPath("user/name");
    TreePath directJsonPath = TreePath.fromJsonPath("$.user.name");
    TreePath directXmlPath = TreePath.fromXmlPath("user/name");

    // All should match the same hierarchical path
    List<String> expectedPath = List.of("user", "name");
    assertTrue(jsonPath.matches(expectedPath));
    assertTrue(xmlPath.matches(expectedPath));
    assertTrue(directJsonPath.matches(expectedPath));
    assertTrue(directXmlPath.matches(expectedPath));

    // Factory methods should be equivalent to direct construction
    assertEquals(jsonPath, directJsonPath);
    assertEquals(xmlPath, directXmlPath);
    assertEquals(jsonPath, xmlPath); // Same internal representation
  }
}
