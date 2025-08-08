package com.streamConverter.factory;

/**
 * Exception thrown when factory operations fail.
 *
 * <p>This exception provides specific error handling for factory-related failures, including
 * instance creation errors, configuration issues, and reflection problems.
 *
 * @since 1.0
 */
public class FactoryException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a FactoryException with the specified detail message.
   *
   * @param message the detail message
   */
  public FactoryException(String message) {
    super(message);
  }

  /**
   * Constructs a FactoryException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public FactoryException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a FactoryException with the specified cause.
   *
   * @param cause the cause of the exception
   */
  public FactoryException(Throwable cause) {
    super(cause);
  }
}
