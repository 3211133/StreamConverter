# Validation

## What
StreamConverter ships with validation commands for CSV and XML inputs to verify structure and data before processing.

| コマンド | 対象 | モジュール |
|---------|------|---------|
| `CsvValidateCommand` | CSV の行数・列・値を検証 | streamconverter-core |
| `ValidateCommand` | XML を XSD スキーマで検証 | streamconverter-core |

> **Note**: JSON バリデーションコマンドは現在未実装です。

## Why
Running validation early catches malformed or unexpected data, preventing downstream errors and improving pipeline reliability.

## How
1. Choose a validation command: `CsvValidateCommand` for CSV, or `ValidateCommand` for XML.
2. Supply required schemas or field requirements.
3. Place the command at the start of the pipeline so later commands receive verified input.

## See also
- [Architecture](architecture.md)
- [Logging](logging.md)
- [Web API](web-api.md)
- [Handbook index](README.md)

For full details, refer to the [validation feature guide](../features/VALIDATION.md).

