package com.streamconverter.context;

/**
 * ThreadLocalでExecutionContextを保持するホルダークラス
 *
 * <p>このクラスは、現在のスレッドに関連付けられたExecutionContextを管理します。 ログ出力時にLogback
 * TurboFilterがこのホルダーから値を取得してMDCに設定します。
 *
 * <p><b>使用例:</b>
 *
 * <pre>{@code
 * ExecutionContext context = ExecutionContext.create();
 * ExecutionContextHolder.set(context);
 * try {
 *   // ログ出力時に自動的にMDCに値が設定される
 *   log.info("Processing...");
 * } finally {
 *   ExecutionContextHolder.clear();
 * }
 * }</pre>
 *
 * <p><b>スレッドセーフ性:</b> ThreadLocalを使用しているため、スレッドごとに独立したコンテキストが管理されます。
 */
public class ExecutionContextHolder {

  private static final ThreadLocal<ExecutionContext> holder = new ThreadLocal<>();

  /**
   * 現在のスレッドにExecutionContextを設定します
   *
   * @param context 設定するExecutionContext
   */
  public static void set(ExecutionContext context) {
    holder.set(context);
  }

  /**
   * 現在のスレッドのExecutionContextを取得します
   *
   * @return 現在のスレッドのExecutionContext、設定されていない場合はnull
   */
  public static ExecutionContext get() {
    return holder.get();
  }

  /**
   * 現在のスレッドのExecutionContextをクリアします
   *
   * <p>メモリリークを防ぐため、スレッドの処理完了時に必ず呼び出してください。
   */
  public static void clear() {
    holder.remove();
  }

  /** Private constructor to prevent instantiation. */
  private ExecutionContextHolder() {
    // Utility class - no instantiation allowed
  }
}
