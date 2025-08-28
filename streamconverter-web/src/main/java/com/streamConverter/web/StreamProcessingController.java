package com.streamConverter.web;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.csv.CsvNavigateCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.rule.PassThroughRule;
import com.streamConverter.path.CSVPath;
import com.streamConverter.path.JSONPath;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    return inputData
        .collectList()
        .map(this::combineDataBuffers)
        .map(data -> processWithStreamConverter(data, CsvNavigateCommand.create(new CSVPath(columnName), new PassThroughRule())))
        .map(result -> ResponseEntity.ok(createDataBufferFlux(result)))
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

    return inputData
        .collectList()
        .map(this::combineDataBuffers)
        .map(data -> processWithStreamConverter(data, JsonNavigateCommand.create(new JSONPath(jsonPath), new PassThroughRule())))
        .map(result -> ResponseEntity.ok(createDataBufferFlux(result)))
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

    return inputData
        .collectList()
        .map(this::combineDataBuffers)
        .map(data -> processWithStreamConverter(data, buildPipelineFromConfig(pipelineConfig)))
        .map(result -> ResponseEntity.ok(createDataBufferFlux(result)))
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
   * Combines multiple DataBuffer objects into a single byte array.
   *
   * @param dataBuffers list of data buffers
   * @return combined byte array
   */
  private byte[] combineDataBuffers(List<DataBuffer> dataBuffers) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      for (DataBuffer buffer : dataBuffers) {
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        outputStream.write(bytes);
      }
      return outputStream.toByteArray();
    } catch (IOException e) {
      log.error("Error combining data buffers", e);
      throw new RuntimeException("Failed to combine input data", e);
    }
  }

  /**
   * Processes data using StreamConverter with given commands.
   *
   * @param inputData input byte array
   * @param commands stream commands to execute
   * @return processed output byte array
   */
  private byte[] processWithStreamConverter(byte[] inputData, IStreamCommand... commands) {
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(inputData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      StreamConverter converter = StreamConverter.create(commands);
      converter.run(inputStream, outputStream);

      return outputStream.toByteArray();

    } catch (IOException e) {
      log.error("Error processing with StreamConverter", e);
      throw new RuntimeException("Stream processing failed", e);
    }
  }

  /**
   * Creates a Flux of DataBuffer from byte array.
   *
   * @param data byte array to convert
   * @return Flux of DataBuffer
   */
  private Flux<DataBuffer> createDataBufferFlux(byte[] data) {
    DataBuffer buffer = new DefaultDataBufferFactory().wrap(data);
    return Flux.just(buffer);
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
            case "json" -> JsonNavigateCommand.create(new JSONPath(parameter), new PassThroughRule());
            case "process" -> new SampleStreamCommand(parameter);
            default -> throw new IllegalArgumentException("Unknown command type: " + commandType);
          };
    }

    log.info("Built pipeline with {} commands", commands.length);
    return commands;
  }
}
