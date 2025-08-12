plugins {
    id("java")
    id("application")
    id("com.diffplug.spotless") version "7.2.1"
    id("org.springframework.boot") version "3.4.5"
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
    
    // SLF4J logging (needed by examples)
    implementation("org.slf4j:slf4j-api")
    implementation("ch.qos.logback:logback-classic")
    
    // H2 database for examples that use databases
    implementation("com.h2database:h2:2.2.224")
    
    // JUnit 5 の依存関係（テスト用）
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Mockito の依存関係（テスト用）
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
}

// Custom tasks for running examples
tasks.register<JavaExec>("runQuickStart") {
    group = "application"
    description = "Run QuickStart example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamConverter.examples.QuickStart")
}

tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Run StreamConverter demo"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamConverter.demo.StreamConverterDemo")
}

tasks.register<JavaExec>("runDataProcessing") {
    group = "application"
    description = "Run DataProcessing examples"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamConverter.examples.DataProcessingExamples")
}

tasks.register<JavaExec>("runAutoLoggingDemo") {
    group = "application"
    description = "Run Auto Logging Demo"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamConverter.examples.AutoLoggingDemo")
}

tasks.register<JavaExec>("runMDC") {
    group = "application"
    description = "Run MDC Multi-Thread Example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamConverter.examples.MDCMultiThreadExample")
}

tasks.register<JavaExec>("runContextDemo") {
    group = "application"
    description = "Run Context Propagation Demo"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamConverter.examples.ContextPropagationDemo")
}

tasks.register<JavaExec>("runFluentApiDemo") {
    group = "application"
    description = "Run Fluent API Demo"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamConverter.examples.FluentApiDemo")
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
    
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Main class configuration for default application task
application {
    mainClass.set("com.streamConverter.examples.QuickStart")
}

// spotlessCheck タスクを無効化
tasks.named("spotlessCheck") {
    enabled = false
}

// check タスクの実行時に spotlessApply を依存タスクとして実行する
tasks.named("check") {
    dependsOn("spotlessApply")
}