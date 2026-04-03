package com.streamconverter.command.impl.analysis;

import com.streamconverter.command.AbstractStreamCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
 * <p>出力: 各 XML の前にモジュール名ヘッダーを付加した連結ストリーム
 *
 * <pre>
 * #module:streamconverter-core
 * &lt;?xml ...&gt;...&lt;/report&gt;
 * #module:streamconverter-db
 * &lt;?xml ...&gt;...&lt;/report&gt;
 * ...
 * </pre>
 */
public class ModuleXmlConcatCommand extends AbstractStreamCommand {

  static final String MODULE_HEADER_PREFIX = "#module:";
  private static final String JACOCO_XML_PATH = "build/reports/jacoco/test/jacocoTestReport.xml";

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
    PrintWriter headerWriter =
        new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
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
        headerWriter.println(MODULE_HEADER_PREFIX + moduleName);
        headerWriter.flush();
        Files.copy(xmlPath, output);
        output.write('\n');
        output.flush();
      }
    }
  }
}
