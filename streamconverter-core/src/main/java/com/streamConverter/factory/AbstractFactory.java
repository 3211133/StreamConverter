package com.streamConverter.factory;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Factory base class providing common factory functionality.
 *
 * <p>This class provides shared infrastructure for all factory implementations, including caching,
 * reflection-based instance creation, and logging integration.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe instance caching with ConcurrentHashMap
 *   <li>Reflection-based instance creation with type compatibility checking
 *   <li>Unified logging and error handling
 *   <li>Configuration management for factory settings
 * </ul>
 *
 * @param <T> the type of instances created by this factory
 * @since 1.0
 */
public abstract class AbstractFactory<T> {
  private static final Logger log = LoggerFactory.getLogger(AbstractFactory.class);

  /** Thread-safe cache for created instances */
  protected final Map<String, T> instanceCache = new ConcurrentHashMap<>();

  /** Factory configuration settings */
  protected final FactoryConfiguration config;

  /** Constructs an AbstractFactory with default configuration. */
  protected AbstractFactory() {
    this.config = FactoryConfiguration.defaultConfig();
    log.debug("Initialized {} with default configuration", getClass().getSimpleName());
  }

  /**
   * Constructs an AbstractFactory with specified configuration.
   *
   * @param config the factory configuration
   */
  protected AbstractFactory(FactoryConfiguration config) {
    this.config = config != null ? config : FactoryConfiguration.defaultConfig();
    log.debug("Initialized {} with custom configuration", getClass().getSimpleName());
  }

  /**
   * Creates an instance using reflection with intelligent constructor matching.
   *
   * @param <U> the specific type of instance to create
   * @param clazz the class to instantiate
   * @param args constructor arguments
   * @return created instance
   * @throws FactoryException if instance creation fails
   */
  @SuppressWarnings("unchecked")
  protected <U extends T> U createInstance(Class<U> clazz, Object... args) throws FactoryException {
    try {
      // Try default constructor first
      if (args.length == 0) {
        Constructor<U> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
      }

      // Find best matching constructor
      Constructor<U> bestMatch = findBestMatchingConstructor(clazz, args);
      bestMatch.setAccessible(true);
      return bestMatch.newInstance(args);

    } catch (Exception e) {
      String message =
          String.format(
              "Failed to create instance of %s with %d arguments",
              clazz.getSimpleName(), args.length);
      log.error(message, e);
      throw new FactoryException(message, e);
    }
  }

  /**
   * Finds the best matching constructor for the given arguments.
   *
   * @param <U> the type to construct
   * @param clazz the class
   * @param args constructor arguments
   * @return best matching constructor
   * @throws NoSuchMethodException if no compatible constructor found
   */
  @SuppressWarnings("unchecked")
  private <U extends T> Constructor<U> findBestMatchingConstructor(Class<U> clazz, Object[] args)
      throws NoSuchMethodException {
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();

    // First pass: exact match
    for (Constructor<?> constructor : constructors) {
      if (isExactMatch(constructor, args)) {
        return (Constructor<U>) constructor;
      }
    }

    // Second pass: compatible match
    for (Constructor<?> constructor : constructors) {
      if (isCompatibleMatch(constructor, args)) {
        return (Constructor<U>) constructor;
      }
    }

    throw new NoSuchMethodException(
        String.format(
            "No compatible constructor found for %s with argument types: %s",
            clazz.getSimpleName(), getArgumentTypes(args)));
  }

  /** Checks if constructor parameters exactly match argument types. */
  private boolean isExactMatch(Constructor<?> constructor, Object[] args) {
    Class<?>[] paramTypes = constructor.getParameterTypes();
    if (paramTypes.length != args.length) {
      return false;
    }

    for (int i = 0; i < paramTypes.length; i++) {
      if (!paramTypes[i].equals(args[i].getClass())) {
        return false;
      }
    }
    return true;
  }

  /** Checks if constructor parameters are compatible with argument types. */
  private boolean isCompatibleMatch(Constructor<?> constructor, Object[] args) {
    Class<?>[] paramTypes = constructor.getParameterTypes();
    if (paramTypes.length != args.length) {
      return false;
    }

    for (int i = 0; i < paramTypes.length; i++) {
      if (!isTypeCompatible(paramTypes[i], args[i].getClass())) {
        return false;
      }
    }
    return true;
  }

  /** Checks type compatibility including primitives and inheritance. */
  private boolean isTypeCompatible(Class<?> paramType, Class<?> argType) {
    if (paramType.equals(argType)) {
      return true;
    }

    // Primitive and wrapper type compatibility
    if (isPrimitiveWrapperMatch(paramType, argType)) {
      return true;
    }

    // Inheritance compatibility
    return paramType.isAssignableFrom(argType);
  }

  /** Map of primitive types to their corresponding wrapper classes. */
  private static final java.util.Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP =
      java.util.Map.of(
          int.class, Integer.class,
          long.class, Long.class,
          boolean.class, Boolean.class,
          double.class, Double.class,
          float.class, Float.class,
          char.class, Character.class,
          byte.class, Byte.class,
          short.class, Short.class);

  /** Checks if parameter and argument are primitive/wrapper type pair. */
  private boolean isPrimitiveWrapperMatch(Class<?> paramType, Class<?> argType) {
    return PRIMITIVE_WRAPPER_MAP.get(paramType) == argType;
  }

  /** Gets string representation of argument types for error messages. */
  private String getArgumentTypes(Object[] args) {
    if (args.length == 0) {
      return "[]";
    }

    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < args.length; i++) {
      if (i > 0) sb.append(", ");
      sb.append(args[i].getClass().getSimpleName());
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Creates a cache key for the given parameters.
   *
   * @param keyComponents components to create key from
   * @return cache key string
   */
  protected String createCacheKey(Object... keyComponents) {
    if (keyComponents.length == 0) {
      return "default";
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < keyComponents.length; i++) {
      if (i > 0) sb.append(":");
      sb.append(keyComponents[i] != null ? keyComponents[i].toString() : "null");
    }
    return sb.toString();
  }

  /**
   * Gets cached instance or null if not found.
   *
   * @param key cache key
   * @return cached instance or null
   */
  protected T getCachedInstance(String key) {
    return instanceCache.get(key);
  }

  /**
   * Caches an instance with the given key.
   *
   * @param key cache key
   * @param instance instance to cache
   */
  protected void cacheInstance(String key, T instance) {
    if (config.isCachingEnabled()) {
      instanceCache.put(key, instance);
      log.debug("Cached instance: {} -> {}", key, instance.getClass().getSimpleName());
    }
  }

  /** Clears all cached instances. */
  public void clearCache() {
    int cacheSize = instanceCache.size();
    instanceCache.clear();
    log.info("Cleared factory cache: {} instances removed", cacheSize);
  }

  /**
   * Gets current cache size.
   *
   * @return number of cached instances
   */
  public int getCacheSize() {
    return instanceCache.size();
  }

  /**
   * Gets factory configuration.
   *
   * @return current configuration
   */
  public FactoryConfiguration getConfiguration() {
    return config;
  }
}
