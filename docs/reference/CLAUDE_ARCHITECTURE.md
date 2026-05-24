# CLAUDE Architecture Notes

## Core API
- `StreamConverter.create(IStreamCommand...)`
- `StreamConverter.create(List<IStreamCommand>)`
- `void StreamConverter.run(InputStream, OutputStream)`

## Context & Logging
- Shared context: `PipelineContext`
- MDC propagation rule: `MdcPropagatingRule`
- Log sync filter: `PipelineContextTurboFilter`

## Available built-in commands (core)
- CSV: `CsvWalker`, `CsvFilterCommand`, `CsvValidateCommand`
- JSON: `JsonWalker`, `JsonExtractCommand`
- XML: `XmlWalker`, `XmlExtractCommand`, `ValidateCommand`
- Text: `LineEndingNormalizeCommand`, `CharacterConvertCommand`

## Path Specifications
- CSV: `CSVPath.of("columnName")`
- JSON: `TreePath.fromJson("$.path.to.value")`
- XML: `TreePath.fromXml("element/child")`

## Important
旧 API (`ExecutionContext`, `createWithContext`, `CommandResult`, `JsonValidateCommand`, `JsonStreamingValidateCommand`, `SampleStreamCommand`) を前提にした実装は現行コードと一致しません。

また以下のリネームが実施済みです（旧名は存在しません）：
- `CsvNavigateCommand` → `CsvWalker`
- `JsonNavigateCommand` → `JsonWalker`
- `XmlNavigateCommand` → `XmlWalker`
- `JsonFilterCommand` → `JsonExtractCommand`
- `XmlFilterCommand` → `XmlExtractCommand`
- `new CSVPath(...)` → `CSVPath.of(...)`
- `new JSONPath(...)` / `new XPath(...)` → `TreePath.fromJson(...)` / `TreePath.fromXml(...)`
