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
 * <p>出力形式: 常に {@code {"末端キー名": [値...]}} の形で返す。マッチなしは {@code null}（末端キー名が確定しないため空配列ラッパーを構築できない）。
 * ルートパス（{@code $}）のみ例外で入力をそのままコピーする。
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
        ExtractionState state = new ExtractionState();
        try {
          traverse(parser, generator, currentPath, state);
        } finally {
          state.writeClose(generator);
        }
      }

      generator.flush();
    }
  }

  private void traverse(
      JsonParser parser, JsonGenerator generator, List<String> currentPath, ExtractionState state)
      throws IOException {
    JsonToken token;
    while ((token = parser.nextToken()) != null) {
      processToken(parser, generator, token, currentPath, state);
    }
  }

  private void traverseObject(
      JsonParser parser, JsonGenerator generator, List<String> currentPath, ExtractionState state)
      throws IOException {
    JsonToken token;
    while ((token = parser.nextToken()) != null && token != JsonToken.END_OBJECT) {
      if (token == JsonToken.FIELD_NAME) {
        currentPath.add(parser.currentName());
        try {
          token = parser.nextToken();
          if (token != null) {
            processToken(parser, generator, token, currentPath, state);
          }
        } finally {
          currentPath.remove(currentPath.size() - 1);
        }
      }
    }
  }

  private void traverseArray(
      JsonParser parser, JsonGenerator generator, List<String> currentPath, ExtractionState state)
      throws IOException {
    JsonToken token;
    while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
      processToken(parser, generator, token, currentPath, state);
    }
  }

  private void processToken(
      JsonParser parser,
      JsonGenerator generator,
      JsonToken token,
      List<String> currentPath,
      ExtractionState state)
      throws IOException {
    if (jsonPath.matches(currentPath)) {
      if (currentPath.isEmpty()) {
        throw new IllegalStateException(
            "ITreeMatcher matched empty path inside traverse(); root-path matching must be"
                + " handled before traverse() is called.");
      }
      // 最初のマッチ時にラッパーオブジェクトと配列を開く
      state.writeOpenIfNeeded(generator, currentPath.get(currentPath.size() - 1));
      JsonValueCopier.copyValue(parser, generator, token);
    } else {
      switch (token) {
        case START_OBJECT -> traverseObject(parser, generator, currentPath, state);
        case START_ARRAY -> traverseArray(parser, generator, currentPath, state);
        default -> {
          /* スカラー: マッチしないので読み捨て */
        }
      }
    }
  }

  /** 抽出中のラッパー出力状態を管理する。 */
  private static final class ExtractionState {
    private boolean opened;

    void writeOpenIfNeeded(JsonGenerator generator, String key) throws IOException {
      if (!opened) {
        generator.writeStartObject();
        generator.writeFieldName(key);
        generator.writeStartArray();
        opened = true;
      }
    }

    void writeClose(JsonGenerator generator) throws IOException {
      if (opened) {
        generator.writeEndArray();
        generator.writeEndObject();
      } else {
        // マッチなし: {キー: []} を出力できないためキー名が不明 → 空オブジェクトは不適切
        // マッチがなかった場合はキー名を持つ空配列を出力できないため null を出力する
        generator.writeNull();
      }
    }
  }
}
