plugins {
    id("java")
    id("jacoco")
    id("application")
    id("com.diffplug.spotless") version "8.3.0"
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
    
    // SLF4J logging (needed by tools)
    implementation("org.slf4j:slf4j-api")
    implementation("ch.qos.logback:logback-classic")
    
    // H2 database for tools that use databases
    implementation("com.h2database:h2:2.4.240")
    
    // JUnit 5 の依存関係（テスト用）
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Mockito の依存関係（テスト用）
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
    
    // JSON processing with Jackson
    implementation("com.fasterxml.jackson.core:jackson-core:2.21.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.21")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.21.1")
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
        events("skipped", "failed")
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
        events("skipped", "failed")
        showStandardStreams = true
    }
}

tasks.register<Test>("benchmarkMemoryEfficiency") {
    group = "benchmark"
    description = "Run memory efficiency benchmarks"
    useJUnitPlatform()
    include("**/MemoryEfficiencyTest*")
    testLogging {
        events("skipped", "failed")
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
        events("skipped", "failed")
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
        events("skipped", "failed")
        showStandardStreams = true
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}

// Main class configuration - DatabaseInspector as default
application {
    mainClass.set(project.findProperty("mainClass")?.toString() ?: "com.streamconverter.tools.DatabaseInspector")
}

tasks.register<JavaExec>("slocCount") {
    group = "analysis"
    description = "Count SLOC across all modules from JaCoCo reports"
    classpath = configurations["runtimeClasspath"] + sourceSets.main.get().output
    mainClass.set("com.streamconverter.sloc.SlocCounter")
    workingDir = rootProject.projectDir

    // JaCoCo が設定されているサブプロジェクトのテスト＆レポート生成タスクに依存
    // test タスクが finalizedBy(jacocoTestReport) を持つため、test に依存するだけで XML が生成される
    //
    // 対象モジュールの制約:
    //   - jacoco プラグイン適用済みモジュール（streamconverter-core, streamconverter-tools）のみを対象とする
    //   - streamconverter-db / streamconverter-http / streamconverter-web 等は jacoco 未適用のため集計対象外
    //   - streamconverter-core は Linux 環境でのみ jacocoTestReport が有効（core/build.gradle.kts 参照）。
    //     macOS/Windows では XML が生成されないためそのモジュールの SLOC は 0 扱いになる。
    //     ローカル開発でも正確な全体集計が必要な場合は Linux 環境（CI）で実行すること。
    val jacocoModules = rootProject.subprojects.filter { sub ->
        sub.plugins.hasPlugin("jacoco")
    }
    dependsOn(jacocoModules.map { "${it.path}:test" })

    // モジュール名を引数として渡す
    args = jacocoModules.map { it.name }
}

// spotlessCheck タスクを無効化
tasks.named("spotlessCheck") {
    enabled = false
}

// check タスクの実行時に spotlessApply を依存タスクとして実行する
tasks.named("check") {
    dependsOn("spotlessApply")
}
