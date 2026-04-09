package com.streamconverter;

import com.streamconverter.command.IStreamCommand;
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

  PipelinePlan build(
      List<IStreamCommand> commands,
      List<String> commandNames,
      InputStream inputStream,
      OutputStream outputStream)
      throws IOException {
    List<PipelinePlan.StageSpec> stageSpecs = new ArrayList<>(commands.size());
    List<AbortablePipedStream> pipes = new ArrayList<>();
    List<AutoCloseable> resources = new ArrayList<>();

    InputStream currentInput = inputStream;
    for (int i = 0; i < commands.size(); i++) {
      IStreamCommand command = commands.get(i);
      String commandName = commandNames.get(i);

      OutputStream commandOutput;
      AbortablePipedStream pipe;
      if (i == commands.size() - 1) {
        commandOutput = outputStream;
        pipe = null;
      } else {
        pipe = new AbortablePipedStream(bufferSize);
        resources.add(pipe);
        pipes.add(pipe);
        commandOutput = pipe.outputStream();
      }

      stageSpecs.add(
          new PipelinePlan.StageSpec(i, command, commandName, currentInput, commandOutput, pipe));

      if (pipe != null) {
        currentInput = pipe.inputStream();
      }
    }

    return new PipelinePlan(stageSpecs, pipes, resources);
  }
}
