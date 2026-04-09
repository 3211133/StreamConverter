package com.streamconverter;

import com.streamconverter.command.IStreamCommand;

/** Resolves best-effort human-readable labels for command diagnostics and logging. */
final class CommandLabels {
  private CommandLabels() {}

  /** Returns a stable fallback label for synthetic or anonymous command implementations. */
  static String resolve(IStreamCommand command) {
    Class<?> implClass = command.getClass();
    if (implClass.isSynthetic()) {
      return "IStreamCommand";
    }
    String simpleName = implClass.getSimpleName();
    return simpleName.isEmpty() ? "IStreamCommand" : simpleName;
  }
}
