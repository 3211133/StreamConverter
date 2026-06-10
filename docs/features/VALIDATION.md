# バリデーション機能ガイド

> 💡 **クイック概要**: まず [Validation Handbook](../handbook/validation.md) で What/Why/How を理解することをお勧めします。

## 📋 概要

StreamConverter は CSV と XML のバリデーションコマンドを提供します。

| コマンド | 対象 | パッケージ |
|---------|------|---------|
| `CsvValidateCommand` | CSV の行数・列・値を検証 | `com.streamconverter.command.impl.csv` |
| `ValidateCommand` | XML を XSD スキーマで検証 | `com.streamconverter.command.impl.xml` |

> **Note**: JSON バリデーションコマンドは現在未実装です。

## 📊 CSV バリデーション

### 基本的な使用方法

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.csv.CsvValidateCommand;

// 必須カラムを指定してバリデーション
CsvValidateCommand validator = CsvValidateCommand.create("name", "email", "age");

StreamConverter converter = StreamConverter.create(validator);
converter.run(inputStream, outputStream);
```

### バリデーション項目

1. **必須カラムの存在** — 指定されたカラムが CSV ヘッダーに含まれているかチェック
2. **行数チェック** — `maxErrorsToReport` 件を超えるとエラーを打ち切る
3. **エラーメッセージ** — 1000文字を超える場合は先頭から切り詰め

### エラーハンドリング

バリデーション失敗時は `StreamProcessingException`（`IOException` のサブクラス）がスローされます。

```java
try {
    converter.run(inputStream, outputStream);
} catch (IOException e) {
    logger.error("Validation failed: " + e.getMessage());
}
```

## 📄 XML バリデーション

### XSD スキーマによる検証

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.xml.ValidateCommand;

ValidateCommand validator = ValidateCommand.create("product-schema.xsd");

StreamConverter converter = StreamConverter.create(validator);
converter.run(inputStream, outputStream);
```

### XSD スキーマの例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="product">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="name" type="xs:string"/>
        <xs:element name="price" type="xs:decimal"/>
      </xs:sequence>
      <xs:attribute name="id" type="xs:positiveInteger" use="required"/>
    </xs:complexType>
  </xs:element>
</xs:schema>
```

### セキュリティ

XML バリデーションは XXE（XML External Entity）攻撃を防ぐため、外部エンティティ参照と DOCTYPE 宣言を無効化しています。

## 🏗️ ValidationResult フレームワーク

バリデーション結果は `ValidationResult` クラスで表現されます。

```java
import com.streamconverter.validation.ValidationResult;

ValidationResult result = ValidationResult.builder()
    .validationType("CSV")
    .schemaPath("schema/products.csv")
    .success(false)
    .addError("必須フィールド 'email' が見つかりません")
    .addWarning("フィールド 'phone' の形式が推奨と異なります")
    .build();

if (!result.isValid()) {
    result.getErrors().forEach(error -> logger.error("Validation error: " + error));
}
```

## ステージ分離パターン

バリデーション失敗時に後続コマンドへデータが流れないようにするには、`FileBufferCommand` でステージを分離します。

```java
import com.streamconverter.command.impl.FileBufferCommand;
import com.streamconverter.command.impl.csv.CsvValidateCommand;
import com.streamconverter.command.impl.csv.CsvWalker;
import com.streamconverter.command.rule.impl.PassThroughRule;
import com.streamconverter.path.CSVPath;

StreamConverter converter = StreamConverter.create(
    CsvValidateCommand.create("name", "email"),  // バリデーション
    FileBufferCommand.create(),                   // ステージ分離
    CsvWalker.create(CSVPath.of("name"), new PassThroughRule())  // 変換
);
converter.run(inputStream, outputStream);
```

`CsvValidateCommand` が `IOException` をスローすると、`FileBufferCommand` 以降はまったく実行されません。

## 📚 関連ドキュメント

- **[セキュリティ分析](../reference/SECURITY_ANALYSIS.md)** — セキュリティ保護の詳細
- **[テスト戦略](../reference/TESTING.md)** — テスト実装ガイド
- **[クイックスタート](../quickstart/basic-usage.md)** — パイプラインの基本
