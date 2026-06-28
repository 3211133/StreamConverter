# Rules Catalog and Usage Examples

This catalog lists currently implemented transformation rules in StreamConverter core and shows concise usage examples.

## Implemented Rules

- CamelToSnakeCaseRule: Convert `camelCase` → `snake_case`.
- SnakeToCamelCaseRule: Convert `snake_case` → `camelCase`.
- TrimRule: Trim leading/trailing whitespace.
- LowerCaseRule: Lowercase strings (ASCII-focused; locale-independent).
- ChainRule: Compose multiple rules in sequence.
- PassThroughRule: No-op rule (useful for demos/tests).

Missing or proposed rules (tracked separately): DateFormatRule, NumberFormatRule, EmailNormalizeRule, PhoneNumberFormatRule.

## JSON Usage

Example: Convert a specific JSON path to snake_case, after trimming and lowercasing.

```java
import com.streamconverter.command.impl.json.JsonWalker;
import com.streamconverter.command.rule.IRule;
import com.streamconverter.command.rule.impl.casing.CamelToSnakeCaseRule;
import com.streamconverter.command.rule.impl.string.LowerCaseRule;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.command.rule.impl.composite.ChainRule;
import com.streamconverter.path.TreePath;

IRule rule = ChainRule.builder()
    .addRule(new TrimRule())
    .addRule(new LowerCaseRule())
    .addRule(CamelToSnakeCaseRule.builder().build())
    .build();

JsonWalker cmd = JsonWalker.create(TreePath.fromJson("$.user.name"), rule);
```

## CSV Usage

Example: Apply trimming to a CSV column (keep structure).

```java
import com.streamconverter.command.impl.csv.CsvWalker;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.CSVPath;

CsvWalker cmd = CsvWalker.create(CSVPath.of("name"), new TrimRule());
```

## Notes

- Locale-sensitive transformations are not enabled by default; keep logic deterministic.
- Prefer composing small rules via ChainRule rather than creating monolithic rules.
- For new rule requests, file separate issues with examples and acceptance criteria.

