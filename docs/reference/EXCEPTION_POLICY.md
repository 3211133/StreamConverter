# StreamConverter 例外規定（Exception Policy）

> 📋 **このドキュメントについて**: StreamConverter プロジェクトの例外の分類・型・報告ルールを定める規定です。
> 新規コードはこの規定に従って実装し、既存コードは段階的に適用します（[適用計画](#-適用計画)参照）。
> 策定の経緯と背景は [issue #795](https://github.com/3211133/StreamConverter/issues/795) を参照してください。
> 旧版（issue #786 ベース）は通知配送軸を持たない技術的分類体系でしたが、本版では「**誰に何を通知するか**」を軸とした体系に再構築しています。

## 🎯 目的

- 例外を「**通知配送軸**（誰に何を伝えるか）」「**並行例外集約軸**（複数同時失敗をどう束ねるか）」「**階層別責務軸**（main / converter / command / rule の各層が何を投げ・捕まえるか）」の3軸で規定する
- 利用者（main 層）が一貫した方法でエンドユーザー通知・ログ出力・管理者通知を実装できるようにする
- 例外解釈責務は **main 層が独占**。converter / command / rule は例外を運ぶだけ
- POC 実装の制約に引っ張られず、あるべき姿として規定する

## 🗂️ 通知3分類

すべての例外は、**誰に通知するか**で次の3分類のいずれかに割り当てる。
これは旧版の「誰が直せば解消するか」（A/B/C 分類）とは別軸であり、本版では通知軸が一次分類となる。

| 分類 | 通知対象 | 意味 | 例外型 |
|---|---|---|---|
| **U** | エンドユーザー | 入力起因。利用者がメッセージを見て入力を直す | `UserInputException` |
| **T** | エンドユーザーに「待て」 | 外部システムの一時障害。再試行で解決する可能性 | `ExternalTransientException` |
| **A** | 管理者 | 内部実装バグ・外部恒久障害・内部システム障害。エンドユーザーには原因を伏せ管理者連絡を促す | `ExternalPermanentException` / `InternalSystemException` |

### 分類の判断基準

- 「**利用者が入力を直せば解消するか**」→ Yes なら **U**
- 「**外部システムの一時障害で、時間をおけば解決する可能性が高いか**」→ Yes なら **T**
- 上記いずれにも該当しないすべての失敗 → **A**

T と A の境界判定は「**外部接続クラス自身が当該プロトコルにおいてどのレスポンスが時間で解決するかを知っている**」という前提に立つ。
判定不能な場合は保守的に A に倒す（エンドユーザーを「待て」と誤誘導しないため）。

### 内部 transient は原則認めない

「内部システムの一時障害で待てば解決」というケースは、1リクエスト内で許容できるタイムアウトでは解決しない待ち時間を要する。
したがって**内部で発生する transient は T として外に出さない**:

- 内部リトライ・内部キューで吸収する
- 解決しなければ A として外に出す
- T で外に出すには明示的な設計判断レビューが必要

## 🏛️ 例外型階層

```
IOException
└── StreamProcessingException                 // abstract 基底（通知3分類の親 / 利用者の catch ターゲット）
    ├── AggregatedStreamProcessingException   // converter→main の集約コンテナ（converter のみ throw 可）
    ├── UserInputException                    // U（動的 userMessage を持つ）
    ├── ExternalSystemException               // 外部起源マーカー（abstract）
    │   ├── ExternalTransientException        // T
    │   └── ExternalPermanentException        // A（外部恒久）
    └── InternalSystemException               // A（内部）
```

設計の要点:

- **`IOException` 配下を維持**。利用者は `catch (IOException)` ワンライナーで全失敗を捕捉でき、通知分類によって細分catch することもできる
- **`StreamProcessingException` は abstract**。直接 throw・直接 new はコンパイル不可とし、規定の縛りを型機構で強制する。利用者は catch ターゲットとしてのみ用いる
- **`ExternalSystemException` も abstract**。T/A 判定不能時に直接 throw することはコンパイル不可、保守的に A サブタイプに倒す
- **`AggregatedStreamProcessingException` は converter→main の伝達専用**。converter が並行集約結果を1つにまとめて main に投げるための具象コンテナで、`getAllFailures()` で個別失敗（`UserInputException` 等の末端型）を返す。converter 以外の層からの throw は規約上禁止

### 各型のスロー可否

| 型 | 直接 throw 可否 | 用途 |
|---|---|---|
| `StreamProcessingException` | 不可（abstract） | 基底 / 利用者の catch ターゲット |
| `AggregatedStreamProcessingException` | 可（converter 層のみ） | 並行集約結果を main へ運ぶコンテナ |
| `UserInputException` | 可 | U 分類のスロー |
| `ExternalSystemException` | 不可（abstract） | 利用者の catch ターゲット（T/A まとめて） |
| `ExternalTransientException` | 可 | T 分類のスロー |
| `ExternalPermanentException` | 可 | A 分類（外部恒久障害）のスロー |
| `InternalSystemException` | 可 | A 分類（内部障害）のスロー |

## 💬 メッセージ機構

利用者がエンドユーザー画面・管理者通知・開発者ログにそれぞれ適切な情報を渡せるよう、例外オブジェクトは**3層のメッセージ機構**を持つ。

### `getUserMessage()`

エンドユーザー向けメッセージを返す。**すべての例外で利用可能**であり、`showUser(e.getUserMessage())` を分岐なしで呼べる。

- `UserInputException`: コンストラクタで指定した**動的メッセージ**（入力のどこがどう悪いかを動的に組み立てる）
- 他の型: 型ごとに **static デフォルトメッセージ** を持ち、`public static final` として公開する

```java
public abstract class StreamProcessingException extends IOException {
    public static final String DEFAULT_USER_MESSAGE =
        "システムエラーが発生しました。管理者にお問い合わせください。";
    public String getUserMessage() { return DEFAULT_USER_MESSAGE; }
}

public class ExternalTransientException extends StreamProcessingException {
    public static final String DEFAULT_USER_MESSAGE =
        "サービスが混雑しています。時間をおいて再試行してください。";
    @Override public String getUserMessage() { return DEFAULT_USER_MESSAGE; }
}
```

- 多言語対応は当面なし。**日本語固定**でスタートする
- 必要組織は `MessageResolver` 注入で対応可能とし、設計余地として残す（`getUserMessage(MessageResolver)` のオーバーロード追加経路）
- `cause.getMessage()` を `userMessage` に転記することを**禁止**する（外部ライブラリ例外メッセージは内部実装の詳細＝パス・SQL・スタックトレース等を内包し得るため）

#### `DEFAULT_USER_MESSAGE` の利用パターン

`DEFAULT_USER_MESSAGE` を `public static final` で公開する目的:

1. **i18n 鍵としての参照**: `MessageResolver` 注入時に「どのデフォルトメッセージを翻訳対象とするか」を型名経由で識別できる
2. **テスト時の期待値参照**: テストコードが `assertEquals(ExternalTransientException.DEFAULT_USER_MESSAGE, e.getUserMessage())` のように型固定の検証を書ける
3. **組織側の上書き起点**: 組織側カスタム例外がデフォルトメッセージを書き換える際の比較基準として参照する

アクセスは**必ず型名経由**（`ExternalTransientException.DEFAULT_USER_MESSAGE`）で行うこと。インスタンス経由（`e.DEFAULT_USER_MESSAGE`）は `AccessStaticViaInstance` 警告となる上、サブクラス側のフィールドを参照しない（`static` は継承されない）ため誤読を招く。

### `getMessage()` / `getCause()` / `getStackTrace()`

標準慣習通り**開発者向け**（フル情報）。

- スタックトレース・cause チェイン・内部詳細を含む
- ログ出力・IDE デバッガ・スタックトレースに使われる

### `getOperatorContext()`

運用管理者向けのサニタイズ済み情報を返す。**デフォルトはサニタイズ済み最小情報**（セキュア by default）。

- 開発者向けフル情報（cause / stacktrace / 内部詳細）は標準慣習通り `getMessage()` / `getCause()` / `getStackTrace()` から取得する。`getOperatorContext()` は**それらとは別経路の、運用管理者に渡して安全な最小スキーマ**を返す
- 最小スキーマ（ライブラリ提供）: `IncidentCode`（分類カタログ）・発生時刻・通知3分類（U/T/A）・例外型名（公開可な範囲）・呼び出し側で組み立てた `userMessage`
- セキュリティ要件が緩く「開発者と運用管理者を分けない」組織は `OperatorContextProvider` を注入して**情報量を増やす**ことができる（フル情報相当へ寄せる）
- 既定の `OperatorContext` は不変オブジェクトとし、自由文字列フィールドを最小化する。`IncidentCode` enum 値カタログの整備・マッピング規約は [§4.1.10](#4110-incidentcode-カタログ整備とマッピング規約) で未確定

## 🧼 サニタイズ規約

### `userMessage` のサニタイズ

`userMessage` はエンドユーザーに表示される前提で、**外部公開可能**な内容のみを含む:

| 含めて良い | 含めてはいけない |
|---|---|
| 入力データの位置情報（行番号・列番号・フィールド名） | 内部ファイルパス・URL |
| 形式名（"CSV", "JSON" 等の公開仕様名） | スタックトレース・例外クラス名 |
| 制約条件（仕様上公開されている） | 内部実装の詳細（クラス名・メソッド名・SQL文・正規表現） |
| | 環境情報（スレッド名・MDC値・ホスト名） |

`UserInputException` のコンストラクタは `userMessage` を必須引数として受け取り、サニタイズ済みであることを呼び出し側の責務とする。
外部ライブラリの例外メッセージ（opencsv 等）を `userMessage` に転記することを禁止する。

```java
// NG
catch (CsvMalformedLineException e) {
    throw new UserInputException(e.getMessage(), e);
}

// OK
catch (CsvMalformedLineException e) {
    String userMessage = "CSV format error at line " + e.getLineNumber();
    throw new UserInputException(userMessage, e);
}
```

### `getOperatorContext()` のサニタイズ

デフォルト実装は**サニタイズ済み最小情報**（セキュア by default）。ライブラリは不変な `OperatorContext` 値オブジェクトと `IncidentCode` enum の骨格を提供し、自由文字列フィールドを最小化する。

| 含めて良い | 含めてはいけない |
|---|---|
| `IncidentCode`（分類カタログのキー） | 内部ファイルパス・URL・SQL 文・正規表現 |
| 発生時刻・通知3分類（U/T/A） | スタックトレース・cause チェイン |
| 公開可な例外型名・サニタイズ済み `userMessage` | 環境情報（スレッド名・MDC値・ホスト名・PID 等） |

フル情報が必要な場面では `getMessage()` / `getCause()` / `getStackTrace()` を使う（開発者向け経路）。`OperatorContextProvider` を注入することで情報量を増やすことができるが、規定上は**情報量を減らす方向のオーバーライドのみが望ましい**。

## 🧵 並行例外集約

StreamConverter のパイプラインは複数ステージ・複数ワーカーが並行実行する。
1回の実行で**同時に複数の例外が発生しうる**ため、main 層への伝達方法を規定する。

### main へのインターフェース

- 届くのは**1つの `AggregatedStreamProcessingException` オブジェクト**（`StreamProcessingException` の具象サブクラス）
- 内部に**全独立失敗のリスト**を保持し、`getAllFailures()` で取得可能
- 単独失敗時もリスト要素1件として正規化（list 化のオーバーヘッドを払ってでも main 側の場合分けを消す）
- 用途別代表選択アクセサ（`primaryForUserNotification()` 等）は**作らない**。main 層が自由に解釈する

```java
public abstract class StreamProcessingException extends IOException {
    public String getUserMessage();
    public OperatorContext getOperatorContext();
}

public final class AggregatedStreamProcessingException extends StreamProcessingException {
    public List<StreamProcessingException> getAllFailures();
    // getAllFailures() の要素は末端型（UserInputException 等）のみ。
    // AggregatedStreamProcessingException の入れ子は禁止（converter で平坦化する）。
}
```

main 層の catch ターゲットは基底の `StreamProcessingException` のままで良い（abstract 基底は catch 可能）。

main 層の利用パターン例:

```java
catch (StreamProcessingException e) {
    List<StreamProcessingException> failures =
        (e instanceof AggregatedStreamProcessingException agg) ? agg.getAllFailures() : List.of(e);

    // ユーザー表示は U を最優先
    failures.stream()
        .filter(f -> f instanceof UserInputException)
        .findFirst()
        .or(() -> Optional.of(failures.get(0)))
        .ifPresent(f -> showUser(f.getUserMessage()));

    // ログ・管理者通知は全件
    failures.forEach(f -> log.error("pipeline failure", f));
}
```

なお、規約上 converter は単独失敗の場合も `AggregatedStreamProcessingException` でラップして main に渡すことを既定とする（main の場合分けを消す目的）。上記サンプルの `instanceof` 分岐は防御的記述。

### converter 内でのサプレス（副次的失敗の吸収）

「**他の例外送出によって発生したことが明確な例外**」は converter 内でサプレスし、`getAllFailures()` に**含めない**。
ただし `getSuppressed()` で調査時に取得可能とする（情報を完全に捨てない）。

サプレス対象（確定）:

| 例外 | サプレス理由 |
|---|---|
| 下流停止通知（POC の `PipeAbortedException` 相当） | 上流の失敗が原因。原因例外が別途報告される |
| キャリア例外（POC の `UncheckedStreamException` 相当） | 運搬機構。unwrap した中身が真の例外 |
| `InterruptedException`（**先行失敗が確定している状態で発生**） | 他ワーカー失敗起因のキャンセル協調による中断 |
| **先行失敗確定後に他ワーカーから発生した I/O 例外**（キャンセル協調でブロッキング I/O が打ち切られた結果として現れた `IOException` 等） | 既に起因例外が確定済み。早期中断要求に伴い連鎖的に発生した I/O 失敗は副次扱い |
| `InterruptedException`（利用者起因のキャンセル） | **サプレスしない**。利用者キャンセルは真の終了理由 |

判定基準:

- **型ヒエラルキー判定が主**（下流停止通知・キャリアは型で機械的に判定）
- `InterruptedException` および**他ワーカー I/O 例外**は**コンテキスト判定**（先行失敗の有無で副次扱いか否かが分かれる）
- 「先行失敗確定」とは converter が早期中断要求を発した後の状態を指す。各ワーカーは自身の失敗時刻と converter の中断要求時刻を比較できる必要がある（実装は converter 内に閉じる）
- 判定不能な場合は**独立失敗扱い**（過剰サプレスを避け、保守的に main に渡す）

### 早期中断ポリシー

- ライブラリ標準は**早期中断**
- 1つ目の致命的失敗を検知した時点で他ワーカーへ中断要求を出す
- リソース節約と速報性のため
- 中断要求が届くまでのラグで複数の独立失敗が並ぶ可能性がある。先行失敗確定後に他ワーカーから発生した I/O 失敗・`InterruptedException` の扱いは上記サプレス表に従う
- 「致命的失敗」とは command 層で吸収されずに converter まで届く失敗を指す（吸収可能な失敗は command 層で `getAllFailures()` の対象にならない形で処理されている前提）
- 「走り切ってから集約」オプションは将来必要になったら追加検討

### `getAllFailures()` の順序

- **command の順序に従う**（パイプライン構成順 = 上流→下流）
- 同一 command 階位の複数ワーカーが同時失敗した場合、リスト内で連続するが**その中の順序は実装依存**
- 時刻順ソート等は main 側の責務（例外オブジェクトに発生時刻を持たせれば main 側でソート可能）

### 通知分類外の例外

通知分類（U/T/A）に乗らない**内部制御例外**カテゴリを認める:

- 下流停止通知（POC の `PipeAbortedException` 相当）
- 非同期境界キャリア（POC の `UncheckedStreamException` 相当）
- JDK `InterruptedException`

これらは converter 内で**必ず吸収または unwrap される**ことが規約。main まで届かない契約。

## 🏗️ 階層別責務（叩き台）

階層: **main / converter / command / rule** の4層。各層に **run** と **それ以前の処理**（構築・準備）がある。

### 各層の責務

#### rule

- 通知分類例外（U/T/A）の**最初の発生源**
- `IRule#apply` の throws 句に `StreamProcessingException`（abstract 基底）を指定し、U/T/A サブクラスを**直接 throw する**。`IOException` より狭く、U/T/A 個別列挙より簡潔に、投げられる例外を U/T/A に限定できる（型機構による強制）
- 自身は例外を**吸収しない**（吸収は command の責務）
- 文脈情報（処理中レコード位置・rule 名等）を付加して投げる（**文脈付加の具体的手段は [§4.1.2](#412-文脈付加の手段) で未確定**）
- 外部接続を持つ rule は**自身が T/A 判定**して投げる責務を持つ
- IRule のファンクショナルインターフェース性との両立は [§4.1.11](#4111-irule-のファンクショナルインターフェース性) を参照

#### command

- rule の例外を**ポリシーに基づき吸収するか伝播する**
- 吸収パターン: スキップ / 隔離 / リトライ
- 伝播時は**型を変えず文脈付加のみ**（**例外型変換の許容範囲・「広い catch」の線引きは [§4.1.1](#411-例外型変換の許容範囲) で未確定**）
- 自身が直接 I/O する場合は通知分類確定して投げる
- **明示ポリシーなき吸収は禁止**（`catch (Exception) { /* ignore */ }` 禁止。**規定強度は [§4.1.6](#416-吸収ポリシーの規定強度) で未確定**）

#### converter

- パイプライン起動と並行管理
- 各ワーカーから発生した例外を**独立失敗 vs 副次的失敗**に振り分け
- 集約結果を**1つの `AggregatedStreamProcessingException`** として main に投げる（単独失敗もラップして正規化）
- 通知分類外の例外（下流停止通知・キャリア）を吸収または unwrap
- **ログ出力の集約点**。rule/command 層は log & rethrow せず、ログ出力は converter で一元化する

#### main

- ライブラリ規定対象外
- 受け取った1つの `AggregatedStreamProcessingException`（基底 `StreamProcessingException` として catch 可）を**自由に解釈**
- `getAllFailures()` で全独立失敗を取得して用途別に処理
- ライブラリ契約: 届くのは1つの `AggregatedStreamProcessingException`・通知分類外の例外は届かない

### 階層別責務の詳細は未確定

各層の **run** と **run 以前** で「何を投げ・何を捕まえ・何を伝播させ・何を吸収するか」の詳細は本規定の現バージョンでは未確定。
[未決事項](#-未決事項) 4.1 を参照。

## 🚫 アンチパターン集

| アンチパターン | 何が起きるか | 正しい形 |
|---|---|---|
| 広い `catch (IOException)` での原因ラベル付け直し | 外部恒久障害が U に誤分類される等の誤ラベル | 具体型 catch のみで分類変換。判定不能なら A（保守的） |
| `cause.getMessage()` を `userMessage` に転記 | 内部実装詳細がエンドユーザーに漏洩 | サニタイズ済み文字列を呼び出し側で組み立て |
| `catch (Exception) { /* ignore */ }`（command 層） | rule の例外が業務ポリシーなく握り潰される | 明示ポリシー（スキップ/隔離/リトライ）に基づく吸収のみ可 |
| `ExternalSystemException` 抽象型の直接 throw | T/A の判定が呼び出し側に押し付けられる | T/A を判定して具体型を投げる。不能なら A |
| 内部 transient を T として外出し | エンドユーザーに「待て」と誤誘導 | 内部で吸収 or A 化 |
| cause を捨てた例外変換 | 根本原因が追跡不能 | 変換時は必ず cause を渡す |
| log & rethrow（rule/command 層） | 同一エラーが二重ログ出力 | ログは converter 層の1か所のみ |

## ❓ 未決事項

新規 issue として起票するにあたり、以下が**論点として明示できているが結論未確定**である。
それぞれ独立に詰める必要がある。

### 4.1 階層別責務（次フェーズ・主要論点）

各層が「何を投げ・何を捕まえ・何を伝播・何を吸収するか」の詳細は本規定の現バージョンでは叩き台のみ。

#### 4.1.1 例外型変換の許容範囲

**問題**: rule で発生した `UserInputException` を command 層が `ExternalTransientException` に変換することは禁止すべきだが、規約だけで縛れるか・型強制で縛るか。`catch (IOException) { throw new UserInputException(...); }` のような広い catch でのラベル付け直しは禁止すべきだが、何が「広い catch」かの線引きが必要。

**確認すべき点**:
- rule から `RuntimeException` が漏れた場合、converter が A 化するのは許されるか・そのまま伝播か
- command が「広い catch」で例外を分類確定するのはどこまで許されるか（自身が直接呼び出す外部ライブラリ例外の具体型 catch のみ許可、等の線引き）
- 通知分類例外（U/T/A）同士の変換は全層で禁止という線で良いか

#### 4.1.2 文脈付加の手段

**問題**: 通知分類型を変えずに文脈情報（処理中レコード位置・command 名・rule 名等）を付加する方法。

選択肢:
- 案A: 同じ型で新例外を生成して元を cause にラップ → 4層貫通で cause が4段になる
- 案B: 元例外の `addSuppressed()` に文脈情報オブジェクトを追加 → JDK 標準慣習を歪める
- 案C: 例外に文脈フィールドを追加しコンストラクタ的に補強 → 例外型 API が膨らむ
- 案D: 例外に伝わる MDC を持たせ、各層が補強 → 例外の不変性を破る

**確認すべき点**:
- cause チェイン深化の許容度（4段なら許容か）
- 「文脈情報」が具体的に何か（コンテキストオブジェクトの設計）

#### 4.1.3 rule のライフサイクル

**問題**: ライブラリが rule インスタンスを**シングルトン的に共有**するか、**リクエストごとに新規生成**するか。外部接続を持つ rule（DB rule 等）はライフサイクルが特に重要で、構築時失敗の発生タイミングが変わる:

- 共有なら起動時に1回だけ発生し、以降の run 中には発生しない
- リクエストごと生成なら毎リクエストの run 以前に発生しうる

**確認すべき点**:
- ライブラリとして共有 vs リクエストごとを選択可能にするか・どちらかに固定するか
- 外部接続 rule の接続オープンタイミングと例外発生タイミングの規約

#### 4.1.4 close 時失敗の扱い

**問題**: 各層がリソースを持つので close 時失敗が発生しうる。run 中の主例外と、close 時失敗（cleanup 例外）の主従関係。

- close 時失敗が単独で発生した場合（run 中は成功・close で失敗）の通知分類
- close 時失敗が複数 command で連鎖した場合の集約方法

**確認すべき点**:
- close 時失敗は run 中の例外と同じく `getAllFailures()` に含めるか・`getSuppressed()` 扱いか
- close 時失敗のみの場合のリスト構造

#### 4.1.5 構築時失敗の集約

**問題**: 構築は順次実行（並行ではない）なので run 中の集約とは別軸。「最初の失敗で中断するか、できる限り構築を進めて複数失敗をまとめて報告するか」の選択肢がある。設定検証では複数失敗の集約が望ましい（ユーザーに一度に全部見せたい）が、依存関係のある構築では中断が必要。

**確認すべき点**:
- 構築時失敗の集約戦略をどう規定するか
- 設定検証と接続オープンを別フェーズとして扱うか

#### 4.1.6 吸収ポリシーの規定強度

**問題**: command の吸収を「ポリシーに基づく明示的吸収のみ」と縛るための仕組み。

選択肢:
- 規定文書のみで運用（`catch (Exception) { /* ignore */ }` 禁止を文書で明記）
- 専用インターフェース（`FailureAbsorbingPolicy` 等）を導入し、これを通さない吸収を禁止する規約
- 型強制まで踏み込まず、PMD/SpotBugs カスタムルールで検出

**確認すべき点**:
- ライブラリレベルで型強制まで踏み込むか・規定のみで運用するか
- 静的解析でどこまで検出可能か

#### 4.1.7 converter 構築失敗の分類

**問題**: 利用者がライブラリ API で converter を組み立てる時の失敗が、U か A か曖昧。利用者が API 呼び出しコードで指定する設定 → 利用者起因だが、入力**データ**ではなく**プログラム引数**。通知3分類では区別が曖昧。

**確認すべき点**:
- API 利用ミス（設定値不正）は A（実装バグ寄り）か U（利用者起因）か
- そもそも API 利用ミスは `IllegalArgumentException` に倒すか、通知分類体系に乗せるか

#### 4.1.8 「吸収された例外数」のメタデータ経路

**問題**: command が rule の例外を業務的に吸収した場合（スキップ・隔離・リトライ）、「100件中5件スキップ」のような情報は業務的に重要。これは例外ではなく**実行結果のメタデータ**として返す方が自然だが、どこに乗せるか。

**確認すべき点**:
- 例外規定の範囲外として別 issue にするか、本シリーズで扱うか
- 「処理結果サマリ」を返す API 設計（パイプライン結果オブジェクト等）

#### 4.1.9 `UserInputException` の `userMessage` サニタイズ強制機構

**問題**: 本規定は `UserInputException` のコンストラクタが受け取る `userMessage` を「呼び出し側の責務でサニタイズ済み」と規定するのみで、強制機構を持たない。`getOperatorContext()` 側では `IncidentCode` enum・値オブジェクト・自由文字列最小化でサニタイズを構造的に強制する方向（[§4.1.10](#4110-incidentcode-カタログ整備とマッピング規約)）に倒したのに対し、U 経路のサニタイズだけが規約レベルに留まる非対称が残る。

選択肢:
- 規定文書のみで運用（現状）
- `UserInputException` コンストラクタに **`UserMessageBuilder`** のような構造化 API を強制し、自由文字列直接渡しを禁止
- 静的解析（PMD/SpotBugs カスタムルール）で `cause.getMessage()` 等の禁止パターンを検出
- 型強制（`SanitizedString` 値オブジェクト）

**確認すべき点**:
- U 経路と A 経路（`getOperatorContext()`）でサニタイズ規定強度を揃えるか・割り切るか
- 構造化 API 強制が利用者の書きやすさを損なわない範囲に収まるか

#### 4.1.10 `IncidentCode` カタログ整備とマッピング規約

**問題**: `getOperatorContext()` のデフォルト最小スキーマに `IncidentCode` enum を据えたが、enum 値カタログをどう設計・運用するかは未確定。

**確認すべき点**:
- 通知3分類（U/T/A）× 発生層（rule/command/converter）の直積で初期カタログを切るか、もっと粗く始めるか
- 利用者が独自に `IncidentCode` を追加できる拡張機構を提供するか
- 各例外型（`UserInputException` 等）とデフォルト `IncidentCode` のマッピングをコンストラクタ強制で結びつけるか

#### 4.1.11 `IRule` のファンクショナルインターフェース性

**前提**: 現状 `IRule` は `@FunctionalInterface`・SAM・throws 句なし（`String apply(String input)`）で定義されている。本規定は rule に対し U/T/A の直接 throw・T/A 判定・ライフサイクル管理・文脈付加・吸収禁止の責務を要求するが、これらは SAM 制約と緊張関係にある。

**確定（案 A 採用）**: `IRule#apply` のシグネチャを `String apply(String input) throws StreamProcessingException` に変更する。`@FunctionalInterface` は throws 句と排他ではなく SAM 性は維持される。throws 句に基底 `StreamProcessingException`（abstract）を指定することで、投げられる例外を U/T/A サブクラスに型機構で限定できる。これにより以下が解決:

- **F-1（throws できない）**: 通知分類例外を直接 throw 可能になり、`UncheckedStreamException` ラッパー経由の必要がなくなる
- **F-2（T/A 判定責務が型で表現できない）**: 外部接続 rule が `ExternalTransientException` / `ExternalPermanentException` を直接投げ分けられる

ラムダ実装は throws 宣言なしで `IRule r = s -> s.trim();` のまま書ける（throws 句は宣言可能だが必須ではない）。移行コストは既存 11 実装の throws 追加と command 層（XmlWalker / JsonWalker 等）の throws 拡張のみ。

**残る未決**: 案 A は F-3〜F-5 を解決しない。以下は別論点として委譲:

- **F-3 ライフサイクル不在**: [§4.1.3](#413-rule-のライフサイクル) で扱う
- **F-4 文脈付加手段**: [§4.1.2](#412-文脈付加の手段) で扱う
- **F-5 吸収禁止の型強制**: [§4.1.6](#416-吸収ポリシーの規定強度) で扱う

**確認すべき点**:
- `JDK Function<String,String>` 互換喪失の許容度（Stream API 連携で `UncheckedStreamException` キャリアが境界で残る可能性）
- 公開 API 破壊的変更の Phase ライン（Phase 3 共通機構実装と同期させるか、先行して切るか）
- 案 A で残る F-3〜F-5 を、案 B（abstract class 格上げ）/ 案 C（軽量 IRule + IConnectedRule 二系統分離）で解決する余地と費用対効果

### 4.2 並行例外集約の残存論点

#### 4.2.1 キャンセル協調プロトコルの規約強度

**問題**: 早期中断ポリシーが機能するためには、各ワーカーが中断要求に応答する必要がある:
- 全ワーカーが `Thread.currentThread().isInterrupted()` を定期チェック
- ブロッキング I/O 中の `InterruptedException` を素直に投げる
- `InterruptedException` を握り潰さず `Thread.interrupt()` で再設定

**確認すべき点**:
- 規定強度（command/rule 実装者への作法規約として明記するか、テスト等で検証可能にするか）
- ブロッキング I/O 中のチェック頻度の指針

### 4.3 通知配送の残存論点

#### 4.3.1 通知分類外の例外をマーカーインターフェースで型表現するか

**問題**: 下流停止通知・キャリア・`InterruptedException` を**通知分類外マーカー**（`InternalControlException` 等）として型で表現するか、規定文書のみで運用するか。

- 型で表現すると converter の境界処理が `catch (InternalControlException e)` で書けて規約が型に表現される
- 一方、型階層が複雑になる。`InterruptedException` は JDK 標準なので実装不可で、規定文書での明示が結局必要になる

**確認すべき点**:
- マーカー導入のメリットが規定文書のみ運用を上回るか
- `InterruptedException` を型機構の外に置く設計の整合性

#### 4.3.2 `AggregatedStreamProcessingException#getUserMessage()` の代表メッセージ

**問題**: `getUserMessage()` を基底 `StreamProcessingException` に置いたため、`AggregatedStreamProcessingException` も継承上 `getUserMessage()` を実装する必要がある。しかし「**用途別代表選択アクセサは作らない・main 層が自由に解釈する**」（[§並行例外集約 §main へのインターフェース](#main-へのインターフェース)）の方針と、集約型が `getUserMessage()` で何らかの代表メッセージを返さざるを得ない実装上の要請とが矛盾する。

選択肢:
- 案A: 集約型の `getUserMessage()` は `UnsupportedOperationException` を投げる（呼び出した時点で API 誤用と型機構で示す）
- 案B: 集約型の `getUserMessage()` は基底の `DEFAULT_USER_MESSAGE`（汎用文言）を返す（呼べるが意味的に何の代表でもないと規約で明示）
- 案C: 集約型では `getUserMessage()` を `@Deprecated` + 別名 `getRepresentativeUserMessage()` を新設し、利用者が代表選択を意識して呼べるようにする
- 案D: 集約型は `failures.get(0).getUserMessage()` を返す（規約として「先頭=代表」を後から明文化）

**確認すべき点**:
- 「代表選択アクセサは作らない」の方針を集約型 `getUserMessage()` にも適用するか、集約型だけ特例とするか
- main 層のサンプルコード（[§main へのインターフェース](#main-へのインターフェース)）で `failures.stream().filter(f -> f instanceof UserInputException).findFirst()...` のように main が代表選択を行うパターンとの整合性

## 🗓️ 適用計画

| Phase | 内容 | 状態 |
|---|---|---|
| **Phase 1** | 本規定文書の策定（通知ベース分類への転換） | 本ドキュメント |
| **Phase 2** | 階層別責務の詳細確定（[未決事項 4.1](#41-階層別責務次フェーズ主要論点) を順次詰める） | 未着手 |
| **Phase 3** | 共通機構の実装 + 単体テスト: 通知分類型新設（`UserInputException` / `ExternalTransientException` / `ExternalPermanentException` / `InternalSystemException`）、`OperatorContext` 機構、メッセージ機構、サニタイズヘルパー、converter 集約機構（`getAllFailures()`） | 未着手 |
| **Phase 4** | 既存コマンドへの適用: POC 実装の規定整合化、既存 issue（#729 / #731 / #740 / #741 / #742 / #748 / #749 / #783 / #784）の規定に沿った再評価・対応 | 未着手 |

各 Phase は別 PR とする。

## 🔗 スコープ外・関連ドキュメント

- **fail-fast / skip / retry の実行時戦略**は本規定のスコープ外（[issue #505](https://github.com/3211133/StreamConverter/issues/505) の論点）
- **過渡期の既存実装**: 本規定は「あるべき姿」を示すもので、POC 実装は本規定に沿って段階的に書き換える対象である。現行 POC の `PipeAbortedException` / `UncheckedStreamException` / `CommandStageRunner` 等の機構は、本規定が示す抽象的な役割（下流停止通知・運搬キャリア・並行集約）の現行実装にすぎず、規定上の役割を満たす形であれば実装は変更可能
- [logging-rules.md](../logging-rules.md): ログ運用ポリシー
- [ARCHITECTURE.md](../ARCHITECTURE.md): パイプラインの実行モデル
- [reference/TESTING.md](TESTING.md): テスト戦略
