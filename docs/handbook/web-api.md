# Web API

## What
The Web API exposes StreamConverter capabilities over HTTP using Spring Boot.

## Why
It allows remote systems to process streams without embedding the library, enabling integration with other services.

## How
1. Start the server with `./gradlew bootRun`.
2. Use endpoints such as `/api/v1/stream/csv/extract` or `/api/v1/stream/json/extract`.
3. Chain multiple commands by supplying an `X-Pipeline-Config` header.

## See also
- [Architecture](architecture.md)
- [Logging](logging.md)
- [Validation](validation.md)
- [Handbook index](README.md)

Further endpoint examples are in the [original Web API guide](../WEB_API.md).
