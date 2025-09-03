package com.streamConverter.path;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class TreePathTest {

  @Test
  void testJsonPathParsing() {
    TreePath path = new TreePath("$.user.name");
    assertEquals("$.user.name", path.toString());

    List<String> expectedPath = List.of("user", "name");
    assertTrue(path.matches(expectedPath));
  }

  @Test
  void testXmlPathParsing() {
    TreePath path = new TreePath("user/profile/name");
    assertEquals("user/profile/name", path.toString());

    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(path.matches(expectedPath));
  }

  @Test
  void testSingleSegmentPath() {
    TreePath path = new TreePath("user");
    assertEquals("user", path.toString());

    List<String> expectedPath = List.of("user");
    assertTrue(path.matches(expectedPath));
  }

  @Test
  void testRootJsonPath() {
    TreePath path = new TreePath("$.");
    assertEquals("$.", path.toString());

    List<String> emptyPath = List.of();
    assertTrue(path.matches(emptyPath));
  }

  @Test
  void testRootXmlPath() {
    TreePath path = new TreePath("/");
    assertEquals("/", path.toString());

    List<String> emptyPath = List.of();
    assertTrue(path.matches(emptyPath));
  }

  @Test
  void testPathMatching() {
    TreePath jsonPath = new TreePath("$.user.profile.name");
    TreePath xmlPath = new TreePath("user/profile/name");

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
    TreePath path = new TreePath("$.user.name");
    assertFalse(path.matches(null));
  }

  @Test
  void testEqualsAndHashCode() {
    TreePath jsonPath = new TreePath("$.user.name");
    TreePath xmlPath = new TreePath("user/name");
    TreePath differentPath = new TreePath("$.user.age");

    // Paths with same segments should be equal
    assertEquals(jsonPath, xmlPath);
    assertEquals(jsonPath.hashCode(), xmlPath.hashCode());

    assertNotEquals(jsonPath, differentPath);
    assertNotEquals(jsonPath.hashCode(), differentPath.hashCode());
  }

  @Test
  void testValidationErrors() {
    assertThrows(IllegalArgumentException.class, () -> new TreePath((String) null));
    assertThrows(IllegalArgumentException.class, () -> new TreePath(""));
    assertThrows(IllegalArgumentException.class, () -> new TreePath("   "));
  }

  @Test
  void testComplexPaths() {
    // Test path with leading/trailing slashes
    TreePath xmlPath = new TreePath("/user/profile/name/");
    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(xmlPath.matches(expectedPath));

    // Test multiple slashes normalization
    TreePath multiSlashPath = new TreePath("//user///profile//name//");
    assertTrue(multiSlashPath.matches(expectedPath));
  }

  @Test
  void testToString() {
    TreePath path = new TreePath("$.user.profile.name");
    assertEquals("$.user.profile.name", path.toString());
  }

  @Test
  void testFromJsonPath() {
    TreePath path = new TreePath("$.user.profile.name");
    assertEquals("$.user.profile.name", path.toString());

    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(path.matches(expectedPath));
  }

  @Test
  void testFromXmlPath() {
    TreePath path = new TreePath("user/profile/name");
    assertEquals("user/profile/name", path.toString());

    List<String> expectedPath = List.of("user", "profile", "name");
    assertTrue(path.matches(expectedPath));
  }

  @Test
  void testFactoryMethodsEquivalent() {
    TreePath jsonPath = new TreePath("$.user.name");
    TreePath xmlPath = new TreePath("user/name");
    TreePath directJsonPath = new TreePath("$.user.name");
    TreePath directXmlPath = new TreePath("user/name");

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
