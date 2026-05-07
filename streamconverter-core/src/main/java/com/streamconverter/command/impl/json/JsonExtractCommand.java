package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.path.ITreeMatcher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * JSON Extract Command
 *
 * <p>指定パスにマッチした JSON 値を抽出して出力する。変換は行わない。 変換を行う場合は {@link JsonWalker} を使用すること。
 *
 * <p>特徴: - {@link ITreeMatcher#matches(java.util.List)} による反復マッチで抽出対象を判定 - マッチした値の型・構造をそのまま保持して出力 -
 * Jackson Streaming API による省メモリ処理 - ワイルドカード・ネストパスを含む式をサポート（$[*].field, $.array[*].nested.field）
 *
 * <p><b>注意:</b> 現実装は {@link ITreeMatcher} の {@code toString()} をパス式として解釈する。 {@code matches()}
 * による走査ベースの実装への移行は別途対応予定。
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
    List<PathSegment> segments = JsonPathParser.parse(jsonPath.toString());

    try (JsonParser parser = jsonFactory.createParser(inputStream);
        JsonGenerator generator = jsonFactory.createGenerator(outputStream)) {
      generator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

      if (segments.isEmpty()) {
        JsonValueCopier.copyValue(parser, generator);
      } else {
        JsonPathExtractor.extractPath(parser, generator, segments, 0);
      }

      generator.flush();
    }
  }
}
