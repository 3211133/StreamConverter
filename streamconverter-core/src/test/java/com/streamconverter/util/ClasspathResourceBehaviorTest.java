package com.streamconverter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.net.URL;
import org.junit.jupiter.api.Test;

/**
 * クラスパスリソース取得の挙動を確認するテストクラス
 *
 * <p>様々なパスパターンでの動作を検証
 */
class ClasspathResourceBehaviorTest {

  @Test
  void test_正常なリソースパス() {
    printResourceInfo("schemas/test.xsd");
  }

  @Test
  void test_先頭スラッシュあり() {
    printResourceInfo("/schemas/test.xsd");
  }

  @Test
  void test_パストラバーサル_ドットドット() {
    printResourceInfo("../schemas/test.xsd");
  }

  @Test
  void test_パストラバーサル_複数() {
    printResourceInfo("../../etc/passwd");
  }

  @Test
  void test_絶対パス_Unix() {
    printResourceInfo("/etc/passwd");
  }

  @Test
  void test_カレントディレクトリ() {
    printResourceInfo("./schemas/test.xsd");
  }

  @Test
  void test_バックスラッシュ() {
    printResourceInfo("schemas\\test.xsd");
  }

  @Test
  void test_META_INFアクセス() {
    printResourceInfo("../META-INF/MANIFEST.MF");
  }

  @Test
  void test_クラスファイルアクセス試行() {
    printResourceInfo("../../com/streamconverter/util/ResourcePathValidator.class");
  }

  @Test
  void test_空文字列() {
    printResourceInfo("");
  }

  @Test
  void test_ドットのみ() {
    printResourceInfo(".");
  }

  @Test
  void test_スラッシュのみ() {
    printResourceInfo("/");
  }

  @Test
  void test_存在するファイル() {
    // logback-test.xml や application.properties など実在するリソースで試す
    printResourceInfo("logback-test.xml");
  }

  private void printResourceInfo(String path) {
    System.out.println("\n========================================");
    System.out.println("Testing path: [" + path + "]");
    System.out.println("========================================");

    // ClassLoader.getResource()
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      URL resourceUrl = classLoader.getResource(path);
      System.out.println("getResource() result: " + resourceUrl);
      if (resourceUrl != null) {
        System.out.println("  Protocol: " + resourceUrl.getProtocol());
        System.out.println("  Path: " + resourceUrl.getPath());
        System.out.println("  File: " + resourceUrl.getFile());
      }
    } catch (Exception e) {
      System.out.println("getResource() threw: " + e.getClass().getName() + ": " + e.getMessage());
    }

    // ClassLoader.getResourceAsStream()
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      InputStream stream = classLoader.getResourceAsStream(path);
      System.out.println(
          "getResourceAsStream() result: " + (stream != null ? "InputStream" : "null"));
      if (stream != null) {
        stream.close();
      }
    } catch (Exception e) {
      System.out.println(
          "getResourceAsStream() threw: " + e.getClass().getName() + ": " + e.getMessage());
    }

    // Class.getResource() (先頭スラッシュありとなしで挙動が変わる)
    try {
      URL resourceUrl = getClass().getResource(path);
      System.out.println("Class.getResource() result: " + resourceUrl);
    } catch (Exception e) {
      System.out.println(
          "Class.getResource() threw: " + e.getClass().getName() + ": " + e.getMessage());
    }

    // Class.getResourceAsStream()
    try {
      InputStream stream = getClass().getResourceAsStream(path);
      System.out.println(
          "Class.getResourceAsStream() result: " + (stream != null ? "InputStream" : "null"));
      if (stream != null) {
        stream.close();
      }
    } catch (Exception e) {
      System.out.println(
          "Class.getResourceAsStream() threw: " + e.getClass().getName() + ": " + e.getMessage());
    }
  }

  @Test
  void test_実際のクラスパス構造を確認() {
    System.out.println("\n========================================");
    System.out.println("Classpath structure check");
    System.out.println("========================================");

    // カレントクラスの場所を確認
    URL classLocation = getClass().getProtectionDomain().getCodeSource().getLocation();
    System.out.println("This class is loaded from: " + classLocation);

    // ClassLoaderのリソースルートを確認
    URL root = getClass().getClassLoader().getResource("");
    System.out.println("ClassLoader resource root: " + root);

    // 実在するリソースを探す
    String[] testPaths = {
      "logback-test.xml", "logback.xml", "application.properties", "META-INF/MANIFEST.MF"
    };

    for (String testPath : testPaths) {
      URL resource = getClass().getClassLoader().getResource(testPath);
      System.out.println("  " + testPath + " -> " + resource);
    }
  }
}
