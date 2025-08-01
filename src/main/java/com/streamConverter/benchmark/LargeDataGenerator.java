package com.streamConverter.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Random;

/**
 * 大容量テストデータを生成するユーティリティクラス
 *
 * <p>XML、JSON、CSVフォーマットで大容量データファイルを生成し、 5GBデータ/50MBメモリ目標のベンチマークテストをサポートします。
 */
public class LargeDataGenerator {

  private static final String[] SAMPLE_NAMES = {
    "田中太郎", "佐藤花子", "鈴木一郎", "高橋美咲", "渡辺健太",
    "伊藤由美", "山田隆", "中村恵子", "小林誠", "加藤真理子"
  };

  private static final String[] SAMPLE_CITIES = {
    "東京", "大阪", "名古屋", "札幌", "福岡", "仙台", "広島", "京都", "神戸", "横浜"
  };

  private static final String[] SAMPLE_PRODUCTS = {
    "スマートフォン", "ノートパソコン", "タブレット", "イヤホン", "キーボード",
    "マウス", "モニター", "プリンター", "カメラ", "充電器"
  };

  /**
   * 大容量XMLファイルを生成します
   *
   * @param targetSizeBytes 目標ファイルサイズ（バイト）
   * @return 生成されたファイルのPath
   * @throws IOException ファイル作成に失敗した場合
   */
  public static Path generateLargeXmlFile(long targetSizeBytes) throws IOException {
    Path tempFile = Files.createTempFile("large-test", ".xml");
    Random random = new Random(42); // 再現可能な結果のため固定シード

    try (Writer writer = Files.newBufferedWriter(tempFile)) {
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      writer.write("<orders>\n");

      long currentSize = 100; // ヘッダー分
      int recordCount = 0;

      while (currentSize < targetSizeBytes) {
        String record = generateXmlRecord(recordCount++, random);
        writer.write(record);
        currentSize += record.getBytes("UTF-8").length;

        // 進捗表示（大きなファイルの場合）
        if (recordCount % 100000 == 0 && targetSizeBytes > 100 * 1024 * 1024) {
          System.out.printf(
              "Generated %d records, %.2f MB\n", recordCount, currentSize / 1024.0 / 1024.0);
        }
      }

      writer.write("</orders>\n");
    }

    return tempFile;
  }

  /**
   * 大容量JSONファイルを生成します
   *
   * @param targetSizeBytes 目標ファイルサイズ（バイト）
   * @return 生成されたファイルのPath
   * @throws IOException ファイル作成に失敗した場合
   */
  public static Path generateLargeJsonFile(long targetSizeBytes) throws IOException {
    Path tempFile = Files.createTempFile("large-test", ".json");
    Random random = new Random(42);

    try (Writer writer = Files.newBufferedWriter(tempFile)) {
      writer.write("{\n  \"orders\": [\n");

      long currentSize = 20; // ヘッダー分
      int recordCount = 0;

      while (currentSize < targetSizeBytes - 100) { // 終了分を考慮
        if (recordCount > 0) {
          writer.write(",\n");
          currentSize += 2;
        }

        String record = generateJsonRecord(recordCount++, random);
        writer.write(record);
        currentSize += record.getBytes("UTF-8").length;

        if (recordCount % 100000 == 0 && targetSizeBytes > 100 * 1024 * 1024) {
          System.out.printf(
              "Generated %d records, %.2f MB\n", recordCount, currentSize / 1024.0 / 1024.0);
        }
      }

      writer.write("\n  ]\n}\n");
    }

    return tempFile;
  }

  /**
   * 大容量CSVファイルを生成します
   *
   * @param targetSizeBytes 目標ファイルサイズ（バイト）
   * @return 生成されたファイルのPath
   * @throws IOException ファイル作成に失敗した場合
   */
  public static Path generateLargeCsvFile(long targetSizeBytes) throws IOException {
    Path tempFile = Files.createTempFile("large-test", ".csv");
    Random random = new Random(42);

    try (Writer writer = Files.newBufferedWriter(tempFile)) {
      // CSVヘッダー
      String header = "id,name,city,product,quantity,price,timestamp\n";
      writer.write(header);
      long currentSize = header.getBytes("UTF-8").length;

      int recordCount = 0;

      while (currentSize < targetSizeBytes) {
        String record = generateCsvRecord(recordCount++, random);
        writer.write(record);
        currentSize += record.getBytes("UTF-8").length;

        if (recordCount % 100000 == 0 && targetSizeBytes > 100 * 1024 * 1024) {
          System.out.printf(
              "Generated %d records, %.2f MB\n", recordCount, currentSize / 1024.0 / 1024.0);
        }
      }
    }

    return tempFile;
  }

  /** XMLレコードを生成 */
  private static String generateXmlRecord(int id, Random random) {
    String name = SAMPLE_NAMES[random.nextInt(SAMPLE_NAMES.length)];
    String city = SAMPLE_CITIES[random.nextInt(SAMPLE_CITIES.length)];
    String product = SAMPLE_PRODUCTS[random.nextInt(SAMPLE_PRODUCTS.length)];
    int quantity = random.nextInt(10) + 1;
    double price = Math.round((random.nextDouble() * 10000 + 1000) * 100) / 100.0;

    return String.format(
        "  <order id=\"%d\">\n"
            + "    <customer>\n"
            + "      <name>%s</name>\n"
            + "      <city>%s</city>\n"
            + "    </customer>\n"
            + "    <product>%s</product>\n"
            + "    <quantity>%d</quantity>\n"
            + "    <price>%.2f</price>\n"
            + "    <timestamp>%s</timestamp>\n"
            + "    <description>%s</description>\n"
            + "  </order>\n",
        id,
        escapeXml(name),
        escapeXml(city),
        escapeXml(product),
        quantity,
        price,
        Instant.now(),
        generateDescription(product, random));
  }

  /** JSONレコードを生成 */
  private static String generateJsonRecord(int id, Random random) {
    String name = SAMPLE_NAMES[random.nextInt(SAMPLE_NAMES.length)];
    String city = SAMPLE_CITIES[random.nextInt(SAMPLE_CITIES.length)];
    String product = SAMPLE_PRODUCTS[random.nextInt(SAMPLE_PRODUCTS.length)];
    int quantity = random.nextInt(10) + 1;
    double price = Math.round((random.nextDouble() * 10000 + 1000) * 100) / 100.0;

    return String.format(
        "    {\n"
            + "      \"id\": %d,\n"
            + "      \"customer\": {\n"
            + "        \"name\": \"%s\",\n"
            + "        \"city\": \"%s\"\n"
            + "      },\n"
            + "      \"product\": \"%s\",\n"
            + "      \"quantity\": %d,\n"
            + "      \"price\": %.2f,\n"
            + "      \"timestamp\": \"%s\",\n"
            + "      \"description\": \"%s\"\n"
            + "    }",
        id,
        name,
        city,
        product,
        quantity,
        price,
        Instant.now(),
        generateDescription(product, random));
  }

  /** CSVレコードを生成 */
  private static String generateCsvRecord(int id, Random random) {
    String name = SAMPLE_NAMES[random.nextInt(SAMPLE_NAMES.length)];
    String city = SAMPLE_CITIES[random.nextInt(SAMPLE_CITIES.length)];
    String product = SAMPLE_PRODUCTS[random.nextInt(SAMPLE_PRODUCTS.length)];
    int quantity = random.nextInt(10) + 1;
    double price = Math.round((random.nextDouble() * 10000 + 1000) * 100) / 100.0;

    return String.format(
        "%d,\"%s\",\"%s\",\"%s\",%d,%.2f,\"%s\"\n",
        id, name, city, product, quantity, price, Instant.now());
  }

  /** 商品の説明文を生成（データボリューム増加のため） */
  private static String generateDescription(String product, Random random) {
    String[] adjectives = {"高品質な", "人気の", "最新の", "おすすめの", "限定の"};
    String[] features = {"機能性", "デザイン", "性能", "品質", "価格"};

    String adj = adjectives[random.nextInt(adjectives.length)];
    String feature = features[random.nextInt(features.length)];

    return escapeXml(adj + product + "で、" + feature + "に優れた商品です。多くのお客様にご愛用いただいております。");
  }

  /** XML用のエスケープ処理 */
  private static String escapeXml(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  /**
   * メモリ効率的な大容量データInputStreamを作成
   *
   * @param format データフォーマット（"XML", "JSON", "CSV"）
   * @param targetSizeBytes 目標サイズ
   * @return 大容量データのInputStream
   */
  public static InputStream createLargeDataStream(String format, long targetSizeBytes) {
    return new LargeDataInputStream(format, targetSizeBytes);
  }

  /** メモリ効率的な大容量データInputStream実装 */
  private static class LargeDataInputStream extends InputStream {
    private final String format;
    private final long totalSize;
    private long bytesGenerated = 0;
    private byte[] buffer = new byte[0];
    private int bufferPosition = 0;
    private int recordCount = 0;
    private boolean headerWritten = false;
    private boolean footerWritten = false;
    private final Random random = new Random(42);

    public LargeDataInputStream(String format, long totalSize) {
      this.format = format.toUpperCase();
      this.totalSize = totalSize;
    }

    @Override
    public int read() throws IOException {
      if (bufferPosition >= buffer.length) {
        generateNextChunk();
        if (buffer.length == 0) {
          return -1; // EOF
        }
        bufferPosition = 0;
      }

      bytesGenerated++;
      return buffer[bufferPosition++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (bytesGenerated >= totalSize && bufferPosition >= buffer.length) {
        return -1; // EOF
      }

      int totalRead = 0;
      while (totalRead < len && bytesGenerated < totalSize) {
        if (bufferPosition >= buffer.length) {
          generateNextChunk();
          if (buffer.length == 0) break;
          bufferPosition = 0;
        }

        int available = buffer.length - bufferPosition;
        int toRead = Math.min(len - totalRead, available);

        System.arraycopy(buffer, bufferPosition, b, off + totalRead, toRead);
        bufferPosition += toRead;
        totalRead += toRead;
        bytesGenerated += toRead;
      }

      return totalRead > 0 ? totalRead : -1;
    }

    private void generateNextChunk() {
      if (bytesGenerated >= totalSize) {
        buffer = new byte[0];
        return;
      }

      StringBuilder chunk = new StringBuilder();

      // ヘッダー生成
      if (!headerWritten) {
        switch (format) {
          case "XML":
            chunk.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<orders>\n");
            break;
          case "JSON":
            chunk.append("{\n  \"orders\": [\n");
            break;
          case "CSV":
            chunk.append("id,name,city,product,quantity,price,timestamp\n");
            break;
        }
        headerWritten = true;
      }

      // レコード生成（チャンクサイズ：64KB）
      while (chunk.length() < 64 * 1024 && bytesGenerated + chunk.length() < totalSize - 100) {
        if (format.equals("JSON") && recordCount > 0) {
          chunk.append(",\n");
        }

        String record;
        switch (format) {
          case "XML":
            record = generateXmlRecord(recordCount++, random);
            break;
          case "JSON":
            record = generateJsonRecord(recordCount++, random);
            break;
          case "CSV":
            record = generateCsvRecord(recordCount++, random);
            break;
          default:
            record = "Unknown format\n";
        }
        chunk.append(record);
      }

      // フッター生成
      if (!footerWritten && bytesGenerated + chunk.length() >= totalSize - 100) {
        switch (format) {
          case "XML":
            chunk.append("</orders>\n");
            break;
          case "JSON":
            chunk.append("\n  ]\n}\n");
            break;
        }
        footerWritten = true;
      }

      try {
        buffer = chunk.toString().getBytes("UTF-8");
      } catch (Exception e) {
        buffer = new byte[0];
      }
      bufferPosition = 0;
    }
  }
}
