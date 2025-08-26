plugins {
    id("java")
    id("application")
    id("com.diffplug.spotless") version "7.2.1"
    id("org.springframework.boot") version "3.4.7"
    id("io.spring.dependency-management") version "1.1.7"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
    
    // SLF4J logging (needed by tools)
    implementation("org.slf4j:slf4j-api")
    implementation("ch.qos.logback:logback-classic")
    
    // H2 database for tools that use databases
    implementation("com.h2database:h2:2.2.224")
    
    // JUnit 5 の依存関係（テスト用）
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Mockito の依存関係（テスト用）
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
    
    // JSON processing with Jackson
    implementation("com.fasterxml.jackson.core:jackson-core:2.19.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.18.2")
}

// Benchmark tasks
tasks.register<Test>("benchmarkLargeData") {
    group = "benchmark"
    description = "Run large data benchmark tests (5GB/50MB target)"
    useJUnitPlatform {
        includeTags("benchmark", "large-data")
    }
    include("**/benchmark/**")
    
    // 5GBテスト用にヒープサイズを大きく設定
    jvmArgs("-Xmx3g", "-Xms1g")
    
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.register<Test>("benchmarkInfrastructure") {
    group = "benchmark"
    description = "Run benchmark infrastructure tests"
    useJUnitPlatform()
    include("**/BenchmarkInfrastructureTest*")
    
    jvmArgs("-Xmx1g", "-Xms512m")
    
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.register<Test>("benchmarkMemoryEfficiency") {
    group = "benchmark"
    description = "Run memory efficiency benchmarks"
    useJUnitPlatform()
    include("**/MemoryEfficiencyTest*")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    // Increase heap size for memory efficiency tests
    jvmArgs("-Xms1g", "-Xmx2g")
}

tasks.register<Test>("benchmarkAll") {
    group = "benchmark"
    description = "Run all benchmark tests"
    useJUnitPlatform()
    include("**/benchmark/**/*Test*", "**/MemoryEfficiencyTest*")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    // Increase heap size for all benchmarks
    jvmArgs("-Xms1g", "-Xmx2g")
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
    useJUnitPlatform {
        // Exclude benchmark tests from normal test execution
        excludeTags("benchmark", "large-data")
    }

    // Exclude benchmark-related tests by class name/path pattern as well
    exclude("**/benchmark/**", "**/*Benchmark*", "**/MemoryEfficiencyTest*")

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Main class configuration - DatabaseInspector as default
application {
    mainClass.set("com.streamConverter.tools.DatabaseInspector")
}

// spotlessCheck タスクを無効化
tasks.named("spotlessCheck") {
    enabled = false
}

// check タスクの実行時に spotlessApply を依存タスクとして実行する
tasks.named("check") {
    dependsOn("spotlessApply")
}
