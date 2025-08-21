# PMD Code Quality Analysis Report

**Generated**: Thu Aug 21 01:40:42 UTC 2025
**Total Violations**: 2160

## 🎯 Top Code Smell Rules

| Rank | Rule | Count | Category |
|------|------|-------|----------|
| | MethodArgumentCouldBeFinal | 608 | Code Style |
| | LocalVariableCouldBeFinal | 484 | Code Style |
| | OnlyOneReturn | 132 | Code Style |
| | GuardLogStatement | 114 | Best Practices |
| | LongVariable | 72 | Code Style |
| | PackageCase | 65 | Code Style |
| | ConsecutiveAppendsShouldReuse | 56 | Performance |
| | ConsecutiveLiteralAppends | 40 | Performance |
| | AvoidCatchingGenericException | 39 | Design |
| | ShortVariable | 36 | Code Style |
| | AvoidLiteralsInIfCondition | 33 | Error Prone |
| | CallSuperInConstructor | 27 | Code Style |
| | CyclomaticComplexity | 26 | Design |
| | ControlStatementBraces | 26 | Code Style |
| | InefficientEmptyStringCheck | 23 | Performance |
| | AppendCharacterWithChar | 21 | Performance |
| | LawOfDemeter | 21 | Design |
| | AvoidFieldNameMatchingMethodName | 21 | Error Prone |
| | CloseResource | 17 | Error Prone |
| | RedundantFieldInitializer | 16 | Performance |

## 📁 Files with Most Issues

| File | Violations |
|------|------------|
| LargeDataGenerator.java | 103 |
| TestFailureAnalyzer.java | 102 |
| DatabaseConnectionPool.java | 98 |
| PmdReportConverter.java | 97 |
| ControllerFactory.java | 84 |
| CommandFactory.java | 77 |
| PerformanceAnalyzer.java | 76 |
| JsonNavigateCommand.java | 70 |
| JsonFilterCommand.java | 67 |
| StreamConverter.java | 63 |
| AbstractFactory.java | 63 |
| ValidationResult.java | 58 |
| PmdXmlToMarkdownCommand.java | 55 |
| CsvValidateCommand.java | 54 |
| EnhancedCommandFactory.java | 52 |

## ⚡ Priority Distribution

| Priority | Count | Description |
|----------|-------|-------------|
| 1 | 34 | 🔴 High - Critical issues |
| 2 | 129 | 🟡 Medium - Important issues |
| 3 | 1977 | 🟢 Low - Minor issues |
| 4 | 17 | ℹ️ Info - Informational |
| 5 | 3 | ❓ Unknown |
