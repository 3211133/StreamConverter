# Validation

## このドキュメントの基礎資料
このドキュメントは以下の資料を基に作成されています：
- [features/VALIDATION.md](../features/VALIDATION.md) - バリデーション機能の詳細ガイド

## What
StreamConverter ships with validation commands for CSV, JSON and XML inputs to verify structure and data before processing.

## Why
Running validation early catches malformed or unexpected data, preventing downstream errors and improving pipeline reliability.

## How
1. Choose a validation command such as `CsvValidateCommand`, `JsonValidateCommand` or `ValidateCommand`.
2. Supply required schemas or field requirements.
3. Place the command at the start of the pipeline so later commands receive verified input.

## See also
- [Architecture](architecture.md)
- [Logging](logging.md)
- [Web API](web-api.md)
- [Handbook index](README.md)

For full details, refer to the [validation feature guide](../features/VALIDATION.md).

