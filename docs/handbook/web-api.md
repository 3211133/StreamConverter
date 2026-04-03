# Web API

## What
Web API は StreamConverter を HTTP から呼び出すための Spring Boot WebFlux インターフェースです。

## Why
ライブラリを直接組み込まなくても、外部システムからストリーム変換を利用できます。

## How
1. `./gradlew :streamconverter-web:bootRun` で起動します。
2. `/api/v1/stream/csv/extract` や `/api/v1/stream/json/extract` を呼び出します。
3. `X-Pipeline-Config` ヘッダーで複数コマンドを連結できます。

## See also
- [Web API 詳細ガイド](../WEB_API.md)
- [Architecture](architecture.md)
- [Logging](logging.md)
- [Validation](validation.md)
- [Handbook index](README.md)
