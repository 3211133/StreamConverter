package com.streamConverter.mock;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jimfsを使用したモックファイルシステム実装
 *
 * <p>インメモリファイルシステムを使用してプラットフォーム非依存のファイル操作テストを実現。 ファイル監視機能のモック実装も含む。
 */
public class MockFileSystemAbstraction implements FileSystemAbstraction {

  private static final Logger logger = LoggerFactory.getLogger(MockFileSystemAbstraction.class);
  private final FileSystem fileSystem;
  private final ScheduledExecutorService scheduler;
  private final ConcurrentHashMap<Path, MockWatchService> watchServices;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);

  /** デフォルトコンストラクタ（Unix風ファイルシステム） */
  public MockFileSystemAbstraction() {
    this(Configuration.unix());
  }

  /**
   * 指定設定でモックファイルシステムを作成
   *
   * @param configuration Jimfs設定
   */
  public MockFileSystemAbstraction(Configuration configuration) {
    this.fileSystem = Jimfs.newFileSystem(configuration);
    this.scheduler = new ScheduledThreadPoolExecutor(2);
    this.watchServices = new ConcurrentHashMap<>();
    logger.info("MockFileSystemAbstraction initialized with configuration: {}", configuration);
  }

  @Override
  public WatchService createWatchService(Path watchPath) throws IOException {
    if (isClosed.get()) {
      throw new IOException("FileSystem is closed");
    }

    // Jimfsパス形式に変換
    Path jimfsPath = convertToJimfsPath(watchPath);

    // ディレクトリが存在しない場合は作成
    if (!Files.exists(jimfsPath)) {
      Files.createDirectories(jimfsPath);
    }

    MockWatchService mockWatchService = new MockWatchService(jimfsPath);
    watchServices.put(jimfsPath, mockWatchService);

    logger.debug("Created MockWatchService for path: {}", jimfsPath);
    return mockWatchService;
  }

  @Override
  public List<WatchEvent<?>> pollFileEvents(WatchService watchService, long timeoutMs)
      throws IOException {
    if (!(watchService instanceof MockWatchService)) {
      throw new IllegalArgumentException("WatchService must be MockWatchService");
    }

    MockWatchService mockWatchService = (MockWatchService) watchService;
    return mockWatchService.pollEvents(timeoutMs);
  }

  @Override
  public CompletableFuture<Void> watchFileChangesAsync(
      Path watchPath, FileChangeEventConsumer eventConsumer) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            WatchService watchService = createWatchService(watchPath);

            while (!isClosed.get()) {
              List<WatchEvent<?>> events = pollFileEvents(watchService, 1000);
              for (WatchEvent<?> event : events) {
                Path eventPath = (Path) event.context();
                eventConsumer.accept(event.kind(), eventPath);
              }
            }
          } catch (IOException e) {
            logger.error("Error in async file watching", e);
            throw new RuntimeException(e);
          }
        },
        scheduler);
  }

  @Override
  public boolean exists(Path path) {
    Path jimfsPath = convertToJimfsPath(path);
    return Files.exists(jimfsPath);
  }

  @Override
  public void createDirectories(Path path) throws IOException {
    Path jimfsPath = convertToJimfsPath(path);
    Files.createDirectories(jimfsPath);

    // 監視中のディレクトリに変更を通知
    notifyWatchers(
        jimfsPath.getParent(), StandardWatchEventKinds.ENTRY_CREATE, jimfsPath.getFileName());
  }

  @Override
  public void createFile(Path path, String content) throws IOException {
    Path jimfsPath = convertToJimfsPath(path);

    // 親ディレクトリが存在しない場合は作成
    if (jimfsPath.getParent() != null && !Files.exists(jimfsPath.getParent())) {
      Files.createDirectories(jimfsPath.getParent());
    }

    Files.write(jimfsPath, content.getBytes());

    // 監視中のディレクトリに変更を通知
    if (jimfsPath.getParent() != null) {
      notifyWatchers(
          jimfsPath.getParent(), StandardWatchEventKinds.ENTRY_CREATE, jimfsPath.getFileName());
    }
  }

  @Override
  public void deleteFile(Path path) throws IOException {
    Path jimfsPath = convertToJimfsPath(path);
    Path parent = jimfsPath.getParent();
    Path fileName = jimfsPath.getFileName();

    Files.deleteIfExists(jimfsPath);

    // 監視中のディレクトリに変更を通知
    if (parent != null) {
      notifyWatchers(parent, StandardWatchEventKinds.ENTRY_DELETE, fileName);
    }
  }

  @Override
  public String readFile(Path path) throws IOException {
    Path jimfsPath = convertToJimfsPath(path);
    return Files.readString(jimfsPath);
  }

  @Override
  public Path createTempDirectory(String prefix) throws IOException {
    Path tempDir = fileSystem.getPath("/tmp");
    if (!Files.exists(tempDir)) {
      Files.createDirectories(tempDir);
    }

    return Files.createTempDirectory(tempDir, prefix);
  }

  @Override
  public void cleanup() throws IOException {
    if (isClosed.compareAndSet(false, true)) {
      // 全ての監視サービスを停止
      watchServices.values().forEach(MockWatchService::close);
      watchServices.clear();

      // スケジューラーを停止
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }

      // ファイルシステムを閉じる
      fileSystem.close();
      logger.info("MockFileSystemAbstraction cleaned up");
    }
  }

  /**
   * パスをJimfsファイルシステムのパスに変換
   *
   * @param path 変換対象パス
   * @return Jimfsパス
   */
  private Path convertToJimfsPath(Path path) {
    if (path.getFileSystem() == fileSystem) {
      return path;
    }

    String pathString = path.toString();
    // Windows形式のパスを正規化
    pathString = pathString.replace('\\', '/');

    return fileSystem.getPath(pathString);
  }

  /**
   * 監視サービスに変更を通知
   *
   * @param watchedPath 監視対象パス
   * @param kind イベント種別
   * @param fileName 変更されたファイル名
   */
  private void notifyWatchers(Path watchedPath, WatchEvent.Kind<?> kind, Path fileName) {
    MockWatchService watchService = watchServices.get(watchedPath);
    if (watchService != null) {
      watchService.addEvent(kind, fileName);
    }
  }

  /** モック監視サービス実装 */
  private static class MockWatchService implements WatchService {
    private final Path watchedPath;
    private final List<WatchEvent<?>> eventQueue = new ArrayList<>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    MockWatchService(Path watchedPath) {
      this.watchedPath = watchedPath;
    }

    void addEvent(WatchEvent.Kind<?> kind, Path fileName) {
      if (!isClosed.get()) {
        synchronized (eventQueue) {
          @SuppressWarnings("unchecked")
          WatchEvent<?> event = new MockWatchEvent<>((WatchEvent.Kind<Path>) kind, fileName);
          eventQueue.add(event);
        }
      }
    }

    List<WatchEvent<?>> pollEvents(long timeoutMs) {
      List<WatchEvent<?>> events = new ArrayList<>();
      synchronized (eventQueue) {
        events.addAll(eventQueue);
        eventQueue.clear();
      }
      return events;
    }

    @Override
    public WatchKey poll() {
      // 簡略化: 常にnullを返す（ポーリングベースの実装）
      return null;
    }

    @Override
    public WatchKey poll(long timeout, TimeUnit unit) {
      // 簡略化: 常にnullを返す（ポーリングベースの実装）
      return null;
    }

    @Override
    public WatchKey take() throws InterruptedException {
      // 簡略化: 常にnullを返す（ポーリングベースの実装）
      return null;
    }

    @Override
    public void close() {
      isClosed.set(true);
      synchronized (eventQueue) {
        eventQueue.clear();
      }
    }
  }

  /** モック監視イベント実装 */
  private static class MockWatchEvent<T> implements WatchEvent<T> {
    private final Kind<T> kind;
    private final T context;

    MockWatchEvent(Kind<T> kind, T context) {
      this.kind = kind;
      this.context = context;
    }

    @Override
    public Kind<T> kind() {
      return kind;
    }

    @Override
    public int count() {
      return 1;
    }

    @Override
    public T context() {
      return context;
    }
  }
}
