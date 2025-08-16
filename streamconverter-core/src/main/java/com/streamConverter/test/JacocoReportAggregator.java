package com.streamConverter.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Aggregates JaCoCo coverage reports from multiple modules into a consolidated CSV format
 *
 * <p>This class parses JaCoCo XML reports and extracts coverage metrics, then appends them to a CSV
 * file for historical tracking. The output format is designed to be conflict-free with timestamps
 * for each entry.
 */
public class JacocoReportAggregator {

  private static final String CSV_HEADER =
      "timestamp,module,total_lines,covered_lines,line_coverage,total_branches,covered_branches,branch_coverage";
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  /** Private constructor to prevent instantiation of utility class */
  private JacocoReportAggregator() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /** Represents coverage metrics for a single module */
  public static class CoverageMetrics {
    private final String moduleName;
    private final int totalLines;
    private final int coveredLines;
    private final double lineCoverage;
    private final int totalBranches;
    private final int coveredBranches;
    private final double branchCoverage;
    private final LocalDateTime timestamp;

    /**
     * Creates a new CoverageMetrics instance
     *
     * @param moduleName the name of the module
     * @param totalLines total number of lines
     * @param coveredLines number of covered lines
     * @param lineCoverage line coverage percentage
     * @param totalBranches total number of branches
     * @param coveredBranches number of covered branches
     * @param branchCoverage branch coverage percentage
     * @param timestamp timestamp when the metrics were collected
     */
    public CoverageMetrics(
        String moduleName,
        int totalLines,
        int coveredLines,
        double lineCoverage,
        int totalBranches,
        int coveredBranches,
        double branchCoverage,
        LocalDateTime timestamp) {
      this.moduleName = moduleName;
      this.totalLines = totalLines;
      this.coveredLines = coveredLines;
      this.lineCoverage = lineCoverage;
      this.totalBranches = totalBranches;
      this.coveredBranches = coveredBranches;
      this.branchCoverage = branchCoverage;
      this.timestamp = timestamp;
    }

    /**
     * Gets the module name
     *
     * @return the module name
     */
    public String getModuleName() {
      return moduleName;
    }

    /**
     * Gets the total number of lines
     *
     * @return the total number of lines
     */
    public int getTotalLines() {
      return totalLines;
    }

    /**
     * Gets the number of covered lines
     *
     * @return the number of covered lines
     */
    public int getCoveredLines() {
      return coveredLines;
    }

    /**
     * Gets the line coverage percentage
     *
     * @return the line coverage percentage
     */
    public double getLineCoverage() {
      return lineCoverage;
    }

    /**
     * Gets the total number of branches
     *
     * @return the total number of branches
     */
    public int getTotalBranches() {
      return totalBranches;
    }

    /**
     * Gets the number of covered branches
     *
     * @return the number of covered branches
     */
    public int getCoveredBranches() {
      return coveredBranches;
    }

    /**
     * Gets the branch coverage percentage
     *
     * @return the branch coverage percentage
     */
    public double getBranchCoverage() {
      return branchCoverage;
    }

    /**
     * Gets the timestamp when metrics were collected
     *
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    /**
     * Converts the metrics to CSV format
     *
     * @return CSV formatted string
     */
    public String toCsvLine() {
      return String.format(
          "%s,%s,%d,%d,%.1f,%d,%d,%.1f",
          timestamp.format(TIMESTAMP_FORMAT),
          moduleName,
          totalLines,
          coveredLines,
          lineCoverage,
          totalBranches,
          coveredBranches,
          branchCoverage);
    }

    @Override
    public String toString() {
      return String.format(
          "CoverageMetrics{module='%s', lineCoverage=%.1f%%, branchCoverage=%.1f%%, timestamp=%s}",
          moduleName, lineCoverage, branchCoverage, timestamp.format(TIMESTAMP_FORMAT));
    }
  }

  /**
   * Parses a JaCoCo XML report file and extracts coverage metrics
   *
   * @param xmlFile the JaCoCo XML report file
   * @param moduleName the name of the module
   * @return CoverageMetrics for the module, or null if parsing fails
   */
  public static CoverageMetrics parseJacocoReport(File xmlFile, String moduleName) {
    if (!xmlFile.exists() || !xmlFile.canRead()) {
      System.err.println("Cannot read JaCoCo report file: " + xmlFile.getAbsolutePath());
      return null;
    }

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // Disable DTD validation to avoid missing DTD file issues
      factory.setValidating(false);
      factory.setFeature("http://xml.org/sax/features/validation", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(xmlFile);

      Element reportElement = doc.getDocumentElement();

      // Find all counter elements and aggregate them
      NodeList counterNodes = reportElement.getElementsByTagName("counter");

      int totalLines = 0;
      int coveredLines = 0;
      int totalBranches = 0;
      int coveredBranches = 0;

      for (int i = 0; i < counterNodes.getLength(); i++) {
        Element counter = (Element) counterNodes.item(i);
        String type = counter.getAttribute("type");

        if ("LINE".equals(type)) {
          int missed = Integer.parseInt(counter.getAttribute("missed"));
          int covered = Integer.parseInt(counter.getAttribute("covered"));
          totalLines += missed + covered;
          coveredLines += covered;
        } else if ("BRANCH".equals(type)) {
          int missed = Integer.parseInt(counter.getAttribute("missed"));
          int covered = Integer.parseInt(counter.getAttribute("covered"));
          totalBranches += missed + covered;
          coveredBranches += covered;
        }
      }

      double lineCoverage = totalLines > 0 ? (coveredLines * 100.0 / totalLines) : 0.0;
      double branchCoverage = totalBranches > 0 ? (coveredBranches * 100.0 / totalBranches) : 0.0;

      return new CoverageMetrics(
          moduleName,
          totalLines,
          coveredLines,
          lineCoverage,
          totalBranches,
          coveredBranches,
          branchCoverage,
          LocalDateTime.now());

    } catch (Exception e) {
      System.err.println("Failed to parse JaCoCo report: " + xmlFile.getName());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Creates a placeholder metrics object for modules without JaCoCo reports
   *
   * @param moduleName the name of the module
   * @return CoverageMetrics with N/A values
   */
  public static CoverageMetrics createPlaceholderMetrics(String moduleName) {
    return new CoverageMetrics(moduleName, 0, 0, 0.0, 0, 0, 0.0, LocalDateTime.now()) {
      @Override
      public String toCsvLine() {
        return String.format(
            "%s,%s,N/A,N/A,N/A,N/A,N/A,N/A",
            getTimestamp().format(TIMESTAMP_FORMAT), getModuleName());
      }

      @Override
      public String toString() {
        return String.format(
            "CoverageMetrics{module='%s', status=NO_REPORT, timestamp=%s}",
            getModuleName(), getTimestamp().format(TIMESTAMP_FORMAT));
      }
    };
  }

  /**
   * Aggregates JaCoCo reports from all modules in the project
   *
   * @param projectRoot the root directory of the project
   * @param outputFile the output CSV file path
   * @throws IOException if file operations fail
   */
  public static void aggregateReports(String projectRoot, String outputFile) throws IOException {
    Path rootPath = Paths.get(projectRoot);
    Path outputPath = Paths.get(outputFile);

    // Ensure output directory exists
    Files.createDirectories(outputPath.getParent());

    List<CoverageMetrics> allMetrics = new ArrayList<>();

    // Known module names
    String[] modules = {
      "streamconverter-core",
      "streamconverter-web",
      "streamconverter-examples",
      "streamconverter-tools"
    };

    for (String module : modules) {
      Path jacocoXmlPath =
          rootPath.resolve(module).resolve("build/reports/jacoco/test/jacocoTestReport.xml");
      File jacocoFile = jacocoXmlPath.toFile();

      CoverageMetrics metrics;
      if (jacocoFile.exists()) {
        metrics = parseJacocoReport(jacocoFile, module);
        if (metrics == null) {
          metrics = createPlaceholderMetrics(module);
        }
      } else {
        metrics = createPlaceholderMetrics(module);
      }

      allMetrics.add(metrics);
      System.out.println("Processed: " + metrics);
    }

    // Write to CSV file (append mode)
    boolean fileExists = Files.exists(outputPath);
    List<String> lines = new ArrayList<>();

    // Add header if file doesn't exist
    if (!fileExists) {
      lines.add(CSV_HEADER);
    }

    // Add metrics lines
    for (CoverageMetrics metrics : allMetrics) {
      lines.add(metrics.toCsvLine());
    }

    // Write all lines
    Files.write(
        outputPath, lines, fileExists ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);

    System.out.println("Coverage report aggregated to: " + outputFile);
    System.out.println("Total modules processed: " + allMetrics.size());
  }

  /**
   * Main method for command-line usage
   *
   * <p>Usage: java JacocoReportAggregator [project-root] [output-file] If no arguments provided,
   * uses current directory and default output file.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    String projectRoot = args.length > 0 ? args[0] : ".";
    String outputFile = args.length > 1 ? args[1] : "docs/reports/jacoco/coverage-history.csv";

    try {
      aggregateReports(projectRoot, outputFile);
    } catch (IOException e) {
      System.err.println("Error aggregating JaCoCo reports: " + e.getMessage());
      System.exit(1);
    }
  }
}
