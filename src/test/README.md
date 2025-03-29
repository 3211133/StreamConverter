# StreamConverter テスト

このディレクトリには、StreamConverterプロジェクトのJUnit 5テストが含まれています。

## テスト構造

テストは以下のパッケージ構造に従って整理されています：

```
src/test/java/com/streamConverter/
├── StreamConverterTest.java       # StreamConverterクラスのテスト
├── MainTest.java                  # Mainクラスのテスト
├── command/
│   ├── AbstractStreamCommandTest.java  # 抽象コマンドクラスのテスト
│   └── impl/
│       ├── SampleStreamCommandTest.java  # サンプルコマンドのテスト
│       ├── charaCode/
│       │   └── ConvertTest.java          # 文字コード変換コマンドのテスト
│       └── xml/
│           └── ValidateTest.java         # XML検証コマンドのテスト
```

## テストリソース

テストで使用するリソースファイルは `src/test/resources/` ディレクトリに配置されています：

```
src/test/resources/
├── test-schema.xsd    # XMLバリデーションテスト用のスキーマ
├── valid-test.xml     # 有効なXMLファイル
└── invalid-test.xml   # 無効なXMLファイル
```

## テスト実行方法

### Gradleを使用してテストを実行

プロジェクトのルートディレクトリで以下のコマンドを実行します：

```bash
./gradlew test
```

### 特定のテストクラスのみ実行

```bash
./gradlew test --tests "com.streamConverter.StreamConverterTest"
```

### 特定のテストメソッドのみ実行

```bash
./gradlew test --tests "com.streamConverter.StreamConverterTest.testRunWithValidStreams"
```

## テストレポート

テスト実行後、テストレポートは以下の場所に生成されます：

- HTML形式のレポート: `build/reports/tests/test/index.html`
- XMLレポート: `build/test-results/test/`

## テストカバレッジ

テストカバレッジを確認するには、JaCoCoプラグインを使用します。build.gradleに以下を追加してください：

```groovy
plugins {
    id 'jacoco'
}

jacocoTestReport {
    reports {
        xml.enabled true
        html.enabled true
    }
}

test {
    finalizedBy jacocoTestReport
}
```

その後、以下のコマンドでテストとカバレッジレポートを生成できます：

```bash
./gradlew test jacocoTestReport
```

カバレッジレポートは `build/reports/jacoco/test/html/index.html` に生成されます。
