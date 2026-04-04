package com.streamconverter.examples;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.LineEndingNormalizeCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.impl.xml.XmlNavigateCommand;
import com.streamconverter.command.rule.IRule;
import com.streamconverter.command.rule.impl.casing.CamelToSnakeCaseRule;
import com.streamconverter.command.rule.impl.composite.ChainRule;
import com.streamconverter.command.rule.impl.string.LowerCaseRule;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 例2: Navigate系コマンド × IRule（CSV/JSON/XML）
 *
 * <p>StreamConverter の「特定フィールドに変換ルールを適用する」パターンを示す。
 *
 * <p><b>この例で学べること:</b>
 *
 * <ul>
 *   <li>{@link CSVPath#of(String)} / {@link TreePath#fromJson(String)} / {@link
 *       TreePath#fromXml(String)} による要素指定の方法
 *   <li>{@link IRule} はラムダ式（{@code s -> s.toUpperCase()}）でも実装できる
 *   <li>{@link IRule} をクラスで実装することで複雑な変換ロジックを表現できる
 *   <li>組み込み Rule（{@link TrimRule}, {@link LowerCaseRule}, {@link CamelToSnakeCaseRule}）の使い方
 *   <li>{@link ChainRule} で複数の Rule を連鎖させる方法
 *   <li>CSV/JSON/XML のいずれも同じ Navigate + Rule のパターンで処理できること
 * </ul>
 *
 * <p><b>シナリオ（CSV 3段パイプライン）:</b>
 *
 * <pre>
 * [コマンド1] CsvNavigateCommand(name列) + ChainRule(TrimRule → LowerCaseRule)
 *             商品名の前後空白を除去して小文字に統一
 *          ↓
 * [コマンド2] CsvNavigateCommand(price列) + カスタムIRule実装クラス（PriceFormattingRule）
 *             価格を "¥1,234" 形式にフォーマット
 *          ↓
 * [コマンド3] CsvNavigateCommand(category列) + ラムダIRule
 *             カテゴリを大文字に変換（ラムダで実装）
 * </pre>
 *
 * <p><b>シナリオ（JSON 3段パイプライン）:</b>
 *
 * <pre>
 * [コマンド1] JsonNavigateCommand($.name) + ChainRule(TrimRule → LowerCaseRule)
 * [コマンド2] JsonNavigateCommand($.category) + CamelToSnakeCaseRule（組み込みRuleの例）
 * [コマンド3] JsonNavigateCommand($.sku) + ラムダIRule（"SKU-" プレフィックス付与）
 * </pre>
 *
 * <p><b>シナリオ（XML 3段パイプライン）:</b>
 *
 * <pre>
 * [コマンド1] XmlNavigateCommand(product/name) + ChainRule(TrimRule → LowerCaseRule)
 * [コマンド2] XmlNavigateCommand(product/category) + CamelToSnakeCaseRule
 * [コマンド3] XmlNavigateCommand(product/sku) + ラムダIRule（"SKU-" プレフィックス付与）
 * </pre>
 */
public class NavigateAndRuleExample {

  private static final Logger log = LoggerFactory.getLogger(NavigateAndRuleExample.class);

  /**
   * @param args コマンドライン引数（未使用）
   * @throws IOException I/O エラー
   */
  public static void main(String[] args) throws IOException {
    log.info("=== 例2: Navigate系コマンド × IRule ===");

    csvPipeline();
    jsonPipeline();
    xmlPipeline();
  }

  // ---------------------------------------------------------------------------
  // CSV パイプライン
  // ---------------------------------------------------------------------------

  private static void csvPipeline() throws IOException {
    log.info("--- CSV パイプライン ---");

    String csv =
        "name,price,category\n"
            + "  Laptop Computer  ,128000,electronics\n"
            + "  Wireless Mouse  ,3200,accessories\n";

    log.info("入力 CSV:\n{}", csv);

    // コマンド1: 組み込み Rule を ChainRule で連鎖させる
    // ChainRule.of() に複数の IRule を渡すと、先頭から順番に適用される
    IRule trimAndLower = ChainRule.of(new TrimRule(), new LowerCaseRule());

    // コマンド2: IRule をクラスで実装した例
    // 複雑な変換ロジックはクラスで実装することで可読性・テスト性が高まる
    IRule priceFormatter = new PriceFormattingRule();

    // コマンド3: ラムダで IRule を実装した例
    // シンプルな変換であればラムダで十分
    IRule upperCase = s -> s.toUpperCase();

    // PriceFormattingRule が "¥128,000" を出力する（カンマを含む）。
    // CsvNavigateCommand は RFC 4180 に従い、カンマを含む値を二重引用符で囲む。
    String csvExpected =
        "name,price,category\r\nlaptop computer,\"¥128,000\",ELECTRONICS\r\nwireless mouse,\"¥3,200\",ACCESSORIES\r\n";
    log.info("期待値:\n{}", csvExpected);

    ByteArrayOutputStream csvOut = new ByteArrayOutputStream();
    StreamConverter.create(
            CsvNavigateCommand.create(CSVPath.of("name"), trimAndLower),
            CsvNavigateCommand.create(CSVPath.of("price"), priceFormatter),
            CsvNavigateCommand.create(CSVPath.of("category"), upperCase))
        .run(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), csvOut);
    log.info("出力:\n{}", csvOut.toString(StandardCharsets.UTF_8));
  }

  // ---------------------------------------------------------------------------
  // JSON パイプライン
  // ---------------------------------------------------------------------------

  private static void jsonPipeline() throws IOException {
    log.info("--- JSON パイプライン ---");

    String json =
        """
        {"name":"  Laptop Computer  ","category":"personalComputer","sku":"lp001"}
        """;

    log.info("入力 JSON:\n{}", json);

    // 組み込み Rule: ChainRule で TrimRule → LowerCaseRule
    IRule trimAndLower = ChainRule.of(new TrimRule(), new LowerCaseRule());

    // 組み込み Rule: CamelToSnakeCaseRule（camelCase → snake_case）
    // builder() で細かい設定も可能
    IRule camelToSnake = CamelToSnakeCaseRule.create();

    // ラムダ Rule: SKU にプレフィックスを付与
    IRule addSkuPrefix = s -> "SKU-" + s.toUpperCase();

    String jsonExpected =
        """
        {"name":"laptop computer","category":"personal_computer","sku":"SKU-LP001"}
        """;
    log.info("期待値:\n{}", jsonExpected);

    ByteArrayOutputStream jsonOut = new ByteArrayOutputStream();
    StreamConverter.create(
            JsonNavigateCommand.create(TreePath.fromJson("$.name"), trimAndLower),
            JsonNavigateCommand.create(TreePath.fromJson("$.category"), camelToSnake),
            JsonNavigateCommand.create(TreePath.fromJson("$.sku"), addSkuPrefix))
        .run(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), jsonOut);
    log.info("出力:\n{}", jsonOut.toString(StandardCharsets.UTF_8));
  }

  // ---------------------------------------------------------------------------
  // XML パイプライン
  // ---------------------------------------------------------------------------

  private static void xmlPipeline() throws IOException {
    log.info("--- XML パイプライン ---");
    // XmlNavigateCommand は対象フィールドを含む断片 XML を出力する。
    // そのため、複数フィールドを別々のコマンドで変換することはできない。
    // 代わりに、name フィールドに複数の Rule を段階的に適用する3段構成にする:
    //   コマンド1: product/name の前後空白をトリム → <name>Laptop Computer</name>
    //   コマンド2: name を小文字化              → <name>laptop computer</name>
    //   コマンド3: LineEndingNormalizeCommand   → 行末を LF に統一

    String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<product>\n"
            + "  <name>  Laptop Computer  </name>\n"
            + "  <category>personalComputer</category>\n"
            + "  <sku>lp001</sku>\n"
            + "</product>\n";

    log.info("入力 XML:\n{}", xml);

    String xmlExpected = "<name>laptop computer</name>\n";
    log.info("期待値（product/name フィールドのみ抽出・変換）:\n{}", xmlExpected);

    ByteArrayOutputStream xmlOut = new ByteArrayOutputStream();
    StreamConverter.create(
            XmlNavigateCommand.create(TreePath.fromXml("product/name"), new TrimRule()),
            XmlNavigateCommand.create(TreePath.fromXml("name"), new LowerCaseRule()),
            new LineEndingNormalizeCommand(LineEndingNormalizeCommand.LineEndingType.UNIX))
        .run(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), xmlOut);
    log.info("出力:\n{}", xmlOut.toString(StandardCharsets.UTF_8));
  }

  // ---------------------------------------------------------------------------
  // カスタム IRule 実装クラスの例
  // ---------------------------------------------------------------------------

  /**
   * 価格文字列を "¥1,234" 形式にフォーマットする Rule。
   *
   * <p>{@link IRule} はインターフェースなので、クラスで実装することで:
   *
   * <ul>
   *   <li>複雑な変換ロジックをメソッドに分割できる
   *   <li>単体テストを書きやすくなる
   *   <li>設定値をフィールドで保持できる
   * </ul>
   */
  static class PriceFormattingRule implements IRule {

    @Override
    public String apply(String input) {
      if (input == null || input.isBlank()) {
        return input;
      }
      try {
        long price = Long.parseLong(input.trim());
        return String.format("¥%,d", price);
      } catch (NumberFormatException e) {
        // 数値でない場合はそのまま返す
        return input;
      }
    }
  }
}
