package com.streamconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for StreamConverter Web API.
 *
 * <p>This application provides REST endpoints for processing data streams using the existing
 * StreamConverter functionality.
 */
// UseUtilityClass: Spring Boot のエントリポイントは private コンストラクタを持てない。
// SpringApplication.run() がこのクラスを構成クラスとして扱うため、ユーティリティクラス化は不可。
@SuppressWarnings("PMD.UseUtilityClass")
@SpringBootApplication
public class StreamConverterWebApplication {

  /**
   * Main method to start the Spring Boot application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(StreamConverterWebApplication.class, args);
  }
}
