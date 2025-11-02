# Rules Catalog and Usage Examples

## このドキュメントの基礎資料
このドキュメントは以下の実装を基に作成されています：
- [com.streamconverter.command.rule.impl](../../streamconverter-core/src/main/java/com/streamconverter/command/rule/impl/) - 変換ルールの実装
- [IRule.java](../../streamconverter-core/src/main/java/com/streamconverter/command/rule/IRule.java) - ルールインターフェース

This catalog lists currently implemented transformation rules in StreamConverter core and shows concise usage examples. It focuses on rules that are already available so teams can apply them consistently.

## Implemented Rules

- CamelToSnakeCaseRule: Convert `camelCase` → `snake_case`.
- SnakeToCamelCaseRule: Convert `snake_case` → `camelCase`.
- TrimRule: Trim leading/trailing whitespace.
- LowerCaseRule: Lowercase strings (ASCII-focused; locale-independent).
- ChainRule: Compose multiple rules in sequence.
- PassThroughRule: No-op rule (useful for demos/tests).

Missing or proposed rules (tracked separately): DateFormatRule, NumberFormatRule, EmailNormalizeRule, PhoneNumberFormatRule. These will be added under new issues for clear scoping.

## JSON Usage

Example: Convert a specific JSON path to snake_case, after trimming and lowercasing.

```java
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.rule.IRule;
import com.streamConverter.command.rule.PassThroughRule;
import com.streamConverter.command.rule.impl.casing.CamelToSnakeCaseRule;
import com.streamConverter.command.rule.impl.string.LowerCaseRule;
import com.streamConverter.command.rule.impl.string.TrimRule;
import com.streamConverter.command.rule.impl.composite.ChainRule;
import com.streamConverter.path.TreePath;

IRule rule = ChainRule.builder()
    .addRule(new TrimRule())
    .addRule(new LowerCaseRule())
    .addRule(CamelToSnakeCaseRule.builder().build())
    .build();

JsonNavigateCommand cmd = JsonNavigateCommand.create(TreePath.fromJson("$.user.name"), rule);
```

## CSV Usage

Example: Apply trimming to a CSV column (keep structure).

```java
import com.streamConverter.command.impl.csv.CsvNavigateCommand;
import com.streamConverter.command.rule.impl.string.TrimRule;
import com.streamConverter.path.CSVPath;

CsvNavigateCommand cmd = CsvNavigateCommand.create(new CSVPath("name"), new TrimRule());
```

## Notes

- Locale-sensitive transformations are not enabled by default; keep logic deterministic.
- Prefer composing small rules via ChainRule rather than creating monolithic rules.
- For new rule requests, file separate issues with examples and acceptance criteria.

