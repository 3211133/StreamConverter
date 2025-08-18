package com.streamConverter.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ファイルシステム操作の抽象化インターフェース
 *
 * <p>プラットフォーム固有のファイルシステム操作を抽象化し、テスト時にはモック実装を使用可能にする。 実装では、実際のファイルシステムとインメモリファイルシステム（Jimfs）の両方をサポート。
 */
public interface FileSystemAbstraction {

  /**
   * ファイル監視サービスを作成
   *
   * @param watchPath 監視対象パス
   * @return WatchService
   * @throws IOException ファイルシステムエラー
   */
  WatchService createWatchService(Path watchPath) throws IOException;

  /**
   * ファイル変更イベントを監視
   *
   * @param watchService 監視サービス
   * @param timeoutMs タイムアウト（ミリ秒）
   * @return 変更イベントのリスト
   * @throws IOException 監視エラー
   */
  List<WatchEvent<?>> pollFileEvents(WatchService watchService, long timeoutMs) throws IOException;

  /**
   * 非同期でファイル変更を監視
   *
   * @param watchPath 監視対象パス
   * @param eventConsumer イベント処理コンシューマー
   * @return 監視タスクのCompletableFuture
   */
  CompletableFuture<Void> watchFileChangesAsync(
      Path watchPath, FileChangeEventConsumer eventConsumer);

  /**
   * ファイルが存在するかチェック
   *
   * @param path ファイルパス
   * @return 存在する場合true
   */
  boolean exists(Path path);

  /**
   * ディレクトリを作成
   *
   * @param path ディレクトリパス
   * @throws IOException 作成エラー
   */
  void createDirectories(Path path) throws IOException;

  /**
   * ファイルを作成
   *
   * @param path ファイルパス
   * @param content ファイル内容
   * @throws IOException 作成エラー
   */
  void createFile(Path path, String content) throws IOException;

  /**
   * ファイルを削除
   *
   * @param path ファイルパス
   * @throws IOException 削除エラー
   */
  void deleteFile(Path path) throws IOException;

  /**
   * ファイル内容を読み取り
   *
   * @param path ファイルパス
   * @return ファイル内容
   * @throws IOException 読み取りエラー
   */
  String readFile(Path path) throws IOException;

  /**
   * 一時ディレクトリを作成
   *
   * @param prefix プレフィックス
   * @return 一時ディレクトリパス
   * @throws IOException 作成エラー
   */
  Path createTempDirectory(String prefix) throws IOException;

  /**
   * リソースのクリーンアップ
   *
   * @throws IOException クリーンアップエラー
   */
  void cleanup() throws IOException;

  /** ファイル変更イベントのコンシューマー */
  @FunctionalInterface
  interface FileChangeEventConsumer {
    void accept(WatchEvent.Kind<?> kind, Path path) throws IOException;
  }
}
