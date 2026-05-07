# 新しいコマンドを実装する

新しいストリーム処理コマンドを追加するための手順書です。
コマンドを実装した後に `CommandStreamingContractTest` を通す手順も含みます。

## 目次

1. [コマンドの実装](#1-コマンドの実装)
2. [ストリーミング契約テスト用プロバイダの作成](#2-ストリーミング契約テスト用プロバイダの作成)
3. [テストを実行して確認する](#3-テストを実行して確認する)
4. [expectation の選び方](#4-expectation-の選び方)
5. [よくあるミスと解決策](#5-よくあるミスと解決策)
6. [プローブテストの仕組み](#6-プローブテストの仕組み)
7. [実装例の参考](#7-実装例の参考)

---

## 1. コマンドの実装

### ファイルの配置場所

```
streamconverter-core/src/main/java/com/streamconverter/command/impl/
```

サブパッケージも利用できます（例: `csv/`, `xml/`, `json/`）。

### 最低限のテンプレート

```java
package com.streamconverter.command.impl;

import com.streamconverter.command.IStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyNewCommand implements IStreamCommand {

    private static final Logger log = LoggerFactory.getLogger(MyNewCommand.class);

    @Override
    public void execute(InputStream inputStream, OutputStream outputStream)
            throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            // ここでバッファの内容を変換する
            outputStream.write(buffer, 0, len);
            // flush() は毎回必須ではない。BufferedOutputStream など
            // バッファ層を挟む場合に意味のある境界で呼ぶ。
        }
    }
}
```

### 必須のルール

- **`IStreamCommand` を implements すること**（直接実装が推奨です）
- ログが必要な場合は `private static final Logger log = LoggerFactory.getLogger(MyNewCommand.class);` を宣言する
- `abstract` クラスは自動テストの対象外になります
- クラス名の末尾に "Command" を付けるのが慣例です

---

## 2. ストリーミング契約テスト用プロバイダの作成

新しいコマンドを実装しても、プロバイダを作成しないと `CommandStreamingContractTest` が失敗します。

### ファイルの配置場所

```
streamconverter-core/src/test/java/com/streamconverter/command/contract/CommandStreamingContractProviders.java
```

### ファイル名・クラス名の命名規則（厳守）

コマンドのクラス名に `StreamingContractProvider` を付けます。

| コマンドクラス名 | プロバイダクラス名 |
|----------------|-----------------|
| `MyNewCommand` | `MyNewCommandStreamingContractProvider` |
| `CsvFilterCommand` | `CsvFilterCommandStreamingContractProvider` |

**この命名規則を守らないとテストフレームワークがプロバイダを見つけられません。**

### プロバイダの最小テンプレート

```java
final class MyNewCommandStreamingContractProvider
        implements CommandStreamingContractProvider {

    @Override
    public IStreamCommand createCommand() {
        return new MyNewCommand(); // または MyNewCommand.create() 等
    }

    @Override
    public byte[] sampleInput() {
        // コマンドが処理できる入力データ
        // プローブは最後の 1 バイトだけをブロックするので、
        // 最低でも 2 バイト以上あれば動作する。
        // コマンドの内部バッファ（例: BufferedReader の 8KB）より
        // 大きいデータを用意すると、より信頼性の高いテストになる。
        return CommandStreamingContractProviders.utf8(
                "line-1\nline-2\nline-3\n".repeat(100));
    }

    @Override
    public StreamingExpectation expectation() {
        return StreamingExpectation.STREAMING_COMPLIANT;
    }
}
```

### 既存のプロバイダクラス（`CommandStreamingContractProviders.java`）への追加

既存のファイルに全プロバイダが定義されています。このファイルの末尾に新しいプロバイダクラスを追加してください。

---

## 3. テストを実行して確認する

```bash
# 契約テストだけを実行
./gradlew :streamconverter-core:test --tests "CommandStreamingContractTest"

# 全テストを実行
./gradlew test
```

### テスト成功時のメッセージ

```
CommandStreamingContractTest > everyCommandImplementationHasAStreamingContractProvider PASSED
CommandStreamingContractTest > allCommandsParticipateInStreamingContract > MyNewCommand PASSED
```

### プロバイダが見つからないときのエラー

```
AssertionError: Missing streaming contract providers for command implementations:
- com.streamconverter.command.impl.MyNewCommand
  -> expected provider com.streamconverter.command.contract.MyNewCommandStreamingContractProvider
```

→ プロバイダクラスの名前・パッケージを確認してください。

---

## 4. `expectation()` の選び方

| 値 | 使うとき | `exemptionReason()` |
|---|---------|---------------------|
| `STREAMING_COMPLIANT` | 入力の一部を受け取った時点で出力を開始できる（通常のケース） | 不要 |
| `ALLOWED_FULL_BUFFERING` | 設計上、全入力を読み切ってから出力する（許容されたバッファリング） | **必須** |
| `KNOWN_STREAMING_VIOLATION` | 実測済みの既知違反（修正予定の制限など） | **必須** |

**`KNOWN_STREAMING_VIOLATION` は「まだ確認していない」という意味で使ってはいけません。**
必ず実際にプローブテストを実行して、非準拠であることを確認した上で使ってください。

### `ALLOWED_FULL_BUFFERING` の例

```java
@Override
public StreamingExpectation expectation() {
    return StreamingExpectation.ALLOWED_FULL_BUFFERING;
}

@Override
public String exemptionReason() {
    return "MyNewCommand intentionally buffers all input before writing output "
           + "because it needs to sort the entire dataset.";
}
```

---

## 5. よくあるミスと解決策

### ミス 1: プロバイダのクラス名を間違える

```java
// ❌ 間違い
final class MyNewCommandProvider implements CommandStreamingContractProvider { ... }

// ✅ 正しい
final class MyNewCommandStreamingContractProvider implements CommandStreamingContractProvider { ... }
```

### ミス 2: `sampleInput()` が 1 バイトしかない

プローブは最後の 1 バイトだけをブロックします。データが 1 バイト以下では
ブロックが発生しません。最低 2 バイト以上、実用的には 100 バイト以上を推奨します。

```java
// ❌ 問題あり: 1バイトでは最後のバイトをブロックできない
public byte[] sampleInput() {
    return "x".getBytes(StandardCharsets.UTF_8); // 1バイト
}

// ✅ 推奨: 十分なサイズ
public byte[] sampleInput() {
    return CommandStreamingContractProviders.utf8("line-\n".repeat(200));
}
```

### ミス 3: 全データを読んでから処理する（非ストリーミング）

```java
// ❌ ストリーミング非準拠: 全データを String に読み込んでから処理
public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    String allData = new String(inputStream.readAllBytes()); // 全読み込み
    // ... allData を処理してから outputStream に書く
}

// ✅ ストリーミング準拠: 読みながら即座に書く
// プローブは write() の呼び出しを検出するので、write するだけで十分
public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[8192];
    int len;
    while ((len = inputStream.read(buffer)) != -1) {
        // ここで変換処理
        outputStream.write(buffer, 0, len);
    }
}
```

### ミス 4: `supportsProbeExecution()` を false にして `probeSkipReason()` を空にする

```java
// ❌ 理由がないとテストが失敗する
@Override
public boolean supportsProbeExecution() {
    return false;
}

// ✅ 必ず理由を説明する
@Override
public boolean supportsProbeExecution() {
    return false;
}

@Override
public String probeSkipReason() {
    return "This command requires a real database connection that is not available in unit tests.";
}
```

---

## 6. プローブテストの仕組み

`CommandStreamingContractTest` は以下の手順でストリーミング準拠を確認します。

```
[入力ストリーム]                    [コマンド]            [出力ストリーム]
      |                                 |                       |
      |  最後の1バイトを除く全データ       |                       |
      |  ─────────────────────────────> |                       |
      |                                 |                       |
      |  最後の1バイトをブロック(最大1秒)  |                       |
      |  = = = = = = = = = = = = =      |                       |
      |                                 |  出力が来た? ─────────> |
      |                                 |  ↑ここを観測           |
      |  最後の1バイトをリリース           |                       |
      |  ─────────────────────────────> |                       |
      |                                 |  完了 (最大5秒)        |
```

**判定基準:**
- ブロック中（残りの入力がリリースされる前）に出力が始まった → `STREAMING_COMPLIANT`
- ブロック中に出力が始まらなかった → `STREAMING_COMPLIANT` を設定していた場合はテスト失敗

---

## 7. 実装例の参考

シンプルな例から複雑な例へ：

| コマンド | 特徴 | ファイル |
|---------|------|---------|
| `CharacterConvertCommand` | 最もシンプル。バッファで読みながら変換 | `impl/charcode/` |
| `LineEndingNormalizeCommand` | 文字列処理のストリーミング例 | `impl/` |
| `CsvValidateCommand` | CSV解析のストリーミング | `impl/csv/` |
| `FileBufferCommand` | `ALLOWED_FULL_BUFFERING` の例 | `impl/` |
| `SendHttpCommand` | `KNOWN_STREAMING_VIOLATION` の例 | `streamconverter-http` モジュール |

---

## 関連ドキュメント

- [TESTING.md](TESTING.md) - テスト戦略全体
- [COMMAND_EXAMPLES.md](COMMAND_EXAMPLES.md) - コマンドの使用例
- [CLAUDE_ARCHITECTURE.md](CLAUDE_ARCHITECTURE.md) - アーキテクチャの詳細
