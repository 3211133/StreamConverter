# ClasspathResourceValidator

## Overview

`ClasspathResourceValidator` provides safe access to JAR-bundled static resources.

## When to Use

**Use for:**
- XSD schemas, JSON schemas bundled in JAR
- Application templates and configuration files
- Static resources packaged with the application
- Resources that don't change after deployment

## Security

- ClassLoader performs exact-name lookup; no filesystem traversal or path normalization
- Path traversal patterns (`../`) and backslashes are treated as non-existent resources
- No symlink vulnerabilities (JAR contents are immutable)
- No TOCTOU race conditions
- Leading slashes are automatically stripped for ClassLoader compatibility

## Example

```java
// Get resource as InputStream
InputStream stream = ClasspathResourceValidator.getResourceAsStream("schemas/test.xsd");
try (stream) {
    // Process the resource
}

// Get resource as URL
URL url = ClasspathResourceValidator.getResourceUrl("templates/email.html");
```

## Path Format

- Use forward slashes (`/`) as path separators
- Do not include leading slash (automatically stripped if present)
- Path is relative to classpath root (e.g., `src/main/resources/`)

## Features

### Automatic Path Normalization
- Leading slashes are automatically removed
- Context ClassLoader is preferred (falls back to class ClassLoader)

### Error Handling
```java
// Null or empty path
ClasspathResourceValidator.getResourceAsStream(null);  // NullPointerException
ClasspathResourceValidator.getResourceAsStream("");    // IllegalArgumentException

// Non-existent resource
ClasspathResourceValidator.getResourceAsStream("nonexistent.txt");  // IllegalArgumentException
```

## Implementation Details

### Security Model
ClassLoader's built-in security features:
- Resources are loaded from sealed JAR files
- Path traversal attempts result in null (resource not found)
- No filesystem access, no symlink risks
- Immutable resources (cannot be modified at runtime)

### Context ClassLoader Support
The validator prefers the thread's context ClassLoader, which ensures compatibility with:
- Application servers (Tomcat, Jetty, WildFly)
- Test frameworks (JUnit, TestNG)
- Plugin systems with custom class loaders

## Use Cases

### Schema Validation
```java
// JSON Schema
InputStream jsonSchema = ClasspathResourceValidator
    .getResourceAsStream("schemas/user-schema.json");

// XML Schema (XSD)
URL xsdUrl = ClasspathResourceValidator
    .getResourceUrl("schemas/order.xsd");
```

### Template Processing
```java
// Email templates
InputStream template = ClasspathResourceValidator
    .getResourceAsStream("templates/welcome-email.html");
```

### Configuration Files
```java
// Default configuration
InputStream config = ClasspathResourceValidator
    .getResourceAsStream("config/defaults.properties");
```

## Non-ASCII Resource Names

The validator supports non-ASCII characters in resource names:

```java
// Japanese characters in path
InputStream stream = ClasspathResourceValidator
    .getResourceAsStream("日本語/ファイル.txt");
```

URLs are automatically encoded according to RFC 3986.

## Testing

Test resources should be placed in `src/test/resources/`:

```java
@Test
void testResourceLoading() {
    InputStream stream = ClasspathResourceValidator
        .getResourceAsStream("test-data/sample.json");
    assertNotNull(stream);
}
```

## Migration from File-based Loading

### Before (File-based)
```java
File schemaFile = new File(schemaPath);
if (!schemaFile.exists()) {
    throw new Exception("Schema not found");
}
InputStream stream = new FileInputStream(schemaFile);
```

### After (Classpath-based)
```java
InputStream stream = ClasspathResourceValidator
    .getResourceAsStream(schemaPath);
// Exception automatically thrown if not found
```

## Limitations

### Not Suitable For:
- User-uploaded files (use filesystem with proper validation)
- Files that change after deployment
- Files generated at runtime
- External configuration that varies by environment

For dynamic file handling, use standard Java NIO with appropriate security measures.
