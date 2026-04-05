package com.streamconverter.command.contract;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.IStreamCommand;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
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

/**
 * Contract test for stream-oriented commands.
 *
 * <p>The probe works by:
 *
 * <ol>
 *   <li>feeding a command an input stream that returns an initial chunk and then blocks
 *   <li>watching whether the command writes anything to the output stream while input is still
 *       blocked
 *   <li>releasing the rest of the input and requiring the command to complete
 * </ol>
 *
 * <p>This test answers a narrow but important question: "does output begin before input
 * completion?" It does not prove full memory safety, throughput, or correctness for arbitrary
 * payloads. Commands classified as {@link StreamingExpectation#KNOWN_STREAMING_VIOLATION} are
 * expected to have been run through this probe already; that status is not a placeholder for
 * unverified work.
 */
class CommandStreamingContractTest {

  private static final Path REPOSITORY_ROOT = Path.of("..").normalize();
  private static final String PROVIDER_PACKAGE_PREFIX = "com.streamconverter.command.contract.";
  private static final Duration FIRST_WRITE_TIMEOUT = Duration.ofSeconds(1);
  private static final Duration COMMAND_COMPLETION_TIMEOUT = Duration.ofSeconds(5);
  private static final List<DiscoveredCommand> DISCOVERED_COMMANDS = discoverCommandClasses();

  @Test
  void everyCommandImplementationHasAStreamingContractProvider() {
    List<String> missingProviders =
        DISCOVERED_COMMANDS.stream()
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
  Stream<DynamicTest> allCommandsParticipateInStreamingContract() {
    return DISCOVERED_COMMANDS.stream()
        .filter(CommandStreamingContractTest::hasProvider)
        .map(
            commandClass ->
                DynamicTest.dynamicTest(
                    commandClass.simpleName(), () -> verifyCommandStreamingContract(commandClass)));
  }

  private static void verifyCommandStreamingContract(DiscoveredCommand commandClass)
      throws Exception {
    CommandStreamingContractProvider provider = instantiateProvider(commandClass);
    assertNotNull(provider, () -> "No provider available for " + commandClass.fqcn());

    if (!provider.supportsProbeExecution()) {
      assertFalse(
          provider.probeSkipReason().isBlank(),
          () -> "Probe-skipped command must explain why: " + commandClass.fqcn());
      return;
    }

    if (provider.expectation() != StreamingExpectation.STREAMING_COMPLIANT) {
      assertFalse(
          provider.exemptionReason().isBlank(),
          () -> "Non-compliant command must explain why: " + commandClass.fqcn());
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
      if (provider.expectation() == StreamingExpectation.STREAMING_COMPLIANT) {
        assertTrue(
            wroteBeforeRelease,
            () ->
                commandClass.simpleName()
                    + " did not start writing before the remaining input was released");
      } else {
        assertFalse(
            wroteBeforeRelease,
            () ->
                commandClass.simpleName()
                    + " started writing early but is marked as non-compliant: "
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
      shutdown(executor, future);
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

  private static void awaitCompletion(DiscoveredCommand commandClass, Future<?> future)
      throws Exception {
    try {
      future.get(COMMAND_COMPLETION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      }
      throw new RuntimeException("Unexpected command failure for " + commandClass.fqcn(), cause);
    } catch (java.util.concurrent.TimeoutException e) {
      future.cancel(true);
      fail(commandClass.simpleName() + " did not finish after the input was released");
    }
  }

  private static void shutdown(ExecutorService executor, Future<?> future) {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(
          COMMAND_COMPLETION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
        future.cancel(true);
        executor.shutdownNow();
        if (!executor.awaitTermination(
            COMMAND_COMPLETION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
          fail("Executor did not terminate promptly after test completion");
        }
      }
    } catch (InterruptedException e) {
      future.cancel(true);
      executor.shutdownNow();
      Thread.currentThread().interrupt();
      fail("Interrupted while shutting down executor", e);
    }
  }

  private static String missingProviderMessage(DiscoveredCommand commandClass) {
    try {
      instantiateProvider(commandClass);
      return null;
    } catch (ReflectiveOperationException | RuntimeException e) {
      return "- "
          + commandClass.fqcn()
          + " -> expected provider "
          + providerClassName(commandClass)
          + " ("
          + e.getClass().getSimpleName()
          + ")";
    }
  }

  private static boolean hasProvider(DiscoveredCommand commandClass) {
    try {
      instantiateProvider(commandClass);
      return true;
    } catch (ReflectiveOperationException | RuntimeException e) {
      return false;
    }
  }

  private static CommandStreamingContractProvider instantiateProvider(
      DiscoveredCommand commandClass) throws ReflectiveOperationException {
    Class<?> providerClass = Class.forName(providerClassName(commandClass));
    Object provider = providerClass.getDeclaredConstructor().newInstance();
    return (CommandStreamingContractProvider) provider;
  }

  private static String providerClassName(DiscoveredCommand commandClass) {
    return PROVIDER_PACKAGE_PREFIX + commandClass.simpleName() + "StreamingContractProvider";
  }

  private static List<DiscoveredCommand> discoverCommandClasses() {
    try {
      return CommandImplementationDiscovery.discover(REPOSITORY_ROOT).stream()
          .map(command -> new DiscoveredCommand(command.fqcn(), command.simpleName()))
          .toList();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to discover command implementations", e);
    }
  }

  private record DiscoveredCommand(String fqcn, String simpleName) {}

  private static final class BlockingProbeInputStream extends InputStream {
    private final byte[] data;
    private final int firstChunkSize;
    private final CountDownLatch releaseLatch = new CountDownLatch(1);
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
          if (!releaseLatch.await(COMMAND_COMPLETION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
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
      releaseLatch.countDown();
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
