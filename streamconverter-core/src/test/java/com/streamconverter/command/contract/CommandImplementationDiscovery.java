package com.streamconverter.command.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class CommandImplementationDiscovery {
  private static final Pattern PACKAGE_PATTERN =
      Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
  private static final Pattern TOP_LEVEL_CLASS_PATTERN =
      Pattern.compile(
          "(?m)^\\s*(?:public\\s+)?(?:final\\s+)?(?:abstract\\s+)?class\\s+(\\w+)\\b([^\\{]*)\\{");

  private CommandImplementationDiscovery() {}

  static List<DiscoveredCommand> discover(Path repositoryRoot) throws IOException {
    try (Stream<Path> pathStream = Files.walk(repositoryRoot)) {
      return pathStream
          .filter(Files::isRegularFile)
          .filter(CommandImplementationDiscovery::isMainJavaSource)
          .sorted(Comparator.naturalOrder())
          .map(CommandImplementationDiscovery::parseDiscoveredCommand)
          .filter(commandClass -> commandClass != null)
          .toList();
    }
  }

  private static boolean isMainJavaSource(Path sourceFile) {
    String normalizedPath = sourceFile.normalize().toString().replace('\\', '/');
    return normalizedPath.contains("/src/main/java/") && normalizedPath.endsWith(".java");
  }

  private static DiscoveredCommand parseDiscoveredCommand(Path sourceFile) {
    try {
      String source = Files.readString(sourceFile);
      String packageName = extractPackageName(source);
      String simpleName = extractTopLevelCommandClassName(source);
      if (packageName == null || simpleName == null) {
        return null;
      }

      return new DiscoveredCommand(packageName + "." + simpleName, simpleName);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read command source " + sourceFile, e);
    }
  }

  private static String extractPackageName(String source) {
    java.util.regex.Matcher matcher = PACKAGE_PATTERN.matcher(source);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static String extractTopLevelCommandClassName(String source) {
    java.util.regex.Matcher matcher = TOP_LEVEL_CLASS_PATTERN.matcher(source);
    if (!matcher.find()) {
      return null;
    }

    String className = matcher.group(1);
    String classHeaderTail = matcher.group(2);
    String classDeclaration = matcher.group(0);

    if (classDeclaration.contains("abstract class")) {
      return null;
    }

    if (!isStreamCommandDeclaration(classHeaderTail)) {
      return null;
    }

    return className;
  }

  private static boolean isStreamCommandDeclaration(String classHeaderTail) {
    return classHeaderTail.contains("extends AbstractStreamCommand")
        || classHeaderTail.contains("extends ConsumerCommand")
        || classHeaderTail.contains("implements IStreamCommand")
        || classHeaderTail.contains(", IStreamCommand");
  }

  record DiscoveredCommand(String fqcn, String simpleName) {}
}
