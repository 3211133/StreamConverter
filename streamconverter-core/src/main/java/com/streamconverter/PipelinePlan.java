package com.streamconverter;

import com.streamconverter.command.IStreamCommand;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Stage wiring plan for a single StreamConverter execution.
 *
 * <p>The plan structure is immutable after construction, though the streams and resources it
 * references remain live execution objects.
 */
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

  /** Returns the ordered stage definitions to execute. */
  List<StageSpec> stageSpecs() {
    return stageSpecs;
  }

  /** Returns all intermediate pipes that may need abort signaling. */
  List<AbortablePipedStream> pipes() {
    return pipes;
  }

  /** Returns resources that must be closed after pipeline completion or failure. */
  List<AutoCloseable> resources() {
    return resources;
  }

  /**
   * Immutable wiring description for one command stage in the pipeline.
   *
   * <p>Each stage knows its execution order, wrapped command, connected input/output streams, and
   * the intermediate pipe it owns when it is not the terminal stage.
   */
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

    /** Returns the stage position in pipeline order. */
    int index() {
      return index;
    }

    /** Returns the wrapped command that should run for this stage. */
    IStreamCommand command() {
      return command;
    }

    /** Returns the human-readable command name used in logs and error messages. */
    String commandName() {
      return commandName;
    }

    /** Returns the input stream connected to this stage. */
    InputStream input() {
      return input;
    }

    /** Returns the output stream connected to this stage. */
    OutputStream output() {
      return output;
    }

    /** Returns the intermediate pipe for this stage, or {@code null} for the terminal stage. */
    AbortablePipedStream pipe() {
      return pipe;
    }
  }
}
