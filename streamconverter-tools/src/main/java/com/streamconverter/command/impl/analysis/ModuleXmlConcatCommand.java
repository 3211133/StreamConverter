package com.streamconverter.command.impl.analysis;

import com.streamconverter.command.AbstractStreamCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
 * <p>出力: 各モジュールの JaCoCo XML をそのまま連結したストリーム。XML 宣言（{@code <?xml ...?>}）は
 * 最初のモジュール以外では除去して連結する。結果は複数のルート要素を持つ Well-formed ではない XML となるが、 {@link
 * JacocoXmlToModuleSlocCommand} が使用する {@link javax.xml.stream.XMLStreamReader}
 * は複数ルート要素を順に読み進めることができるため実用上は問題ない。
 *
 * <p><b>YAGNI:</b> 厳密な Well-formed XML が必要になった場合は、ラッパー要素（例: {@code <jacoco-reports>}）で包む対応が容易にできる。
 */
public class ModuleXmlConcatCommand extends AbstractStreamCommand {

  private static final String JACOCO_XML_PATH = "build/reports/jacoco/test/jacocoTestReport.xml";
  private static final String XML_DECLARATION_PREFIX = "<?xml";

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
    boolean first = true;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      String moduleName;
      while ((moduleName = reader.readLine()) != null) {
        if (moduleName.isBlank()) {
          continue;
        }
        Path xmlPath = normalizedRoot.resolve(moduleName).resolve(JACOCO_XML_PATH).normalize();
        if (!xmlPath.startsWith(normalizedRoot)) {
          log.warn("path traversal detected, skipping: {}", moduleName);
          continue;
        }
        if (!Files.exists(xmlPath)) {
          log.warn("report not found, skipping: {}", xmlPath);
          continue;
        }
        if (first) {
          Files.copy(xmlPath, output);
          first = false;
        } else {
          writeWithoutXmlDeclaration(xmlPath, output);
        }
        output.write('\n');
        output.flush();
      }
    }
  }

  private void writeWithoutXmlDeclaration(Path xmlPath, OutputStream output) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(
            new java.io.InputStreamReader(Files.newInputStream(xmlPath), StandardCharsets.UTF_8))) {
      String line;
      boolean skippedDeclaration = false;
      while ((line = reader.readLine()) != null) {
        if (!skippedDeclaration && line.stripLeading().startsWith(XML_DECLARATION_PREFIX)) {
          skippedDeclaration = true;
          continue;
        }
        output.write(line.getBytes(StandardCharsets.UTF_8));
        output.write('\n');
      }
    }
  }
}
