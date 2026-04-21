import java.math.BigDecimal

plugins {
    id("java")
    id("jacoco")
    id("pmd")
    id("com.github.spotbugs") version "6.5.1"
    id("com.diffplug.spotless") version "8.4.0"
    id("info.solidsoft.pitest") version "1.19.0"
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
    mavenCentral {
        // ネットワークタイムアウト対策：リトライとタイムアウト設定
        content {
            // Maven Centralから取得するアーティファクトを明示的に指定
            includeGroupByRegex(".*")
        }
    }
    // フォールバック用の代替リポジトリ
    gradlePluginPortal()
}

dependencies {
    // Logging
    implementation("ch.qos.logback:logback-core:1.5.32")
    implementation("ch.qos.logback:logback-classic:1.5.32")

    // メインの依存関係
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("commons-io:commons-io:2.21.0")

    // JSON Schema validation
    implementation("com.networknt:json-schema-validator:3.0.1")

    // JsonSurfer for streaming JSON processing
    implementation("com.github.jsurfer:jsurfer-jackson:1.6.5")

    // CSV validation support
    implementation("com.opencsv:opencsv:5.12.0")

    // JUnit 5 の依存関係（テスト用）
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.5"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.pitest:pitest-junit5-plugin:1.2.3")

    // Mockito の依存関係（テスト用）
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework:spring-test")

    // In-memory filesystem for cross-platform file system tests
    testImplementation("com.google.jimfs:jimfs:1.3.1")
    // Classpath scanning for streaming contract test discovery
    testImplementation("io.github.classgraph:classgraph:4.8.184")
    testImplementation(project(":streamconverter-http"))
    testImplementation(project(":streamconverter-tools"))
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
    // JUnit 5 を使うための設定
    useJUnitPlatform {
        // ベンチマークテストを通常のテスト実行から除外
        excludeTags("benchmark", "large-data")
    }

    // Exclude benchmark-related tests by class name and path patterns as well
    exclude("**/benchmark/**", "**/*Benchmark*", "**/MemoryEfficiencyTest*")
    
    // クロスプラットフォーム対応のJVM設定
    jvmArgs("-Xmx2g", "-Xms1g", "-Dfile.encoding=UTF-8")
    
    // Windows/macOS環境での並列実行制御
    if (org.gradle.internal.os.OperatingSystem.current().isWindows() || 
        org.gradle.internal.os.OperatingSystem.current().isMacOsX()) {
        maxParallelForks = 1  // シーケンシャル実行
        forkEvery = 1         // テストクラス毎にJVM再起動
    }
    
    // CI環境での安定性を考慮した条件付きタスク実行
    if (org.gradle.internal.os.OperatingSystem.current().isLinux()) {
        // テスト完了後にJaCoCoレポートを生成（Linuxのみ、より安定）
        finalizedBy(tasks.jacocoTestReport)
    }
    // テスト実行後にjavadocを生成
    finalizedBy(tasks.javadoc)
}

// JaCoCoレポートの設定（Windows以外でのみ実行）
tasks.jacocoTestReport {
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
    // Linux以外では無効化してネットワーク問題を回避（Windows/macOS対策）
    enabled = org.gradle.internal.os.OperatingSystem.current().isLinux()
}

// PITレポートの設定
tasks.pitest {
    targetClasses.set(listOf("com.streamconverter.*")) // テスト対象のクラスを指定
    outputFormats.set(listOf("HTML")) // 出力形式を指定
    // タイムアウト設定を追加
    timeoutConstInMillis.set(10000) // 10秒でタイムアウト
    timeoutFactor.set(BigDecimal("1.5")) // 1.5倍のマージン
    // 対象クラスを絞り込んでパフォーマンスを向上
    excludedClasses.set(listOf(
        "com.streamconverter.examples.*", // サンプルコードを除外
        "com.streamconverter.demo.*"      // デモコードを除外
    ))
}

// javadocタスクの設定
tasks.javadoc {
    options.encoding = "UTF-8"
    options.memberLevel = org.gradle.external.javadoc.JavadocMemberLevel.PROTECTED
    setDestinationDir(layout.buildDirectory.dir("docs/javadoc").get().asFile)
}

// spotlessCheck タスクを無効化
tasks.named("spotlessCheck") {
    enabled = false
}

// PMD configuration for code smell detection
pmd {
    isConsoleOutput = false
    toolVersion = "7.16.0"
    rulesMinimumPriority = 5
    ruleSetFiles = rootProject.files("config/pmd/ruleset.xml")
    ruleSets = emptyList()
    isIgnoreFailures = true // PMD違反があってもビルドを継続
}

// PMD task configuration
tasks.pmdMain {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    exclude("**/examples/**", "**/demo/**")
}

// SpotBugs configuration for bug pattern detection
spotbugs {
    toolVersion.set("4.8.6")
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
    excludeFilter.set(file("../spotbugs-exclude.xml"))
}

// SpotBugs task configuration
tasks.spotbugsMain {
    reports.create("html") {
        required.set(true)
        outputLocation.set(file("build/reports/spotbugs/main.html"))
    }
    reports.create("xml") {
        required.set(true)
        outputLocation.set(file("build/reports/spotbugs/main.xml"))
    }
    reports.create("sarif") {
        required.set(true) 
        outputLocation.set(file("build/reports/spotbugs/main.sarif"))
    }
}

tasks.spotbugsTest {
}

// check タスクの実行時に spotlessApply を依存タスクとして実行する
tasks.named("check") {
    dependsOn("spotlessApply")
}
