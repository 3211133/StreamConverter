# Logging

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
