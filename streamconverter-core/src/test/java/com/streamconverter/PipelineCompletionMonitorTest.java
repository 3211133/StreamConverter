package com.streamconverter;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@DisplayName("PipelineCompletionMonitor Test")
class PipelineCompletionMonitorTest {

  @Test
  @Timeout(10)
  @DisplayName("await が割り込まれたとき InterruptedException が cause として保存される")
  void awaitPreservesInterruptedExceptionAsCause() throws InterruptedException {
    PipelineCompletionMonitor monitor = new PipelineCompletionMonitor();
    CompletableFuture<Void> blocker = new CompletableFuture<>();
    StreamProcessingException[] caught = new StreamProcessingException[1];
    CountDownLatch done = new CountDownLatch(1);

    Thread worker =
        Thread.ofPlatform()
            .start(
                () -> {
                  try {
                    monitor.await(List.of(blocker));
                  } catch (StreamProcessingException e) {
                    caught[0] = e;
                  } catch (Exception ignored) {
                  } finally {
                    done.countDown();
                  }
                });

    Thread.sleep(50);
    worker.interrupt();
    done.await();

    assertNotNull(caught[0], "StreamProcessingException が投げられること");
    assertNotNull(caught[0].getCause(), "InterruptedException が cause として保存されていること");
    assertInstanceOf(InterruptedException.class, caught[0].getCause());
  }
}
