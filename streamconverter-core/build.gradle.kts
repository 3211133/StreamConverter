import java.math.BigDecimal

plugins {
    id("java")
    id("jacoco")
    id("com.diffplug.spotless") version "7.2.1"
    id("info.solidsoft.pitest") version "1.19.0-rc.1"
    id("org.springframework.boot") version "3.4.1"
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
    // Spring Boot WebFlux
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // メインの依存関係
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("commons-io:commons-io:2.20.0")
    
    // JSON Schema validation
    implementation("com.networknt:json-schema-validator:1.5.8")
    
    // CSV validation support
    implementation("com.opencsv:opencsv:5.12.0")
    
    // IP address validation
    implementation("com.google.guava:guava:33.4.0-jre")
    
    // JSON processing with Jackson
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    
    // Database support
    implementation("com.zaxxer:HikariCP:6.2.1")
    testImplementation("com.h2database:h2:2.2.224")
    
    // Spring Boot Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")

    // JUnit 5 の依存関係（テスト用）
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.pitest:pitest-junit5-plugin:1.2.3")
    
    // Mockito の依存関係（テスト用）
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
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
    
    // メモリ効率テスト用にJVMヒープサイズを設定
    jvmArgs("-Xmx1g", "-Xms512m")
    
    // テスト実行時の詳細ログを表示
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    
    // テスト完了後にJaCoCoレポートを生成
    finalizedBy(tasks.jacocoTestReport)
    // テスト実行後にjavadocを生成
    finalizedBy(tasks.javadoc)
}

// JaCoCoレポートの設定
tasks.jacocoTestReport {
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}

// PITレポートの設定
tasks.pitest {
    targetClasses.set(listOf("com.streamConverter.*")) // テスト対象のクラスを指定
    outputFormats.set(listOf("HTML")) // 出力形式を指定
    // タイムアウト設定を追加
    timeoutConstInMillis.set(10000) // 10秒でタイムアウト
    timeoutFactor.set(BigDecimal("1.5")) // 1.5倍のマージン
    // 対象クラスを絞り込んでパフォーマンスを向上
    excludedClasses.set(listOf(
        "com.streamConverter.examples.*", // サンプルコードを除外
        "com.streamConverter.demo.*"      // デモコードを除外
    ))
}

// javadocタスクの設定
tasks.javadoc {
    options.encoding = "UTF-8"
    options.memberLevel = org.gradle.external.javadoc.JavadocMemberLevel.PROTECTED
    setDestinationDir(file("docs/javadoc"))
}

// spotlessCheck タスクを無効化
tasks.named("spotlessCheck") {
    enabled = false
}

// check タスクの実行時に spotlessApply を依存タスクとして実行する
tasks.named("check") {
    dependsOn("spotlessApply")
}