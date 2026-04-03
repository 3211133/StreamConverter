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
- CSV: `CsvNavigateCommand`, `CsvFilterCommand`, `CsvValidateCommand`
- JSON: `JsonNavigateCommand`, `JsonFilterCommand`
- XML: `XmlNavigateCommand`, `XmlFilterCommand`, `ConvertCommand`, `ValidateCommand`
- Text: `LineEndingNormalizeCommand`, `CharacterConvertCommand`

## Important
旧 API (`ExecutionContext`, `createWithContext`, `CommandResult`, `JsonValidateCommand`, `JsonStreamingValidateCommand`, `SampleStreamCommand`) を前提にした実装は現行コードと一致しません。
