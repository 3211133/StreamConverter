package com.streamconverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.StreamConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FileBufferCommand Tests")
class FileBufferCommandTest {

  // ---------------------------------------------------------------------------
  // Plain mode
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Plain: input bytes are fully reproduced in output")
  void testPlain_inputEqualsOutput() throws IOException {
    byte[] input = "Hello, FileBufferCommand!".getBytes(StandardCharsets.UTF_8);
    byte[] output = execute(FileBufferCommand.create(), input);
    assertArrayEquals(input, output);
  }

  @Test
  @DisplayName("Plain: empty input produces empty output")
  void testPlain_emptyInput() throws IOException {
    byte[] output = execute(FileBufferCommand.create(), new byte[0]);
    assertEquals(0, output.length);
  }

  @Test
  @DisplayName("Plain: data larger than 64 KB is transferred correctly")
  void testPlain_largeInput() throws IOException {
    byte[] input = buildLargeInput(128 * 1024); // 128 KB
    byte[] output = execute(FileBufferCommand.create(), input);
    assertArrayEquals(input, output);
  }

  @Test
  @DisplayName("Plain: temporary file is deleted after execution")
  void testPlain_tempFileDeletedAfterExecution(@TempDir Path tempDir) throws IOException {
    System.setProperty("java.io.tmpdir", tempDir.toString());
    try {
      execute(FileBufferCommand.create(), "test data".getBytes(StandardCharsets.UTF_8));
      String[] remaining = tempDir.toFile().list((d, n) -> n.startsWith("streamconverter-"));
      assertEquals(
          0,
          remaining == null ? 0 : remaining.length,
          "Temporary file should be deleted after execution");
    } finally {
      System.clearProperty("java.io.tmpdir");
    }
  }

  @Test
  @DisplayName("Plain: temporary file is deleted even when IOException is thrown mid-read")
  void testPlain_tempFileDeletedOnException(@TempDir Path tempDir) throws IOException {
    System.setProperty("java.io.tmpdir", tempDir.toString());
    try {
      try (InputStream failingStream =
              new InputStream() {
                private int count = 0;

                @Override
                public int read(byte[] buf, int off, int len) throws IOException {
                  if (count++ > 2) throw new IOException("Simulated read failure");
                  buf[off] = 'X';
                  return 1;
                }

                @Override
                public int read() throws IOException {
                  return 'X';
                }
              };
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        assertThrows(
            IOException.class,
            () -> FileBufferCommand.create().execute(failingStream, outputStream));
      }

      String[] remaining = tempDir.toFile().list((d, n) -> n.startsWith("streamconverter-"));
      assertEquals(
          0,
          remaining == null ? 0 : remaining.length,
          "Temporary file should be deleted after exception");
    } finally {
      System.clearProperty("java.io.tmpdir");
    }
  }

  @Test
  @DisplayName("Plain: shutdown hook is deregistered after normal execution")
  void testPlain_shutdownHookDeregisteredAfterExecution() throws IOException {
    int hooksBefore = countShutdownHooks();
    execute(FileBufferCommand.create(), "data".getBytes(StandardCharsets.UTF_8));
    int hooksAfter = countShutdownHooks();
    assertEquals(hooksBefore, hooksAfter, "Shutdown hook count should be the same after execution");
  }

  // ---------------------------------------------------------------------------
  // Encrypted mode
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Encrypted: input bytes are fully reproduced in output")
  void testEncrypted_inputEqualsOutput() throws IOException {
    byte[] input = "Sensitive data for encryption test.".getBytes(StandardCharsets.UTF_8);
    byte[] output = execute(FileBufferCommand.createEncrypted(), input);
    assertArrayEquals(input, output);
  }

  @Test
  @DisplayName("Encrypted: empty input produces empty output")
  void testEncrypted_emptyInput() throws IOException {
    byte[] output = execute(FileBufferCommand.createEncrypted(), new byte[0]);
    assertEquals(0, output.length);
  }

  @Test
  @DisplayName("Encrypted: data larger than 64 KB is transferred correctly")
  void testEncrypted_largeInput() throws IOException {
    byte[] input = buildLargeInput(128 * 1024);
    byte[] output = execute(FileBufferCommand.createEncrypted(), input);
    assertArrayEquals(input, output);
  }

  @Test
  @DisplayName("Encrypted: temporary file bytes differ from plaintext (directly verified)")
  void testEncrypted_tempFileContainsEncryptedData() throws Exception {
    byte[] input = "Sensitive plaintext payload.".getBytes(StandardCharsets.UTF_8);

    // 実際の tmpdir をスキャン。execute() 開始直前のスナップショットとの差分で
    // このテストが作成した一時ファイルを確実に特定する。
    Path actualTmpDir = Path.of(System.getProperty("java.io.tmpdir"));
    java.util.Set<String> before =
        java.util.Arrays.stream(
                actualTmpDir.toFile().list((d, n) -> n.startsWith("streamconverter-")))
            .collect(java.util.stream.Collectors.toSet());

    AtomicReference<byte[]> capturedTempFileBytes = new AtomicReference<>();
    CountDownLatch firstWriteReceived = new CountDownLatch(1);
    CountDownLatch writeMayProceed = new CountDownLatch(1);

    // readDecrypted() が outputStream.write() を呼んだ瞬間 = 暗号化書き込みが完全に終わった後。
    // その最初の write() で一時ファイルを横取りし、処理をブロックする。
    try (OutputStream interceptingOutput =
        new OutputStream() {
          private boolean intercepted = false;

          @Override
          public void write(byte[] buf, int off, int len) throws IOException {
            maybeIntercept();
          }

          @Override
          public void write(int b) throws IOException {
            maybeIntercept();
          }

          private void maybeIntercept() throws IOException {
            if (!intercepted) {
              intercepted = true;
              // スナップショット差分でこのテストの一時ファイルを特定する
              String[] current =
                  actualTmpDir.toFile().list((d, n) -> n.startsWith("streamconverter-"));
              if (current != null) {
                for (String name : current) {
                  if (!before.contains(name)) {
                    try {
                      capturedTempFileBytes.set(Files.readAllBytes(actualTmpDir.resolve(name)));
                    } catch (IOException e) {
                      // 読み取れない場合は null のまま
                    }
                    break;
                  }
                }
              }
              firstWriteReceived.countDown();
              try {
                if (!writeMayProceed.await(10, TimeUnit.SECONDS)) {
                  throw new IOException("Timed out waiting for test to proceed");
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted", e);
              }
            }
          }
        }) {
      // 別スレッドで execute() を走らせる
      Thread executor =
          new Thread(
              () -> {
                try {
                  FileBufferCommand.createEncrypted()
                      .execute(new ByteArrayInputStream(input), interceptingOutput);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
      executor.start();

      // 最初の write() が来るまで待ってからアンブロック（タイムアウト付き）
      assertTrue(
          firstWriteReceived.await(10, TimeUnit.SECONDS),
          "Timed out waiting for encrypted output to be written");
      writeMayProceed.countDown();
      executor.join(10_000);
      assertFalse(executor.isAlive(), "Executor thread did not finish in time");
    }

    byte[] fileBytes = capturedTempFileBytes.get();
    assertNotNull(fileBytes, "Temp file bytes should have been captured mid-execution");
    // ファイルサイズは IV(12B) + 暗号文 + GCMタグ(16B) なので plaintext より大きい
    assertTrue(
        fileBytes.length > input.length,
        "Encrypted file ("
            + fileBytes.length
            + "B) must be larger than plaintext ("
            + input.length
            + "B): IV=12B + ciphertext + GCM tag=16B");
    // 一時ファイルの内容が平文を含まないこと（暗号化されていること）
    assertFalse(
        containsSubsequence(fileBytes, input), "Temporary file must not contain plaintext bytes");
  }

  @Test
  @DisplayName("Encrypted: temporary file is deleted after execution")
  void testEncrypted_tempFileDeletedAfterExecution(@TempDir Path tempDir) throws IOException {
    System.setProperty("java.io.tmpdir", tempDir.toString());
    try {
      execute(FileBufferCommand.createEncrypted(), "secret".getBytes(StandardCharsets.UTF_8));
      String[] remaining = tempDir.toFile().list((d, n) -> n.startsWith("streamconverter-"));
      assertEquals(
          0,
          remaining == null ? 0 : remaining.length,
          "Temporary file should be deleted after encrypted execution");
    } finally {
      System.clearProperty("java.io.tmpdir");
    }
  }

  // ---------------------------------------------------------------------------
  // Pipeline integration
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Pipeline: cmd1 → FileBufferCommand → cmd2 produces correct output")
  void testPipelineIntegration() throws IOException {
    byte[] input = "input".getBytes(StandardCharsets.UTF_8);

    StreamConverter converter =
        StreamConverter.create(
            (in, out) -> {
              byte[] data = in.readAllBytes();
              String text = new String(data, StandardCharsets.UTF_8) + " [stage1]";
              out.write(text.getBytes(StandardCharsets.UTF_8));
            },
            FileBufferCommand.create(),
            (in, out) -> {
              byte[] data = in.readAllBytes();
              String text = new String(data, StandardCharsets.UTF_8) + " [stage2]";
              out.write(text.getBytes(StandardCharsets.UTF_8));
            });

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    converter.run(new ByteArrayInputStream(input), output);

    assertEquals("input [stage1] [stage2]", output.toString(StandardCharsets.UTF_8));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private byte[] execute(FileBufferCommand command, byte[] inputBytes) throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(inputBytes);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    command.execute(in, out);
    return out.toByteArray();
  }

  /** Returns true if {@code haystack} contains {@code needle} as a contiguous subsequence. */
  private boolean containsSubsequence(byte[] haystack, byte[] needle) {
    if (needle.length == 0) return true;
    outer:
    for (int i = 0; i <= haystack.length - needle.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j]) continue outer;
      }
      return true;
    }
    return false;
  }

  private byte[] buildLargeInput(int size) {
    byte[] data = new byte[size];
    for (int i = 0; i < size; i++) {
      data[i] = (byte) (i % 256);
    }
    return data;
  }

  /**
   * Counts the number of registered shutdown hooks by inspecting the JVM's ApplicationShutdownHooks
   * via reflection.
   */
  private int countShutdownHooks() {
    try {
      Class<?> clazz = Class.forName("java.lang.ApplicationShutdownHooks");
      Field field = clazz.getDeclaredField("hooks");
      field.setAccessible(true);
      java.util.IdentityHashMap<?, ?> hooks = (java.util.IdentityHashMap<?, ?>) field.get(null);
      return hooks.size();
    } catch (ReflectiveOperationException | RuntimeException e) {
      return -1;
    }
  }
}
