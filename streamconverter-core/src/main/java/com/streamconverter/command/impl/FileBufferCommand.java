package com.streamconverter.command.impl;

import com.streamconverter.command.AbstractStreamCommand;
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
public class FileBufferCommand extends AbstractStreamCommand {

  private static final int BUFFER_SIZE = 64 * 1024;
  private static final int AES_KEY_BITS = 256;
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
    Thread shutdownHook = new Thread(() -> deleteSilently(tempFile), "FileBufferCommand-cleanup");

    try {
      registerShutdownHook(shutdownHook, tempFile);
      if (encrypted) {
        SecretKey key = generateAesKey();
        byte[] iv = generateIv();
        writeEncrypted(inputStream, tempFile, key, iv);
        readDecrypted(tempFile, outputStream, key);
      } else {
        write(inputStream, tempFile);
        read(tempFile, outputStream);
      }
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } finally {
        deregisterShutdownHook(shutdownHook);
      }
    }
  }

  private void write(InputStream inputStream, Path tempFile) throws IOException {
    try (OutputStream fileOut = Files.newOutputStream(tempFile)) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        fileOut.write(buffer, 0, bytesRead);
      }
    }
  }

  private void read(Path tempFile, OutputStream outputStream) throws IOException {
    try (InputStream fileIn = Files.newInputStream(tempFile)) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = fileIn.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
    }
  }

  private void writeEncrypted(InputStream inputStream, Path tempFile, SecretKey key, byte[] iv)
      throws IOException {
    try (OutputStream fileOut = Files.newOutputStream(tempFile)) {
      fileOut.write(iv);
      Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, key, iv);
      try (CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          cipherOut.write(buffer, 0, bytesRead);
        }
      }
    }
  }

  private void readDecrypted(Path tempFile, OutputStream outputStream, SecretKey key)
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
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = cipherIn.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }
      }
    }
  }

  private SecretKey generateAesKey() throws IOException {
    try {
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(AES_KEY_BITS, SECURE_RANDOM);
      return keyGen.generateKey();
    } catch (GeneralSecurityException e) {
      throw new IOException("Failed to generate AES key", e);
    }
  }

  private byte[] generateIv() {
    byte[] iv = new byte[GCM_IV_LENGTH];
    SECURE_RANDOM.nextBytes(iv);
    return iv;
  }

  private Cipher initCipher(int mode, SecretKey key, byte[] iv) throws IOException {
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(mode, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
      return cipher;
    } catch (GeneralSecurityException e) {
      throw new IOException("Encryption/decryption failed", e);
    }
  }

  private void registerShutdownHook(Thread shutdownHook, Path tempFile) {
    try {
      Runtime.getRuntime().addShutdownHook(shutdownHook);
    } catch (IllegalStateException | SecurityException e) {
      // JVM shutting down or security manager disallows hooks: fall back to deleteOnExit
      log.debug(
          "Could not register shutdown hook; falling back to deleteOnExit for {}", tempFile, e);
      tempFile.toFile().deleteOnExit();
    }
  }

  private void deregisterShutdownHook(Thread shutdownHook) {
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    } catch (IllegalStateException e) {
      log.debug("JVM is shutting down; could not deregister shutdown hook", e);
    } catch (SecurityException e) {
      log.debug("Security manager prevented shutdown hook deregistration", e);
    }
  }

  private void deleteSilently(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      log.warn("Failed to delete temporary file: {}", path, e);
    }
  }
}
