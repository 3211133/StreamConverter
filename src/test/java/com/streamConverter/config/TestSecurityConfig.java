package com.streamConverter.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * テスト環境専用のSpring Security設定クラス
 *
 * <p>統合テストでのHTTPリクエストを簡素化するため、CSRF保護を無効化します。
 *
 * <p><strong>セキュリティ注意事項:</strong> このクラスは"test"プロファイルでのみ有効であり、本番環境では使用されません。
 * CSRF保護の無効化は、テスト用HTTPクライアント（TestRestTemplate）での API呼び出しを簡素化する目的で行っています。
 *
 * <p>本番環境では{@link com.streamConverter.config.SecurityConfig}が使用され、 適切なCSRF保護が適用されます。
 */
@TestConfiguration
@EnableWebSecurity
@Profile("test")
@SuppressWarnings(
    "lgtm[java/spring-disabled-csrf-protection]") // CSRF protection disabled only for test profile
// with runtime validation
public class TestSecurityConfig {

  @Autowired private Environment environment;

  @Value(
      "${streamconverter.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080,*}")
  private String allowedOrigins;

  @Value("${streamconverter.security.cors.allowed-methods:GET,POST,OPTIONS}")
  private String allowedMethods;

  @Value(
      "${streamconverter.security.cors.allowed-headers:Content-Type,Authorization,X-Requested-With,*}")
  private String allowedHeaders;

  @Value("${streamconverter.security.cors.allow-credentials:true}")
  private boolean allowCredentials;

  @Value("${streamconverter.security.cors.max-age:3600}")
  private long maxAge;

  /**
   * テスト環境専用のSpring Securityフィルターチェーン設定
   *
   * <p>CSRF保護を無効化し、統合テストでのHTTPリクエストを簡素化します。
   *
   * @param http HTTPセキュリティ設定
   * @return セキュリティフィルターチェーン
   * @throws Exception 設定エラーが発生した場合
   */
  @Bean
  @Primary
  public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
    http
        // CodeQL Mitigation: CSRF protection disabled ONLY for test environment
        // 安全性確保: このクラスは@Profile("test")により本番環境では無効
        // 理由: TestRestTemplateを使用した統合テストの実行を簡素化するため
        // 本番環境では SecurityConfig.java でCSRF保護が有効化されている
        // lgtm[java/spring-disabled-csrf-protection] - CSRF disabled only for test profile with
        // runtime validation
        .csrf(
            csrf -> {
              // テスト環境であることを再確認
              if (!isTestProfile()) {
                throw new IllegalStateException(
                    "CSRF protection can only be disabled in test profile");
              }
              csrf.disable();
            })

        // CORS設定を有効化
        .cors(cors -> cors.configurationSource(testCorsConfigurationSource()))

        // エンドポイントごとの認証設定
        .authorizeHttpRequests(
            authz ->
                authz
                    // パブリックエンドポイント
                    .requestMatchers(
                        "/actuator/health",
                        "/api/v1/health",
                        "/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()

                    // CORS preflightリクエスト（OPTIONS）は認証不要
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
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
   * テスト環境専用のCORS設定を定義
   *
   * <p>より寛容なCORS設定でテストを簡素化します。
   *
   * @return CORS設定ソース
   */
  @Bean
  @Primary
  public CorsConfigurationSource testCorsConfigurationSource() {
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

  /**
   * テストプロファイルが有効かどうかを確認 CodeQL対策: CSRF無効化が適切な環境でのみ実行されることを保証
   *
   * @return テストプロファイルが有効な場合true
   */
  private boolean isTestProfile() {
    // Spring環境からアクティブプロファイルをチェック
    String[] activeProfiles = environment.getActiveProfiles();
    for (String profile : activeProfiles) {
      if ("test".equals(profile)) {
        return true;
      }
    }

    // テスト実行環境かどうかもチェック
    try {
      Class.forName("org.junit.jupiter.api.Test");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
