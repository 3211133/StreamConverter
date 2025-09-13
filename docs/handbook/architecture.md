# Architecture

## What
The StreamConverter architecture uses direct command instantiation. Commands are chained in a pipeline and executed by the `StreamConverter` core.

## Why
This approach removes factory complexity, avoids reflection overhead and keeps dependencies explicit. It results in a simpler and more performant system.

## How
1. Create commands with their required parameters.
2. Combine them into an array and pass it to `StreamConverter`.
3. Optionally wrap commands with the logging decorator for diagnostics.

## See also
- [Logging](logging.md)
- [Web API](web-api.md)
- [Validation](validation.md)
- [Handbook index](README.md)

For full details, refer to the [original architecture document](../ARCHITECTURE.md).
