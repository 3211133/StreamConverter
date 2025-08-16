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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Utility class to analyze test failures from XML test reports
 *
 * <p>This class parses JUnit XML test reports and extracts detailed failure information for easier
 * debugging and analysis.
 */
public class TestFailureAnalyzer {

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
        String testClass,
        String testMethod,
        String failureType,
        String failureMessage,
        String stackTrace,
        double executionTime) {
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
      StringBuilder sb = new StringBuilder();
      sb.append("=== TEST FAILURE ===\n");
      sb.append("Class: ").append(testClass).append("\n");
      sb.append("Method: ").append(testMethod).append("\n");
      sb.append("Execution Time: ").append(executionTime).append("s\n");
      sb.append("Failure Type: ").append(failureType).append("\n");
      sb.append("Message: ").append(failureMessage).append("\n");
      sb.append("Stack Trace:\n").append(stackTrace).append("\n");
      sb.append("====================\n");
      return sb.toString();
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
        int totalTests, int failures, int errors, int skipped, List<TestFailure> failureDetails) {
      this.totalTests = totalTests;
      this.failures = failures;
      this.errors = errors;
      this.skipped = skipped;
      this.failureDetails = failureDetails;
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
      return failureDetails;
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
      StringBuilder sb = new StringBuilder();
      sb.append("=== TEST SUMMARY ===\n");
      sb.append("Total Tests: ").append(totalTests).append("\n");
      sb.append("Failures: ").append(failures).append("\n");
      sb.append("Errors: ").append(errors).append("\n");
      sb.append("Skipped: ").append(skipped).append("\n");
      sb.append("Success Rate: ")
          .append(String.format("%.1f%%", (totalTests - failures - errors) * 100.0 / totalTests))
          .append("\n");
      sb.append("====================\n");

      if (hasFailures()) {
        sb.append("\n");
        for (TestFailure failure : failureDetails) {
          sb.append(failure.toString()).append("\n");
        }
      }

      return sb.toString();
    }
  }

  /**
   * Analyzes test results from XML reports in the given directory
   *
   * @param testResultsPath Path to test-results directory (e.g., "build/test-results/test")
   * @return TestSummary containing failure details
   * @throws IOException if unable to read test result files
   */
  public static TestSummary analyzeTestResults(String testResultsPath) throws IOException {
    Path resultsDir = Paths.get(testResultsPath);

    if (!Files.exists(resultsDir) || !Files.isDirectory(resultsDir)) {
      throw new IOException("Test results directory not found: " + testResultsPath);
    }

    List<TestFailure> allFailures = new ArrayList<>();
    int totalTests = 0;
    int totalFailures = 0;
    int totalErrors = 0;
    int totalSkipped = 0;

    try (Stream<Path> xmlFiles = Files.list(resultsDir)) {
      for (Path xmlFile : xmlFiles.filter(p -> p.toString().endsWith(".xml")).toList()) {
        TestSummary fileSummary = parseXmlTestReport(xmlFile.toFile());

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
  public static TestSummary parseXmlTestReport(File xmlFile) {
    List<TestFailure> failures = new ArrayList<>();

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(xmlFile);

      Element testsuite = doc.getDocumentElement();
      int tests = Integer.parseInt(testsuite.getAttribute("tests"));
      int failureCount = Integer.parseInt(testsuite.getAttribute("failures"));
      int errorCount = Integer.parseInt(testsuite.getAttribute("errors"));
      int skippedCount = Integer.parseInt(testsuite.getAttribute("skipped"));

      NodeList testcases = testsuite.getElementsByTagName("testcase");

      for (int i = 0; i < testcases.getLength(); i++) {
        Element testcase = (Element) testcases.item(i);
        String testClass = testcase.getAttribute("classname");
        String testMethod = testcase.getAttribute("name");
        double time = Double.parseDouble(testcase.getAttribute("time"));

        // Check for failure
        NodeList failureNodes = testcase.getElementsByTagName("failure");
        if (failureNodes.getLength() > 0) {
          Element failure = (Element) failureNodes.item(0);
          String failureType = failure.getAttribute("type");
          String failureMessage = failure.getAttribute("message");
          String stackTrace = failure.getTextContent();

          failures.add(
              new TestFailure(
                  testClass, testMethod, failureType, failureMessage, stackTrace, time));
        }

        // Check for error
        NodeList errorNodes = testcase.getElementsByTagName("error");
        if (errorNodes.getLength() > 0) {
          Element error = (Element) errorNodes.item(0);
          String errorType = error.getAttribute("type");
          String errorMessage = error.getAttribute("message");
          String stackTrace = error.getTextContent();

          failures.add(
              new TestFailure(testClass, testMethod, errorType, errorMessage, stackTrace, time));
        }
      }

      return new TestSummary(tests, failureCount, errorCount, skippedCount, failures);

    } catch (Exception e) {
      System.err.println("Failed to parse XML file: " + xmlFile.getName());
      e.printStackTrace();
      return new TestSummary(0, 0, 0, 0, new ArrayList<>());
    }
  }

  /**
   * Main method for command-line usage
   *
   * <p>Usage: java TestFailureAnalyzer [test-results-path] If no path specified, uses
   * "build/test-results/test"
   *
   * @param args command line arguments, optional test results path
   */
  public static void main(String[] args) {
    String testResultsPath = args.length > 0 ? args[0] : "build/test-results/test";

    try {
      TestSummary summary = analyzeTestResults(testResultsPath);
      System.out.println(summary);

      if (summary.hasFailures()) {
        System.exit(1); // Exit with error code if there are failures
      }

    } catch (IOException e) {
      System.err.println("Error analyzing test results: " + e.getMessage());
      System.exit(1);
    }
  }
}
