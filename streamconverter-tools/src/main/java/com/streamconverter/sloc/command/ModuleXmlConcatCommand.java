package com.streamconverter.sloc.command;

import com.streamconverter.command.AbstractStreamCommand;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * モジュール名リストを読み込み、各モジュールの JaCoCo XML ファイルを連結して出力するコマンド。
 *
 * <p>入力: モジュール名を1行ずつ列挙したテキスト（プロジェクトルートからの相対パス解決に使用）
 *
 * <pre>
 * streamconverter-core
 * streamconverter-db
 * ...
 * </pre>
 *
 * <p>出力: 各モジュールの {@code <report>} 要素を連結したストリーム。XML宣言・DOCTYPE宣言は全モジュールで除去し、 先頭に {@code <?xml
 * version="1.0" encoding="UTF-8"?>} を1つだけ付与する。結果は複数のルート要素を持つ Well-formed ではない XML となるが、{@link
 * JacocoXmlToModuleSlocCommand} が {@code <jacoco-reports>} ラッパーで包んでパースするため実用上は問題ない。
 *
 * <p><b>YAGNI:</b> 厳密な Well-formed XML が必要になった場合は、ラッパー要素で包む対応が容易にできる。
 */
public class ModuleXmlConcatCommand extends AbstractStreamCommand {

  static final String JACOCO_XML_PATH = "build/reports/jacoco/test/jacocoTestReport.xml";

  private final Path projectRoot;

  /**
   * @param projectRoot JaCoCo XML を探すプロジェクトルートディレクトリ
   */
  public ModuleXmlConcatCommand(Path projectRoot) {
    this.projectRoot = projectRoot;
  }

  @Override
  public void execute(InputStream input, OutputStream output) throws IOException {
    Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
    try (var reader =
        new java.io.BufferedReader(new java.io.InputStreamReader(input, StandardCharsets.UTF_8))) {
      String moduleName;
      while ((moduleName = reader.readLine()) != null) {
        if (moduleName.isBlank()) {
          continue;
        }
        Path xmlPath = normalizedRoot.resolve(moduleName).resolve(JACOCO_XML_PATH).normalize();
        if (!xmlPath.startsWith(normalizedRoot)) {
          log.error(
              "Path traversal detected: resolved path '{}' escapes project root '{}' — aborting",
              xmlPath,
              normalizedRoot);
          throw new IOException("Path traversal detected: resolved path escapes project root");
        }
        if (!Files.exists(xmlPath)) {
          log.warn("report not found, skipping: {}", xmlPath);
          continue;
        }
        writeReportOnly(xmlPath, output);
        output.write('\n');
        output.flush();
      }
    }
  }

  /**
   * JaCoCo XML ファイルから XML宣言・DOCTYPE宣言を除去して {@code <report>} 要素のみ書き出す。
   *
   * <p>JaCoCo の出力は {@code <?xml...?><!DOCTYPE...><report...>} が1行に連結されているため、 バイト単位で {@code <}
   * を探しながら宣言部分をスキップする。
   */
  private void writeReportOnly(Path xmlPath, OutputStream output) throws IOException {
    try (InputStream in = new BufferedInputStream(Files.newInputStream(xmlPath))) {
      skipPrologues(in);
      in.transferTo(output);
    }
  }

  /** ストリーム先頭の {@code <?...?>} および {@code <!...>} を全てスキップし、最初の {@code <[a-zA-Z]} の直前まで読み進める。 */
  private void skipPrologues(InputStream in) throws IOException {
    while (true) {
      in.mark(2);
      int c1 = in.read();
      if (c1 != '<') {
        if (c1 != -1) in.reset();
        return;
      }
      int c2 = in.read();
      if (c2 == '?') {
        // <?...?> をスキップ
        skipUntil(in, '?', '>');
      } else if (c2 == '!') {
        // <!...> をスキップ
        skipUntil(in, '\0', '>');
      } else {
        // 通常の要素開始タグ — 巻き戻して返す
        in.reset();
        return;
      }
    }
  }

  /** {@code end} の直前が {@code pre}（'\0' は任意）になるまで読み飛ばす。 */
  private void skipUntil(InputStream in, char pre, char end) throws IOException {
    int prev = -1;
    int cur;
    while ((cur = in.read()) != -1) {
      if (cur == end && (pre == '\0' || prev == pre)) return;
      prev = cur;
    }
  }
}
