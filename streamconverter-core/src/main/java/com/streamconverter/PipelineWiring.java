package com.streamconverter;

import java.io.Closeable;
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
    List<Closeable> resources = new ArrayList<>();

    InputStream currentInput = inputStream;
    for (int i = 0; i < stageCount; i++) {
      if (i == stageCount - 1) {
        stageIos.add(new WiredStageIo(currentInput, outputStream, null));
      } else {
        AbortablePipedStream pipe = new AbortablePipedStream(bufferSize);
        resources.add(pipe);
        pipes.add(pipe);
        stageIos.add(new WiredStageIo(currentInput, pipe.outputStream(), pipe));
        currentInput = pipe.inputStream();
      }
    }

    return new PipelinePlan(stageIos, pipes, resources);
  }
}
