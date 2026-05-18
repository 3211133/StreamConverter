plugins {
    id("java")
}

description = "StreamConverter HTTP module - SendHttpCommand for HTTP stream processing"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":streamconverter-core"))

    // Import Spring Boot BOM to align Spring/Reactor/Netty versions
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    // Override Netty version to 4.2.13.Final (fixes CVE-2026-42577 et al.)
    implementation(platform("io.netty:netty-bom:4.2.13.Final"))

    // Logging (version via BOM)
    implementation("ch.qos.logback:logback-core")
    implementation("ch.qos.logback:logback-classic")

    // Reactive HTTP Client
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework:spring-context")
    implementation("io.projectreactor.netty:reactor-netty-http")
    implementation("io.netty:netty-handler")
    implementation("io.netty:netty-common")

    // IP address validation
    implementation("com.google.guava:guava:33.6.0-jre")

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework:spring-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Xmx2g", "-Xms1g", "-Dfile.encoding=UTF-8")
}
