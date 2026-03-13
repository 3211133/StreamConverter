# ScopedValue 移行計画

## 概要

このドキュメントは、`PipelineContext` における `ThreadLocal` から Java の `ScopedValue` への移行計画を記録します。

## 背景

現在の `PipelineContext` は `ThreadLocal<PipelineContext>` を使用してパイプライン実行コンテキストをスレッドに紐付けています。Java 21 では仮想スレッドが正式導入され、`ThreadLocal` の代替として `ScopedValue` (JEP 446、Java 21 Preview → Java 25 LTS 予定) が提案されています。

### 現行実装

```java
// PipelineContext.java
private static final ThreadLocal<PipelineContext> HOLDER = new ThreadLocal<>();

public static void set(PipelineContext context) {
    HOLDER.set(context);
}

public static void clear() {
    HOLDER.remove();
}
```

`StreamConverter` 内では各仮想スレッドで `PipelineContext.set(pipelineContext)` → 処理 → `PipelineContext.clear()` のライフサイクルで管理しています。

## ScopedValue の優位性

| 観点 | ThreadLocal | ScopedValue |
|------|-------------|-------------|
| スコープ管理 | 手動 set/clear 必須 | `ScopedValue.where(...).run(...)` で自動 |
| 仮想スレッド | 親から継承されない（`InheritableThreadLocal` で対応） | 構造化並行性と連携 |
| メモリリーク | clear 忘れのリスク | スコープ終了で自動クリア |
| イミュータビリティ | 値の再設定が可能 | スコープ内で不変（設計上の安全性） |

## 移行設計

### 目標アーキテクチャ

```java
// 移行後のイメージ（Java 25 LTS 以降）
private static final ScopedValue<PipelineContext> HOLDER = ScopedValue.newInstance();

// StreamConverter.executeCommands() 内
ScopedValue.where(PipelineContext.HOLDER, pipelineContext)
    .call(() -> {
        command.execute(commandInput, commandOutput);
        return null;
    });
```

### 移行手順

**Step 1: 抽象化レイヤーの導入**（移行準備）
- `PipelineContext` の `set()`/`clear()` を `PipelineContext.Scope` インターフェースでラップ
- `ThreadLocalScope` 実装（現行）と `ScopedValueScope` 実装（Java 25 用）を用意

**Step 2: ScopedValue Preview での検証**
- Java 25 Early Access ビルドで `ScopedValueScope` を実装・テスト
- `StreamConverter` の構造化並行性（`StructuredTaskScope`）との組み合わせを検証

**Step 3: 本番移行**（Java 25 LTS リリース後）
- `ThreadLocalScope` を削除し `ScopedValueScope` に一本化
- `InheritableMDCAdapter` との連携を再検討

## 凍結条件

以下の条件が揃うまで実装を凍結します：

| 条件 | 状態 |
|------|------|
| Java 25 LTS リリース | 未リリース（予定、as of 2026-03） |
| `ScopedValue` の final 化 (JEP 487 相当) | Java 25 Preview |
| プロジェクトの最小 Java バージョン引き上げ | 現在 Java 21 |

**凍結解除トリガー**: Java 25 LTS の GA リリース、かつプロジェクトの最小バージョン要件を Java 25 以上に更新する決定。

## 関連情報

- JEP 446: Scoped Values (Preview) — Java 21
- JEP 487: Scoped Values (Fourth Preview) — Java 24
- 参照: [`PipelineContext.java`](../../streamconverter-core/src/main/java/com/streamconverter/context/PipelineContext.java)
- 参照: [`InheritableMDCAdapter.java`](../../streamconverter-core/src/main/java/com/streamconverter/logging/InheritableMDCAdapter.java)
- 参照: Issue #498
