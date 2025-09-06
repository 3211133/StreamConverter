package com.streamconverter.context;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.MDC;

/**
 * 実行コンテキストを管理するクラス
 *
 * <p>StreamConverterの実行全体を通じて一意の識別子とコンテキスト情報を提供し、 マルチスレッド環境でのMDC伝播を支援します。
 */
public class ExecutionContext {

  private final String executionId;
  private final Instant startTime;
  private final AtomicInteger commandSequence;
  private final Map<String, String> globalContext;
  private final Map<String, String> userContext;

  // 標準的なコンテキストキー
  /** 実行ID用のMDCキー */
  public static final String EXECUTION_ID_KEY = "executionId";

  /** 開始時刻用のMDCキー */
  public static final String START_TIME_KEY = "startTime";

  /** コマンドシーケンス用のMDCキー */
  public static final String COMMAND_SEQUENCE_KEY = "commandSequence";

  /** スレッド名用のMDCキー */
  public static final String THREAD_NAME_KEY = "threadName";

  /** ステージ用のMDCキー */
  public static final String STAGE_KEY = "stage";

  private ExecutionContext(Builder builder) {
    this.executionId = builder.executionId;
    this.startTime = builder.startTime;
    this.commandSequence = new AtomicInteger(0);
    this.globalContext = Collections.unmodifiableMap(new HashMap<>(builder.globalContext));
    this.userContext = new HashMap<>(builder.userContext);
  }

  /**
   * 新しい実行コンテキストを生成
   *
   * @return 新しいExecutionContextインスタンス
   */
  public static ExecutionContext create() {
    return new Builder().build();
  }

  /**
   * カスタム設定でExecutionContextを作成するためのビルダーを取得
   *
   * @return Builderインスタンス
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * 実行ID（一意識別子）を取得
   *
   * @return 実行ID
   */
  public String getExecutionId() {
    return executionId;
  }

  /**
   * 実行開始時刻を取得
   *
   * @return 開始時刻
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * 次のコマンドシーケンス番号を取得（自動インクリメント）
   *
   * @return シーケンス番号
   */
  public int getNextCommandSequence() {
    return commandSequence.incrementAndGet();
  }

  /**
   * 現在のコマンドシーケンス番号を取得
   *
   * @return 現在のシーケンス番号
   */
  public int getCurrentCommandSequence() {
    return commandSequence.get();
  }

  /**
   * グローバルコンテキスト値を取得
   *
   * @param key キー
   * @return 値（存在しない場合はnull）
   */
  public String getGlobalContext(String key) {
    return globalContext.get(key);
  }

  /**
   * 全てのグローバルコンテキストを取得
   *
   * @return 読み取り専用のコンテキストマップ
   */
  public Map<String, String> getAllGlobalContext() {
    return globalContext;
  }

  /**
   * ユーザーコンテキスト値を取得
   *
   * @param key キー
   * @return 値（存在しない場合はnull）
   */
  public String getUserContext(String key) {
    return userContext.get(key);
  }

  /**
   * ユーザーコンテキスト値を設定
   *
   * @param key キー
   * @param value 値
   */
  public void setUserContext(String key, String value) {
    if (value == null) {
      userContext.remove(key);
    } else {
      userContext.put(key, value);
    }
  }

  /**
   * 全てのユーザーコンテキストを取得
   *
   * @return ユーザーコンテキストマップ
   */
  public Map<String, String> getAllUserContext() {
    return new HashMap<>(userContext);
  }

  /**
   * 現在のコンテキストをMDCに設定
   *
   * <p>このメソッドを呼び出すことで、実行コンテキストの情報が 現在のスレッドのMDCに設定されます。
   */
  public void applyToMDC() {
    // 基本的なコンテキスト情報をMDCに設定
    MDC.put(EXECUTION_ID_KEY, executionId);
    MDC.put(START_TIME_KEY, startTime.toString());
    MDC.put(COMMAND_SEQUENCE_KEY, String.valueOf(getCurrentCommandSequence()));
    MDC.put(THREAD_NAME_KEY, Thread.currentThread().getName());

    // グローバルコンテキストをMDCに設定
    globalContext.forEach(MDC::put);

    // ユーザーコンテキストをMDCに設定
    userContext.forEach(MDC::put);
  }

  /**
   * 特定のステージでのMDC設定
   *
   * @param stageName ステージ名
   */
  public void applyToMDCWithStage(String stageName) {
    applyToMDC();
    MDC.put(STAGE_KEY, stageName);
  }

  /**
   * コンテキストのコピーを作成 ユーザーコンテキストは複製されますが、グローバルコンテキストは共有されます。 コピー時はコマンドシーケンスは0にリセットされます。
   *
   * @return コンテキストのコピー
   */
  public ExecutionContext copy() {
    return new Builder()
        .executionId(this.executionId)
        .startTime(this.startTime)
        .globalContext(this.globalContext)
        .userContext(this.userContext)
        .build();
    // コマンドシーケンスは新しいコピーとして0から開始
  }

  /** ExecutionContext作成用のBuilderクラス */
  public static class Builder {
    private String executionId = generateExecutionId();
    private Instant startTime = Instant.now();
    private Map<String, String> globalContext = new HashMap<>();
    private Map<String, String> userContext = new HashMap<>();

    /** Creates a new builder. */
    public Builder() {}

    /**
     * 実行IDを設定
     *
     * @param executionId 実行ID
     * @return Builderインスタンス
     */
    public Builder executionId(String executionId) {
      this.executionId = Objects.requireNonNull(executionId, "executionId cannot be null");
      return this;
    }

    /**
     * 開始時刻を設定
     *
     * @param startTime 開始時刻
     * @return Builderインスタンス
     */
    public Builder startTime(Instant startTime) {
      this.startTime = Objects.requireNonNull(startTime, "startTime cannot be null");
      return this;
    }

    /**
     * グローバルコンテキスト値を設定
     *
     * @param key キー
     * @param value 値
     * @return Builderインスタンス
     */
    public Builder globalContext(String key, String value) {
      Objects.requireNonNull(key, "key cannot be null");
      if (value == null) {
        this.globalContext.remove(key);
      } else {
        this.globalContext.put(key, value);
      }
      return this;
    }

    /**
     * グローバルコンテキストを一括設定
     *
     * @param context コンテキストマップ
     * @return Builderインスタンス
     */
    public Builder globalContext(Map<String, String> context) {
      if (context != null) {
        this.globalContext.putAll(context);
      }
      return this;
    }

    /**
     * ユーザーコンテキスト値を設定
     *
     * @param key キー
     * @param value 値
     * @return Builderインスタンス
     */
    public Builder userContext(String key, String value) {
      Objects.requireNonNull(key, "key cannot be null");
      if (value == null) {
        this.userContext.remove(key);
      } else {
        this.userContext.put(key, value);
      }
      return this;
    }

    /**
     * ユーザーコンテキストを一括設定
     *
     * @param context コンテキストマップ
     * @return Builderインスタンス
     */
    public Builder userContext(Map<String, String> context) {
      if (context != null) {
        this.userContext.putAll(context);
      }
      return this;
    }

    /**
     * ExecutionContextインスタンスを生成
     *
     * @return ExecutionContextインスタンス
     */
    public ExecutionContext build() {
      return new ExecutionContext(this);
    }

    private static String generateExecutionId() {
      return "EXEC-"
          + UUID.randomUUID().toString().substring(0, 8)
          + "-"
          + System.currentTimeMillis();
    }
  }

  @Override
  public String toString() {
    return String.format(
        "ExecutionContext{executionId='%s', startTime=%s, commandSequence=%d}",
        executionId, startTime, getCurrentCommandSequence());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    ExecutionContext that = (ExecutionContext) obj;
    return Objects.equals(executionId, that.executionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(executionId);
  }
}
