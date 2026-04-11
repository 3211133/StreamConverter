package com.streamconverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Builds stage-level IO wiring for a {@link StreamConverter} execution. */
final class PipelineWiring {
  private final int bufferSize;

  PipelineWiring(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  /**
   * Builds the executable IO plan for one pipeline run.
   *
   * <p>Non-terminal stages write into newly created {@link AbortablePipedStream}s, while the final
   * stage writes directly to the caller-provided output stream.
   *
   * @param stageCount number of stages to wire
   * @param inputStream caller-provided pipeline input
   * @param outputStream caller-provided pipeline output
   * @return immutable execution plan containing stage IO wiring and closeable resources
   * @throws IOException if an intermediate pipe cannot be created
   */
  PipelinePlan build(int stageCount, InputStream inputStream, OutputStream outputStream)
      throws IOException {
    List<WiredStageIo> stageIos = new ArrayList<>(stageCount);
    List<AbortablePipedStream> pipes = new ArrayList<>();
    List<AutoCloseable> resources = new ArrayList<>();

    InputStream currentInput = inputStream;
    for (int i = 0; i < stageCount; i++) {
      OutputStream commandOutput;
      AbortablePipedStream pipe;
      if (i == stageCount - 1) {
        commandOutput = outputStream;
        pipe = null;
      } else {
        pipe = new AbortablePipedStream(bufferSize);
        resources.add(pipe);
        pipes.add(pipe);
        commandOutput = pipe.outputStream();
      }

      stageIos.add(new WiredStageIo(currentInput, commandOutput, pipe));

      if (pipe != null) {
        currentInput = pipe.inputStream();
      }
    }

    return new PipelinePlan(stageIos, pipes, resources);
  }
}
