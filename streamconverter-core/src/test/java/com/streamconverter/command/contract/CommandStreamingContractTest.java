package com.streamconverter.command.contract;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.IStreamCommand;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class CommandStreamingContractTest {

  private static final Path COMMAND_IMPL_ROOT =
      Path.of("src/main/java/com/streamconverter/command/impl");
  private static final String COMMAND_PACKAGE_PREFIX = "com.streamconverter.command.impl.";
  private static final String PROVIDER_PACKAGE_PREFIX = "com.streamconverter.command.contract.";
  private static final Duration FIRST_WRITE_TIMEOUT = Duration.ofSeconds(1);
  private static final Duration COMMAND_COMPLETION_TIMEOUT = Duration.ofSeconds(5);

  @Test
  void everyCommandImplementationHasAStreamingContractProvider() throws IOException {
    List<String> missingProviders =
        discoverCommandClasses().stream()
            .map(CommandStreamingContractTest::missingProviderMessage)
            .filter(message -> message != null)
            .toList();

    assertTrue(
        missingProviders.isEmpty(),
        () ->
            "Missing streaming contract providers for command implementations:\n"
                + String.join("\n", missingProviders));
  }

  @TestFactory
  Stream<DynamicTest> allCommandsParticipateInStreamingContract() throws IOException {
    return discoverCommandClasses().stream()
        .map(
            commandClass ->
                DynamicTest.dynamicTest(
                    commandClass.getSimpleName(),
                    () -> verifyCommandStreamingContract(commandClass)));
  }

  private static void verifyCommandStreamingContract(Class<?> commandClass) throws Exception {
    CommandStreamingContractProvider provider = instantiateProvider(commandClass);
    assertNotNull(provider, () -> "No provider available for " + commandClass.getName());

    if (provider.expectation() == StreamingExpectation.EXEMPT_FROM_STREAMING_CONTRACT) {
      assertFalse(
          provider.exemptionReason().isBlank(),
          () -> "Exempt command must explain why: " + commandClass.getName());
    }

    BlockingProbeInputStream inputStream =
        new BlockingProbeInputStream(provider.sampleInput(), provider.firstChunkSize());
    SignalingOutputStream outputStream = new SignalingOutputStream();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<?> future =
        executor.submit(() -> executeCommand(provider.createCommand(), inputStream, outputStream));

    boolean wroteBeforeRelease = false;
    AssertionError failure = null;
    try {
      wroteBeforeRelease = outputStream.awaitFirstWrite(FIRST_WRITE_TIMEOUT);
      if (provider.expectation() == StreamingExpectation.MUST_WRITE_BEFORE_INPUT_COMPLETE) {
        assertTrue(
            wroteBeforeRelease,
            () ->
                commandClass.getSimpleName()
                    + " did not start writing before the remaining input was released");
      } else {
        assertFalse(
            wroteBeforeRelease,
            () ->
                commandClass.getSimpleName()
                    + " started writing early but is marked exempt: "
                    + provider.exemptionReason());
      }
    } catch (AssertionError e) {
      failure = e;
    } finally {
      inputStream.releaseRemainingInput();
    }

    try {
      awaitCompletion(commandClass, future);
    } finally {
      shutdown(executor);
    }

    if (failure != null) {
      throw failure;
    }
  }

  private static Void executeCommand(
      IStreamCommand command, InputStream inputStream, ByteArrayOutputStream outputStream) {
    try {
      command.execute(inputStream, outputStream);
      return null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void awaitCompletion(Class<?> commandClass, Future<?> future) throws Exception {
    try {
      future.get(COMMAND_COMPLETION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      }
      throw new RuntimeException("Unexpected command failure for " + commandClass.getName(), cause);
    } catch (java.util.concurrent.TimeoutException e) {
      fail(commandClass.getSimpleName() + " did not finish after the input was released");
    }
  }

  private static void shutdown(ExecutorService executor) {
    executor.shutdown();
  }

  private static String missingProviderMessage(Class<?> commandClass) {
    try {
      instantiateProvider(commandClass);
      return null;
    } catch (ReflectiveOperationException e) {
      return "- "
          + commandClass.getName()
          + " -> expected provider "
          + providerClassName(commandClass)
          + " ("
          + e.getClass().getSimpleName()
          + ")";
    }
  }

  private static CommandStreamingContractProvider instantiateProvider(Class<?> commandClass)
      throws ReflectiveOperationException {
    Class<?> providerClass = Class.forName(providerClassName(commandClass));
    Object provider = providerClass.getDeclaredConstructor().newInstance();
    return (CommandStreamingContractProvider) provider;
  }

  private static String providerClassName(Class<?> commandClass) {
    return PROVIDER_PACKAGE_PREFIX + commandClass.getSimpleName() + "StreamingContractProvider";
  }

  private static List<Class<?>> discoverCommandClasses() throws IOException {
    try (Stream<Path> pathStream = Files.walk(COMMAND_IMPL_ROOT)) {
      return pathStream
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith("Command.java"))
          .sorted(Comparator.naturalOrder())
          .map(CommandStreamingContractTest::toCommandClassName)
          .map(CommandStreamingContractTest::loadClass)
          .filter(commandClass -> !Modifier.isAbstract(commandClass.getModifiers()))
          .toList();
    }
  }

  private static String toCommandClassName(Path sourceFile) {
    Path relative = COMMAND_IMPL_ROOT.relativize(sourceFile);
    String suffix =
        relative
            .toString()
            .replace(sourceFile.getFileSystem().getSeparator(), ".")
            .replace(".java", "");
    return COMMAND_PACKAGE_PREFIX + suffix;
  }

  private static Class<?> loadClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Failed to load command class " + className, e);
    }
  }

  private static final class BlockingProbeInputStream extends InputStream {
    private final byte[] data;
    private final int firstChunkSize;
    private final CountDownLatch releaseRemainingInput = new CountDownLatch(1);
    private int index;

    private BlockingProbeInputStream(byte[] data, int firstChunkSize) {
      this.data = data;
      this.firstChunkSize = Math.min(Math.max(1, firstChunkSize), Math.max(1, data.length - 1));
    }

    @Override
    public int read() throws IOException {
      byte[] singleByte = new byte[1];
      int read = read(singleByte, 0, 1);
      return read == -1 ? -1 : singleByte[0] & 0xFF;
    }

    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
      if (index >= data.length) {
        return -1;
      }

      if (index >= firstChunkSize) {
        try {
          if (!releaseRemainingInput.await(
              COMMAND_COMPLETION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new IOException("Timed out waiting to release remaining input");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted while waiting to release remaining input", e);
        }
      }

      int upperBound = index < firstChunkSize ? firstChunkSize : data.length;
      int bytesToCopy = Math.min(len, upperBound - index);
      System.arraycopy(data, index, buffer, off, bytesToCopy);
      index += bytesToCopy;
      return bytesToCopy;
    }

    private void releaseRemainingInput() {
      releaseRemainingInput.countDown();
    }
  }

  private static final class SignalingOutputStream extends ByteArrayOutputStream {
    private final CountDownLatch firstWriteLatch = new CountDownLatch(1);

    @Override
    public synchronized void write(int b) {
      firstWriteLatch.countDown();
      super.write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
      if (len > 0) {
        firstWriteLatch.countDown();
      }
      super.write(b, off, len);
    }

    private boolean awaitFirstWrite(Duration timeout) throws InterruptedException {
      return firstWriteLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
  }
}
