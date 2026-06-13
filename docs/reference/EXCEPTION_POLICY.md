# StreamConverter 例外規定（Exception Policy）

> 📋 **このドキュメントについて**: StreamConverter プロジェクトの例外の分類・型・報告ルールを定める規定です。
> 新規コードはこの規定に従って実装し、既存コードは段階的に適用します（[適用計画](#-適用計画)参照）。
> 策定の経緯と背景は [issue #786](https://github.com/3211133/StreamConverter/issues/786) を参照してください。

## 🎯 目的

2026-05〜06 のバグ起票・修正のほとんどが例外処理の不備（誤ラベル、黙殺、二重ログ、スロー型の不統一）に
起因していた。個別対処では再発を防げないため、以下を規定として明文化する。

- 例外の**分類体系**と、分類ごとの**例外型・メッセージ規約**
- アーキテクチャ階層（L1〜L4）ごとの**例外責務**（生成・伝播・記録の分担）
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

## 🏗️ アーキテクチャ階層と例外責務

分類（A/B/C）が「**誰が直せば解消するか**」の軸であるのに対し、本節は「**どの層が例外を生成・伝播・記録するか**」
の軸を定める。両者は直交しており、各層は分類を変えずに伝播させるのが大原則である。

### 層の定義（ARCHITECTURE.md の構造に対応）

| 層 | 構成要素 | モジュール |
|---|---|---|
| **L1 利用者・統合層** | 外部システム、REST（`StreamProcessingController`）、examples | streamconverter-web / 利用者コード |
| **L2 パイプライン層（エンジン）** | `StreamConverter` / `CommandStageRunner` / `PipelineFailureHandler` / `PipelineCompletionMonitor` / `AbortablePipedStream` / `withLogging` | streamconverter-core |
| **L3 コマンド層** | `IStreamCommand` 実装（Walker / Filter / Validate / Convert）、`ConsumerCommand` 等の中間抽象 | streamconverter-core / http |
| **L4 ルール層** | `IRule` 実装（`PassThroughRule` / `ChainRule` / `DatabaseFetchRule` 等） | streamconverter-core / db |
| 横断 | 外部ライブラリ連携ヘルパー（fault 検査 Reader 等）、外部ライブラリ（opencsv / Jackson / StAX / JDBC） | — |

`UncheckedStreamException`（内部キャリア）が public である理由は、L4 が別モジュール
（streamconverter-db 等）に存在するためである。役割はあくまで層間の運搬であり、L2 を越えて
呼び出し元に漏れたら実装バグ（分類 C）として扱う。

### エンジン起源障害（補助軸）

L2 エンジン自身が失敗の発生源になる場合（中間出力 close の失敗、待機中の割り込み、原因を特定
できないステージ失敗）は、分類 A/B/C の主語（入力・環境・コマンド実装）に当てはまらない
「**エンジン起源障害**」であり、**`StreamProcessingException`（基底型）で報告する**。現行の
`closeStageOutput()` / `PipelineCompletionMonitor` の挙動はこの規定に適合する。
L1 で機械的に区別する需要が生じた場合は `EngineFailureException extends StreamProcessingException`
を切り出す（拡張点として予約）。分類 B への吸収は行わない — 割り込みや stage close 失敗は
外部環境 I/O とは remediation が異なるためである。

### 層別責務マトリクス

| 層 | スローしてよいもの | 変換 | unwrap | 失敗ログ |
|---|---|---|---|---|
| **L1 利用者・統合層** | 任意（本規定の対象外） | 表現変換（HTTP ステータス等）は L1 の責務（[付録](#-付録-l1利用者統合層向け指針non-normative)参照） | — | 自文脈での記録可 |
| **L2 エンジン** | コマンド由来: 境界送出規約に従い元の型のまま／エンジン起源障害: SPE | 型を変えない。文脈は suppressed（`StageFailureContext`） | **キャリアの必須 unwrap 点** | **core 内で唯一の失敗ログ点**（`withLogging`、失敗ステージごとに1回） |
| **L3 コマンド** | 想定される失敗（入力不正・環境障害）は `IOException` 系のみ。**実装バグは RuntimeException のまま漏れてよい**（分類 C の成立要件） | 具体型 catch → 分類 A への変換のみ可。B は素通し | ルールを直接呼ぶ箇所（Walker）はキャリアを unwrap | **禁止**（log & rethrow 禁止） |
| **L4 ルール** | unchecked のみ（`IRule` の言語制約）。checked I/O はキャリアで包む | checked → キャリアのみ | — | **log & rethrow 禁止**。例外を伴わない状態ログ（warn/info）は可 |

中間抽象（`ConsumerCommand` 等）は**文脈付加は可**。分類の変更は、その抽象自身が意味的責務を
持つ場合（例: consume 実装が検証失敗を `InvalidInputDataException` に変換する）に限り可とする。

### 層越え規約

1. 例外の識別は**型のみ**で行う。メッセージ文字列によるルーティング・分岐を禁止する
2. **意味を変えるラップは公開境界で最大1回**。キャリア（`UncheckedStreamException`）は実装上の
   運搬であり、ラップ回数には数えない
3. 上位層は下位層の内部機構（キャリア、suppressed の内部マーカー等）に依存したハンドリングを書かない

## 📐 スロー規定（コマンド公開 API）

1. `IStreamCommand.execute()` がスローしてよいのは **`IOException` とそのサブタイプのみ**
2. 検証失敗・パース失敗（分類 A）は `InvalidInputDataException` としてスローする
   - checked であり `execute()` の契約に適合する。RuntimeException で代用してはならない
3. 環境障害（分類 B）は捕捉せず素通しする（後述の catch 規約参照）
4. **想定される失敗**（入力不正・環境障害）でスローしてよいのは `IOException` 系のみ。一方、
   分類 C（実装バグ）の `RuntimeException` はコマンド境界から**そのまま漏れてよい**（包み直さない）。
   これは L2 が分類 C を成立させるための前提であり、`IOException` 系への矯正は行わない
5. `UncheckedStreamException` は内部キャリアであり、コマンド境界の外へ漏らしてはならない（Walker が unwrap する）

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

1. **core 内の失敗ログ点は `withLogging` ラッパーのみ**。`StreamConverter` が全コマンドに適用し、
   失敗したステージごとに1回 ERROR を出力する（パイプライン全体で「最外層1回」ではない点に注意。
   L1 が自文脈で追加記録することは妨げない — 層別責務マトリクス参照）
2. L3 / L4 での **log & rethrow を禁止**する（issue #749 の二重出力の再発防止）。
   例外を伴わない状態ログ（結果件数の warn / 進行状況の info 等）は各層で出力してよい
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

なお、ステージ失敗の翻訳とは別に、**L2 エンジン自身が発生源となる障害**（中間出力 close の失敗、
待機中の割り込み等）は[エンジン起源障害](#エンジン起源障害補助軸)として `StreamProcessingException`
で送出する。

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
| **Phase 3** | 既存コマンドへの適用: #783 / #784 の修正、#729 / #731 の再評価（起票時の前提が現行コードと異なるため規定に照らして判断）、#740 / #741 / #742 / #749 の対応、ルール層の log & rethrow 是正（階層監査で発見。PR #792 で対応）、web 層（L1）の例外マッピング検討 | 未着手 |

各 Phase は別 PR とする。新規コードは Phase 2 を待たず、本規定の分類・catch・ログ規約に従うこと
（新設型が必要な箇所は `StreamProcessingException` + 規約準拠メッセージで代用し、Phase 2 で置換する）。

## 📎 付録: L1（利用者・統合層）向け指針（non-normative）

`StreamConverter.run()` から呼び出し元に届く例外は次の3系統＋エンジン起源障害である。

| 届く型 | 意味 | HTTP への推奨マッピング（事前検証フェーズの失敗のみ） |
|---|---|---|
| `InvalidInputDataException` | 分類 A: 入力データ不正 | 400 / 422 |
| 素の `IOException` | 分類 B: 環境（I/O）障害 | 502 / 503 |
| `RuntimeException` | 分類 C: 実装バグ | 500 |
| `StreamProcessingException`（上記以外） | エンジン起源障害 | 500 |

**ストリーミング応答の制約（重要）**: 上記マッピングが可能なのは**レスポンスヘッダ送信前に検知
できた失敗のみ**である。ストリーミング処理の性質上、多くの失敗は 200 OK 送信後のボディ転送中に
発生し、その時点ではステータスコードを変更できない（接続切断・チャンク中断等の表現になる）。
不完全出力を外部に見せない保証が必要な場合は、L1 側で一時領域への書き出し→成功時 publish の
二段階設計を検討すること（本ライブラリは「メモリに全て持たない」設計のため、この保証を
エンジン側では提供しない）。

本付録は推奨であり規定（normative）ではない。

## 🔗 スコープ外・関連ドキュメント

- **fail-fast / skip / retry の実行時戦略**は本規定のスコープ外（[issue #505](https://github.com/3211133/StreamConverter/issues/505) の論点）
- [logging-rules.md](../logging-rules.md): ログ運用ポリシー（本規定のログ規約はこの上に成り立つ）
- [ARCHITECTURE.md](../ARCHITECTURE.md): パイプラインの実行モデル（境界送出規約の前提）
- [reference/TESTING.md](TESTING.md): テスト戦略（known-bug 証明テストの運用を含む）
