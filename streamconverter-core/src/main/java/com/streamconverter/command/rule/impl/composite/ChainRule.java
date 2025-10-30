package com.streamconverter.command.rule.impl.composite;

import com.streamconverter.command.rule.IRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chains multiple transformation rules to be applied in sequence.
 *
 * <p>This rule allows combining multiple {@link IRule} instances to create complex transformations
 * by applying them one after another. The output of each rule becomes the input for the next rule
 * in the chain.
 *
 * <p>Examples:
 *
 * <pre>{@code
 * // Trim whitespace then convert to snake_case
 * IRule chainedRule = ChainRule.builder()
 *     .addRule(new TrimRule())
 *     .addRule(CamelToSnakeCaseRule.create())
 *     .build();
 *
 * // Multiple transformations
 * IRule complexRule = ChainRule.of(
 *     new TrimRule(),
 *     new LowerCaseRule(),
 *     CamelToSnakeCaseRule.create()
 * );
 * }</pre>
 *
 * <p>Thread Safety: This class is thread-safe as long as the individual rules in the chain are
 * thread-safe.
 *
 * @since 1.0
 */
public class ChainRule implements IRule {

  /** List of rules to apply in sequence */
  private final List<IRule> rules;

  /** Private constructor for builder pattern */
  private ChainRule(List<IRule> rules) {
    if (rules == null || rules.isEmpty()) {
      throw new IllegalArgumentException("Chain must contain at least one rule");
    }
    this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
  }

  @Override
  public String apply(String input) {
    String result = input;

    // Apply each rule in sequence
    for (IRule rule : rules) {
      result = rule.apply(result);
    }

    return result;
  }

  /**
   * Gets the number of rules in this chain.
   *
   * @return number of rules
   */
  public int size() {
    return rules.size();
  }

  /**
   * Gets an unmodifiable view of the rules in this chain.
   *
   * @return unmodifiable list of rules
   */
  public List<IRule> getRules() {
    return rules;
  }

  /**
   * Creates a builder for configuring the ChainRule.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a ChainRule with the specified rules.
   *
   * @param rules rules to chain together
   * @return new ChainRule instance
   * @throws IllegalArgumentException if rules array is null or empty
   */
  public static ChainRule of(IRule... rules) {
    if (rules == null || rules.length == 0) {
      throw new IllegalArgumentException("Chain must contain at least one rule");
    }
    return new Builder().addRules(rules).build();
  }

  /**
   * Creates a ChainRule with the specified rules.
   *
   * @param rules list of rules to chain together
   * @return new ChainRule instance
   * @throws IllegalArgumentException if rules list is null or empty
   */
  public static ChainRule of(List<IRule> rules) {
    return new Builder().addRules(rules).build();
  }

  /** Builder class for ChainRule configuration. */
  public static class Builder {
    private final List<IRule> rules = new ArrayList<>();

    /**
     * Adds a rule to the end of the chain.
     *
     * @param rule rule to add (null rules are ignored)
     * @return this builder
     */
    public Builder addRule(IRule rule) {
      if (rule != null) {
        this.rules.add(rule);
      }
      return this;
    }

    /**
     * Adds multiple rules to the end of the chain.
     *
     * @param rules rules to add (null rules are ignored)
     * @return this builder
     */
    public Builder addRules(IRule... rules) {
      if (rules != null) {
        for (IRule rule : rules) {
          addRule(rule);
        }
      }
      return this;
    }

    /**
     * Adds multiple rules to the end of the chain.
     *
     * @param rules list of rules to add (null rules are ignored)
     * @return this builder
     */
    public Builder addRules(List<IRule> rules) {
      if (rules != null) {
        for (IRule rule : rules) {
          addRule(rule);
        }
      }
      return this;
    }

    /**
     * Inserts a rule at the specified position in the chain.
     *
     * @param index position to insert at
     * @param rule rule to insert
     * @return this builder
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Builder insertRule(int index, IRule rule) {
      if (rule != null) {
        this.rules.add(index, rule);
      }
      return this;
    }

    /**
     * Removes all rules from the chain.
     *
     * @return this builder
     */
    public Builder clear() {
      this.rules.clear();
      return this;
    }

    /**
     * Gets the current number of rules in the builder.
     *
     * @return number of rules
     */
    public int size() {
      return rules.size();
    }

    /**
     * Builds the ChainRule with current configuration.
     *
     * @return configured rule instance
     * @throws IllegalArgumentException if no rules have been added
     */
    public ChainRule build() {
      return new ChainRule(rules);
    }
  }

  @Override
  public String toString() {
    return String.format("ChainRule{rules=%d, chain=%s}", rules.size(), rules);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    ChainRule chainRule = (ChainRule) obj;
    return rules.equals(chainRule.rules);
  }

  @Override
  public int hashCode() {
    return rules.hashCode();
  }
}
