package com.streamconverter.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
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
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.command.IStreamCommand;
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

  private static final Logger logger = LoggerFactory.getLogger(StreamProcessingController.class);

  private static final int MAX_PIPELINE_CONFIG_LENGTH = 1000;
  private static final int MAX_PIPELINE_COMMANDS = 10;
  private static final int MAX_PARAMETER_LENGTH = 500;

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

    logger.info("Processing CSV extraction for column: {}", columnName);

    return Mono
        .fromCallable(
            () ->
                ResponseEntity.ok(
                    processWithStreamConverter(
                        inputData,
                        CsvNavigateCommand.create(
                            CSVPath.of(columnName), new PassThroughRule()))))
        .onErrorResume(
            e -> {
              logger.error("CSV extraction failed: {}", e.getMessage(), e);
              return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
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

    logger.info("Processing JSON extraction for path: {}", jsonPath);

    return Mono
        .fromCallable(
            () ->
                ResponseEntity.ok(
                    processWithStreamConverter(
                        inputData,
                        JsonNavigateCommand.create(
                            TreePath.fromJson(jsonPath), new PassThroughRule()))))
        .onErrorResume(
            e -> {
              logger.error("JSON extraction failed: {}", e.getMessage(), e);
              return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
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

    return Mono
        .fromCallable(() -> buildPipelineFromConfig(pipelineConfig))
        .map(
            commands -> {
              logger.info("Processing pipeline with {} commands", commands.length);
              return ResponseEntity.ok(processWithStreamConverter(inputData, commands));
            })
        .onErrorResume(
            IllegalArgumentException.class,
            e -> {
              logger.warn("Invalid pipeline config: {}", e.getMessage());
              return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
            })
        .onErrorResume(
            e -> {
              logger.error("Pipeline processing failed: {}", e.getMessage(), e);
              return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
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
   * @throws IllegalArgumentException config が null/空/長すぎる、またはコマンド数・パラメータが不正な場合
   */
  private IStreamCommand[] buildPipelineFromConfig(String config) {
    if (config == null || config.isBlank()) {
      throw new IllegalArgumentException("Pipeline config must not be null or blank");
    }
    if (config.length() > MAX_PIPELINE_CONFIG_LENGTH) {
      throw new IllegalArgumentException(
          "Pipeline config exceeds maximum length of " + MAX_PIPELINE_CONFIG_LENGTH);
    }

    String[] commandConfigs = config.split(",", -1);
    if (commandConfigs.length > MAX_PIPELINE_COMMANDS) {
      throw new IllegalArgumentException(
          "Pipeline config exceeds maximum command count of " + MAX_PIPELINE_COMMANDS);
    }

    IStreamCommand[] commands = new IStreamCommand[commandConfigs.length];

    for (int i = 0; i < commandConfigs.length; i++) {
      String[] parts = commandConfigs[i].split(":", 2);
      String commandType = parts[0].trim();
      if (commandType.isEmpty()) {
        throw new IllegalArgumentException("Command type must not be empty at index " + i);
      }
      String parameter = parts.length > 1 ? parts[1].trim() : "";
      if (parameter.length() > MAX_PARAMETER_LENGTH) {
        throw new IllegalArgumentException(
            "Parameter exceeds maximum length of " + MAX_PARAMETER_LENGTH + " at index " + i);
      }

      commands[i] =
          switch (commandType.toLowerCase(Locale.ROOT)) {
            case "csv" -> {
              if (parameter.isEmpty()) {
                throw new IllegalArgumentException("csv command requires a column name at index " + i);
              }
              yield CsvNavigateCommand.create(CSVPath.of(parameter), new PassThroughRule());
            }
            case "json" -> {
              if (parameter.isEmpty()) {
                throw new IllegalArgumentException("json command requires a path at index " + i);
              }
              yield JsonNavigateCommand.create(TreePath.fromJson(parameter), new PassThroughRule());
            }
            case "process" -> new AbstractStreamCommand() {
              @Override
              public void execute(InputStream in, java.io.OutputStream out) throws IOException {
                in.transferTo(out);
              }
            };
            default -> throw new IllegalArgumentException("Unknown command type: " + commandType);
          };
    }

    logger.info("Built pipeline with {} commands", commands.length);
    return commands;
  }
}
