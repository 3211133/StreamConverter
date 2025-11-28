package com.streamconverter.logging;

import ch.qos.logback.classic.LoggerContext;
import java.lang.reflect.Field;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/**
 * InheritableMDCAdapterを初期化するユーティリティクラス
 *
 * <p>このクラスは、LogbackのMDCAdapterをInheritableMDCAdapterに置き換えます。 SLF4JとLogbackの両方でMDCAdapterの参照を更新するため、
 * リフレクションを使用してSLF4JのMDCAdapter参照を書き換えます。
 *
 * <p><b>使用方法:</b>
 *
 * <pre>{@code
 * // アプリケーション起動時、最初のログ出力より前に実行
 * public class Application {
 *   static {
 *     MDCInitializer.initialize();
 *   }
 *
 *   public static void main(String[] args) {
 *     // この後は通常通りMDC.put()を使うだけで、Virtual Threadにも伝播する
 *     MDC.put("userId", "USER123");
 *     // ...
 *   }
 * }
 * }</pre>
 *
 * <p><b>技術的背景:</b>
 *
 * <p>Logbackのデフォルト実装では、MDCAdapterは通常のThreadLocalを使用するため、 親スレッドのMDC値が子スレッドに伝播しません。
 * InheritableMDCAdapterはInheritableThreadLocalを使用することで、 親スレッドの値を子スレッド（Virtual Thread含む）に自動的に継承します。
 *
 * <p>ただし、`loggerContext.setMDCAdapter()`だけではSLF4Jの`MDC`クラスが 起動時に取得したアダプター参照は変わりません。
 * そのため、リフレクションを使ってSLF4JのMDCAdapter参照も書き換えています。
 *
 * @see InheritableMDCAdapter
 * @since 1.0
 */
public class MDCInitializer {

  private static volatile boolean initialized = false;

  /**
   * InheritableMDCAdapterを初期化します
   *
   * <p>このメソッドは、以下の処理を実行します:
   *
   * <ol>
   *   <li>LoggerContextを取得
   *   <li>InheritableMDCAdapterのインスタンスを作成
   *   <li>LoggerContextのMDCAdapterを置き換え
   *   <li>リフレクションでSLF4JのMDC_ADAPTERフィールドを書き換え
   * </ol>
   *
   * <p><b>重要:</b> このメソッドは、アプリケーション起動時、最初のログ出力より前に呼び出す必要があります。 複数回呼び出しても、2回目以降は何もしません（冪等性保証）。
   *
   * @throws RuntimeException MDCAdapterの初期化に失敗した場合
   */
  public static void initialize() {
    if (initialized) {
      return;
    }

    synchronized (MDCInitializer.class) {
      if (initialized) {
        return;
      }

      try {
        // LoggerContextを取得
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // InheritableMDCAdapterを作成
        InheritableMDCAdapter inheritableMDCAdapter = new InheritableMDCAdapter();

        // LoggerContextのMDCAdapterを置き換え
        loggerContext.setMDCAdapter(inheritableMDCAdapter);

        // SLF4JのMDC_ADAPTERフィールドもリフレクションで書き換え
        replaceSLF4JMDCAdapter(inheritableMDCAdapter);

        initialized = true;

      } catch (Exception e) {
        throw new RuntimeException("Failed to initialize InheritableMDCAdapter", e);
      }
    }
  }

  /**
   * リフレクションを使ってSLF4JのMDC_ADAPTERフィールドを置き換えます
   *
   * @param adapter 新しいMDCAdapter
   * @throws Exception リフレクション操作に失敗した場合
   */
  private static void replaceSLF4JMDCAdapter(MDCAdapter adapter) throws Exception {
    Field mdcAdapterField = MDC.class.getDeclaredField("MDC_ADAPTER");
    mdcAdapterField.setAccessible(true);
    mdcAdapterField.set(null, adapter);
  }

  /**
   * 初期化状態を確認します
   *
   * @return 初期化済みの場合true
   */
  public static boolean isInitialized() {
    return initialized;
  }

  /**
   * Private constructor to prevent instantiation.
   *
   * <p>このクラスは静的ユーティリティクラスであり、インスタンス化を禁止します。
   */
  private MDCInitializer() {
    throw new AssertionError("Utility class should not be instantiated");
  }
}
