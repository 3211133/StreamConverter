# StreamConverter プロジェクト JUnit 5 テスト方針

## 1. テスト対象

- StreamConverter クラス
- SampleStreamCommand クラス
- その他の実装コマンドクラス

## 2. テスト方針

### 2.1 単体テスト

各クラスの責務に応じたテストを実施する。

#### StreamConverter クラス
- コンストラクタのテスト（正常系・異常系）
- run メソッドのテスト（正常系・異常系）
- 複数コマンドの連携テスト

#### IStreamCommand 実装クラス
- execute メソッドのテスト（正常系・異常系）
- 特殊条件下での動作テスト

### 2.2 テスト手法

- JUnit 5 の機能を活用（@DisplayName, @ParameterizedTest など）
- 適切なアサーションの使用
- テストデータの準備と検証
- 例外テストの実施

### 2.3 モック/スタブの活用

- 入出力ストリームのモック化
- 依存コンポーネントのモック化（必要に応じて）

## 3. テストケース例

### StreamConverterTest

| テスト名 | 内容 | 期待結果 |
|---------|------|---------|
| コンストラクタ正常系 | 有効なコマンド配列でインスタンス化 | 例外なく生成される |
| コンストラクタ異常系（null） | null コマンドでインスタンス化 | NullPointerException が発生 |
| コンストラクタ異常系（空配列） | 空のコマンド配列でインスタンス化 | IllegalArgumentException が発生 |
| run メソッド正常系 | 有効な入出力ストリームで実行 | 正常に処理が完了し、期待通りの出力が得られる |
| run メソッド異常系 | null 入力ストリームで実行 | NullPointerException が発生 |

### SampleStreamCommandTest

| テスト名 | 内容 | 期待結果 |
|---------|------|---------|
| execute 正常系 | 有効な入出力ストリームで実行 | 入力データが出力ストリームにコピーされる |
| execute 異常系 | null 入力ストリームで実行 | NullPointerException が発生 |

## 4. テスト実行環境

- JUnit 5.9.3
- 必要に応じて Mockito などのモックフレームワークを追加
