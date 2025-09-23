# StreamConverter クイックスタート

このガイドでは、StreamConverterの基本的な使用方法を説明します。

## 前提条件

- Java 21 以上
- Gradle 8.0 以上（プロジェクトに含まれています）

## 基本的な使用例

### 1. シンプルなCSVデータ抽出

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;

// CSVファイルから特定の列を抽出
IStreamCommand[] pipeline = {
    new CsvNavigateCommand("name")  // "name"列を抽出
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

### 2. JSON データの変換

```java
import com.streamconverter.command.impl.json.JsonNavigateCommand;

// JSONから特定のパスの値を抽出
IStreamCommand[] pipeline = {
    new JsonNavigateCommand("$.user.email")  // JSONPathで値を抽出
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

詳細な使用方法については [README.md](../../README.md) を参照してください。