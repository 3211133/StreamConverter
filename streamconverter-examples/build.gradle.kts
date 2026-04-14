plugins {
    id("java")
    id("application")
    id("com.diffplug.spotless") version "8.4.0"
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        // Automatically detect available Java 21 installations
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
}

dependencies {
    // Core module dependency
    implementation(project(":streamconverter-core"))

    // SLF4J logging (needed by examples)
    implementation("org.slf4j:slf4j-api")
    implementation("ch.qos.logback:logback-classic")
    
    // JUnit 5 の依存関係（テスト用）
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Mockito の依存関係（テスト用）
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
}

// Custom tasks for running examples
tasks.register<JavaExec>("runPipelineBasics") {
    group = "application"
    description = "例1: IStreamCommand と StreamConverter の仕組み（ラムダ・クラス・組み込みコマンド）"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamconverter.examples.PipelineBasicsExample")
}

tasks.register<JavaExec>("runNavigateAndRule") {
    group = "application"
    description = "例2: Navigate系コマンド × IRule（CSV/JSON/XML、組み込み・ラムダ・カスタムRule）"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamconverter.examples.NavigateAndRuleExample")
}

tasks.register<JavaExec>("runPipelineContext") {
    group = "application"
    description = "例3: PipelineContext によるスレッド間値共有と MDC 伝搬"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamconverter.examples.PipelineContextExample")
}


tasks.register<JavaExec>("runValidationPipeline") {
    group = "application"
    description = "例5: ConsumerCommand × FileBufferCommand による安全なパイプライン"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamconverter.examples.ValidationPipelineExample")
}

// Spotless configuration for code formatting
spotless {
    java {
        // Use Google's Java formatting style
        googleJavaFormat()
        // Remove unused imports
        importOrder()
        // Remove trailing whitespace
        trimTrailingWhitespace()
        // Ensure files end with a newline
        endWithNewline()
    }
}

tasks.test {
    useJUnitPlatform()
    
}

// Main class configuration for default application task
application {
    mainClass.set("com.streamconverter.examples.PipelineBasicsExample")
}

// spotlessCheck タスクを無効化
tasks.named("spotlessCheck") {
    enabled = false
}

// check タスクの実行時に spotlessApply を依存タスクとして実行する
tasks.named("check") {
    dependsOn("spotlessApply")
}
