package com.streamConverter.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security設定クラス
 *
 * <p>StreamConverter WebAPIのセキュリティ設定を定義します。 開発環境では基本認証を使用し、本番環境では適切な認証機構に変更することを想定しています。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${streamconverter.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
  private String allowedOrigins;

  @Value("${streamconverter.security.cors.allowed-methods:GET,POST,OPTIONS}")
  private String allowedMethods;

  @Value("${streamconverter.security.cors.allowed-headers:Content-Type,Authorization,X-Requested-With}")
  private String allowedHeaders;

  @Value("${streamconverter.security.cors.allow-credentials:true}")
  private boolean allowCredentials;

  @Value("${streamconverter.security.cors.max-age:3600}")
  private long maxAge;

  /**
   * Spring Securityのフィルターチェーンを設定
   *
   * @param http HTTPセキュリティ設定
   * @return セキュリティフィルターチェーン
   * @throws Exception 設定エラーが発生した場合
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // CSRF無効化（REST APIのため）
        .csrf(AbstractHttpConfigurer::disable)

        // CORS設定を有効化
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // エンドポイントごとの認証設定
        .authorizeHttpRequests(
            authz ->
                authz
                    // パブリックエンドポイント
                    .requestMatchers(
                        "/actuator/health", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()

                    // APIエンドポイントは認証必要
                    .requestMatchers("/api/v1/**")
                    .authenticated()

                    // その他のエンドポイント
                    .anyRequest()
                    .authenticated())

        // 基本認証を使用
        .httpBasic(
            httpBasic -> {
              // デフォルト設定を使用
            });

    return http.build();
  }

  /**
   * CORS設定を定義
   *
   * <p>環境変数から設定を読み取り、セキュアなCORS設定を提供します。
   *
   * @return CORS設定ソース
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 許可するオリジンを環境変数から設定
    List<String> origins = Arrays.asList(allowedOrigins.split(","));
    for (String origin : origins) {
      configuration.addAllowedOrigin(origin.trim());
    }

    // 許可するHTTPメソッドを環境変数から設定
    List<String> methods = Arrays.asList(allowedMethods.split(","));
    for (String method : methods) {
      configuration.addAllowedMethod(method.trim());
    }

    // 許可するヘッダーを環境変数から設定
    List<String> headers = Arrays.asList(allowedHeaders.split(","));
    for (String header : headers) {
      configuration.addAllowedHeader(header.trim());
    }

    // 認証情報の送信を環境変数から設定
    configuration.setAllowCredentials(allowCredentials);

    // プリフライトリクエストのキャッシュ時間を環境変数から設定
    configuration.setMaxAge(maxAge);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}
