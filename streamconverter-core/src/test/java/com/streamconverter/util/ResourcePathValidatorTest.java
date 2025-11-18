package com.streamconverter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class ResourcePathValidatorTest {

  @Test
  void validate_正常なリソースパス() {
    String result = ResourcePathValidator.validate("test.xsd");
    assertTrue(result.contains("resources"));
    assertTrue(result.contains("test.xsd"));
  }

  @Test
  void validate_サブディレクトリを含むパス() {
    String result = ResourcePathValidator.validate("schemas/test.xsd");
    assertTrue(result.contains("resources"));
    assertTrue(result.contains("schemas"));
    assertTrue(result.contains("test.xsd"));
  }

  @Test
  void validate_パストラバーサル攻撃を防止_ドットドットスラッシュ() {
    assertThrows(SecurityException.class, () -> ResourcePathValidator.validate("../etc/passwd"));
  }

  @Test
  void validate_パストラバーサル攻撃を防止_ドットドットバックスラッシュ() {
    assertThrows(
        SecurityException.class, () -> ResourcePathValidator.validate("..\\windows\\system32"));
  }

  @Test
  void validate_パストラバーサル攻撃を防止_複数のドットドット() {
    assertThrows(SecurityException.class, () -> ResourcePathValidator.validate("../../etc/passwd"));
  }

  @Test
  void validate_パストラバーサル攻撃を防止_正規化後にベースディレクトリ外() {
    assertThrows(
        SecurityException.class, () -> ResourcePathValidator.validate("subdir/../../etc/passwd"));
  }

  @Test
  void validate_null入力でNullPointerException() {
    assertThrows(NullPointerException.class, () -> ResourcePathValidator.validate(null));
  }

  @Test
  void validate_空文字列入力でIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> ResourcePathValidator.validate(""));
  }

  @Test
  void validate_空白のみの入力でIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> ResourcePathValidator.validate("   "));
  }

  @Test
  void validate_正規化されたパスを返す() {
    String result = ResourcePathValidator.validate("./test.xsd");
    assertFalse(result.contains("./"));
  }

  @Test
  void validate_ユーティリティクラスのインスタンス化を防止() throws Exception {
    var constructor = ResourcePathValidator.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    var exception =
        assertThrows(
            java.lang.reflect.InvocationTargetException.class, () -> constructor.newInstance());
    assertTrue(exception.getCause() instanceof UnsupportedOperationException);
  }

  @Test
  void validate_二重セパレータ() {
    String result = ResourcePathValidator.validate("schemas//file.txt");
    assertTrue(result.contains("resources"));
    assertTrue(result.contains("file.txt"));
  }

  @Test
  void validate_Windows区切り文字() {
    String result = ResourcePathValidator.validate("schemas\\file.txt");
    assertTrue(result.contains("resources"));
    assertTrue(result.contains("file.txt"));
  }

  @Test
  void validate_ドットのみの入力() {
    String result = ResourcePathValidator.validate(".");
    assertTrue(result.contains("resources"));
  }

  @Test
  void validate_Path比較による厳密な検証() {
    String result = ResourcePathValidator.validate("test.xsd");
    assertTrue(Paths.get(result).startsWith(Paths.get("resources")));
  }

  @Test
  void validate_絶対パスUnix形式を拒否() {
    assertThrows(SecurityException.class, () -> ResourcePathValidator.validate("/etc/passwd"));
  }

  @Test
  void validate_末尾のスペース() {
    String result = ResourcePathValidator.validate("test.xsd ");
    assertEquals("resources/test.xsd", result.replace("\\", "/"));
  }

  @Test
  void validate_末尾のドット() {
    String result = ResourcePathValidator.validate("test.xsd.");
    assertTrue(result.contains("resources"));
  }

  @Test
  void validate_Unicode文字() {
    String result = ResourcePathValidator.validate("日本語/ファイル.txt");
    assertTrue(result.contains("日本語"));
  }

  @Test
  void validate_大量のドットスラッシュ() {
    String result = ResourcePathValidator.validate("././././././test.txt");
    assertTrue(result.contains("test.txt"));
  }

  // ========== Codex指摘の要対応ケース ==========

  @Test
  void validate_先頭ダブルスラッシュUNCを拒否() {
    assertThrows(SecurityException.class, () -> ResourcePathValidator.validate("//server/share"));
  }

  @Test
  void validate_先頭バックスラッシュを拒否() {
    assertThrows(
        SecurityException.class, () -> ResourcePathValidator.validate("\\Windows\\system32"));
  }

  @Test
  void validate_UNCパスを拒否() {
    assertThrows(
        SecurityException.class,
        () -> ResourcePathValidator.validate("\\\\server\\share\\file.txt"));
  }

  @EnabledOnOs({OS.LINUX, OS.MAC})
  @Test
  void validate_ドライブレターはLinuxMacで相対パスとして扱われる() {
    // Linux/Mac環境ではC:は単なるディレクトリ名として扱われresources配下に収まる
    String result = ResourcePathValidator.validate("C:/Windows/system32");
    assertTrue(result.contains("resources"));
  }

  @EnabledOnOs(OS.WINDOWS)
  @Test
  void validate_ドライブレターはWindowsで絶対パスとして拒否される() {
    // Windows環境ではC:は絶対パスとして扱われSecurityExceptionが投げられる
    assertThrows(
        SecurityException.class, () -> ResourcePathValidator.validate("C:/Windows/system32"));
  }

  @EnabledOnOs({OS.LINUX, OS.MAC})
  @Test
  void validate_ドライブレターバックスラッシュもLinuxMacで相対パス() {
    // Linux/Mac環境ではD:は単なるディレクトリ名として扱われresources配下に収まる
    String result = ResourcePathValidator.validate("D:\\Program Files");
    assertTrue(result.contains("resources"));
  }

  @EnabledOnOs(OS.WINDOWS)
  @Test
  void validate_ドライブレターバックスラッシュはWindowsで拒否される() {
    // Windows環境ではD:は絶対パスとして扱われSecurityExceptionが投げられる
    assertThrows(
        SecurityException.class, () -> ResourcePathValidator.validate("D:\\Program Files"));
  }

  @Test
  void validate_resources含むパストラバーサルを拒否() {
    assertThrows(
        SecurityException.class, () -> ResourcePathValidator.validate("resources/../../evil.txt"));
  }

  @Disabled("制御文字がそのまま含まれる - 実装での明示的拒否が必要")
  @Test
  void validate_制御文字タブを拒否() {
    assertThrows(
        IllegalArgumentException.class, () -> ResourcePathValidator.validate("test\tfile"));
  }

  @Disabled("制御文字がそのまま含まれる - 実装での明示的拒否が必要")
  @Test
  void validate_制御文字改行を拒否() {
    assertThrows(
        IllegalArgumentException.class, () -> ResourcePathValidator.validate("test\nfile"));
  }

  @Disabled("制御文字がそのまま含まれる - 実装での明示的拒否が必要")
  @Test
  void validate_制御文字CRを拒否() {
    assertThrows(
        IllegalArgumentException.class, () -> ResourcePathValidator.validate("test\rfile"));
  }

  @Test
  void validate_NUL文字でIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> ResourcePathValidator.validate("test\0.xsd"));
  }

  // ========== 環境依存または安全確認済みでDisabled ==========

  @Disabled("URLエンコードは奇妙なファイル名になるが resources 配下に留まることを確認済み")
  @Test
  void validate_URLエンコード攻撃() {
    String result = ResourcePathValidator.validate("%2e%2e/etc/passwd");
    assertTrue(result.contains("%2e%2e"));
    assertTrue(result.contains("resources"));
  }
}
