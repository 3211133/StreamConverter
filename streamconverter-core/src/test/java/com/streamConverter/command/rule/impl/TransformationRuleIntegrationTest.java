package com.streamConverter.command.rule.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.rule.impl.casing.CamelToSnakeCaseRule;
import com.streamConverter.command.rule.impl.casing.SnakeToCamelCaseRule;
import com.streamConverter.command.rule.impl.composite.ChainRule;
import com.streamConverter.command.rule.impl.string.LowerCaseRule;
import com.streamConverter.command.rule.impl.string.TrimRule;
import com.streamConverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for transformation rules with Navigate commands.
 *
 * <p>Updated to match the new JsonNavigateCommand behavior which preserves JSON structure while
 * transforming matching fields, rather than extracting values.
 */
class TransformationRuleIntegrationTest {

  @Test
  void testCamelToSnakeCaseWithJsonNavigate() throws Exception {
    String inputJson = "{\"userName\": \"john_doe\", \"firstName\": \"John\"}";

    IStreamCommand command =
        JsonNavigateCommand.create(TreePath.fromJson("$.userName"), CamelToSnakeCaseRule.create());

    String result = executeCommand(command, inputJson);
    // JsonNavigateCommand preserves structure and transforms the matching field value
    assertTrue(
        result.contains("\"userName\":\"john_doe\""),
        "Should contain transformed userName field value");
    assertTrue(result.contains("\"firstName\":\"John\""), "Should preserve other fields unchanged");
  }

  @Test
  void testSnakeToCamelCaseWithJsonNavigate() throws Exception {
    String inputJson = "{\"user_name\": \"john_doe\", \"first_name\": \"John\"}";

    IStreamCommand command =
        JsonNavigateCommand.create(TreePath.fromJson("$.user_name"), SnakeToCamelCaseRule.create());

    String result = executeCommand(command, inputJson);
    // Structure preserved, user_name field value transformed
    assertTrue(
        result.contains("\"user_name\":\"johnDoe\""), "Should transform user_name field value");
    assertTrue(
        result.contains("\"first_name\":\"John\""), "Should preserve other fields unchanged");
  }

  @Test
  void testPascalCaseConversion() throws Exception {
    String inputJson = "{\"user_name\": \"hello_world\"}";

    IStreamCommand command =
        JsonNavigateCommand.create(
            TreePath.fromJson("$.user_name"), SnakeToCamelCaseRule.createPascalCase());

    String result = executeCommand(command, inputJson);
    // Structure preserved, value transformed to PascalCase
    assertTrue(result.contains("\"user_name\":\"HelloWorld\""), "Should transform to PascalCase");
  }

  @Test
  void testChainRuleWithJsonNavigate() throws Exception {
    String inputJson = "{\"fieldName\": \"  XMLHttpRequest  \"}";

    // Chain: trim -> camel to snake -> lowercase
    ChainRule chainRule =
        ChainRule.builder()
            .addRule(new TrimRule())
            .addRule(CamelToSnakeCaseRule.create())
            .addRule(new LowerCaseRule())
            .build();

    IStreamCommand command =
        JsonNavigateCommand.create(TreePath.fromJson("$.fieldName"), chainRule);

    String result = executeCommand(command, inputJson);
    // Structure preserved, chain transformation applied to field value
    assertTrue(
        result.contains("\"fieldName\":\"xml_http_request\""),
        "Should apply chain transformation to field value");
  }

  @Test
  void testComplexChainTransformation() throws Exception {
    String inputJson = "{\"userAccountID\": \"  MyComplexVariableName  \"}";

    // Complex transformation chain
    ChainRule chainRule =
        ChainRule.of(
            new TrimRule(),
            CamelToSnakeCaseRule.builder().handleAcronyms(true).preserveUnderscores(false).build());

    IStreamCommand command =
        JsonNavigateCommand.create(TreePath.fromJson("$.userAccountID"), chainRule);

    String result = executeCommand(command, inputJson);
    // Structure preserved, complex transformation applied to field value
    assertTrue(
        result.contains("\"userAccountID\":\"my_complex_variable_name\""),
        "Should apply complex chain transformation");
  }

  @Test
  void testRoundTripConversion() throws Exception {
    String inputJson = "{\"original\": \"userName\"}";

    // Convert camelCase to snake_case, then back to camelCase
    ChainRule roundTrip =
        ChainRule.of(CamelToSnakeCaseRule.create(), SnakeToCamelCaseRule.create());

    IStreamCommand command = JsonNavigateCommand.create(TreePath.fromJson("$.original"), roundTrip);

    String result = executeCommand(command, inputJson);
    // Structure preserved, round trip transformation applied
    assertTrue(
        result.contains("\"original\":\"userName\""), "Should apply round trip transformation");
  }

  @Test
  void testMultipleFieldProcessing() throws Exception {
    String inputJson = "{\"firstName\": \"John\", \"lastName\": \"Doe\"}";

    // Process firstName field
    IStreamCommand command1 =
        JsonNavigateCommand.create(TreePath.fromJson("$.firstName"), CamelToSnakeCaseRule.create());
    String result1 = executeCommand(command1, inputJson);
    // Structure preserved, firstName value transformed
    assertTrue(result1.contains("\"firstName\":\"john\""), "Should transform firstName value");
    assertTrue(result1.contains("\"lastName\":\"Doe\""), "Should preserve lastName unchanged");

    // Process lastName field
    IStreamCommand command2 =
        JsonNavigateCommand.create(TreePath.fromJson("$.lastName"), CamelToSnakeCaseRule.create());
    String result2 = executeCommand(command2, inputJson);
    // Structure preserved, lastName value transformed
    assertTrue(result2.contains("\"lastName\":\"doe\""), "Should transform lastName value");
    assertTrue(result2.contains("\"firstName\":\"John\""), "Should preserve firstName unchanged");
  }

  @Test
  void testErrorHandling() throws Exception {
    String inputJson = "{\"field\": null}";

    IStreamCommand command =
        JsonNavigateCommand.create(TreePath.fromJson("$.field"), CamelToSnakeCaseRule.create());

    // Should handle null gracefully and preserve structure
    String result = executeCommand(command, inputJson);
    assertTrue(result.contains("\"field\":null"), "Should preserve null field in structure");
  }

  @Test
  void testNonExistentField() throws Exception {
    String inputJson = "{\"existing\": \"value\"}";

    IStreamCommand command =
        JsonNavigateCommand.create(
            TreePath.fromJson("$.nonExistent"), CamelToSnakeCaseRule.create());

    String result = executeCommand(command, inputJson);
    // Non-existent path should preserve original structure unchanged
    assertTrue(
        result.contains("\"existing\":\"value\""),
        "Should preserve original structure when path not found");
  }

  /** Helper method to execute a command and return the result as a string. */
  private String executeCommand(IStreamCommand command, String input) throws Exception {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    return outputStream.toString(StandardCharsets.UTF_8);
  }
}
