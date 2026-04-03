package com.streamconverter.tools;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.analysis.JacocoXmlToModuleSlocCommand;
import com.streamconverter.command.impl.analysis.ModuleXmlConcatCommand;
import com.streamconverter.command.impl.analysis.SlocAggregateCommand;
import com.streamconverter.command.impl.analysis.SlocReportFormatCommand;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * 全モジュールの JaCoCo XML レポートから SLOC（実ステップ数）を集計するツール。
 *
 * <p>モジュール名リストを起点に4コマンドのパイプラインで処理する：
 *
 * <ol>
 *   <li>{@link ModuleXmlConcatCommand} — モジュール名→XML連結ストリーム
 *   <li>{@link JacocoXmlToLineCounterCsvCommand} — XML→CSV行
 *   <li>{@link SlocCsvAggregateCommand} — CSV集約・合計行追加
 *   <li>{@link SlocReportFormatCommand} — 整形出力
 * </ol>
 *
 * <p><strong>使用方法:</strong>
 *
 * <pre>
 * ./gradlew :streamconverter-tools:slocCount
 * </pre>
 *
 * <p>モジュール名は Gradle タスクから引数として渡される。直接実行する場合は引数にモジュール名を列挙する：
 *
 * <pre>
 * java SlocCounter streamconverter-core streamconverter-db ...
 * </pre>
 */
public class SlocCounter {

  /**
   * 指定されたモジュールの SLOC を集計し、結果を出力する。
   *
   * @param projectRoot プロジェクトルートディレクトリ
   * @param modules 集計対象のモジュール名リスト
   * @param output 集計結果の出力先
   * @throws IOException レポートの読み込みまたは出力に失敗した場合
   */
  public void run(Path projectRoot, List<String> modules, OutputStream output) throws IOException {
    byte[] moduleList = String.join("\n", modules).getBytes(StandardCharsets.UTF_8);
    StreamConverter.create(
            new ModuleXmlConcatCommand(projectRoot),
            new JacocoXmlToModuleSlocCommand(),
            new SlocAggregateCommand(),
            new SlocReportFormatCommand())
        .run(new ByteArrayInputStream(moduleList), output);
  }

  /** CLI エントリポイント。引数にモジュール名を列挙する。プロジェクトルートはカレントディレクトリを使用する。 */
  public static void main(String[] args) throws IOException {
    new SlocCounter().run(Path.of("."), Arrays.asList(args), System.out);
  }
}
