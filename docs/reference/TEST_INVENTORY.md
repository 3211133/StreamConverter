# Test Inventory

> **作成日**: 2025-11-02
> **対象ブランチ**: docs/separate-architecture-diagram-roles-396
> **目的**: 全テストファイルの包括的な目録作成

## 概要

このドキュメントはStreamConverterプロジェクトの全テストメソッドの包括的な目録です。各テストメソッドの目的と観測ポイントを記載しています。

**総テストファイル数**: 39ファイル
**主要モジュール**: streamconverter-core, streamconverter-web, streamconverter-tools

---

## streamconverter-core

### AbstractStreamCommandTest
- testConstructor: コンストラクタのテスト
- testExecuteImplementation: execute実装のテスト
- testIStreamCommandImplementation: IStreamCommandインターフェース実装のテスト

### StreamProcessingExceptionTest
- testConstructorWithMessage: メッセージ付きコンストラクタのテスト
- testConstructorWithMessageAndCause: メッセージと原因付きコンストラクタのテスト
- testConstructorWithCause: 原因付きコンストラクタのテスト
- testExceptionIsRuntimeException: RuntimeException継承の確認

### StreamConverterTest
- testConstructorWithValidCommandArray: 配列コンストラクタの正常系テスト
- testConstructorWithValidCommandList: リストコンストラクタの正常系テスト
- testConstructorWithNullCommandArray: null配列での例外テスト
- testConstructorWithNullCommandList: nullリストでの例外テスト
- testConstructorWithEmptyCommandArray: 空配列での例外テスト
- testConstructorWithEmptyCommandList: 空リストでの例外テスト
- testRunWithValidStreams: 正常系のrun実行テスト
- testRunWithNullInputStream: null入力ストリームでの例外テスト
- testRunWithNullOutputStream: null出力ストリームでの例外テスト
- testMultipleCommands: 複数コマンド統合テスト
- testLargeDataMemoryEfficiency: 大容量データのメモリ効率性テスト（Linux環境のみ）

### StreamConverterMDCIntegrationTest
- testAutomaticMDCGeneration: 既存APIでのMDC機能自動有効化テスト
- testCustomExecutionContext: カスタムExecutionContextを使用したテスト
- testMultipleCommandsMDC: 複数コマンドでのMDC機能テスト
- testContextPersistenceInFactory: ファクトリメソッドでのコンテキスト保持テスト

### StreamConverterIntegrationTest
- testMultipleCommandsChain: 複数コマンドチェーンの統合テスト
- testEmptyInputHandling: 空入力の処理テスト
- testLargeDataProcessing: 大量データ処理テスト（10,000行）
- testInvalidHttpUrlHandling: 無効なHTTP URLハンドリングテスト
- testValidHttpUrlCreation: 有効なHTTP URL作成テスト

### CsvNavigateCommandTest
- testCommandCreation: コマンドインスタンス作成テスト
- testBasicCsvProcessing: 基本的なCSV処理機能テスト
- testEmptyInput: 空入力の処理テスト
- testLargeInput: 大量入力の処理テスト（1000レコード）
- testStreamingCsvNavigationBehavior: CSVナビゲーションのストリーミング動作検証
- testIncrementalCsvNavigationProcessing: CSV増分処理の検証（200レコード、複雑なデータ構造）

### CsvValidateCommandTest
- testConstructorWithValidRequiredColumns: 有効な必須カラム指定でのコンストラクタテスト
- testConstructorWithNullRequiredColumns: null必須カラムでの例外テスト
- testConstructorWithEmptyRequiredColumns: 空必須カラム配列でのコンストラクタテスト
- testConstructorWithHasHeaderParameter: hasHeaderパラメータ付きコンストラクタテスト
- testValidCsvWithRequiredColumnsSuccess: 必須カラム検証成功テスト
- testCsvMissingRequiredColumnsFailure: 必須カラム欠落時の検証失敗テスト
- testCsvWithDuplicateHeadersFailure: 重複ヘッダー検出時の検証失敗テスト
- testCsvWithInconsistentRowLengthFailure: 不整合な行長検出時の検証失敗テスト
- testEmptyCsvInputFailure: 空CSV入力での検証失敗テスト
- testCsvWithOnlyHeaderFailure: ヘッダーのみCSVでの検証失敗テスト
- testCsvWithoutHeaderValidation: ヘッダーなしCSV検証テスト
- testLargeCsvValidation: 大量CSV検証テスト（1000レコード）
- testCsvWithSpecialCharactersValidation: 特殊文字を含むCSV検証テスト（多言語、クォート）
- testNullInputStream: null入力ストリームでの例外テスト
- testCsvWithQuotedFieldsContainingNewlines: 改行を含むクォートフィールドの検証テスト
- testCsvWithEmptyFieldsValidation: 空フィールドを含むCSV検証テスト
- testMalformedCsvWithUnclosedQuotes: クォート閉じていない不正CSVでの検証失敗テスト
- testCsvValidationWithNoRequiredColumns: 必須カラムなしでの検証テスト
- testStreamingValidationBehavior: ストリーミング検証動作の検証（50レコード）
- testIncrementalCsvValidation: 増分CSV検証の検証（100レコード）

### JsonNavigateCommandTest
- testCommandCreation: コマンドインスタンス作成テスト
- testBasicJsonProcessing: 基本的なJSON処理機能テスト
- testComplexJsonProcessing: 複雑なネスト構造JSON処理テスト
- testEmptyInput: 空入力の処理テスト
- testInvalidJsonInput: 無効なJSON入力テスト（無効化されているテスト）
- testStreamingJsonNavigationBehavior: JSONナビゲーションのストリーミング動作検証（100オブジェクト）
- testIncrementalJsonNavigationProcessing: JSON増分処理の検証（50部門、ネスト構造）

### JsonValidateCommandTest
- testConstructorWithValidSchemaPath: 有効なスキーマパスでのコンストラクタテスト
- testConstructorWithNullSchemaPath: nullスキーマパスでの例外テスト
- testConstructorWithEmptySchemaPath: 空スキーマパスでの例外テスト
- testConstructorWithWhitespaceSchemaPath: 空白のみスキーマパスでの例外テスト
- testValidJsonValidationSuccess: 有効なJSON検証成功テスト
- testInvalidJsonValidationFailure: 無効なJSON検証失敗テスト（複数の検証エラー）
- testMissingRequiredFieldsValidationFailure: 必須フィールド欠落時の検証失敗テスト
- testMalformedJsonInput: 不正な形式のJSON入力での検証失敗テスト
- testEmptyJsonInput: 空JSON入力での検証失敗テスト
- testNonExistentSchemaFile: 存在しないスキーマファイルでの検証失敗テスト
- testInvalidSchemaFile: 無効なスキーマファイルでの警告テスト
- testLargeJsonDocumentValidation: 大量JSONドキュメント検証テスト
- testNullInputStream: null入力ストリームでの例外テスト
- testComplexNestedJsonValidation: 複雑なネスト構造JSON検証テスト
- testJsonWithSpecialCharactersValidation: 特殊文字を含むJSON検証テスト（多言語）
- testStreamingJsonValidationBehavior: ストリーミングJSON検証動作の検証
- testIncrementalJsonValidation: 増分JSON検証の検証（複雑なスキーマ）

### JsonStreamingValidateCommandTest
- testFactoryWithValidSchemaPath: 有効なスキーマパスでのファクトリメソッドテスト
- testFactoryWithNullSchemaPath: nullスキーマパスでのファクトリメソッド例外テスト
- testFactoryWithEmptySchemaPath: 空スキーマパスでのファクトリメソッド例外テスト
- testFactoryWithWhitespaceSchemaPath: 空白のみスキーマパスでのファクトリメソッド例外テスト
- testFactoryWithNullSchemaRegistry: nullスキーマレジストリでのファクトリメソッド例外テスト
- testFactoryWithCustomSchemaRegistry: カスタムスキーマレジストリでのファクトリメソッドテスト

### XmlNavigateCommandTest
- testCommandCreation: コマンドインスタンス作成テスト
- testBasicXmlProcessing: 基本的なXML処理機能テスト
- testComplexXmlProcessing: 複雑なネスト構造XML処理テスト
- testEmptyInput: 空入力での例外テスト
- testInvalidXmlInput: 無効なXML入力での例外テスト

### ValidateTest (XML)
- testConstructor: コンストラクタのテスト
- testExecuteWithValidXml: 有効なXMLでのexecuteテスト（スキーマ検証成功）
- testExecuteWithInvalidXml: 無効なXMLでのexecuteテスト（スキーマ検証失敗）
- testExecuteWithNullInputStream: null入力ストリームでの例外テスト
- testExecuteWithNullOutputStream: null出力ストリームでの例外テスト
- testExecuteWithNonExistentSchemaFile: 存在しないスキーマファイルでの例外テスト
- testExecuteWithEmptyInputStream: 空入力ストリームでの例外テスト
- testStreamingXmlValidationBehavior: ストリーミングXML検証動作の検証（50要素）
- testIncrementalXmlValidationProcessing: 増分XML検証処理の検証（100要素、15KB以上）

### ConvertCommandTest (XML)
- testBasicXmlConversion: 基本的なXML変換テスト（単純な要素変換）
- testNestedElementConversion: ネスト要素の変換テスト
- testConstructorValidation: コンストラクタパラメータの検証テスト
- testEmptyXmlHandling: 空XML処理テスト
- testInvalidXmlHandling: 無効なXML処理での例外テスト
- testLargeXmlProcessing: 大量XML処理テスト（1000要素）
- testStreamingXmlConversionBehavior: ストリーミングXML変換動作の検証（100プロダクト）
- testIncrementalXmlConversionProcessing: 増分XML変換処理の検証（150ブック、15KB以上）

### ConvertTest (CharCode)
- testConstructor: コンストラクタのテスト
- testExecuteWithValidCharsets: 有効な文字コードでのexecuteテスト（パラメータ化テスト）
- testExecuteWithNullInputStream: null入力ストリームでの例外テスト
- testExecuteWithNullOutputStream: null出力ストリームでの例外テスト
- testExecuteWithInvalidInputCharset: 無効な入力文字コードでの例外テスト
- testExecuteWithInvalidOutputCharset: 無効な出力文字コードでの例外テスト
- testExecuteWithEmptyInputStream: 空入力ストリームでのexecuteテスト
- testJapaneseCharacterConversion: 日本語文字変換テスト
- testStreamingCharacterConversionBehavior: ストリーミング文字変換動作の検証（多言語、100行）
- testIncrementalCharacterConversionProcessing: 増分文字変換処理の検証（多言語、200エントリ、15KB以上）

### SampleStreamCommandTest
- testDefaultConstructor: デフォルトコンストラクタのテスト
- testConstructorWithArgument: 引数付きコンストラクタのテスト（パラメータ化テスト）
- testExecuteWithValidStreams: 有効なストリームでのexecuteテスト
- testExecuteWithNullInputStream: null入力ストリームでの例外テスト
- testExecuteWithNullOutputStream: null出力ストリームでの例外テスト
- testExecuteWithEmptyInputStream: 空入力ストリームでのexecuteテスト
- testToString: toStringメソッド出力検証テスト
- testStreamingSampleProcessingBehavior: ストリーミングサンプル処理動作の検証（100行）
- testIncrementalSampleStreamProcessing: 増分サンプルストリーム処理の検証（200行、1KB以上）

### FilterCommandBasicTest
- testJsonFilterCommand_SimpleProperty: JSON単純プロパティフィルタテスト
- testJsonFilterCommand_RootPath: JSONルートパスフィルタテスト
- testCsvFilterCommand_SingleColumn: CSV単一カラムフィルタテスト
- testCsvFilterCommand_MultipleColumns: CSV複数カラムフィルタテスト
- testXmlFilterCommand_SimpleElement: XML単純要素フィルタテスト
- testJsonFilterCommand_NonExistentProperty: JSON存在しないプロパティフィルタテスト
- testCsvFilterCommand_NumericIndex: CSV数値インデックスフィルタテスト
- testStreamingJsonFilterBehavior: ストリーミングJSONフィルタ動作の検証（100オブジェクト）
- testStreamingCsvFilterBehavior: ストリーミングCSVフィルタ動作の検証（200レコード）
- testStreamingXmlFilterBehavior: ストリーミングXMLフィルタ動作の検証（150レコード）
- testIncrementalFilterProcessing: 増分フィルタ処理の検証（JSON、300ユーザー、20KB以上）

### SendHttpCommandTest
- testValidHttpsUrlCreation: 有効なHTTPS URL作成テスト
- testValidHttpUrlCreation: 有効なHTTP URL作成テスト
- testNullUrl: null URLでの例外テスト
- testEmptyUrl: 空URLでの例外テスト
- testInvalidProtocol: 無効なプロトコルでの例外テスト（FTP、fileなど）
- testNoScheme: スキームなしURLでの例外テスト
- testLocalhostBlocked: localhostアクセスブロックテスト
- testPrivateIpBlocked: プライベートIPアクセスブロックテスト（192.168.x.x、10.x.x.x、172.16-31.x.x）
- testInvalidUrlFormat: 無効なURL形式での例外テスト
- testActualHttpRequest: httpbin.orgを使った実際のHTTP通信テスト（ネットワークテスト）
- testNullInputStream: null入力ストリームでの例外テスト
- testNullOutputStream: null出力ストリームでの例外テスト
- testNonExistentHost: 存在しないホストでの例外テスト（ネットワークテスト）
- testLargeDataStreamingProcessing: 大容量データストリーミング処理テスト（20MB、ネットワークテスト）
- testMemoryEfficientStreamingProcessing: メモリ効率的ストリーミング処理テスト（1MB、ネットワークテスト）
- testStreamingBlockingBehavior: ストリーミングブロッキング動作テスト（5MB、ネットワークテスト）
- testInputStreamCloseTimingVerification: InputStreamクローズタイミング検証テスト（並列処理検証、ネットワークテスト）

### SendHttpCommandSecurityTest
- testLocalhostValidation: localhost URL検証テスト
- testLoopbackValidation: 127.0.0.1 URL検証テスト
- testPrivateIpValidation: プライベートIP URL検証テスト
- testValidExternalUrl: 有効な外部URL検証テスト

### LineEndingNormalizeCommandTest
- testUnixToWindows: Unix→Windows改行コード変換テスト
- testWindowsToUnix: Windows→Unix改行コード変換テスト
- testMixedToUnix: 混合→Unix改行コード変換テスト
- testToMacClassic: Mac Classic改行コード変換テスト
- testSystemDefault: システムデフォルト改行コード使用テスト
- testEmptyInput: 空入力処理テスト
- testSingleLineWithoutEnding: 改行なし単一行処理テスト
- testSingleLineWithEnding: 改行付き単一行処理テスト
- testPreserveInput: 入力保持テスト（簡易実装）
- testConstructorNullValidation: コンストラクタnull検証テスト
- testGetCommandDetails: コマンド詳細取得テスト
- testLargeInput: 大量入力処理テスト（1000行）
- testBufferBoundaryLineEndings: バッファ境界改行処理テスト（8192バイト境界）
- testCRLFSpanningBufferBoundary: バッファ境界をまたぐCRLF処理テスト
- testMixedLineEndingsNearBoundary: バッファ境界付近の混合改行処理テスト
- testStreamingBehavior: ストリーミング動作検証（5行）
- testIncrementalProcessing: 増分処理検証（100行、1KB以上）

### CrossPlatformTest
- testLineSeparatorIsPlatformAppropriate: プラットフォーム適切な改行コードテスト
- testCreateTestDataUsesCorrectLineSeparators: テストデータ作成の改行コード使用テスト
- testLineEndingNormalization: 改行コード正規化テスト（Windows、Mac、Unix、混合）
- testAssertEqualsIgnoreLineEndings: 改行コード無視等価性テスト
- testCsvDataCreation: CSVデータ作成テスト
- testNullHandlingInNormalization: 正規化でのnull処理テスト

### SecureXPathValidatorTest
- testValidXPathExpressions: 正常なXPath式検証テスト
- testXPathInjectionDetection: XPathインジェクション攻撃検出テスト（パラメータ化テスト）
- testDangerousFunctionDetection: 危険な関数検出テスト（document、unparsed-text、sql:execute等、パラメータ化テスト）
- testExternalReferenceDetection: 外部参照検出テスト（http、https、file、ftp等、パラメータ化テスト）
- testSqlLikeInjectionDetection: SQL類似インジェクション検出テスト（UNION、INSERT、DROP等、パラメータ化テスト）
- testXPathSanitization: XPath式サニタイズテスト
- testNullInput: null入力処理テスト
- testAllowedXPathFunctions: 許可されたXPath関数リスト取得テスト
- testLongXPathInStrictMode: 長いXPath式の厳格モードテスト（1000文字以上）
- testDeepNestingInStrictMode: 深いネストの厳格モードテスト（15レベル以上）
- testExcessiveWildcardsInStrictMode: 過度なワイルドカードの厳格モードテスト

### TreePathTest
- testJsonPathParsing: JSONパス解析テスト
- testXmlPathParsing: XMLパス解析テスト
- testSingleSegmentPath: 単一セグメントパステスト
- testRootJsonPath: ルートJSONパステスト
- testRootXmlPath: ルートXMLパステスト
- testPathMatching: パスマッチングテスト
- testNullHandling: null処理テスト
- testEqualsAndHashCode: 等価性とハッシュコードテスト
- testValidationErrors: 検証エラーテスト
- testComplexPaths: 複雑なパステスト（スラッシュ正規化）
- testToString: toString実装テスト
- testFromJsonPath: JSONパス生成テスト
- testFromXmlPath: XMLパス生成テスト
- testFactoryMethodsEquivalent: ファクトリメソッド同等性テスト

### ExecutionContextTest
- testCreateBasicContext: 基本コンテキスト作成テスト
- testBuilderWithCustomValues: カスタム値でのビルダーテスト
- testCommandSequenceIncrement: コマンドシーケンス増分テスト
- testGlobalContextImmutability: グローバルコンテキスト不変性テスト
- testUserContextMutability: ユーザーコンテキスト可変性テスト
- testApplyToMDC: MDC適用テスト
- testApplyToMDCWithStage: ステージ付きMDC適用テスト
- testContextCopy: コンテキストコピーテスト
- testBuilderWithNullValues: null値でのビルダー例外テスト
- testBuilderWithNullContextMaps: nullコンテキストマップでのビルダーテスト
- testEqualityAndHashCode: 等価性とハッシュコードテスト
- testToString: toString実装テスト
- testMDCContextPreservation: MDCコンテキスト保持テスト

### ValidationResultTest
- testCreateSuccessfulValidationResult: 成功検証結果作成テスト
- testCreateFailedValidationResult: 失敗検証結果作成テスト
- testBuilderPatternBasicFunctionality: ビルダーパターン基本機能テスト
- testBuilderWithDefaultValues: デフォルト値でのビルダーテスト
- testValidationResultImmutability: 検証結果不変性テスト
- testErrorAndWarningListImmutability: エラーと警告リスト不変性テスト
- testBuilderValidationNullValidationType: null検証タイプでのビルダー例外テスト
- testBuilderValidationEmptyValidationType: 空検証タイプでのビルダー例外テスト
- testBuilderValidationNullSchemaPath: nullスキーマパスでのビルダー例外テスト
- testBuilderValidationSuccessWithErrors: エラー付き成功でのビルダー例外テスト
- testBuilderMultipleErrorsAccumulation: 複数エラー蓄積テスト
- testBuilderMultipleWarningsAccumulation: 複数警告蓄積テスト
- testToStringMethod: toStringメソッドテスト
- testSuccessFactoryMethodWithAllParameters: 全パラメータ付き成功ファクトリメソッドテスト
- testFailureFactoryMethodWithAllParameters: 全パラメータ付き失敗ファクトリメソッドテスト
- testValidationResultWithExecutionTimeBounds: 実行時間境界での検証結果テスト
- testValidationResultEqualityAndHashCode: 検証結果等価性とハッシュコードテスト

### DatabaseFetchRuleTest
- testApplyWithSingleResult: 単一結果取得テスト（モック使用）
- testApplyWithEmptyResult: 空結果取得テスト（モック使用）
- testApplyWithMultipleColumns: 複数列結果取得テスト（モック使用）
- testApplyWithMultipleRows: 複数行結果取得テスト（モック使用）
- testApplyWithSQLException: SQLエラー発生時のテスト（モック使用）
- testApplyWithNullResult: NULL値結果取得テスト（モック使用）

### DatabaseFetchRuleIntegrationTest
- testBasicDataFetch: 基本的なデータ取得テスト（H2インメモリDB使用）
- testNonExistentId: 存在しないID検索テスト
- testNullValueFetch: NULL値を含むデータ取得テスト
- testMultipleColumns: 複数列クエリテスト
- testMultipleRows: 複数行クエリテスト
- testNumericDataFetch: 数値データ取得テスト
- testParameterlessQuery: パラメータなしクエリテスト
- testSQLInjectionPrevention: SQLインジェクション防止テスト
- testInvalidDatabaseUrl: 不正なデータベースURL拒否テスト
- testInputSanitization: 入力サニタイズテスト
- testLargeDatasetPerformance: 大規模データセットパフォーマンステスト（1000レコード、100クエリ、5秒以内）
- testConnectionErrorHandling: 接続エラーハンドリングテスト

### PooledDatabaseFetchRuleIntegrationTest
- testBasicPooledDataFetch: 基本的なプール対応データ取得テスト（HikariCP使用）
- testPoolStats: プール統計情報取得テスト
- testPooledPerformance: プール対応パフォーマンステスト（500クエリ、3秒以内）
- testConcurrentAccess: 並行アクセステスト（10スレッド、50クエリ/スレッド）
- testPoolExhaustion: プール枯渇時の動作テスト（最大2接続、5タスク）
- testPoolVsNonPoolPerformance: プールvs非プールパフォーマンス比較テスト（100クエリ）
- testPoolShutdownBehavior: プールシャットダウン後の動作テスト

### TransformationRuleIntegrationTest
- testCamelToSnakeCaseWithJsonNavigate: CamelからSnake caseへの変換とJSONナビゲート統合テスト
- testSnakeToCamelCaseWithJsonNavigate: SnakeからCamel caseへの変換とJSONナビゲート統合テスト
- testPascalCaseConversion: Pascal case変換テスト
- testChainRuleWithJsonNavigate: チェーンルールとJSONナビゲート統合テスト
- testComplexChainTransformation: 複雑なチェーン変換テスト
- testRoundTripConversion: 往復変換テスト
- testMultipleFieldProcessing: 複数フィールド処理テスト
- testErrorHandling: エラーハンドリングテスト（null値）
- testNonExistentField: 存在しないフィールドテスト

### ChainRuleTest
- testBasicChaining: 基本的なチェーン処理テスト
- testBuilderPattern: ビルダーパターンテスト
- testOfMethods: ofメソッドテスト（可変長引数、リスト）
- testSingleRule: 単一ルールテスト
- testNullHandling: null処理テスト
- testEmptyChainThrowsException: 空チェーンでの例外テスト
- testBuilderOperations: ビルダー操作テスト（addRules、insertRule、clear）
- testComplexTransformation: 複雑な変換テスト
- testGetRules: ルール取得テスト
- testToString: toString実装テスト
- testEqualsAndHashCode: 等価性とハッシュコードテスト
- testCustomRule: カスタムルールテスト

### CamelToSnakeCaseRuleTest
- testDefaultConfiguration: デフォルト設定でのCamel→Snake変換基本動作検証
- testBasicConversions: 11ケースのパラメータ化入力で変換結果を総合検証
- testNullAndEmptyInputs: null／空文字入力時の挙動確認
- testPreserveUnderscores: アンダースコア保持オプション有効時の動作検証
- testCleanUpUnderscores: アンダースコア整理オプション有効時の変換確認
- testAcronymHandling: 略語ハンドリング有効時の大文字連続処理検証
- testNoAcronymHandling: 略語ハンドリング無効時の変換結果確認
- testBuilderConfiguration: ビルダー設定の組み合わせ適用確認
- testToString: toString出力に設定内容が含まれることの検証
- testEdgeCases: 数値・特殊文字・既存snake_case入力の境界検証

### DatabaseRuleIntegrationTest
- testJsonUserIdToNameConversion: JSON内ユーザーIDをDB値へ置換する統合動作検証
- testCsvProductCodeToNameConversion: CSV商品コードをDBの商品名へ置換する統合テスト
- testNonExistentIdHandling: 存在しないID時に空文字へ置換される失敗パス検証
- testMultipleRecordBatchProcessing: 複数レコード配列をバッチ処理し部署名へ変換する検証
- testNumericDataProcessing: 数値列（価格）取得時の置換処理と残存値確認

---

## streamconverter-web

### StreamProcessingControllerTest
- testHealthEndpoint: /api/v1/stream/healthの疎通とステータスメッセージ確認
- testCsvExtractionEndpoint: CSV抽出APIが列保持したまま結果を返すことの検証
- testJsonExtractionEndpoint: JSON抽出APIがJSON構造を保ちつつレスポンス返却する確認
- testPipelineProcessingEndpoint: パイプライン構成ヘッダー指定時のストリーム処理整合性検証
- testInvalidPipelineConfiguration: 不正パイプライン指定で5xxエラーとなることの確認

---

## streamconverter-tools

### BenchmarkInfrastructureTest
- testResourceMonitorBasicOperation: ResourceMonitorの開始・終了計測と使用量取得検証
- testXmlDataGeneration: XML大規模データ生成のサイズ・構造・タグ整合性チェック
- testJsonDataGeneration: JSON大規模データ生成のサイズ変動とorders配列存在確認
- testCsvDataGeneration: CSV生成時のサイズ範囲とヘッダー・行数検証
- testResourceUsage5GB50MBTarget: ResourceUsageの目標判定ロジック（5GB/50MB）評価
- testIntegratedSmallDataProcessing: 監視＋コピー処理統合フローのリソース計測確認
- testPerformanceAnalyzerBasicFunctionality: PerformanceAnalyzer記録・レポート生成検証
- testMultipleRecordsAndStatistics: 複数記録時の統計値とレポート内容検証
- testSmallScaleBenchmark: 2段構成パイプラインの小規模メモリ使用量検証
- testBenchmarkConsistency: （Disabled）同一ベンチを複数回実行した揺らぎ評価

### LargeDataGeneratorDebugTest
- debugXmlSizeDiscrepancy: XML生成のファイル／ストリーム差異と構造解析ログ出力
- debugJsonSizeDiscrepancy: JSON生成のサイズ乖離調査と構造・整形式検証
- debugCsvSizeDiscrepancy: CSV生成のサイズ差分計測とレコード構造解析

### MemoryEfficiencyQuickTest
- testCharacterConversionMemoryEfficiency: 文字コード変換パイプラインの環境適応型メモリ制約検証（Linux限定）

### QuickSmokeTest
- testBasicPipelineOperation: 小規模データでの単段パイプライン実行結果確認
- testMultiStagePipeline: 3段パイプラインの実行成否と出力有無検証
- testErrorHandling: エラールートでも例外発生しない実行確認

### MemoryEfficiencyTest
- testAdaptiveMemoryEfficiency: 3段パイプライン大容量処理でのメモリ50%制約検証（Linux）
- testSingleCommandOptimalPath: 単一コマンド時のメモリ20%制約検証（Linux）
- testParallelProcessingNoStack: 大容量並列処理での30%制約とスループット測定（Linux）

### PmdConverterTest
- main（手動テスト）: PMD XMLレポートをMarkdown/CSV/JSONへ変換し各出力結果をログ確認

### DatabaseInspectorTest
- testInspectorInitialization: DatabaseInspector初期化処理が例外なく完了することの確認

---

## テストカバレッジの特徴

### 網羅的にテストされている機能
- ✅ コアStreamConverter機能（コンストラクタ、実行、エラーハンドリング）
- ✅ ストリーミング動作検証（CSV、JSON、XML、文字コード変換）
- ✅ 検証機能（CSV、JSON、XML）
- ✅ データベースルール（モック、統合、プール）
- ✅ セキュリティ検証（XPath、HTTP URL、SQLインジェクション防止）
- ✅ 変換ルール（ケース変換、チェーンルール）
- ✅ パス処理（TreePath、クロスプラットフォーム）
- ✅ 実行コンテキスト（MDC、ビルダー、不変性）

### テストパターン
1. **正常系テスト**: 基本機能の動作確認
2. **異常系テスト**: null入力、無効な入力、エラーハンドリング
3. **パフォーマンステスト**: 大量データ、メモリ効率性
4. **ストリーミング動作テスト**: 増分処理、バッファ境界
5. **統合テスト**: データベース、HTTP通信、複数コマンドチェーン
6. **セキュリティテスト**: インジェクション防止、URL検証

### 注意事項
- ネットワークテストは`skipNetworkTests=true`で無効化可能
- 一部のテストは特定OS環境（Linux）でのみ実行
- 大量データテストはプラットフォームに応じた閾値調整あり
- モックを使用したユニットテストと実際のDBを使用した統合テストの両方を実装

---

**更新履歴**:
- 2025-11-02: 初期作成（docs/separate-architecture-diagram-roles-396ブランチ）
