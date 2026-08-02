package com.streamconverter.web;

import com.streamconverter.StreamConverter;
import com.streamconverter.UncheckedStreamException;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvWalker;
import com.streamconverter.command.impl.json.JsonWalker;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
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

  private static final Logger logger = LoggerFactory.getLogger(StreamProcessingController.class);

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

    return Mono.fromCallable(
            () ->
                ResponseEntity.ok(
                    processWithStreamConverter(
                        inputData,
                        CsvWalker.create(CSVPath.of(columnName), new PassThroughRule()))))
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

    return Mono.fromCallable(
            () ->
                ResponseEntity.ok(
                    processWithStreamConverter(
                        inputData,
                        JsonWalker.create(TreePath.fromJson(jsonPath), new PassThroughRule()))))
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

    return Mono.fromCallable(() -> PipelineConfigParser.parse(pipelineConfig))
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
    return Flux.from(
            DataBufferUtils.outputStreamPublisher(
                outputStream -> {
                  try (InputStream inputStream =
                      DataBufferUtils.subscriberInputStream(inputData, 4096)) {
                    StreamConverter converter = StreamConverter.create(commands);
                    converter.run(inputStream, outputStream);
                  } catch (IOException e) {
                    // OutputStream コールバックはチェック例外を宣言できないため、
                    // IOException はキャリア例外に載せて搬送する（onErrorResume で 500 に変換される）。
                    throw new UncheckedStreamException(
                        new IOException("Stream processing failed", e));
                  }
                },
                bufferFactory,
                executor))
        .doFinally(signalType -> executor.shutdown());
  }
}
