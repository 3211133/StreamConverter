package com.streamconverter.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.SampleStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Web API controller for StreamConverter processing.
 *
 * <p>Provides REST endpoints to process data streams using existing StreamConverter functionality.
 */
@RestController
@RequestMapping("/api/v1/stream")
public class StreamProcessingController {

  private static final Logger log = LoggerFactory.getLogger(StreamProcessingController.class);

  /**
   * Process data stream with CSV extraction.
   *
   * @param inputData binary input data
   * @param columnName CSV column name to extract
   * @return processed data as binary stream
   */
  @PostMapping(
      value = "/csv/extract",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public Mono<ResponseEntity<Flux<DataBuffer>>> processCsvExtraction(
      @RequestBody Flux<DataBuffer> inputData, @RequestParam String columnName) {

    log.info("Processing CSV extraction for column: {}", columnName);

    return Mono
        .fromCallable(
            () ->
                ResponseEntity.ok(
                    processWithStreamConverter(
                        inputData,
                        CsvNavigateCommand.create(
                            new CSVPath(columnName), new PassThroughRule()))))
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
  }

  /**
   * Process data stream with JSON path extraction.
   *
   * @param inputData binary input data
   * @param jsonPath JSON path expression
   * @return processed data as binary stream
   */
  @PostMapping(
      value = "/json/extract",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public Mono<ResponseEntity<Flux<DataBuffer>>> processJsonExtraction(
      @RequestBody Flux<DataBuffer> inputData, @RequestParam String jsonPath) {

    log.info("Processing JSON extraction for path: {}", jsonPath);

    return Mono
        .fromCallable(
            () ->
                ResponseEntity.ok(
                    processWithStreamConverter(
                        inputData,
                        JsonNavigateCommand.create(
                            TreePath.fromJson(jsonPath), new PassThroughRule()))))
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
  }

  /**
   * Process data stream with custom command pipeline.
   *
   * @param inputData binary input data
   * @param pipelineConfig pipeline configuration header
   * @return processed data as binary stream
   */
  @PostMapping(
      value = "/process",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public Mono<ResponseEntity<Flux<DataBuffer>>> processWithPipeline(
      @RequestBody Flux<DataBuffer> inputData,
      @RequestHeader("X-Pipeline-Config") String pipelineConfig) {

    log.info("Processing with pipeline config: {}", pipelineConfig);

    return Mono
        .fromCallable(
            () ->
                ResponseEntity.ok(
                    processWithStreamConverter(
                        inputData, buildPipelineFromConfig(pipelineConfig))))
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
  }

  /**
   * Health check endpoint.
   *
   * @return simple health status
   */
  @GetMapping("/health")
  public Mono<ResponseEntity<String>> health() {
    return Mono.just(ResponseEntity.ok("StreamConverter Web API is running"));
  }

  /**
   * Processes data using StreamConverter with given commands in a streaming fashion.
   *
   * @param inputData reactive stream of input data buffers
   * @param commands stream commands to execute
   * @return processed output as Flux of DataBuffer
   */
  private Flux<DataBuffer> processWithStreamConverter(
      Flux<DataBuffer> inputData, IStreamCommand... commands) {
    DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    return Flux
        .from(
            DataBufferUtils.outputStreamPublisher(
                outputStream -> {
                  try (InputStream inputStream =
                      DataBufferUtils.subscriberInputStream(inputData, 4096)) {
                    StreamConverter converter = StreamConverter.create(commands);
                    converter.run(inputStream, outputStream);
                  } catch (IOException e) {
                    throw new RuntimeException("Stream processing failed", e);
                  }
                },
                bufferFactory,
                executor))
        .doFinally(signalType -> executor.shutdown());
  }

  /**
   * Builds a pipeline from configuration string.
   *
   * @param config pipeline configuration (e.g., "csv:name,json:$.result,process:validator")
   * @return array of stream commands
   */
  private IStreamCommand[] buildPipelineFromConfig(String config) {
    String[] commandConfigs = config.split(",");
    IStreamCommand[] commands = new IStreamCommand[commandConfigs.length];

    for (int i = 0; i < commandConfigs.length; i++) {
      String[] parts = commandConfigs[i].split(":", 2);
      String commandType = parts[0].trim();
      String parameter = parts.length > 1 ? parts[1].trim() : "";

      commands[i] =
          switch (commandType.toLowerCase()) {
            case "csv" -> CsvNavigateCommand.create(new CSVPath(parameter), new PassThroughRule());
            case "json" -> JsonNavigateCommand.create(TreePath.fromJson(parameter), new PassThroughRule());
            case "process" -> new SampleStreamCommand(parameter);
            default -> throw new IllegalArgumentException("Unknown command type: " + commandType);
          };
    }

    log.info("Built pipeline with {} commands", commands.length);
    return commands;
  }
}
