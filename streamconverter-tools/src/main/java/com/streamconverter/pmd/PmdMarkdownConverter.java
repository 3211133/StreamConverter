package com.streamconverter.pmd;

import com.streamconverter.StreamConverter;
import com.streamconverter.pmd.command.PmdXmlToMarkdownCommand;
import com.streamconverter.pmd.command.PmdXmlToViolationsCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/** PMD XML レポートを Markdown に変換する CLI ツール。 */
public class PmdMarkdownConverter {

  /**
   * CLI エントリポイント。PMD XML を Markdown レポートに変換して出力ファイルに書き込む。
   *
   * @param args args[0]: PMD XML ファイルパス、args[1]: 出力 Markdown ファイルパス
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Usage: PmdMarkdownConverter <pmd-xml> <output.md>");
      System.exit(1);
    }

    StreamConverter converter =
        StreamConverter.create(new PmdXmlToViolationsCommand(), new PmdXmlToMarkdownCommand());

    try (InputStream in = Files.newInputStream(Paths.get(args[0]));
        OutputStream out = Files.newOutputStream(Paths.get(args[1]))) {
      converter.run(in, out);
    }
  }
}
