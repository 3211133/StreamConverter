package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.UserInputException;
import org.junit.jupiter.api.Test;

class PassThroughRuleTest {

  @Test
  void applyReturnsInputUnchanged() throws UserInputException {
    PassThroughRule rule = new PassThroughRule();
    assertEquals("hello", rule.apply("hello"));
    assertEquals("", rule.apply(""));
  }

  @Test
  void applyWithNullInputThrowsUserInputException() {
    PassThroughRule rule = new PassThroughRule();
    assertThrows(UserInputException.class, () -> rule.apply(null));
  }
}
