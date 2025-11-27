# Logging

## このドキュメントの基礎資料
このドキュメントは以下の資料を基に作成されています：
- [AUTO_LOGGING.md](../AUTO_LOGGING.md) - 自動ログ機能の詳細仕様

## What
StreamConverter provides an auto‑logging system for monitoring command execution and performance.

## Why
Detailed logs help troubleshoot large stream operations and give insight into timing, memory use and errors.

## How
1. All commands extending `AbstractStreamCommand` have automatic built-in logging.
2. No additional wrapper or configuration needed - logging works out of the box.
3. Configure log levels and output format through logging frameworks (e.g., logback).

## See also
- [Architecture](architecture.md)
- [Web API](web-api.md)
- [Validation](validation.md)
- [Handbook index](README.md)

More examples are available in the [original auto‑logging guide](../AUTO_LOGGING.md).
