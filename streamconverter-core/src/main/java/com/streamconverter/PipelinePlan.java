package com.streamconverter;

import java.util.List;

/**
 * Stage-level IO wiring plan for a single StreamConverter execution.
 *
 * <p>The plan structure is immutable after construction, though the streams and resources it
 * references remain live execution objects.
 */
final class PipelinePlan {
  private final List<WiredStageIo> stageIos;
  private final List<AbortablePipedStream> pipes;
  private final List<AutoCloseable> resources;

  PipelinePlan(
      List<WiredStageIo> stageIos,
      List<AbortablePipedStream> pipes,
      List<AutoCloseable> resources) {
    this.stageIos = List.copyOf(stageIos);
    this.pipes = List.copyOf(pipes);
    this.resources = List.copyOf(resources);
  }

  /** Returns the ordered IO connections for each stage. */
  List<WiredStageIo> stageIos() {
    return stageIos;
  }

  /** Returns all intermediate pipes that may need abort signaling. */
  List<AbortablePipedStream> pipes() {
    return pipes;
  }

  /** Returns resources that must be closed after pipeline completion or failure. */
  List<AutoCloseable> resources() {
    return resources;
  }
}
