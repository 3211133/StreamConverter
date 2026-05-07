package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.path.ITreeMatcher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON Extract Command
 *
 * <p>指定パスにマッチした JSON 値を抽出して出力する。変換は行わない。 変換を行う場合は {@link JsonWalker} を使用すること。
 *
 * <p>特徴: - {@link ITreeMatcher#matches(java.util.List)} による反復マッチで抽出対象を判定 - マッチした値の型・構造をそのまま保持して出力 -
 * Jackson Streaming API による省メモリ処理 - ワイルドカード・ネストパスを含む式をサポート（$[*].field, $.array[*].nested.field）
 *
 * <p>走査は currentPath にオブジェクトフィールド名のみ積む。配列要素はパスに現れない透過的な走査とする。 そのため {@link ITreeMatcher} の実装（{@link
 * com.streamconverter.path.TreePath} 等）は 配列インデックスを含まないセグメントリストと比較すること。
 */
public class JsonExtractCommand implements IStreamCommand {

  private final ITreeMatcher jsonPath;
  private final JsonFactory jsonFactory;

  private JsonExtractCommand(ITreeMatcher jsonPath) {
    this.jsonPath = jsonPath;
    this.jsonFactory = new JsonFactory();
  }

  /**
   * JSON 抽出コマンドを生成する。
   *
   * @param jsonPath 抽出対象を判定する {@link ITreeMatcher}
   * @return JsonExtractCommand インスタンス
   * @throws IllegalArgumentException jsonPath が null の場合
   */
  public static JsonExtractCommand create(ITreeMatcher jsonPath) {
    if (jsonPath == null) {
      throw new IllegalArgumentException("jsonPath cannot be null");
    }
    return new JsonExtractCommand(jsonPath);
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (JsonParser parser = jsonFactory.createParser(inputStream);
        JsonGenerator generator = jsonFactory.createGenerator(outputStream)) {
      generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

      List<String> currentPath = new ArrayList<>();

      // ルートパス（セグメントなし）は入力をそのままコピー
      if (jsonPath.matches(currentPath)) {
        JsonValueCopier.copyValue(parser, generator);
      } else {
        int matched = traverse(parser, generator, currentPath);
        if (matched == 0) {
          generator.writeNull();
        }
      }

      generator.flush();
    }
  }

  /**
   * JSON ストリームを走査し、{@link #jsonPath} にマッチした値を generator に書き出す。
   *
   * @return マッチして出力した値の個数
   */
  private int traverse(JsonParser parser, JsonGenerator generator, List<String> currentPath)
      throws IOException {
    int matched = 0;
    JsonToken token;
    while ((token = parser.nextToken()) != null) {
      matched += processToken(parser, generator, token, currentPath);
    }
    return matched;
  }

  private int traverseObject(JsonParser parser, JsonGenerator generator, List<String> currentPath)
      throws IOException {
    int matched = 0;
    JsonToken token;
    while ((token = parser.nextToken()) != null && token != JsonToken.END_OBJECT) {
      if (token == JsonToken.FIELD_NAME) {
        currentPath.add(parser.currentName());
        token = parser.nextToken();
        if (token != null) {
          matched += processToken(parser, generator, token, currentPath);
        }
        currentPath.remove(currentPath.size() - 1);
      }
    }
    return matched;
  }

  private int traverseArray(JsonParser parser, JsonGenerator generator, List<String> currentPath)
      throws IOException {
    int matched = 0;
    JsonToken token;
    while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
      matched += processToken(parser, generator, token, currentPath);
    }
    return matched;
  }

  private int processToken(
      JsonParser parser, JsonGenerator generator, JsonToken token, List<String> currentPath)
      throws IOException {
    if (jsonPath.matches(currentPath)) {
      JsonValueCopier.copyValue(parser, generator, token);
      return 1;
    }
    return switch (token) {
      case START_OBJECT -> traverseObject(parser, generator, currentPath);
      case START_ARRAY -> traverseArray(parser, generator, currentPath);
      default -> 0;
    };
  }
}
