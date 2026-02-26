plugins {
    id("java")
    id("application")
    id("com.diffplug.spotless") version "8.1.0"
    id("org.springframework.boot") version "3.5.7"
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
    implementation(project(":streamconverter-db"))
    
    // SLF4J logging (needed by examples)
    implementation("org.slf4j:slf4j-api")
    implementation("ch.qos.logback:logback-classic")
    
    // H2 database for examples that use databases
    implementation("com.h2database:h2:2.4.240")
    
    // JUnit 5 の依存関係（テスト用）
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Mockito の依存関係（テスト用）
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
}

// Custom tasks for running examples
tasks.register<JavaExec>("runQuickStart") {
    group = "application"
    description = "Run QuickStart example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamconverter.examples.QuickStart")
}

tasks.register<JavaExec>("runMDC") {
    group = "application"
    description = "Run MDC Multi-Thread Example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamconverter.examples.StreamConverterMDCDemo")
}

// Optional helpers for specific examples (kept minimal)
tasks.register<JavaExec>("runComplexPipeline") {
    group = "application"
    description = "Run Complex Pipeline Example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamconverter.examples.ComplexPipelineExample")
}
tasks.register<JavaExec>("runValidationExample") {
    group = "application"
    description = "Run Validation Example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamconverter.examples.ValidationExample")
}
tasks.register<JavaExec>("runDatabaseRuleDemo") {
    group = "application"
    description = "Run Database Rule Demo"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.streamconverter.examples.DatabaseRuleDemo")
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
        events("skipped", "failed")
        showStandardStreams = true
    }
}

// Main class configuration for default application task
application {
    mainClass.set("com.streamconverter.examples.QuickStart")
}

// spotlessCheck タスクを無効化
tasks.named("spotlessCheck") {
    enabled = false
}

// check タスクの実行時に spotlessApply を依存タスクとして実行する
tasks.named("check") {
    dependsOn("spotlessApply")
}
