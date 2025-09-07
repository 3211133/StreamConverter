# Logging

## What
StreamConverter provides an auto‑logging system for monitoring command execution and performance.

## Why
Detailed logs help troubleshoot large stream operations and give insight into timing, memory use and errors.

## How
1. Wrap any command in a `LoggingDecorator` to enable logs.
2. Use `CommandFactory.createWithLogging` for automatic decorator application.
3. Configure log levels through `CommandConfig` or logging frameworks.

## See also
- [Architecture](architecture.md)
- [Web API](web-api.md)
- [Validation](validation.md)
- [Handbook index](README.md)

More examples are available in the [original auto‑logging guide](../AUTO_LOGGING.md).
