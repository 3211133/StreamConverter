# StreamConverter 例外規定（Exception Policy）

> 📋 **このドキュメントについて**: StreamConverter プロジェクトの例外の分類・型・報告ルールを定める規定です。
> 新規コードはこの規定に従って実装し、既存コードは段階的に適用します（[適用計画](#-適用計画)参照）。
> 策定の経緯と背景は [issue #786](https://github.com/3211133/StreamConverter/issues/786) を参照してください。

## 🎯 目的

2026-05〜06 のバグ起票・修正のほとんどが例外処理の不備（誤ラベル、黙殺、二重ログ、スロー型の不統一）に
起因していた。個別対処では再発を防げないため、以下を規定として明文化する。

- 例外の**分類体系**と、分類ごとの**例外型・メッセージ規約**
- コマンドが公開 API として**スローしてよい例外**の範囲
- **catch 節・ログ出力**の規約
- 依存ライブラリが例外を**黙殺**する場合の検出責務
- パイプライン実行境界での**最終送出規約**

## 🗂️ 例外の3分類

すべての例外は、**誰が直せば解消するか**で次の3分類のいずれかに割り当てる。

| 分類 | 意味 | 直す人 | 例外型 | 例 |
|---|---|---|---|---|
| **A. 入力データ不正** | 入力を直せば解消する失敗（パース失敗・検証失敗） | 入力データの提供者 | `InvalidInputDataException`（Phase 2 で新設。`StreamProcessingException` のサブクラス） | CSV 構文不正、スキーマ検証失敗、必須カラム欠落 |
| **B. 環境（I/O）障害** | ネットワーク・ディスク等の障害。リトライ・運用対処の対象 | オペレーター / インフラ | JDK の `IOException` を**ラップせずそのまま伝播** | `read()` / `write()` の `IOException`、接続切断 |
| **C. 実装バグ** | コードの誤り。呼び出し側で回復不能 | 開発者 | `IllegalArgumentException` / `IllegalStateException`（unchecked） | 不正な設定値、API 契約違反 |

### 分類の判断基準

- 「入力ファイルを正しいものに差し替えたら成功するか？」→ Yes なら **A**
- 「同じ入力でリトライしたら成功し得るか？」→ Yes なら **B**
- 「どんな入力・環境でも発生するコードの誤りか？」→ Yes なら **C**

### 分類 A の型は基底1型で開始する

`InvalidInputDataException` を基底とし、当面はこの1型で運用する。
CSV 構文不正と業務バリデーション失敗のようにメッセージ粒度・テスト観点が分かれてきた場合は、
`MalformedInputException` / `ValidationException` 等のサブタイプを**この基底の下に**追加してよい
（拡張点として明示的に許容する）。最初から細分化はしない。

## 🏛️ 例外型カタログ

| 型 | 継承 | 位置づけ |
|---|---|---|
| `StreamProcessingException` | `extends IOException` | ストリーム処理失敗の基底型。**IOException 継承は維持する**（`IStreamCommand.execute()` の `throws IOException` 契約と整合し、呼び出し側が単一の `catch (IOException)` で全失敗を扱える） |
| `InvalidInputDataException` | `extends StreamProcessingException`（Phase 2 で新設） | 分類 A 専用型。検証・パース失敗はすべてこの型（またはサブタイプ）でスローする |
| `PipeAbortedException` | `extends IOException` | 対向コマンド異常終了による**分類 B の副次的失敗**。現行の位置づけを維持（`StreamConverter` は根本例外を優先して呼び出し元へ伝える） |
| JDK `IOException` | — | 分類 B。**包み直さない**のが原則 |
| `IllegalArgumentException` / `IllegalStateException` | unchecked | 分類 C。コマンドは catch しない |
| `UncheckedStreamException` | `extends RuntimeException`（Phase 2 で新設） | **内部キャリア専用**。`IRule` 等 checked 例外をスローできない箇所で `IOException` を運び、コマンド境界で unwrap する。公開 API から漏らしてはならない |

### ⚠️ 型階層の罠（必読）

`StreamProcessingException` と opencsv の `CsvMalformedLineException` はいずれも `IOException` の
サブクラスである。したがって **`catch (IOException)` は「環境障害だけを捕まえる」という意味にならない**。
自前の例外・パース失敗例外まで捕捉し、誤ラベルの温床になる（issue #748 / PR #782 で実際に発生）。
この罠への対処が次節の catch 規約である。

## 📐 スロー規定（コマンド公開 API）

1. `IStreamCommand.execute()` がスローしてよいのは **`IOException` とそのサブタイプのみ**
2. 検証失敗・パース失敗（分類 A）は `InvalidInputDataException` としてスローする
   - checked であり `execute()` の契約に適合する。RuntimeException で代用してはならない
3. 環境障害（分類 B）は捕捉せず素通しする（後述の catch 規約参照）
4. RuntimeException がコマンド境界から漏れてよいのは分類 C（実装バグ）の場合のみ
5. `UncheckedStreamException` は内部実装の詳細であり、コマンド境界の外へ漏らしてはならない

## 🧤 catch 節の規約

1. **広い `catch (IOException)` で原因ラベルを付け直すことを禁止する**
   - 分類 A への変換は、**具体型の catch のみ**で行う（例: `catch (CsvValidationException | CsvMalformedLineException e)`）
   - 素の `IOException`（分類 B）は触らず伝播させる。「Failed to parse ...」等のラベルを付けて包み直してはならない
2. **原因例外（cause）は必ず保持する**。例外を変換する場合は必ず元例外を cause に渡す
3. ライブラリ固有の例外型は**分類で扱いを決める**
   - opencsv の `CsvMalformedLineException` は `IOException` のサブクラスだが**分類 A**（入力データ不正）として扱う
4. 例外の分類・変換ロジックは共通ヘルパーに一本化する（Phase 2 で `ExceptionClassifier` 的な変換関数を導入予定）。
   コマンドごとに独自の分類 if/instanceof を書かない

## 📝 ログ規約

1. **失敗ログは最外層の1回のみ**。`IStreamCommand.withLogging()` ラッパーが出力する
2. コマンド内部での **log & rethrow を禁止**する（issue #749 の二重出力の再発防止）
3. コマンド内部で文脈を付加したい場合は「**変換してスロー**」のみ行い、ログは書かない（issue #742 の方針を内包）
4. メッセージ規約
   - 分類 A: 「何が・どの位置で不正か」を含める（行番号・カラム名等）
   - 分類 B: ラップしないため独自メッセージを付けない（文脈付加は境界送出規約に従う）
   - 分類 C: 違反した契約・前提を明記する

## 🔀 パイプライン境界の送出規約

`CommandStageRunner`（`StreamConverter.run()` 経路）はステージ失敗を呼び出し元へ送出する**唯一の境界**であり、
ここでの包み直しが分類を壊してはならない。現行の `toStageFailure()` は `IOException` も `RuntimeException` も
一律 `StreamProcessingException` に包み直しており、Phase 2 で以下の規約に改める。

| ステージ失敗の原因 | 送出 |
|---|---|
| `InvalidInputDataException`（分類 A） | **そのまま**伝播 |
| 素の `IOException`（分類 B） | **`IOException` のまま**伝播。コマンド名等の文脈が必要な場合は suppressed 例外や context 付与で行い、**型は変えない** |
| `RuntimeException`（分類 C） | 実装バグとして**包み直さず**伝播 |
| `Error` | 包まず再スロー（現行どおり） |

## 🕳️ 依存ライブラリの例外黙殺対策

opencsv 5.12.0 の `CSVReader.readNext()` は、下位ストリームの `IOException` を伝播せず EOF（null）として
扱うことが実測で確認されている（issue #783 / #784）。`while ((row = readNext()) != null)` ループは
I/O 障害と入力の終端を区別できず、途中切断された入力を完全な入力として処理してしまう。

### 検出責務

**外部ライブラリにストリームを渡す層**（`CsvWalker` / `CsvFilterCommand` / `CsvValidateCommand` 等）が、
黙殺を検出して分類 B として報告する責務を負う。

### 共通機構の要件（Phase 2 で実装）

- `read()` で発生した `IOException` を記録して再スローし、ライブラリが握りつぶした場合に備えて
  **終了時に必ず検査が走る**形状とする
  - `close()` 時に記録済み障害を再送出する `Reader` / `InputStream` ラッパー、または
  - `CSVReader` 利用全体を囲む高階 API（`withFaultCheckedCsvReader(...)` のような形状）
- **利用者に明示チェックを書かせない**。「ループ終了後に `rethrowIfFaulted()` を呼ぶ」のような
  呼び忘れに弱い API 形状は**採用しない**
- 配置はライブラリ連携ヘルパーとして閉じ込める（汎用 I/O ユーティリティとして公開しない）

## 🧩 IRule 系の checked 例外伝搬

`IRule.apply(String): String` は throws 宣言を持たない公開関数型インターフェースであり、
シグネチャ変更（`throws IOException` 追加）はソース互換・バイナリ互換の両面で影響が大きいため**行わない**。

1. ルール実装内の `IOException` は `UncheckedStreamException`（内部キャリア）で包む
2. コマンド境界（Walker / `CommandStageRunner`）で unwrap し、`IOException` として再スローする
3. `sneakyThrow` は**全廃**し、この機構に統一する（issue #740 の解決。現行の散在箇所:
   `CommandStageRunner` / `JsonWalker`）
4. 将来 checked 例外を型で表現したくなった場合は、`ThrowingRule` 等の**別インターフェース追加**
   （コマンド側で overload）を本命とする。`IRule` の破壊的変更をやる場合は別 issue・別メジャーバージョンとして扱う

## 🚫 アンチパターン集

| アンチパターン | 何が起きるか | 実例 | 正しい形 |
|---|---|---|---|
| 広い `catch (IOException)` での原因ラベル付け直し | 環境障害が「パース失敗」等に誤分類される | #748 | 具体型 catch のみで分類 A に変換。素の `IOException` は素通し |
| `readNext() != null` ループを黙殺検査なしで使う | I/O 障害が EOF 扱いになり出力が黙って切り詰められる | #783 / #784 | fault 検査付きヘルパー経由で読む |
| コマンド内部での log & rethrow | 同一エラーが二重にログ出力される | #749 | ログは `withLogging()` の1回のみ |
| cause を捨てた例外変換 | 根本原因が追跡不能になる | #742 | 変換時は必ず cause を渡す |
| `sneakyThrow` の個別実装 | checked 例外の迂回手段が散在し追跡不能 | #740 | `UncheckedStreamException` キャリアに統一 |
| 境界での一律 `StreamProcessingException` 包み直し | 分類 B が分類不能になり呼び出し側が切り分けられない | `toStageFailure()` 現行実装 | 境界送出規約に従い分類別に送出 |

### 静的検査の方針

規約はレビューだけでは守りきれないため、以下を段階的に導入する（Phase 2 以降）。

- PMD カスタムルール等による「広い `catch (IOException)` + 包み直し」パターンの検出
- `sneakyThrow` 実装（`@SuppressWarnings("unchecked")` + `throw (T)`）の新規追加検出
- 導入までの間は、本ドキュメントの[アンチパターン集](#-アンチパターン集)を PR レビューのチェック観点として用いる

## 🗓️ 適用計画

| Phase | 内容 | 状態 |
|---|---|---|
| **Phase 1** | 本規定文書の策定 | 本ドキュメント |
| **Phase 2** | 共通機構の実装 + 単体テスト: `InvalidInputDataException` 新設、CSV 連携 fault 検査ヘルパー、`UncheckedStreamException`（`sneakyThrow` 置換）、`CommandStageRunner` 送出規約の見直し | 未着手 |
| **Phase 3** | 既存コマンドへの適用: #783 / #784 の修正、#729 / #731 の再評価（起票時の前提が現行コードと異なるため規定に照らして判断）、#740 / #741 / #742 / #749 の対応 | 未着手 |

各 Phase は別 PR とする。新規コードは Phase 2 を待たず、本規定の分類・catch・ログ規約に従うこと
（新設型が必要な箇所は `StreamProcessingException` + 規約準拠メッセージで代用し、Phase 2 で置換する）。

## 🔗 スコープ外・関連ドキュメント

- **fail-fast / skip / retry の実行時戦略**は本規定のスコープ外（[issue #505](https://github.com/3211133/StreamConverter/issues/505) の論点）
- [logging-rules.md](../logging-rules.md): ログ運用ポリシー（本規定のログ規約はこの上に成り立つ）
- [ARCHITECTURE.md](../ARCHITECTURE.md): パイプラインの実行モデル（境界送出規約の前提）
- [reference/TESTING.md](TESTING.md): テスト戦略（known-bug 証明テストの運用を含む）
