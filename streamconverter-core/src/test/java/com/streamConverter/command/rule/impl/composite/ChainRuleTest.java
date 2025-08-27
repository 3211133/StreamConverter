package com.streamConverter.command.rule.impl.composite;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.rule.IRule;
import com.streamConverter.command.rule.impl.casing.CamelToSnakeCaseRule;
import com.streamConverter.command.rule.impl.string.LowerCaseRule;
import com.streamConverter.command.rule.impl.string.TrimRule;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Tests for ChainRule composite transformation logic. */
class ChainRuleTest {

  @Test
  void testBasicChaining() {
    ChainRule rule =
        ChainRule.of(new TrimRule(), CamelToSnakeCaseRule.create(), new LowerCaseRule());

    assertEquals("user_name", rule.apply("  UserName  "));
    assertEquals("first_name_field", rule.apply("\tFirstNameField\n"));
  }

  @Test
  void testBuilderPattern() {
    ChainRule rule =
        ChainRule.builder()
            .addRule(new TrimRule())
            .addRule(CamelToSnakeCaseRule.create())
            .addRule(new LowerCaseRule())
            .build();

    assertEquals("xml_http_request", rule.apply("  XMLHttpRequest  "));
  }

  @Test
  void testOfMethods() {
    // Test varargs method
    ChainRule rule1 = ChainRule.of(new TrimRule(), new LowerCaseRule());
    assertEquals("hello", rule1.apply("  HELLO  "));

    // Test list method
    ChainRule rule2 = ChainRule.of(Arrays.asList(new TrimRule(), new LowerCaseRule()));
    assertEquals("world", rule2.apply("  WORLD  "));
  }

  @Test
  void testSingleRule() {
    ChainRule rule = ChainRule.of(new TrimRule());
    assertEquals("hello", rule.apply("  hello  "));
    assertEquals(1, rule.size());
  }

  @Test
  void testNullHandling() {
    // Test null input
    ChainRule rule = ChainRule.of(new TrimRule(), new LowerCaseRule());
    assertNull(rule.apply(null));

    // Test null rules are ignored in builder
    ChainRule ruleWithNulls =
        ChainRule.builder()
            .addRule(new TrimRule())
            .addRule(null)
            .addRule(new LowerCaseRule())
            .build();

    assertEquals(2, ruleWithNulls.size());
    assertEquals("hello", ruleWithNulls.apply("  HELLO  "));
  }

  @Test
  void testEmptyChainThrowsException() {
    // Empty varargs
    assertThrows(IllegalArgumentException.class, () -> ChainRule.of());

    // Null varargs
    IRule[] nullRules = null;
    assertThrows(IllegalArgumentException.class, () -> ChainRule.of(nullRules));

    // Empty list
    assertThrows(IllegalArgumentException.class, () -> ChainRule.of(Arrays.asList()));

    // Empty builder
    assertThrows(IllegalArgumentException.class, () -> ChainRule.builder().build());
  }

  @Test
  void testBuilderOperations() {
    ChainRule.Builder builder =
        ChainRule.builder().addRule(new TrimRule()).addRule(new LowerCaseRule());

    assertEquals(2, builder.size());

    // Add multiple rules
    builder.addRules(CamelToSnakeCaseRule.create(), new TrimRule());
    assertEquals(4, builder.size());

    // Insert rule
    builder.insertRule(1, new LowerCaseRule());
    assertEquals(5, builder.size());

    // Clear and rebuild
    builder.clear();
    assertEquals(0, builder.size());

    builder.addRule(new TrimRule());
    ChainRule rule = builder.build();
    assertEquals(1, rule.size());
  }

  @Test
  void testComplexTransformation() {
    // Create a complex transformation: trim -> camel to snake -> lowercase
    ChainRule rule =
        ChainRule.builder()
            .addRule(new TrimRule())
            .addRule(CamelToSnakeCaseRule.create())
            .addRule(new LowerCaseRule())
            .build();

    String input = "  MyComplexVariableName  ";
    String result = rule.apply(input);
    assertEquals("my_complex_variable_name", result);
  }

  @Test
  void testGetRules() {
    TrimRule trim = new TrimRule();
    LowerCaseRule lower = new LowerCaseRule();

    ChainRule rule = ChainRule.of(trim, lower);

    assertEquals(2, rule.getRules().size());
    assertTrue(rule.getRules().contains(trim));
    assertTrue(rule.getRules().contains(lower));

    // Verify list is unmodifiable
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          rule.getRules().add(new TrimRule());
        });
  }

  @Test
  void testToString() {
    ChainRule rule = ChainRule.of(new TrimRule(), new LowerCaseRule());
    String str = rule.toString();

    assertTrue(str.contains("ChainRule"));
    assertTrue(str.contains("rules=2"));
  }

  @Test
  void testEqualsAndHashCode() {
    TrimRule trim = new TrimRule();
    LowerCaseRule lower = new LowerCaseRule();

    ChainRule rule1 = ChainRule.of(trim, lower);
    ChainRule rule2 = ChainRule.of(new TrimRule(), new LowerCaseRule());
    ChainRule rule3 = ChainRule.of(lower, trim); // Different order

    assertEquals(rule1, rule2);
    assertEquals(rule1.hashCode(), rule2.hashCode());

    assertNotEquals(rule1, rule3); // Different order
    assertNotEquals(rule1, null);
    assertNotEquals(rule1, "not a rule");
  }

  @Test
  void testCustomRule() {
    // Create custom rule for testing
    IRule doubleRule = input -> input != null ? input + input : null;

    ChainRule rule = ChainRule.of(new TrimRule(), doubleRule);
    assertEquals("hellohello", rule.apply("  hello  "));
  }
}
