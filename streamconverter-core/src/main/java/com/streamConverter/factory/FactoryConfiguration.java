package com.streamConverter.factory;

/**
 * Configuration class for factory settings.
 *
 * <p>This class encapsulates common configuration options for factory implementations, providing a
 * centralized way to manage factory behavior.
 *
 * <p>Configuration options include:
 *
 * <ul>
 *   <li>Caching control for created instances
 *   <li>Logging level control for factory operations
 *   <li>Error handling strategies
 *   <li>Performance optimization settings
 * </ul>
 *
 * @since 1.0
 */
public class FactoryConfiguration {

  /** Default cache size limit */
  public static final int DEFAULT_CACHE_SIZE = 100;

  /** Default logging enabled flag */
  public static final boolean DEFAULT_LOGGING_ENABLED = true;

  private final boolean cachingEnabled;
  private final boolean detailedLoggingEnabled;
  private final int maxCacheSize;
  private final long cacheExpirationMinutes;
  private final boolean failFastOnError;

  /**
   * Creates a factory configuration with specified settings.
   *
   * @param cachingEnabled whether to enable instance caching
   * @param detailedLoggingEnabled whether to enable detailed logging
   * @param maxCacheSize maximum number of cached instances
   * @param cacheExpirationMinutes cache expiration time in minutes (0 = no expiration)
   * @param failFastOnError whether to fail immediately on errors
   */
  public FactoryConfiguration(
      boolean cachingEnabled,
      boolean detailedLoggingEnabled,
      int maxCacheSize,
      long cacheExpirationMinutes,
      boolean failFastOnError) {
    this.cachingEnabled = cachingEnabled;
    this.detailedLoggingEnabled = detailedLoggingEnabled;
    this.maxCacheSize = Math.max(1, maxCacheSize);
    this.cacheExpirationMinutes = Math.max(0, cacheExpirationMinutes);
    this.failFastOnError = failFastOnError;
  }

  /**
   * Creates default factory configuration.
   *
   * @return default configuration instance
   */
  public static FactoryConfiguration defaultConfig() {
    return new FactoryConfiguration(
        true, // caching enabled
        false, // detailed logging disabled by default
        DEFAULT_CACHE_SIZE,
        0, // no cache expiration
        true // fail fast on errors
        );
  }

  /**
   * Creates a production-optimized configuration.
   *
   * @return production configuration instance
   */
  public static FactoryConfiguration productionConfig() {
    return new FactoryConfiguration(
        true, // caching enabled for performance
        false, // detailed logging disabled for performance
        200, // larger cache for production
        30, // 30-minute cache expiration
        true // fail fast on errors
        );
  }

  /**
   * Creates a development-friendly configuration.
   *
   * @return development configuration instance
   */
  public static FactoryConfiguration developmentConfig() {
    return new FactoryConfiguration(
        false, // disable caching for development flexibility
        true, // enable detailed logging for debugging
        50, // smaller cache for development
        0, // no cache expiration for consistency
        false // don't fail fast for easier debugging
        );
  }

  /**
   * Creates a testing configuration.
   *
   * @return testing configuration instance
   */
  public static FactoryConfiguration testingConfig() {
    return new FactoryConfiguration(
        false, // disable caching for test isolation
        true, // enable detailed logging for test debugging
        10, // minimal cache for testing
        0, // no cache expiration
        true // fail fast for clear test failures
        );
  }

  /**
   * Creates a configuration builder for custom settings.
   *
   * @return configuration builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Checks if caching is enabled.
   *
   * @return true if caching is enabled
   */
  public boolean isCachingEnabled() {
    return cachingEnabled;
  }

  /**
   * Checks if detailed logging is enabled.
   *
   * @return true if detailed logging is enabled
   */
  public boolean isDetailedLoggingEnabled() {
    return detailedLoggingEnabled;
  }

  /**
   * Gets maximum cache size.
   *
   * @return maximum cache size
   */
  public int getMaxCacheSize() {
    return maxCacheSize;
  }

  /**
   * Gets cache expiration time in minutes.
   *
   * @return cache expiration minutes (0 = no expiration)
   */
  public long getCacheExpirationMinutes() {
    return cacheExpirationMinutes;
  }

  /**
   * Checks if fail-fast error handling is enabled.
   *
   * @return true if fail-fast is enabled
   */
  public boolean isFailFastOnError() {
    return failFastOnError;
  }

  /** Builder class for creating custom factory configurations. */
  public static class Builder {
    private boolean cachingEnabled = true;
    private boolean detailedLoggingEnabled = false;
    private int maxCacheSize = DEFAULT_CACHE_SIZE;
    private long cacheExpirationMinutes = 0;
    private boolean failFastOnError = true;

    /** Creates a new builder. */
    public Builder() {}

    /**
     * Sets caching enabled flag.
     *
     * @param enabled whether to enable caching
     * @return this builder instance
     */
    public Builder caching(boolean enabled) {
      this.cachingEnabled = enabled;
      return this;
    }

    /**
     * Sets detailed logging enabled flag.
     *
     * @param enabled whether to enable detailed logging
     * @return this builder instance
     */
    public Builder detailedLogging(boolean enabled) {
      this.detailedLoggingEnabled = enabled;
      return this;
    }

    /**
     * Sets maximum cache size.
     *
     * @param size maximum cache size
     * @return this builder instance
     */
    public Builder maxCacheSize(int size) {
      this.maxCacheSize = Math.max(1, size);
      return this;
    }

    /**
     * Sets cache expiration time.
     *
     * @param minutes expiration time in minutes (0 = no expiration)
     * @return this builder instance
     */
    public Builder cacheExpiration(long minutes) {
      this.cacheExpirationMinutes = Math.max(0, minutes);
      return this;
    }

    /**
     * Sets fail-fast error handling.
     *
     * @param enabled whether to enable fail-fast
     * @return this builder instance
     */
    public Builder failFast(boolean enabled) {
      this.failFastOnError = enabled;
      return this;
    }

    /**
     * Builds the configuration instance.
     *
     * @return created configuration instance
     */
    public FactoryConfiguration build() {
      return new FactoryConfiguration(
          cachingEnabled,
          detailedLoggingEnabled,
          maxCacheSize,
          cacheExpirationMinutes,
          failFastOnError);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "FactoryConfiguration{caching=%b, detailedLogging=%b, maxCacheSize=%d, "
            + "cacheExpiration=%d min, failFast=%b}",
        cachingEnabled,
        detailedLoggingEnabled,
        maxCacheSize,
        cacheExpirationMinutes,
        failFastOnError);
  }
}
