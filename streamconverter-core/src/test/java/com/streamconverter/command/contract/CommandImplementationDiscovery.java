package com.streamconverter.command.contract;

import com.streamconverter.command.IStreamCommand;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

final class CommandImplementationDiscovery {

  private CommandImplementationDiscovery() {}

  /**
   * @param repositoryRoot unused; retained for API compatibility with {@link
   *     CommandImplementationDiscoveryTest}
   */
  static List<DiscoveredCommand> discover(Path repositoryRoot) throws IOException {
    try (ScanResult scanResult =
        new ClassGraph().acceptPackages("com.streamconverter").enableClassInfo().scan()) {
      return scanResult.getClassesImplementing(IStreamCommand.class).stream()
          .filter(ci -> !ci.isAbstract() && !ci.getName().contains("$"))
          .sorted(Comparator.comparing(ci -> ci.getName()))
          .map(ci -> new DiscoveredCommand(ci.getName(), ci.getSimpleName()))
          .toList();
    }
  }

  record DiscoveredCommand(String fqcn, String simpleName) {}
}
