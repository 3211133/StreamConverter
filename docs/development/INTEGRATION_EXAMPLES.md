# StreamConverter 統合パターンと実装例

## このドキュメントの基礎資料
このドキュメントは以下の資料および実装を基に作成されています：
- [ARCHITECTURE.md](../ARCHITECTURE.md) - アーキテクチャ設計と設計原則
- [handbook/web-api.md](../handbook/web-api.md) - Web API概要
- [StreamProcessingController.java](../../streamconverter-web/src/main/java/com/streamconverter/web/StreamProcessingController.java) - Spring Boot統合の実装
- Spring Boot / Quarkus / Vert.x の公式ドキュメント

> 意図と実行タイミングについて
>
> - 目的: ここに掲載する例は、コマンド/ルールの組み合わせや入出力の流れを理解するための学習・動作確認用です。性能比較や厳密な仕様検証は目的にしていません。
> - 利用場面: CSV/JSON/XML 向けのコマンド配線、ルール構成、API 利用の参考として活用してください。再現性の高い確認は各モジュールの `src/test/java` にあるテストコードで行います。
> - 環境依存: ネットワークやOS依存のケースは、実運用ではモック (WireMock/Testcontainers など) を使うことを推奨します。本ドキュメントでは説明の簡潔化のため省略している場合があります。

## 📋 概要

StreamConverterライブラリは様々なフレームワークや環境との統合が可能です。本ドキュメントでは、主要な統合パターンと実装例を紹介し、実際のプロジェクトでの活用方法を説明します。

## 🚀 Spring Boot WebFlux統合

### アーキテクチャ概要

**実装**: `StreamConverterWebApplication.java:1`  
**特徴**: Spring Boot WebFluxベースの非同期・ノンブロッキングWebAPI

```java
@SpringBootApplication
public class StreamConverterWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamConverterWebApplication.class, args);
    }
}
```

### 1. 基本設定

#### Maven/Gradle設定

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // StreamConverter Core
    implementation(project(":streamconverter-core"))
}
```

#### アプリケーション設定

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: stream-converter-api
  webflux:
    multipart:
      max-in-memory-size: 1MB
      max-disk-usage-per-part: 10MB

logging:
  level:
    com.streamConverter: INFO
```

### 2. Web APIコントローラー実装

**実装**: `StreamProcessingController.java:1`

#### エンドポイント一覧

| エンドポイント | メソッド | 説明 | 実装行 |
|---------------|---------|------|-------|
| `/api/v1/stream/health` | GET | ヘルスチェック | `112-115` |
| `/api/v1/stream/csv/extract` | POST | CSV列抽出 | `41-56` |
| `/api/v1/stream/json/extract` | POST | JSONパス抽出 | `65-80` |
| `/api/v1/stream/process` | POST | パイプライン処理 | `89-105` |

#### CSV列抽出エンドポイント

```java
@PostMapping(
    value = "/csv/extract",
    consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
    produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
public Mono<ResponseEntity<Flux<DataBuffer>>> processCsvExtraction(
    @RequestBody Flux<DataBuffer> inputData, 
    @RequestParam String columnName) {
    
    return inputData
        .collectList()
        .map(this::combineDataBuffers)
        .map(data -> processWithStreamConverter(data, new CsvNavigateCommand(columnName)))
        .map(result -> ResponseEntity.ok(createDataBufferFlux(result)))
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
}
```

#### パイプライン処理エンドポイント

```java
@PostMapping(value = "/process")
public Mono<ResponseEntity<Flux<DataBuffer>>> processWithPipeline(
    @RequestBody Flux<DataBuffer> inputData,
    @RequestHeader("X-Pipeline-Config") String pipelineConfig) {
    
    return inputData
        .collectList()
        .map(this::combineDataBuffers)
        .map(data -> processWithStreamConverter(data, buildPipelineFromConfig(pipelineConfig)))
        .map(result -> ResponseEntity.ok(createDataBufferFlux(result)))
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
}
```

### 3. 使用例

#### cURL での API呼び出し

```bash
# 1. ヘルスチェック
curl -X GET "http://localhost:8080/api/v1/stream/health"

# 2. CSV列抽出
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @input.csv \
  "http://localhost:8080/api/v1/stream/csv/extract?columnName=name"

# 3. JSON パス抽出
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @input.json \
  "http://localhost:8080/api/v1/stream/json/extract?jsonPath=$.user.name"

# 4. パイプライン処理
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  -H "X-Pipeline-Config: csv:name,json:$.result" \
  --data-binary @input.csv \
  "http://localhost:8080/api/v1/stream/process"
```

#### Java WebClient での呼び出し

```java
@Service
public class StreamConverterWebClient {
    
    private final WebClient webClient;
    
    public StreamConverterWebClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://localhost:8080").build();
    }
    
    public Mono<byte[]> extractCsvColumn(byte[] csvData, String columnName) {
        return webClient.post()
            .uri("/api/v1/stream/csv/extract?columnName={column}", columnName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(csvData)
            .retrieve()
            .bodyToMono(byte[].class);
    }
    
    public Mono<byte[]> processWithPipeline(byte[] inputData, String pipelineConfig) {
        return webClient.post()
            .uri("/api/v1/stream/process")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("X-Pipeline-Config", pipelineConfig)
            .bodyValue(inputData)
            .retrieve()
            .bodyToMono(byte[].class);
    }
}
```

### 4. 高度な統合パターン

#### 動的バリデーション統合

```java
@RestController
@RequestMapping("/api/v1/advanced")
public class AdvancedStreamController {

    @PostMapping("/process-with-validation")
    public Mono<ResponseEntity<Flux<DataBuffer>>> processWithValidation(
        @RequestBody Flux<DataBuffer> inputData,
        @RequestParam String dataType,
        @RequestParam String validationRules) {

        return inputData
            .collectList()
            .map(this::combineDataBuffers)
            .map(data -> {
                // バリデーションコマンドを動的作成
                IStreamCommand validator = switch (dataType.toLowerCase()) {
                    case "csv" -> new CsvValidateCommand(parseValidationRules(validationRules));
                    case "json" -> JsonValidateCommand.create(validationRules);
                    case "xml" -> new ValidateCommand(validationRules);
                    default -> throw new IllegalArgumentException("Unsupported data type: " + dataType);
                };

                return processWithStreamConverter(data, validator);
            })
            .map(result -> ResponseEntity.ok(createDataBufferFlux(result)))
            .onErrorReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }
}
```

#### リアクティブストリーミング処理

```java
@Value("${stream.buffer.size:1000}")
private int bufferSize;

@PostMapping("/stream/reactive")
public Flux<DataBuffer> processReactiveStream(
    @RequestBody Flux<DataBuffer> inputData,
    @RequestParam String command) {
    
    return inputData
        .buffer(bufferSize) // バッファリング（設定値を利用）
        .flatMap(buffers -> Mono.fromCallable(() -> {
            byte[] data = combineDataBuffers(buffers);
            IStreamCommand cmd = createCommandFromString(command);
            return processWithStreamConverter(data, cmd);
        }))
        .map(this::createDataBufferFlux)
        .flatMap(flux -> flux);
}
```

## 🔄 Spring Boot WebMVC統合

### 同期型REST API実装

```java
@RestController
@RequestMapping("/api/v2/stream")
public class SyncStreamController {
    
    @PostMapping("/csv/extract")
    public ResponseEntity<byte[]> extractCsvColumn(
        @RequestBody byte[] csvData,
        @RequestParam String columnName) {
        
        try (ByteArrayInputStream input = new ByteArrayInputStream(csvData);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            
            StreamConverter converter = StreamConverter.create(
                new CsvNavigateCommand(columnName)
            );
            converter.run(input, output);
            
            return ResponseEntity.ok(output.toByteArray());
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

## 📊 バッチ処理統合

### Spring Batch統合

```java
@Configuration
@EnableBatchProcessing
public class StreamConverterBatchConfig {
    
    @Bean
    public Job dataProcessingJob(JobRepository jobRepository, Step processStep) {
        return new JobBuilder("dataProcessingJob", jobRepository)
            .start(processStep)
            .build();
    }
    
    @Bean
    public Step processStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("processStep", jobRepository)
            .<String, String>chunk(100, transactionManager)
            .reader(fileReader())
            .processor(streamConverterProcessor())
            .writer(fileWriter())
            .build();
    }
    
    @Bean
    public ItemProcessor<String, String> streamConverterProcessor() {
        return new ItemProcessor<String, String>() {
            @Override
            public String process(String input) throws Exception {
                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    
                    StreamConverter converter = StreamConverter.create(
                        new CsvNavigateCommand("processedData"),
                        new JsonNavigateCommand("$.result")
                    );
                    converter.run(inputStream, outputStream);
                    
                    return outputStream.toString(StandardCharsets.UTF_8);
                }
            }
        };
    }
}
```

## 🌐 マイクロサービス統合

### Docker コンテナ化

```dockerfile
# Dockerfile
FROM openjdk:21-jre-slim

COPY build/libs/streamconverter-web-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  streamconverter-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SERVER_PORT=8080
    volumes:
      - ./logs:/app/logs
    networks:
      - microservices
    
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - streamconverter-api
    networks:
      - microservices

networks:
  microservices:
    driver: bridge
```

### Kubernetes デプロイメント

```yaml
# k8s-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: streamconverter-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: streamconverter-api
  template:
    metadata:
      labels:
        app: streamconverter-api
    spec:
      containers:
      - name: api
        image: streamconverter-api:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: streamconverter-service
spec:
  selector:
    app: streamconverter-api
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

## 🔌 カスタムフレームワーク統合

### Quarkus統合

```java
@Path("/stream")
@ApplicationScoped
public class QuarkusStreamResource {
    
    @POST
    @Path("/csv/extract")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> extractCsvColumn(
        byte[] inputData,
        @QueryParam("column") String columnName) {
        
        return Uni.createFrom().item(() -> {
            try (ByteArrayInputStream input = new ByteArrayInputStream(inputData);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                
                StreamConverter converter = StreamConverter.create(
                    new CsvNavigateCommand(columnName)
                );
                converter.run(input, output);
                
                return Response.ok(output.toByteArray()).build();
                
            } catch (IOException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        });
    }
}
```

### Vert.x統合

```java
public class VertxStreamVerticle extends AbstractVerticle {
    
    @Override
    public void start() {
        Router router = Router.router(vertx);
        
        router.post("/api/stream/csv/extract")
            .consumes("application/octet-stream")
            .handler(this::handleCsvExtraction);
        
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080);
    }
    
    private void handleCsvExtraction(RoutingContext context) {
        String columnName = context.request().getParam("column");
        
        context.request().bodyHandler(buffer -> {
            vertx.executeBlocking(promise -> {
                try (ByteArrayInputStream input = new ByteArrayInputStream(buffer.getBytes());
                     ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    
                    StreamConverter converter = StreamConverter.create(
                        new CsvNavigateCommand(columnName)
                    );
                    converter.run(input, output);
                    
                    promise.complete(output.toByteArray());
                    
                } catch (IOException e) {
                    promise.fail(e);
                }
            }, result -> {
                if (result.succeeded()) {
                    context.response()
                        .putHeader("Content-Type", "application/octet-stream")
                        .end(Buffer.buffer((byte[]) result.result()));
                } else {
                    context.response().setStatusCode(500).end();
                }
            });
        });
    }
}
```

## 🧪 テスト統合

### WebMvcTest統合

```java
@WebMvcTest(StreamProcessingController.class)
class StreamProcessingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCsvExtraction() throws Exception {
        String csvData = "name,age\\nJohn,30\\nJane,25";
        
        mockMvc.perform(post("/api/v1/stream/csv/extract")
                .param("columnName", "name")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(csvData.getBytes()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }
}
```

### WebFluxTest統合

```java
@WebFluxTest(StreamProcessingController.class)
class StreamProcessingControllerWebFluxTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void testJsonExtraction() {
        String jsonData = "{\\"user\\":{\\"name\\":\\"John\\"}}";
        
        webTestClient.post()
            .uri("/api/v1/stream/json/extract?jsonPath=$.user.name")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(jsonData.getBytes())
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM);
    }
}
```

## 📊 性能最適化パターン

### Connection Pooling

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient streamConverterWebClient() {
        ConnectionProvider provider = ConnectionProvider.builder("streamconverter")
            .maxConnections(100)
            .maxIdleTime(Duration.ofSeconds(20))
            .maxLifeTime(Duration.ofSeconds(60))
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .evictInBackground(Duration.ofSeconds(120))
            .build();
        
        HttpClient httpClient = HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(30));
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl("http://streamconverter-api:8080")
            .build();
    }
}
```

### Caching統合

```java
@Service
@CacheConfig(cacheNames = "streamConverter")
public class CachedStreamService {
    
    @Cacheable(key = "#command + '_' + #data.hashCode()")
    public byte[] processWithCache(byte[] data, String command) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            
            IStreamCommand cmd = createCommandFromString(command);
            StreamConverter converter = StreamConverter.create(cmd);
            converter.run(input, output);
            
            return output.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException("Processing failed", e);
        }
    }
}
```

## 📚 関連ドキュメント

- **[WebAPI仕様書](../../streamconverter-core/src/main/resources/README-WebAPI.md)** - 詳細なAPI仕様
- **[設定ガイド](CONFIGURATION.md)** - アプリケーション設定詳細
- **[セキュリティ分析](../security/SECURITY_ANALYSIS.md)** - セキュリティ考慮事項
- **[アーキテクチャ](../architecture/ARCHITECTURE.md)** - システム全体設計

## 🔄 更新履歴

- **2025-08-16**: 初版作成 - Spring Boot WebFlux統合、マイクロサービス統合パターンの文書化
