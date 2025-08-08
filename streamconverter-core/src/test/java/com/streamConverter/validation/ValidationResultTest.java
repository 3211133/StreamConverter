package com.streamConverter.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ValidationResultクラスのテスト */
@DisplayName("ValidationResult Tests")
public class ValidationResultTest {

  @Test
  @DisplayName("Create successful validation result")
  void testCreateSuccessfulValidationResult() {
    long executionTime = 150L;
    ValidationResult result = ValidationResult.success("JSON", "schema/user.json", executionTime);

    assertTrue(result.isValid());
    assertEquals("JSON", result.getValidationType());
    assertEquals("schema/user.json", result.getSchemaPath());
    assertEquals(executionTime, result.getExecutionTimeMillis());
    assertTrue(result.getErrors().isEmpty());
    assertTrue(result.getWarnings().isEmpty());
    assertNotNull(result.getValidationTime());
  }

  @Test
  @DisplayName("Create failed validation result")
  void testCreateFailedValidationResult() {
    List<String> errors =
        Arrays.asList("Field 'email' is required", "Field 'age' must be a number");
    long executionTime = 200L;

    ValidationResult result =
        ValidationResult.failure("JSON", "schema/user.json", errors, executionTime);

    assertFalse(result.isValid());
    assertEquals("JSON", result.getValidationType());
    assertEquals("schema/user.json", result.getSchemaPath());
    assertEquals(executionTime, result.getExecutionTimeMillis());
    assertEquals(2, result.getErrors().size());
    assertTrue(result.getErrors().contains("Field 'email' is required"));
    assertTrue(result.getErrors().contains("Field 'age' must be a number"));
    assertTrue(result.getWarnings().isEmpty());
    assertNotNull(result.getValidationTime());
  }

  @Test
  @DisplayName("Builder pattern basic functionality")
  void testBuilderPatternBasicFunctionality() {
    Instant now = Instant.now();

    ValidationResult result =
        ValidationResult.builder()
            .validationType("CSV")
            .schemaPath("schema/products.csv")
            .success(false)
            .addError("Missing required column: id")
            .addError("Invalid data in row 5")
            .addWarning("Column 'notes' has empty values")
            .validationTime(now)
            .executionTimeMillis(300L)
            .dataSource("test-data.csv")
            .build();

    assertFalse(result.isValid());
    assertEquals("CSV", result.getValidationType());
    assertEquals("schema/products.csv", result.getSchemaPath());
    assertEquals(300L, result.getExecutionTimeMillis());
    assertEquals(now, result.getValidationTime());
    assertEquals("test-data.csv", result.getDataSource());

    assertEquals(2, result.getErrors().size());
    assertTrue(result.getErrors().contains("Missing required column: id"));
    assertTrue(result.getErrors().contains("Invalid data in row 5"));

    assertEquals(1, result.getWarnings().size());
    assertTrue(result.getWarnings().contains("Column 'notes' has empty values"));
  }

  @Test
  @DisplayName("Builder with default values")
  void testBuilderWithDefaultValues() {
    ValidationResult result =
        ValidationResult.builder()
            .validationType("XML")
            .schemaPath("schema/config.xsd")
            .success(true)
            .build();

    assertTrue(result.isValid());
    assertEquals("XML", result.getValidationType());
    assertEquals("schema/config.xsd", result.getSchemaPath());
    assertTrue(result.getErrors().isEmpty());
    assertTrue(result.getWarnings().isEmpty());
    assertNotNull(result.getValidationTime());
    assertEquals(0L, result.getExecutionTimeMillis());
    assertNull(result.getDataSource());
  }

  @Test
  @DisplayName("Validation result immutability")
  void testValidationResultImmutability() {
    ValidationResult.Builder builder =
        ValidationResult.builder()
            .validationType("JSON")
            .schemaPath("schema/test.json")
            .success(false)
            .addError("Test error");

    ValidationResult result = builder.build();

    // Builderに追加の変更を加える
    builder.addError("Additional error").addWarning("Test warning");

    ValidationResult newResult = builder.build();

    // 元の結果は変更されていないことを確認
    assertEquals(1, result.getErrors().size());
    assertEquals(0, result.getWarnings().size());

    // 新しい結果は変更を含むことを確認
    assertEquals(2, newResult.getErrors().size());
    assertEquals(1, newResult.getWarnings().size());
  }

  @Test
  @DisplayName("Error and warning list immutability")
  void testErrorAndWarningListImmutability() {
    ValidationResult result =
        ValidationResult.builder()
            .validationType("CSV")
            .schemaPath("test.csv")
            .success(false)
            .addError("Original error")
            .addWarning("Original warning")
            .build();

    List<String> errors = result.getErrors();
    List<String> warnings = result.getWarnings();

    // リストが変更不可能であることを確認
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          errors.add("New error");
        });

    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          warnings.add("New warning");
        });
  }

  @Test
  @DisplayName("Builder validation - null validation type throws exception")
  void testBuilderValidationNullValidationType() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ValidationResult.builder()
                    .validationType(null)
                    .schemaPath("test.json")
                    .success(true)
                    .build());

    assertEquals("Validation type cannot be null or empty", exception.getMessage());
  }

  @Test
  @DisplayName("Builder validation - empty validation type throws exception")
  void testBuilderValidationEmptyValidationType() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ValidationResult.builder()
                    .validationType("")
                    .schemaPath("test.json")
                    .success(true)
                    .build());

    assertEquals("Validation type cannot be null or empty", exception.getMessage());
  }

  @Test
  @DisplayName("Builder validation - null schema path throws exception")
  void testBuilderValidationNullSchemaPath() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ValidationResult.builder()
                    .validationType("JSON")
                    .schemaPath(null)
                    .success(true)
                    .build());

    assertEquals("Schema path cannot be null or empty", exception.getMessage());
  }

  @Test
  @DisplayName("Builder validation - success with errors throws exception")
  void testBuilderValidationSuccessWithErrors() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                ValidationResult.builder()
                    .validationType("JSON")
                    .schemaPath("test.json")
                    .success(true)
                    .addError("This should not be allowed")
                    .build());

    assertEquals(
        "ValidationResult cannot be marked as valid when errors are present",
        exception.getMessage());
  }

  @Test
  @DisplayName("Builder validation - multiple errors accumulation")
  void testBuilderMultipleErrorsAccumulation() {
    ValidationResult result =
        ValidationResult.builder()
            .validationType("XML")
            .schemaPath("test.xsd")
            .success(false)
            .addError("Error 1")
            .addError("Error 2")
            .addError("Error 3")
            .build();

    assertEquals(3, result.getErrors().size());
    assertEquals("Error 1", result.getErrors().get(0));
    assertEquals("Error 2", result.getErrors().get(1));
    assertEquals("Error 3", result.getErrors().get(2));
  }

  @Test
  @DisplayName("Builder validation - multiple warnings accumulation")
  void testBuilderMultipleWarningsAccumulation() {
    ValidationResult result =
        ValidationResult.builder()
            .validationType("CSV")
            .schemaPath("test.csv")
            .success(true)
            .addWarning("Warning 1")
            .addWarning("Warning 2")
            .build();

    assertEquals(2, result.getWarnings().size());
    assertEquals("Warning 1", result.getWarnings().get(0));
    assertEquals("Warning 2", result.getWarnings().get(1));
  }

  @Test
  @DisplayName("toString method provides meaningful output")
  void testToStringMethod() {
    ValidationResult result =
        ValidationResult.builder()
            .validationType("JSON")
            .schemaPath("schema/user.json")
            .success(false)
            .addError("Missing required field")
            .executionTimeMillis(123L)
            .build();

    String toString = result.toString();

    assertTrue(toString.contains("JSON"));
    assertTrue(toString.contains("schema/user.json"));
    assertTrue(toString.contains("valid=false"));
    assertTrue(toString.contains("errors=1"));
    assertTrue(toString.contains("123"));
  }

  @Test
  @DisplayName("Success factory method with all parameters")
  void testSuccessFactoryMethodWithAllParameters() {
    String dataSource = "input.json";
    long executionTime = 250L;

    ValidationResult result =
        ValidationResult.builder()
            .validationType("JSON")
            .schemaPath("schema.json")
            .success(true)
            .executionTimeMillis(executionTime)
            .dataSource(dataSource)
            .build();

    assertTrue(result.isValid());
    assertEquals("JSON", result.getValidationType());
    assertEquals("schema.json", result.getSchemaPath());
    assertEquals(executionTime, result.getExecutionTimeMillis());
    assertEquals(dataSource, result.getDataSource());
    assertTrue(result.getErrors().isEmpty());
    assertTrue(result.getWarnings().isEmpty());
  }

  @Test
  @DisplayName("Failure factory method with all parameters")
  void testFailureFactoryMethodWithAllParameters() {
    List<String> errors = Arrays.asList("Error 1", "Error 2");
    String dataSource = "bad-input.csv";
    long executionTime = 300L;

    ValidationResult result =
        ValidationResult.builder()
            .validationType("CSV")
            .schemaPath("schema.csv")
            .success(false)
            .executionTimeMillis(executionTime)
            .dataSource(dataSource)
            .addError("Error 1")
            .addError("Error 2")
            .build();

    assertFalse(result.isValid());
    assertEquals("CSV", result.getValidationType());
    assertEquals("schema.csv", result.getSchemaPath());
    assertEquals(executionTime, result.getExecutionTimeMillis());
    assertEquals(dataSource, result.getDataSource());
    assertEquals(2, result.getErrors().size());
    assertTrue(result.getWarnings().isEmpty());
  }

  @Test
  @DisplayName("Validation result with execution time bounds")
  void testValidationResultWithExecutionTimeBounds() {
    // 負の実行時間でも受け入れられる（実装の柔軟性のため）
    ValidationResult result1 =
        ValidationResult.builder()
            .validationType("JSON")
            .schemaPath("test.json")
            .success(true)
            .executionTimeMillis(-10L)
            .build();

    assertEquals(-10L, result1.getExecutionTimeMillis());

    // 非常に大きな実行時間も受け入れられる
    ValidationResult result2 =
        ValidationResult.builder()
            .validationType("JSON")
            .schemaPath("test.json")
            .success(true)
            .executionTimeMillis(Long.MAX_VALUE)
            .build();

    assertEquals(Long.MAX_VALUE, result2.getExecutionTimeMillis());
  }

  @Test
  @DisplayName("Validation result equality and hash code")
  void testValidationResultEqualityAndHashCode() {
    Instant now = Instant.now();

    ValidationResult result1 =
        ValidationResult.builder()
            .validationType("JSON")
            .schemaPath("test.json")
            .success(true)
            .validationTime(now)
            .executionTimeMillis(100L)
            .build();

    ValidationResult result2 =
        ValidationResult.builder()
            .validationType("JSON")
            .schemaPath("test.json")
            .success(true)
            .validationTime(now)
            .executionTimeMillis(100L)
            .build();

    ValidationResult result3 =
        ValidationResult.builder()
            .validationType("CSV")
            .schemaPath("test.csv")
            .success(true)
            .validationTime(now)
            .executionTimeMillis(100L)
            .build();

    // 同じ内容のオブジェクトは等しい
    assertEquals(result1, result2);
    assertEquals(result1.hashCode(), result2.hashCode());

    // 異なる内容のオブジェクトは等しくない
    assertNotEquals(result1, result3);
    assertNotEquals(result1.hashCode(), result3.hashCode());
  }
}
