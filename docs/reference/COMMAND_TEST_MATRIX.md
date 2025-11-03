# Command Test Coverage Matrix

> **作成日**: 2025-11-03
> **目的**: 各Commandクラスのテストカバレッジを可視化
> **基準**: TEST_INVENTORY.mdとCOMMAND_TEST_CHECKLIST.mdに基づく分析

## 概要

このマトリックスは、どのCommandがどのテスト観点をカバーしているかを示します。TEST_INVENTORY.mdの実際のテストメソッド名を分析し、各観点のカバレッジを判定しました。

**凡例**:
- ✅ テスト実装済み
- ⚠️  部分的に実装
- ❌ 未実装
- - 該当なし（そのCommandには不要な観点）

## Core Commands

### Stream Processing Commands

| Command | コンストラクタ | 正常系 | null Stream | 空入力 | 無効入力 | 大量データ | ストリーミング | 増分処理 | 特殊文字 | 改行/クォート | ネスト | 複数要素 | セキュリティ | ネットワーク | DB | クロスプラット | エラー | パフォーマンス |
|---------|--------------|-------|------------|--------|---------|-----------|--------------|---------|---------|-------------|-------|---------|------------|-----------|----|--------------|----|-------------|
| AbstractStreamCommand | ✅ | ✅ | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - |
| SampleStreamCommand | ✅ | ✅ | ✅ | ✅ | - | - | ✅ | ✅ | - | - | - | - | - | - | - | - | ✅ | - |
| StreamConverter | ✅ | ✅ | ✅ | - | - | ✅ | - | - | - | - | - | ✅ | - | - | - | - | ✅ | ✅ |

### CSV Commands

| Command | コンストラクタ | 正常系 | null Stream | 空入力 | 無効入力 | 大量データ | ストリーミング | 増分処理 | 特殊文字 | 改行/クォート | ネスト | 複数要素 | セキュリティ | ネットワーク | DB | クロスプラット | エラー | パフォーマンス |
|---------|--------------|-------|------------|--------|---------|-----------|--------------|---------|---------|-------------|-------|---------|------------|-----------|----|--------------|----|-------------|
| CsvNavigateCommand | ✅ | ✅ | - | ✅ | - | ✅ | ✅ | ✅ | - | - | - | - | - | - | - | - | ✅ | - |
| CsvValidateCommand | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | - | - | - | - | - | - | ✅ | - |
| CsvFilterCommand | - | ✅ | - | - | - | - | ✅ | - | - | - | - | ✅ | - | - | - | - | - | - |

### JSON Commands

| Command | コンストラクタ | 正常系 | null Stream | 空入力 | 無効入力 | 大量データ | ストリーミング | 増分処理 | 特殊文字 | 改行/クォート | ネスト | 複数要素 | セキュリティ | ネットワーク | DB | クロスプラット | エラー | パフォーマンス |
|---------|--------------|-------|------------|--------|---------|-----------|--------------|---------|---------|-------------|-------|---------|------------|-----------|----|--------------|----|-------------|
| JsonNavigateCommand | ✅ | ✅ | - | ✅ | ⚠️ | - | ✅ | ✅ | - | - | ✅ | - | - | - | - | - | ✅ | - |
| JsonValidateCommand | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | - | ✅ | - | - | - | - | - | ✅ | - |
| JsonStreamingValidateCommand | ✅ | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - |
| JsonFilterCommand | - | ✅ | - | - | - | - | ✅ | - | - | - | - | ✅ | - | - | - | - | - | - |

### XML Commands

| Command | コンストラクタ | 正常系 | null Stream | 空入力 | 無効入力 | 大量データ | ストリーミング | 増分処理 | 特殊文字 | 改行/クォート | ネスト | 複数要素 | セキュリティ | ネットワーク | DB | クロスプラット | エラー | パフォーマンス |
|---------|--------------|-------|------------|--------|---------|-----------|--------------|---------|---------|-------------|-------|---------|------------|-----------|----|--------------|----|-------------|
| XmlNavigateCommand | ✅ | ✅ | - | ✅ | ✅ | - | - | - | - | - | ✅ | - | - | - | - | - | ✅ | - |
| XmlValidateCommand | ✅ | ✅ | ✅ | ✅ | ✅ | - | ✅ | ✅ | - | - | - | ✅ | - | - | - | - | ✅ | - |
| XmlConvertCommand | ✅ | ✅ | - | ✅ | ✅ | ✅ | ✅ | ✅ | - | - | ✅ | - | - | - | - | - | ✅ | - |
| XmlFilterCommand | - | ✅ | - | - | - | - | ✅ | - | - | - | - | ✅ | - | - | - | - | - | - |

### Character Encoding Commands

| Command | コンストラクタ | 正常系 | null Stream | 空入力 | 無効入力 | 大量データ | ストリーミング | 増分処理 | 特殊文字 | 改行/クォート | ネスト | 複数要素 | セキュリティ | ネットワーク | DB | クロスプラット | エラー | パフォーマンス |
|---------|--------------|-------|------------|--------|---------|-----------|--------------|---------|---------|-------------|-------|---------|------------|-----------|----|--------------|----|-------------|
| CharCodeConvertCommand | ✅ | ✅ | ✅ | ✅ | ✅ | - | ✅ | ✅ | ✅ | - | - | - | - | - | - | - | ✅ | - |
| LineEndingNormalizeCommand | ✅ | ✅ | - | ✅ | - | ✅ | ✅ | ✅ | - | ✅ | - | - | - | - | - | ✅ | ✅ | - |

### Network Commands

| Command | コンストラクタ | 正常系 | null Stream | 空入力 | 無効入力 | 大量データ | ストリーミング | 増分処理 | 特殊文字 | 改行/クォート | ネスト | 複数要素 | セキュリティ | ネットワーク | DB | クロスプラット | エラー | パフォーマンス |
|---------|--------------|-------|------------|--------|---------|-----------|--------------|---------|---------|-------------|-------|---------|------------|-----------|----|--------------|----|-------------|
| SendHttpCommand | ✅ | ✅ | ✅ | - | ✅ | ✅ | ✅ | - | - | - | - | - | ✅ | ✅ | - | - | ✅ | ✅ |

## Supporting Classes

### Rule Classes

| Class | コンストラクタ | 正常系 | null Stream | 空入力 | 無効入力 | 大量データ | ストリーミング | 増分処理 | 特殊文字 | 改行/クォート | ネスト | 複数要素 | セキュリティ | ネットワーク | DB | クロスプラット | エラー | パフォーマンス |
|---------|--------------|-------|------------|--------|---------|-----------|--------------|---------|---------|-------------|-------|---------|------------|-----------|----|--------------|----|-------------|
| DatabaseFetchRule | - | ✅ | - | - | - | ✅ | - | - | - | - | - | ✅ | ✅ | - | ✅ | - | ✅ | ✅ |
| PooledDatabaseFetchRule | - | ✅ | - | - | - | - | - | - | - | - | - | - | - | - | ✅ | - | - | ✅ |
| ChainRule | ✅ | ✅ | - | - | - | - | - | - | - | - | - | ✅ | - | - | - | - | ✅ | - |
| CamelToSnakeCaseRule | - | ✅ | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - |
| TransformationRule | - | ✅ | - | - | - | - | - | - | - | - | - | ✅ | - | - | - | - | ✅ | - |

### Utility Classes

| Class | コンストラクタ | 正常系 | null Stream | 空入力 | 無効入力 | 大量データ | ストリーミング | 増分処理 | 特殊文字 | 改行/クォート | ネスト | 複数要素 | セキュリティ | ネットワーク | DB | クロスプラット | エラー | パフォーマンス |
|---------|--------------|-------|------------|--------|---------|-----------|--------------|---------|---------|-------------|-------|---------|------------|-----------|----|--------------|----|-------------|
| SecureXPathValidator | - | ✅ | - | - | - | - | - | - | - | - | - | - | ✅ | - | - | - | ✅ | - |
| TreePath | - | ✅ | - | - | - | - | - | - | - | - | - | - | - | - | - | - | ✅ | - |
| ExecutionContext | ✅ | ✅ | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - |
| ValidationResult | ✅ | ✅ | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - |
| StreamProcessingException | ✅ | ✅ | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - |

## 統計情報

### カバレッジサマリー（観点別）

各テスト観点について、該当するCommandのうちどれだけが実装しているかを示します。

| 観点 | 実装済みCommand数 | 該当Command総数 | 実装率 |
|-----|-----------------|---------------|-------|
| コンストラクタ検証 | 14 | 17 | 82% |
| 正常系処理 | 26 | 26 | 100% |
| null Stream検証 | 9 | 17 | 53% |
| 空入力処理 | 13 | 17 | 76% |
| 無効入力処理 | 10 | 17 | 59% |
| 大量データ処理 | 9 | 17 | 53% |
| ストリーミング動作 | 16 | 17 | 94% |
| 増分処理 | 10 | 17 | 59% |
| 特殊文字/マルチバイト | 4 | 10 | 40% |
| 改行/クォート処理 | 2 | 5 | 40% |
| ネスト構造 | 4 | 8 | 50% |
| 複数要素処理 | 9 | 12 | 75% |
| セキュリティ | 3 | 6 | 50% |
| ネットワーク | 1 | 1 | 100% |
| データベース | 2 | 2 | 100% |
| クロスプラットフォーム | 1 | 5 | 20% |
| エラーハンドリング | 20 | 26 | 77% |
| パフォーマンス | 5 | 17 | 29% |

### Command別カバレッジ（上位10）

各Commandがカバーしている観点数をランキング形式で示します。

| Command | カバー観点数 | 該当観点総数 | カバレッジ率 |
|---------|------------|-----------|-----------|
| CsvValidateCommand | 12 | 15 | 80% |
| JsonValidateCommand | 12 | 15 | 80% |
| CharCodeConvertCommand | 11 | 15 | 73% |
| XmlConvertCommand | 10 | 15 | 67% |
| SendHttpCommand | 10 | 15 | 67% |
| LineEndingNormalizeCommand | 10 | 15 | 67% |
| XmlValidateCommand | 9 | 15 | 60% |
| CsvNavigateCommand | 8 | 15 | 53% |
| StreamConverter | 8 | 15 | 53% |
| JsonNavigateCommand | 8 | 15 | 53% |

### ギャップ分析

#### 高優先度の改善項目

以下は重要な観点でテストが不足しているCommandです：

**1. null Stream検証の不足 (9/17 = 53%)**
- CsvNavigateCommand: null InputStreamのテストが未実装
- JsonNavigateCommand: null InputStreamのテストが未実装
- XmlNavigateCommand: null InputStreamのテストが未実装
- XmlConvertCommand: null InputStreamのテストが未実装
- LineEndingNormalizeCommand: null InputStreamのテストが未実装

**2. 大量データ処理の不足 (9/17 = 53%)**
- JsonNavigateCommand: 大量データテストが未実装
- XmlNavigateCommand: 大量データテストが未実装
- XmlValidateCommand: 大量データテストが未実装
- CharCodeConvertCommand: 大量データテストが未実装
- SampleStreamCommand: 大量データテストが未実装

**3. 無効入力処理の不足 (10/17 = 59%)**
- CsvNavigateCommand: 無効なCSV形式のテストが未実装
- SampleStreamCommand: 無効入力のテストが未実装
- LineEndingNormalizeCommand: 無効入力のテストが未実装

**4. パフォーマンステストの不足 (5/17 = 29%)**
- ほとんどのCommandでパフォーマンステストが未実装
- 実装済み: StreamConverter, SendHttpCommand, DatabaseFetchRule, PooledDatabaseFetchRule, LineEndingNormalizeCommand

**5. クロスプラットフォームテストの不足 (1/5 = 20%)**
- LineEndingNormalizeCommand以外でクロスプラットフォームテストが未実装
- 該当Command: CsvNavigateCommand, CharCodeConvertCommand, XmlConvertCommand

#### 中優先度の改善項目

**6. 特殊文字/マルチバイト処理の不足 (4/10 = 40%)**
- CsvNavigateCommand: 特殊文字処理が未実装
- JsonNavigateCommand: 特殊文字処理が未実装
- XmlNavigateCommand: 特殊文字処理が未実装
- XmlConvertCommand: 特殊文字処理が未実装

**7. ネスト構造の不足 (4/8 = 50%)**
- CsvValidateCommand: ネスト構造は該当なしだが、複雑なデータ構造テストは追加可能
- XmlValidateCommand: 深いネスト構造のテストが不足

**8. セキュリティテストの不足 (3/6 = 50%)**
- XMLコマンド群: XXE攻撃の防御テストが一部不足
- Filter系コマンド: インジェクション攻撃の防御テストが未実装

#### 推奨される改善アクション

1. **即座に対応すべき項目**:
   - すべてのCommandでnull Stream検証を追加（セキュリティ上重要）
   - 大量データ処理テストを追加（スケーラビリティの保証）

2. **中期的に対応すべき項目**:
   - パフォーマンステストの標準化と全Commandへの適用
   - クロスプラットフォームテストの拡充（特に改行処理、ファイルパス関連）

3. **長期的に対応すべき項目**:
   - 特殊文字・マルチバイト文字の網羅的テスト
   - セキュリティテストの全Commandへの展開

## 観点別詳細説明

### 1. コンストラクタ検証
- **定義**: null引数、空引数、無効な引数でのコンストラクタの動作
- **実装例**: `CsvValidateCommandTest.testConstructorWithNullRequiredColumns()`
- **重要度**: 高（すべてのCommandで必須）

### 2. 正常系処理
- **定義**: 基本的な入力データの正常な処理
- **実装例**: `CsvNavigateCommandTest.testBasicCsvProcessing()`
- **重要度**: 最高（すべてのCommandで必須）

### 3. null Stream検証
- **定義**: null InputStream/OutputStreamでの例外スロー
- **実装例**: `SampleStreamCommandTest.testExecuteWithNullInputStream()`
- **重要度**: 高（セキュリティとロバスト性）

### 4. 空入力処理
- **定義**: 空のInputStreamの適切な処理
- **実装例**: `CsvNavigateCommandTest.testEmptyInput()`
- **重要度**: 高（エッジケースの処理）

### 5. 無効入力処理
- **定義**: 不正フォーマットのデータでの例外処理
- **実装例**: `XmlNavigateCommandTest.testInvalidXmlInput()`
- **重要度**: 高（エラーハンドリング）

### 6. 大量データ処理
- **定義**: 1000+レコード/要素の処理
- **実装例**: `CsvValidateCommandTest.testLargeCsvValidation()` (1000レコード)
- **重要度**: 高（スケーラビリティ）

### 7. ストリーミング動作
- **定義**: データを段階的に処理、バッファリングしない
- **実装例**: `CsvNavigateCommandTest.testStreamingCsvNavigationBehavior()`
- **重要度**: 最高（StreamConverterの核心機能）

### 8. 増分処理
- **定義**: データが段階的に処理されることの時系列検証
- **実装例**: `JsonValidateCommandTest.testIncrementalJsonValidation()`
- **重要度**: 中（ストリーミング動作の詳細検証）

### 9. 特殊文字/マルチバイト
- **定義**: 特殊文字、日本語、中国語などの正確な処理
- **実装例**: `CsvValidateCommandTest.testCsvWithSpecialCharactersValidation()`
- **重要度**: 中（国際化対応）

### 10. 改行/クォート処理
- **定義**: フィールド内改行、クォート、エスケープの処理
- **実装例**: `CsvValidateCommandTest.testCsvWithQuotedFieldsContainingNewlines()`
- **重要度**: 中（CSV/テキスト処理の正確性）

### 11. ネスト構造
- **定義**: 複雑な階層構造の正確な処理
- **実装例**: `JsonNavigateCommandTest.testComplexJsonProcessing()`
- **重要度**: 中（構造化データの処理）

### 12. 複数要素処理
- **定義**: 複数列/フィールド/要素の同時処理
- **実装例**: `FilterCommandBasicTest.testCsvFilterCommand_MultipleColumns()`
- **重要度**: 中（機能の完全性）

### 13. セキュリティ
- **定義**: インジェクション防止、入力サニタイズ、XXE防止
- **実装例**: `DatabaseFetchRuleIntegrationTest.testSQLInjectionPrevention()`
- **重要度**: 最高（セキュリティは必須）

### 14. ネットワーク
- **定義**: HTTP通信、タイムアウト、エラーハンドリング
- **実装例**: `SendHttpCommandTest.testActualHttpRequest()`
- **重要度**: 高（ネットワークCommandで必須）

### 15. データベース
- **定義**: DB統合、接続プーリング、SQLエラー処理
- **実装例**: `DatabaseFetchRuleIntegrationTest.testBasicDataFetch()`
- **重要度**: 高（DBCommandで必須）

### 16. クロスプラットフォーム
- **定義**: Windows/Linux/Macでの動作保証
- **実装例**: `CrossPlatformTest.testLineEndingNormalization()`
- **重要度**: 中（移植性）

### 17. エラーハンドリング
- **定義**: 例外の適切なスロー、エラーメッセージの明確性
- **実装例**: `CsvNavigateCommandTest.testEmptyInput()`
- **重要度**: 高（ロバスト性）

### 18. パフォーマンス
- **定義**: 実行時間の制約、メモリ効率性
- **実装例**: `DatabaseFetchRuleIntegrationTest.testLargeDatasetPerformance()` (5秒以内)
- **重要度**: 中（ベンチマークと最適化）

## 判定基準

このマトリックスの判定基準は以下の通りです：

### ✅ テスト実装済み
- 該当する観点を明確にテストするメソッドが存在する
- テストメソッド名から観点が明確に読み取れる
- 例: `testNullInputStream()` → null Stream検証

### ⚠️ 部分的に実装
- 該当する観点の一部のみをテストしている
- または、テストは存在するが無効化されている（`@Disabled`など）
- 例: `testInvalidJsonInput()` が無効化されている場合

### ❌ 未実装
- 該当する観点のテストが全く存在しない
- またはテストメソッドが見当たらない

### - 該当なし
- そのCommandの性質上、その観点が適用されない
- 例: FilterCommandにコンストラクタ検証が不要な場合

## 改善のための推奨事項

1. **優先度1（即座に対応）**: null Stream検証、大量データ処理
2. **優先度2（中期的）**: パフォーマンステスト、無効入力処理
3. **優先度3（長期的）**: クロスプラットフォーム、特殊文字処理

各Commandの開発者は、このマトリックスを参照して不足しているテストを追加実装してください。

## 更新履歴

- 2025-11-03: 初期作成（TEST_INVENTORY.mdとCOMMAND_TEST_CHECKLIST.mdに基づく分析）

---

**参考ドキュメント**:
- [TEST_INVENTORY.md](TEST_INVENTORY.md) - 全テストメソッドの詳細目録
- [COMMAND_TEST_CHECKLIST.md](COMMAND_TEST_CHECKLIST.md) - テスト作成時のチェックリスト
- [TESTING.md](TESTING.md) - テスト戦略とベンチマーク実行
