package com.streamConverter.command.rule.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.rule.impl.casing.CamelToSnakeCaseRule;
import com.streamConverter.command.rule.impl.casing.SnakeToCamelCaseRule;
import com.streamConverter.command.rule.impl.composite.ChainRule;
import com.streamConverter.command.rule.impl.string.LowerCaseRule;
import com.streamConverter.command.rule.impl.string.TrimRule;
import com.streamConverter.path.JSONPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Integration tests for transformation rules with Navigate commands. */
class TransformationRuleIntegrationTest {

  @Test
  void testCamelToSnakeCaseWithJsonNavigate() throws Exception {
    String inputJson = "{\"userName\": \"john_doe\", \"firstName\": \"John\"}";

    IStreamCommand command =
        JsonNavigateCommand.createExtractValue(
            new JSONPath("$.userName"), CamelToSnakeCaseRule.create());

    String result = executeCommand(command, inputJson);
    assertEquals("john_doe", result.trim());
  }

  @Test
  void testSnakeToCamelCaseWithJsonNavigate() throws Exception {
    String inputJson = "{\"user_name\": \"john_doe\", \"first_name\": \"John\"}";

    IStreamCommand command =
        JsonNavigateCommand.createExtractValue(
            new JSONPath("$.user_name"), SnakeToCamelCaseRule.create());

    String result = executeCommand(command, inputJson);
    assertEquals("johnDoe", result.trim());
  }

  @Test
  void testPascalCaseConversion() throws Exception {
    String inputJson = "{\"user_name\": \"hello_world\"}";

    IStreamCommand command =
        JsonNavigateCommand.createExtractValue(
            new JSONPath("$.user_name"), SnakeToCamelCaseRule.createPascalCase());

    String result = executeCommand(command, inputJson);
    assertEquals("HelloWorld", result.trim());
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
        JsonNavigateCommand.createExtractValue(new JSONPath("$.fieldName"), chainRule);

    String result = executeCommand(command, inputJson);
    assertEquals("xml_http_request", result.trim());
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
        JsonNavigateCommand.createExtractValue(new JSONPath("$.userAccountID"), chainRule);

    String result = executeCommand(command, inputJson);
    assertEquals("my_complex_variable_name", result.trim());
  }

  @Test
  void testRoundTripConversion() throws Exception {
    String inputJson = "{\"original\": \"userName\"}";

    // Convert camelCase to snake_case, then back to camelCase
    ChainRule roundTrip =
        ChainRule.of(CamelToSnakeCaseRule.create(), SnakeToCamelCaseRule.create());

    IStreamCommand command =
        JsonNavigateCommand.createExtractValue(new JSONPath("$.original"), roundTrip);

    String result = executeCommand(command, inputJson);
    assertEquals("userName", result.trim());
  }

  @Test
  void testMultipleFieldProcessing() throws Exception {
    String inputJson = "{\"firstName\": \"John\", \"lastName\": \"Doe\"}";

    // Process firstName field
    IStreamCommand command1 =
        JsonNavigateCommand.createExtractValue(
            new JSONPath("$.firstName"), CamelToSnakeCaseRule.create());
    String result1 = executeCommand(command1, inputJson);
    assertEquals("john", result1.trim()); // CamelToSnakeCaseRule always converts to lowercase

    // Process lastName field
    IStreamCommand command2 =
        JsonNavigateCommand.createExtractValue(
            new JSONPath("$.lastName"), CamelToSnakeCaseRule.create());
    String result2 = executeCommand(command2, inputJson);
    assertEquals("doe", result2.trim()); // CamelToSnakeCaseRule always converts to lowercase
  }

  @Test
  void testErrorHandling() throws Exception {
    String inputJson = "{\"field\": null}";

    IStreamCommand command =
        JsonNavigateCommand.createExtractValue(
            new JSONPath("$.field"), CamelToSnakeCaseRule.create());

    // Should handle null gracefully
    String result = executeCommand(command, inputJson);
    assertEquals("null", result.trim());
  }

  @Test
  void testNonExistentField() throws Exception {
    String inputJson = "{\"existing\": \"value\"}";

    IStreamCommand command =
        JsonNavigateCommand.createExtractValue(
            new JSONPath("$.nonExistent"), CamelToSnakeCaseRule.create());

    String result = executeCommand(command, inputJson);
    // JsonPath returns empty when field doesn't exist
    assertTrue(result.trim().isEmpty() || result.trim().equals("null"));
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
