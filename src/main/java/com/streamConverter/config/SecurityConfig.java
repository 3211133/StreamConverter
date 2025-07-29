package com.streamConverter.config;

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
   * @return CORS設定ソース
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 許可するオリジン（開発環境用設定）
    configuration.addAllowedOrigin("http://localhost:3000");
    configuration.addAllowedOrigin("http://localhost:8080");
    configuration.addAllowedOriginPattern("*"); // 開発環境のみ

    // 許可するHTTPメソッド
    configuration.addAllowedMethod("GET");
    configuration.addAllowedMethod("POST");
    configuration.addAllowedMethod("PUT");
    configuration.addAllowedMethod("DELETE");
    configuration.addAllowedMethod("OPTIONS");

    // 許可するヘッダー
    configuration.addAllowedHeader("*");

    // 認証情報の送信を許可
    configuration.setAllowCredentials(true);

    // プリフライトリクエストのキャッシュ時間
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}
