plugins {
    id("java")
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

description = "StreamConverter Web API module - Example implementation"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":streamconverter-core"))

    // Spring Boot Web dependencies
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Explicit Netty overrides retained for security/compat compatibility
    implementation("io.netty:netty-handler:4.2.4.Final")
    implementation("io.netty:netty-common:4.2.4.Final")

    // IP address validation
    implementation("com.google.guava:guava:33.4.8-jre")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveClassifier.set("boot")
}

tasks.jar {
    enabled = true
    archiveClassifier.set("")
}
