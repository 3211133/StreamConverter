package com.streamconverter;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Strategy interface for creating executor services used by {@link StreamConverter}.
 *
 * <p>Provides built-in factories for virtual-thread-per-task execution and platform thread pools.
 */
@FunctionalInterface
public interface ThreadingStrategy {

  /**
   * Creates an executor service tailored for the provided command count.
   *
   * @param commandCount number of commands scheduled for execution
   * @return a newly created executor service
   */
  ExecutorService createExecutor(int commandCount);

  /**
   * Returns a strategy that spins up a new virtual thread for each submitted task.
   *
   * <p>Virtual threads dramatically reduce the cost of blocking operations, making them a great fit
   * for the I/O-heavy work typically performed by StreamConverter commands.
   *
   * @return a threading strategy backed by virtual threads
   */
  static ThreadingStrategy virtualThreadPerTask() {
    ThreadFactory factory = Thread.ofVirtual().name("stream-converter-virtual-", 0).factory();
    return commandCount -> Executors.newThreadPerTaskExecutor(factory);
  }

  /**
   * Returns a strategy that uses a fixed-size pool of platform threads sized to the workload.
   *
   * @param desiredParallelism preferred level of parallelism; must be positive
   * @return a platform thread-based strategy capped at the desired parallelism
   */
  static ThreadingStrategy platformThreadPool(int desiredParallelism) {
    if (desiredParallelism <= 0) {
      throw new IllegalArgumentException("desiredParallelism must be greater than zero");
    }
    ThreadFactory factory = Thread.ofPlatform().name("stream-converter-platform-", 0).factory();
    return commandCount ->
        Executors.newFixedThreadPool(Math.min(commandCount, desiredParallelism), factory);
  }

  /**
   * Returns a platform thread strategy tuned to available processors and command count.
   *
   * @return a platform thread pool strategy sized adaptively
   */
  static ThreadingStrategy adaptivePlatformThreadPool() {
    ThreadFactory factory = Thread.ofPlatform().name("stream-converter-platform-", 0).factory();
    return commandCount -> {
      int availableCores = Runtime.getRuntime().availableProcessors();
      int optimalSize = Math.min(commandCount, Math.max(2, availableCores));
      return Executors.newFixedThreadPool(optimalSize, factory);
    };
  }

  /**
   * Utility to guard against null strategies while offering fluent overrides.
   *
   * @param strategy the strategy to validate
   * @return the provided strategy if it is non-null
   */
  static ThreadingStrategy requireNonNull(ThreadingStrategy strategy) {
    return Objects.requireNonNull(strategy, "threadingStrategy cannot be null");
  }
}
