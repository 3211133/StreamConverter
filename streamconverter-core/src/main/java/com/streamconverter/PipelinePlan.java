package com.streamconverter;

import java.io.Closeable;
import java.util.List;

/**
 * Stage-level IO wiring plan for a single StreamConverter execution.
 *
 * <p>The plan structure is immutable after construction, though the streams and resources it
 * references remain live execution objects.
 */
final class PipelinePlan {
  private final List<WiredStageIo> stageIoList;
  private final List<AbortablePipedStream> pipeList;
  private final List<Closeable> resourceList;

  PipelinePlan(
      List<WiredStageIo> stageIos, List<AbortablePipedStream> pipes, List<Closeable> resources) {
    this.stageIoList = List.copyOf(stageIos);
    this.pipeList = List.copyOf(pipes);
    this.resourceList = List.copyOf(resources);
  }

  /** Returns the ordered IO connections for each stage. */
  List<WiredStageIo> stageIos() {
    return stageIoList;
  }

  /** Returns all intermediate pipes that may need abort signaling. */
  List<AbortablePipedStream> pipes() {
    return pipeList;
  }

  /** Returns resources that must be closed after pipeline completion or failure. */
  List<Closeable> resources() {
    return resourceList;
  }
}
