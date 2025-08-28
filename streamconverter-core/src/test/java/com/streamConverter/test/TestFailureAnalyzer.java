package com.streamConverter.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utility class to analyze test failures from XML test reports
 *
 * <p>This class parses JUnit XML test reports and extracts detailed failure information for easier
 * debugging and analysis.
 */
public final class TestFailureAnalyzer {
  private static final Logger LOG = LoggerFactory.getLogger(TestFailureAnalyzer.class);

  /** Private constructor to prevent instantiation of utility class */
  private TestFailureAnalyzer() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /** Represents a single test failure with detailed information */
  public static class TestFailure {
    private final String testClass;
    private final String testMethod;
    private final String failureType;
    private final String failureMessage;
    private final String stackTrace;
    private final double executionTime;

    /**
     * Creates a new TestFailure instance with the provided details
     *
     * @param testClass the fully qualified name of the test class
     * @param testMethod the name of the failed test method
     * @param failureType the type of failure (e.g., AssertionError, NullPointerException)
     * @param failureMessage the failure message
     * @param stackTrace the complete stack trace of the failure
     * @param executionTime the time in seconds the test took to execute
     */
    public TestFailure(
        final String testClass,
        final String testMethod,
        final String failureType,
        final String failureMessage,
        final String stackTrace,
        final double executionTime) {
      this.testClass = testClass;
      this.testMethod = testMethod;
      this.failureType = failureType;
      this.failureMessage = failureMessage;
      this.stackTrace = stackTrace;
      this.executionTime = executionTime;
    }

    /**
     * Gets the fully qualified name of the test class
     *
     * @return the fully qualified name of the test class
     */
    public String getTestClass() {
      return testClass;
    }

    /**
     * Gets the name of the failed test method
     *
     * @return the name of the failed test method
     */
    public String getTestMethod() {
      return testMethod;
    }

    /**
     * Gets the type of failure
     *
     * @return the type of failure (e.g., AssertionError, NullPointerException)
     */
    public String getFailureType() {
      return failureType;
    }

    /**
     * Gets the failure message
     *
     * @return the failure message
     */
    public String getFailureMessage() {
      return failureMessage;
    }

    /**
     * Gets the complete stack trace of the failure
     *
     * @return the complete stack trace of the failure
     */
    public String getStackTrace() {
      return stackTrace;
    }

    /**
     * Gets the execution time for the test
     *
     * @return the time in seconds the test took to execute
     */
    public double getExecutionTime() {
      return executionTime;
    }

    @Override
    public String toString() {
      return String.format(
          "=== TEST FAILURE ===%nClass: %s%nMethod: %s%nExecution Time: %ss%nFailure Type: %s%nMessage: %s%nStack Trace:%n%s%n====================%n",
          testClass, testMethod, executionTime, failureType, failureMessage, stackTrace);
    }
  }

  /** Represents a summary of test execution results including failure details */
  public static class TestSummary {
    private final int totalTests;
    private final int failures;
    private final int errors;
    private final int skipped;
    private final List<TestFailure> failureDetails;

    /**
     * Creates a new TestSummary instance
     *
     * @param totalTests the total number of tests executed
     * @param failures the number of test failures
     * @param errors the number of test errors
     * @param skipped the number of skipped tests
     * @param failureDetails list of detailed failure information
     */
    public TestSummary(
        final int totalTests,
        final int failures,
        final int errors,
        final int skipped,
        final List<TestFailure> failureDetails) {
      this.totalTests = totalTests;
      this.failures = failures;
      this.errors = errors;
      this.skipped = skipped;
      this.failureDetails =
          failureDetails != null ? new ArrayList<>(failureDetails) : new ArrayList<>();
    }

    /**
     * Gets the total number of tests executed
     *
     * @return the total number of tests executed
     */
    public int getTotalTests() {
      return totalTests;
    }

    /**
     * Gets the number of test failures
     *
     * @return the number of test failures
     */
    public int getFailures() {
      return failures;
    }

    /**
     * Gets the number of test errors
     *
     * @return the number of test errors
     */
    public int getErrors() {
      return errors;
    }

    /**
     * Gets the number of skipped tests
     *
     * @return the number of skipped tests
     */
    public int getSkipped() {
      return skipped;
    }

    /**
     * Gets the list of detailed failure information
     *
     * @return list of detailed failure information
     */
    public List<TestFailure> getFailureDetails() {
      return new ArrayList<>(failureDetails);
    }

    /**
     * Checks if there are any failures or errors
     *
     * @return true if there are any failures or errors
     */
    public boolean hasFailures() {
      return failures > 0 || errors > 0;
    }

    @Override
    public String toString() {
      final String summary =
          String.format(
              "=== TEST SUMMARY ===%nTotal Tests: %d%nFailures: %d%nErrors: %d%nSkipped: %d%nSuccess Rate: %.1f%%%n====================%n",
              totalTests,
              failures,
              errors,
              skipped,
              (totalTests - failures - errors) * 100.0 / totalTests);
      String result = summary;
      if (hasFailures()) {
        final StringBuilder builder = new StringBuilder(summary);
        builder.append(System.lineSeparator());
        for (final TestFailure failure : failureDetails) {
          builder.append(failure).append(System.lineSeparator());
        }
        result = builder.toString();
      }
      return result;
    }
  }

  /**
   * Analyzes test results from XML reports in the given directory
   *
   * @param testResultsPath Path to test-results directory (e.g., "build/test-results/test")
   * @return TestSummary containing failure details
   * @throws IOException if unable to read test result files
   */
  public static TestSummary analyzeTestResults(final String testResultsPath) throws IOException {
    final Path resultsDir = Paths.get(testResultsPath);

    if (!Files.exists(resultsDir) || !Files.isDirectory(resultsDir)) {
      throw new IOException("Test results directory not found: " + testResultsPath);
    }

    final List<TestFailure> allFailures = new ArrayList<>();
    int totalTests = 0;
    int totalFailures = 0;
    int totalErrors = 0;
    int totalSkipped = 0;

    try (Stream<Path> xmlFiles = Files.list(resultsDir)) {
      for (final Path xmlFile : xmlFiles.filter(p -> p.toString().endsWith(".xml")).toList()) {
        final TestSummary fileSummary = parseXmlTestReport(xmlFile.toFile());

        totalTests += fileSummary.getTotalTests();
        totalFailures += fileSummary.getFailures();
        totalErrors += fileSummary.getErrors();
        totalSkipped += fileSummary.getSkipped();
        allFailures.addAll(fileSummary.getFailureDetails());
      }
    }

    return new TestSummary(totalTests, totalFailures, totalErrors, totalSkipped, allFailures);
  }

  /**
   * Parses a single XML test report file
   *
   * @param xmlFile XML test report file
   * @return TestSummary for this file
   */
  public static TestSummary parseXmlTestReport(final File xmlFile) {
    final List<TestFailure> failures = new ArrayList<>();
    TestSummary summary;
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder builder = factory.newDocumentBuilder();
      final Document doc = builder.parse(xmlFile);

      final Element testsuite = doc.getDocumentElement();
      final int tests = Integer.parseInt(testsuite.getAttribute("tests"));
      final int failureCount = Integer.parseInt(testsuite.getAttribute("failures"));
      final int errorCount = Integer.parseInt(testsuite.getAttribute("errors"));
      final int skippedCount = Integer.parseInt(testsuite.getAttribute("skipped"));

      final NodeList testcases = testsuite.getElementsByTagName("testcase");

      for (int i = 0; i < testcases.getLength(); i++) {
        final Element testcase = (Element) testcases.item(i);
        final String testClass = testcase.getAttribute("classname");
        final String testMethod = testcase.getAttribute("name");
        final double time = Double.parseDouble(testcase.getAttribute("time"));

        // Check for failure
        final NodeList failureNodes = testcase.getElementsByTagName("failure");
        if (failureNodes.getLength() > 0) {
          final Element failure = (Element) failureNodes.item(0);
          final String failureType = failure.getAttribute("type");
          final String failureMessage = failure.getAttribute("message");
          final String failureStackTrace = failure.getTextContent();

          failures.add(
              new TestFailure(
                  testClass, testMethod, failureType, failureMessage, failureStackTrace, time));
        }

        // Check for error
        final NodeList errorNodes = testcase.getElementsByTagName("error");
        if (errorNodes.getLength() > 0) {
          final Element error = (Element) errorNodes.item(0);
          final String errorType = error.getAttribute("type");
          final String errorMessage = error.getAttribute("message");
          final String errorStackTrace = error.getTextContent();

          failures.add(
              new TestFailure(
                  testClass, testMethod, errorType, errorMessage, errorStackTrace, time));
        }
      }

      summary = new TestSummary(tests, failureCount, errorCount, skippedCount, failures);

    } catch (ParserConfigurationException | SAXException | IOException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Failed to parse XML file: {}", xmlFile.getName(), e);
      }
      summary = new TestSummary(0, 0, 0, 0, new ArrayList<>());
    }
    return summary;
  }

  /**
   * Main method for command-line usage
   *
   * <p>Usage: java TestFailureAnalyzer [test-results-path] If no path specified, uses
   * "build/test-results/test"
   *
   * @param args command line arguments, optional test results path
   */
  public static void main(final String[] args) {
    final String testResultsPath = args.length > 0 ? args[0] : "build/test-results/test";

    try {
      final TestSummary summary = analyzeTestResults(testResultsPath);
      if (LOG.isInfoEnabled()) {
        LOG.info("{}", summary);
      }

      if (summary.hasFailures()) {
        System.exit(1); // Exit with error code if there are failures
      }

    } catch (IOException e) {
      LOG.error("Error analyzing test results", e);
      System.exit(1);
    }
  }
}
