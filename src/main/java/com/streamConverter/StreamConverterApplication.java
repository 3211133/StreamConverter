package com.streamConverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StreamConverter WebAPI アプリケーションのメインクラス
 *
 * <p>Spring Bootを使用してStreamConverterをWebAPIとして起動します。 以下のエンドポイントが提供されます：
 *
 * <ul>
 *   <li>/api/v1/transform - メイン変換API
 *   <li>/api/v1/validate - バリデーション専用API
 *   <li>/actuator/health - ヘルスチェック
 *   <li>/swagger-ui.html - API仕様書
 * </ul>
 */
@SpringBootApplication
public class StreamConverterApplication {

  /**
   * アプリケーションのメインエントリーポイント
   *
   * @param args コマンドライン引数
   */
  public static void main(String[] args) {
    SpringApplication.run(StreamConverterApplication.class, args);
  }
}
