package com.streamconverter.command.impl;

import com.streamconverter.command.IStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pipeline command that buffers data through a temporary file between pipeline stages.
 *
 * <p>This command solves the problem where validation failures in a preceding stage can cause
 * invalid data to be written to the output stream. By buffering through a temporary file, the
 * upstream stage completes fully before the downstream stage begins, ensuring data integrity.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * StreamConverter.create(
 *     validateCmd,
 *     FileBufferCommand.create(),
 *     transformCmd
 * );
 * }</pre>
 *
 * <p>The encrypted variant ({@link #createEncrypted()}) uses AES-256-GCM to protect sensitive data
 * written to the temporary file.
 */
public class FileBufferCommand implements IStreamCommand {

  private static final Logger log = LoggerFactory.getLogger(FileBufferCommand.class);
  private static final int BUFFER_SIZE = 64 * 1024;

  private final boolean encrypted;

  private FileBufferCommand(boolean encrypted) {
    this.encrypted = encrypted;
  }

  /**
   * Creates a {@code FileBufferCommand} that buffers data through a plaintext temporary file.
   *
   * @return a new {@code FileBufferCommand} instance
   */
  public static FileBufferCommand create() {
    return new FileBufferCommand(false);
  }

  /**
   * Creates a {@code FileBufferCommand} that buffers data through an AES-256-GCM encrypted
   * temporary file.
   *
   * <p>The encryption key and IV are generated per execution and never persisted, providing
   * confidentiality for sensitive pipeline data.
   *
   * @return a new {@code FileBufferCommand} instance with encryption enabled
   */
  public static FileBufferCommand createEncrypted() {
    return new FileBufferCommand(true);
  }

  /**
   * Executes the buffering operation: writes the input stream to a temporary file, then reads the
   * temporary file to the output stream.
   *
   * @param inputStream the input stream to read data from
   * @param outputStream the output stream to write data to
   * @throws IOException if an I/O error occurs, or if decryption authentication fails
   */
  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    Path tempFile = Files.createTempFile("streamconverter-", ".tmp");
    Thread shutdownHook =
        new Thread(
            () -> TempFileLifecycle.deleteSilently(log, tempFile), "FileBufferCommand-cleanup");

    try {
      TempFileLifecycle.register(log, shutdownHook, tempFile);
      if (encrypted) {
        SecretKey key = EncryptionHelper.generateAesKey();
        byte[] iv = EncryptionHelper.generateIv();
        EncryptionHelper.writeEncrypted(inputStream, tempFile, key, iv, BUFFER_SIZE);
        EncryptionHelper.readDecrypted(tempFile, outputStream, key, BUFFER_SIZE);
      } else {
        writePlain(inputStream, tempFile);
        readPlain(tempFile, outputStream);
      }
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } finally {
        TempFileLifecycle.deregister(log, shutdownHook);
      }
    }
  }

  private void writePlain(InputStream inputStream, Path tempFile) throws IOException {
    try (OutputStream fileOut = Files.newOutputStream(tempFile)) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      // Drain the source stream into the temp file until EOF.
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        fileOut.write(buffer, 0, bytesRead);
      }
    }
  }

  private void readPlain(Path tempFile, OutputStream outputStream) throws IOException {
    try (InputStream fileIn = Files.newInputStream(tempFile)) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      // Replay the buffered file to the caller until EOF.
      while ((bytesRead = fileIn.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
    }
  }

  /** AES-256-GCM encryption and decryption of temp-file content. */
  private static final class EncryptionHelper {

    private static final int AES_KEY_BITS = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private EncryptionHelper() {}

    static SecretKey generateAesKey() throws IOException {
      try {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_BITS, SECURE_RANDOM);
        return keyGen.generateKey();
      } catch (GeneralSecurityException e) {
        throw new IOException("Failed to generate AES key", e);
      }
    }

    static byte[] generateIv() {
      byte[] iv = new byte[GCM_IV_LENGTH];
      SECURE_RANDOM.nextBytes(iv);
      return iv;
    }

    static void writeEncrypted(
        InputStream inputStream, Path tempFile, SecretKey key, byte[] iv, int bufferSize)
        throws IOException {
      try (OutputStream fileOut = Files.newOutputStream(tempFile)) {
        fileOut.write(iv);
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, key, iv);
        try (CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
          byte[] buffer = new byte[bufferSize];
          int bytesRead;
          // Encrypt streamed chunks into the temp file until EOF.
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            cipherOut.write(buffer, 0, bytesRead);
          }
        }
      }
    }

    static void readDecrypted(
        Path tempFile, OutputStream outputStream, SecretKey key, int bufferSize)
        throws IOException {
      try (InputStream fileIn = Files.newInputStream(tempFile)) {
        byte[] storedIv = new byte[GCM_IV_LENGTH];
        int totalRead = 0;
        while (totalRead < GCM_IV_LENGTH) {
          int bytesRead = fileIn.read(storedIv, totalRead, GCM_IV_LENGTH - totalRead);
          if (bytesRead == -1) {
            throw new IOException("Unexpected end of encrypted file while reading IV");
          }
          totalRead += bytesRead;
        }
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE, key, storedIv);
        try (CipherInputStream cipherIn = new CipherInputStream(fileIn, cipher)) {
          byte[] buffer = new byte[bufferSize];
          int bytesRead;
          // Decrypt streamed chunks from the temp file until EOF.
          while ((bytesRead = cipherIn.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
          }
        }
      }
    }

    private static Cipher initCipher(int mode, SecretKey key, byte[] iv) throws IOException {
      try {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(mode, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher;
      } catch (GeneralSecurityException e) {
        throw new IOException("Encryption/decryption failed", e);
      }
    }
  }

  /** Manages the shutdown-hook that deletes the temp file if the JVM exits unexpectedly. */
  private static final class TempFileLifecycle {

    private TempFileLifecycle() {}

    static void register(Logger logger, Thread shutdownHook, Path tempFile) {
      try {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
      } catch (IllegalStateException | SecurityException e) {
        // JVM shutting down or security manager disallows hooks: fall back to deleteOnExit
        logger.debug(
            "Could not register shutdown hook; falling back to deleteOnExit for {}", tempFile, e);
        tempFile.toFile().deleteOnExit();
      }
    }

    static void deregister(Logger logger, Thread shutdownHook) {
      try {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      } catch (IllegalStateException e) {
        logger.debug("JVM is shutting down; could not deregister shutdown hook", e);
      } catch (SecurityException e) {
        logger.debug("Security manager prevented shutdown hook deregistration", e);
      }
    }

    static void deleteSilently(Logger logger, Path path) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException e) {
        logger.warn("Failed to delete temporary file: {}", path, e);
      }
    }
  }
}
