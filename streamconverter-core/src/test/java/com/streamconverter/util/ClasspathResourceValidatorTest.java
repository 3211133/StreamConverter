package com.streamconverter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.net.URL;
import org.junit.jupiter.api.Test;

class ClasspathResourceValidatorTest {

  @Test
  void getResourceAsStream_正常なリソースパス() throws Exception {
    InputStream stream = ClasspathResourceValidator.getResourceAsStream("logback-test.xml");
    assertNotNull(stream);
    stream.close();
  }

  @Test
  void getResourceAsStream_サブディレクトリを含むパス() throws Exception {
    InputStream stream = ClasspathResourceValidator.getResourceAsStream("schemas/test.xsd");
    assertNotNull(stream);
    stream.close();
  }

  @Test
  void getResourceAsStream_先頭スラッシュは自動的に除去される() throws Exception {
    // 先頭スラッシュありでも正常に取得できる
    InputStream stream = ClasspathResourceValidator.getResourceAsStream("/logback-test.xml");
    assertNotNull(stream);
    stream.close();
  }

  @Test
  void getResourceAsStream_先頭スラッシュのみの場合は空文字列として扱われる() {
    // 先頭スラッシュを除去すると空文字列になるため、例外が発生する
    assertThrows(
        IllegalArgumentException.class, () -> ClasspathResourceValidator.getResourceAsStream("/"));
  }

  @Test
  void getResourceAsStream_null入力でNullPointerException() {
    assertThrows(
        NullPointerException.class, () -> ClasspathResourceValidator.getResourceAsStream(null));
  }

  @Test
  void getResourceAsStream_空文字列入力でIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> ClasspathResourceValidator.getResourceAsStream(""));
  }

  @Test
  void getResourceAsStream_存在しないリソースでIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ClasspathResourceValidator.getResourceAsStream("nonexistent.txt"));
  }

  @Test
  void getResourceAsStream_パストラバーサルは自動的に拒否される() {
    // ClassLoaderが自動的にnullを返す
    assertThrows(
        IllegalArgumentException.class,
        () -> ClasspathResourceValidator.getResourceAsStream("../etc/passwd"));
  }

  @Test
  void getResourceAsStream_複数のパストラバーサルも拒否される() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ClasspathResourceValidator.getResourceAsStream("../../etc/passwd"));
  }

  @Test
  void getResourceAsStream_バックスラッシュは自動的に拒否される() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ClasspathResourceValidator.getResourceAsStream("schemas\\test.xsd"));
  }

  @Test
  void getResourceAsStream_非ASCII文字のリソースパス() throws Exception {
    // 日本語ディレクトリとファイル名でも正常に取得できる
    InputStream stream = ClasspathResourceValidator.getResourceAsStream("日本語/ファイル.txt");
    assertNotNull(stream);
    stream.close();
  }

  @Test
  void getResourceUrl_正常なリソースパス() {
    URL url = ClasspathResourceValidator.getResourceUrl("logback-test.xml");
    assertNotNull(url);
    assertTrue(url.toString().contains("logback-test.xml"));
  }

  @Test
  void getResourceUrl_サブディレクトリを含むパス() {
    URL url = ClasspathResourceValidator.getResourceUrl("schemas/test.xsd");
    assertNotNull(url);
    assertTrue(url.toString().contains("schemas/test.xsd"));
  }

  @Test
  void getResourceUrl_先頭スラッシュは自動的に除去される() {
    // 先頭スラッシュありでも正常に取得できる
    URL url = ClasspathResourceValidator.getResourceUrl("/logback-test.xml");
    assertNotNull(url);
    assertTrue(url.toString().contains("logback-test.xml"));
  }

  @Test
  void getResourceUrl_先頭スラッシュのみの場合は空文字列として扱われる() {
    // 先頭スラッシュを除去すると空文字列になるため、例外が発生する
    assertThrows(
        IllegalArgumentException.class, () -> ClasspathResourceValidator.getResourceUrl("/"));
  }

  @Test
  void getResourceUrl_null入力でNullPointerException() {
    assertThrows(NullPointerException.class, () -> ClasspathResourceValidator.getResourceUrl(null));
  }

  @Test
  void getResourceUrl_空文字列入力でIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class, () -> ClasspathResourceValidator.getResourceUrl(""));
  }

  @Test
  void getResourceUrl_存在しないリソースでIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ClasspathResourceValidator.getResourceUrl("nonexistent.txt"));
  }

  @Test
  void getResourceUrl_パストラバーサルは自動的に拒否される() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ClasspathResourceValidator.getResourceUrl("../etc/passwd"));
  }

  @Test
  void getResourceUrl_非ASCII文字のリソースパス() {
    // 日本語ディレクトリとファイル名でも正常に取得できる
    URL url = ClasspathResourceValidator.getResourceUrl("日本語/ファイル.txt");
    assertNotNull(url);
    // URLはエンコードされる（小文字）
    assertTrue(
        url.toString().contains("日本語") || url.toString().contains("%e6%97%a5%e6%9c%ac%e8%aa%9e"));
  }

  @Test
  void ユーティリティクラスのインスタンス化を防止() throws Exception {
    var constructor = ClasspathResourceValidator.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    var exception =
        assertThrows(
            java.lang.reflect.InvocationTargetException.class, () -> constructor.newInstance());
    assertTrue(exception.getCause() instanceof UnsupportedOperationException);
  }
}
