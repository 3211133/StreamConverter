package com.streamconverter;

import com.streamconverter.command.IStreamCommand;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/** Immutable stage wiring plan for a single StreamConverter execution. */
final class PipelinePlan {
  private final List<StageSpec> stageSpecs;
  private final List<AbortablePipedStream> pipes;
  private final List<AutoCloseable> resources;

  PipelinePlan(
      List<StageSpec> stageSpecs, List<AbortablePipedStream> pipes, List<AutoCloseable> resources) {
    this.stageSpecs = List.copyOf(stageSpecs);
    this.pipes = List.copyOf(pipes);
    this.resources = List.copyOf(resources);
  }

  List<StageSpec> stageSpecs() {
    return stageSpecs;
  }

  List<AbortablePipedStream> pipes() {
    return pipes;
  }

  List<AutoCloseable> resources() {
    return resources;
  }

  static final class StageSpec {
    private final int index;
    private final IStreamCommand command;
    private final String commandName;
    private final InputStream input;
    private final OutputStream output;
    private final AbortablePipedStream pipe;

    StageSpec(
        int index,
        IStreamCommand command,
        String commandName,
        InputStream input,
        OutputStream output,
        AbortablePipedStream pipe) {
      this.index = index;
      this.command = command;
      this.commandName = commandName;
      this.input = input;
      this.output = output;
      this.pipe = pipe;
    }

    int index() {
      return index;
    }

    IStreamCommand command() {
      return command;
    }

    String commandName() {
      return commandName;
    }

    InputStream input() {
      return input;
    }

    OutputStream output() {
      return output;
    }

    AbortablePipedStream pipe() {
      return pipe;
    }
  }
}
