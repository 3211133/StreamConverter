# Gemini CLI Review Prompt

## レビュー依頼コマンド

```bash
# 基本的なコードレビュー
gemini "StreamConverterプロジェクトのログ基盤実装のコードレビューをお願いします。

## 対象コミット
- Commit ID: 89ef314
- 変更ファイル: 30 files
- 追加行数: 3,745 lines

## 主な実装内容
1. SLF4J + Logbackによる統一ログフレームワーク
2. AbstractStreamCommandの自動ログ出力機能
3. LoggingDecoratorパターンによる詳細ログ出力
4. CommandFactoryによるログ機能付きコマンド生成
5. MeasuredStreamによる透過的データサイズ測定

## 重点レビューポイント
- AbstractStreamCommand.execute()のfinal化の影響
- LoggingDecoratorとAbstractStreamCommandの重複ログ問題
- パフォーマンスへの影響（特に自動測定機能）
- CommandFactoryのリフレクション使用の妥当性
- エラーハンドリングの網羅性

## 確認したいファイル
- src/main/java/com/streamConverter/command/AbstractStreamCommand.java
- src/main/java/com/streamConverter/command/LoggingDecorator.java
- src/main/java/com/streamConverter/command/CommandFactory.java
- src/main/java/com/streamConverter/util/MeasuredInputStream.java
- src/main/java/com/streamConverter/util/MeasuredOutputStream.java

設計の妥当性、パフォーマンスへの影響、潜在的な問題点について詳細にレビューしてください。"
```

## 詳細レビュー用コマンド

```bash
# 特定ファイルに焦点を当てたレビュー
gemini "以下のJavaファイルのコードレビューをお願いします。

## ファイル: AbstractStreamCommand.java
- execute()メソッドをfinal化
- 自動ログ出力機能を追加
- パフォーマンス測定機能を組み込み

## レビューポイント
1. final化による既存コードへの影響
2. 自動ログ出力のオーバーヘッド
3. エラーハンドリングの適切性
4. メモリ使用量測定の精度

$(cat src/main/java/com/streamConverter/command/AbstractStreamCommand.java)"
```

## 設計レビュー用コマンド

```bash
# アーキテクチャ設計のレビュー
gemini "StreamConverterプロジェクトの自動ログ出力システムの設計をレビューしてください。

## 設計アプローチ
多層防御アプローチを採用:
- Level 1: AbstractStreamCommand（基本的な自動ログ出力）
- Level 2: LoggingDecorator（詳細ログ追加）
- Level 3: CommandFactory（統一的な生成管理）

## 設計文書
$(cat docs/auto-logging-design.md)

## 実装報告
$(cat docs/auto-logging-implementation.md)

## 評価ポイント
1. 設計の妥当性とスケーラビリティ
2. パフォーマンスへの影響
3. 保守性と拡張性
4. 使いやすさ（開発者体験）

改善提案や懸念点があれば指摘してください。"
```

## パフォーマンスレビュー用コマンド

```bash
# パフォーマンス重点レビュー
gemini "StreamConverterの自動ログ出力機能のパフォーマンス影響をレビューしてください。

## 対象機能
1. 自動データサイズ測定（MeasuredStream）
2. 実行時間測定
3. メモリ使用量監視
4. 詳細ログ出力（LoggingDecorator）

## 懸念点
- 大量データ処理時のオーバーヘッド
- メモリ使用量測定の精度と影響
- ログ出力の頻度と性能への影響

## 測定結果例
$(cat src/main/java/com/streamConverter/examples/AutoLoggingDemo.java | head -50)

パフォーマンスボトルネックの可能性と最適化提案をお願いします。"
```

## 使用方法

1. **基本レビュー**: 最初のコマンドで全体的なレビューを依頼
2. **詳細レビュー**: 特定のファイルや機能に焦点を当てたレビュー
3. **設計レビュー**: アーキテクチャや設計判断のレビュー
4. **パフォーマンスレビュー**: 性能面での詳細な分析

## 注意事項

- ファイル内容が長い場合は、`head -100`などで適切に切り取る
- レビュー結果は必ず保存して、改善点を整理する
- 複数回に分けてレビューすることで、より詳細なフィードバックを得られる