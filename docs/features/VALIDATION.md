# バリデーション機能ガイド

> 💡 **クイック概要**: まず [Validation Handbook](../handbook/validation.md) で What/Why/How を理解することをお勧めします。

## 📋 概要

StreamConverterは包括的なデータバリデーション機能を提供します。CSV、JSON、XMLの各形式に対応したバリデーションコマンドと統一された結果フレームワークにより、堅牢なデータ処理パイプラインを構築できます。

## 🎯 対応形式

### CSV バリデーション
- **コマンド**: `CsvValidateCommand`
- **機能**: CSV構造とデータ妥当性の検証
- **実装**: `com.streamConverter.command.impl.csv.CsvValidateCommand`

### JSON バリデーション
- **コマンド**: `JsonValidateCommand`
- **機能**: JSON Schemaを使用したJSONデータ検証
- **実装**: `com.streamConverter.command.impl.json.JsonValidateCommand`

### XML バリデーション
- **コマンド**: `xml.ValidateCommand`
- **機能**: XSDスキーマを使用したXML構造検証
- **実装**: `com.streamConverter.command.impl.xml.ValidateCommand`

## 📊 CSVバリデーション

### 基本的な使用方法

```java
import com.streamConverter.command.impl.csv.CsvValidateCommand;
import java.util.Arrays;

// 必須カラムを指定してバリデーション
String[] requiredColumns = {"name", "email", "age"};
CsvValidateCommand validator = new CsvValidateCommand(Arrays.asList(requiredColumns));

// パイプラインで使用
IStreamCommand[] pipeline = {
    validator,
    // 他のコマンド...
};
```

### バリデーション項目

1. **必須カラムの存在**
   - 指定されたカラムがCSVヘッダーに含まれているかチェック

2. **データ整合性検証**
   - 空値や不正な形式のデータを検出

3. **重複検出**
   - 重複行の検出（設定可能）

### エラーハンドリング

```java
try {
    StreamConverter converter = StreamConverter.create(pipeline);
    converter.run(inputStream, outputStream);
} catch (StreamProcessingException e) {
    // バリデーションエラーの詳細を取得
    logger.error("CSV validation failed: " + e.getMessage());
}
```

## 🔍 JSONバリデーション

### JSON Schemaによる検証

```java
import com.streamConverter.command.impl.json.JsonValidateCommand;

// JSON Schemaファイルを指定
JsonValidateCommand validator = JsonValidateCommand.create("user-schema.json");

// 使用例
IStreamCommand[] pipeline = {
    validator,
    new JsonNavigateCommand("$.validatedData")
};
```

### JSON Schemaの例

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "name": {
      "type": "string",
      "minLength": 1
    },
    "email": {
      "type": "string",
      "format": "email"
    },
    "age": {
      "type": "integer",
      "minimum": 0,
      "maximum": 150
    }
  },
  "required": ["name", "email"]
}
```

### バリデーション機能

1. **スキーマ準拠チェック**
   - JSON Schema仕様に基づく構造検証

2. **データ型検証**
   - 型安全性の確保

3. **フォーマット検証**
   - email、date、URI等の標準フォーマット

4. **制約チェック**
   - 値の範囲、文字列長、パターンマッチング

## 📄 XMLバリデーション

### XSDスキーマによる検証

```java
import com.streamConverter.command.impl.xml.ValidateCommand;

// XSDスキーマファイルを指定
ValidateCommand validator = new ValidateCommand("product-schema.xsd");

// パイプラインで使用
IStreamCommand[] pipeline = {
    validator,
    new XmlNavigateCommand("//product[@id]")
};
```

### XSDスキーマの例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="product">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="name" type="xs:string"/>
        <xs:element name="price" type="xs:decimal"/>
        <xs:element name="category" type="xs:string"/>
      </xs:sequence>
      <xs:attribute name="id" type="xs:positiveInteger" use="required"/>
    </xs:complexType>
  </xs:element>
</xs:schema>
```

## 🏗️ ValidationResultフレームワーク

### 概要

StreamConverterは統一されたバリデーション結果フレームワークを提供します。

**実装**: `com.streamConverter.validation.ValidationResult`

### 特徴

1. **Builder パターン**
   - 流暢なAPIでバリデーション結果を構築

2. **エラー・警告分類**
   - エラーレベルの区別と詳細情報

3. **タイムスタンプ**
   - バリデーション実行時刻の記録

4. **構造化された結果**
   - 一貫したレスポンス形式

### 使用例

```java
import com.streamConverter.validation.ValidationResult;

// ValidationResultの構築
ValidationResult result = ValidationResult.builder()
    .success(false)
    .addError("必須フィールド 'email' が見つかりません")
    .addWarning("フィールド 'phone' の形式が推奨と異なります")
    .timestamp(Instant.now())
    .build();

// 結果の確認
if (!result.isValid()) {
    result.getErrors().forEach(error -> 
        logger.error("Validation error: " + error));
    result.getWarnings().forEach(warning -> 
        logger.warn("Validation warning: " + warning));
}
```

## 🔧 高度な設定

### カスタムバリデーションルール

各バリデーションコマンドは、カスタムルールの追加が可能です：

```java
// CSVカスタムバリデーション
CsvValidateCommand customValidator = new CsvValidateCommand(
    Arrays.asList("name", "email"),
    true,  // 重複チェック有効
    true   // 空値チェック有効
);

// JSONカスタムバリデーション（独自フォーマット）
JsonValidateCommand customJsonValidator = JsonValidateCommand.create(
    "custom-schema.json",
    SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
);
```

### パフォーマンス考慮事項

1. **ストリーミング処理**
   - 大きなファイルでもメモリ効率的に処理

2. **スキーマキャッシュ**
   - 同一スキーマの再利用による性能向上

3. **早期終了**
   - 致命的エラー検出時の早期終了オプション

## 🛡️ セキュリティ

### セキュリティ保護

1. **XXE攻撃防止**
   - XMLバリデーションでのXXE攻撃防止機能

2. **パストラバーサル保護**
   - スキーマファイルパスの安全性検証

3. **リソース制限**
   - メモリ使用量とCPU時間の制限

### 設定例

```java
// セキュアなXMLバリデーション
ValidateCommand secureValidator = new ValidateCommand("schema.xsd")
    .withXXEProtection(true)
    .withResourceLimits(1024 * 1024); // 1MB制限
```

## 🧪 テスト戦略

### バリデーションテスト

バリデーション機能のテストは以下の観点で実施：

1. **正常系テスト**
   - 有効なデータでのバリデーション成功

2. **異常系テスト**
   - 無効なデータでの適切なエラー検出

3. **境界値テスト**
   - 制限値近辺でのバリデーション動作

4. **性能テスト**
   - 大量データでの処理性能

### テスト実装例

```java
@Test
public void testCsvValidation_ValidData() {
    // 有効なCSVデータでテスト
    String csvData = "name,email,age\\nJohn,john@example.com,25";
    CsvValidateCommand validator = new CsvValidateCommand(
        Arrays.asList("name", "email", "age"));
    
    // バリデーション実行とアサーション
    assertDoesNotThrow(() -> {
        validator.execute(new ByteArrayInputStream(csvData.getBytes()), 
                         new ByteArrayOutputStream());
    });
}

@Test
public void testJsonValidation_InvalidData() {
    // 無効なJSONデータでテスト
    String invalidJson = "{\\"name\\": \\"\\", \\"email\\": \\"invalid-email\\"}";
    JsonValidateCommand validator = JsonValidateCommand.create("user-schema.json");
    
    // バリデーションエラーの発生を確認
    assertThrows(StreamProcessingException.class, () -> {
        validator.execute(new ByteArrayInputStream(invalidJson.getBytes()), 
                         new ByteArrayOutputStream());
    });
}
```

## 📚 関連ドキュメント

- **[セキュリティ分析](../reference/SECURITY_ANALYSIS.md)** - セキュリティ保護の詳細
- **[コマンドアーキテクチャ](../archived/COMMAND_ARCHITECTURE.md)** - コマンドパターンの設計
- **[テスト戦略](../reference/TESTING.md)** - テスト実装ガイド

## 🔄 更新履歴

- **2025-08-16**: 初版作成 - CSV、JSON、XMLバリデーション機能の文書化