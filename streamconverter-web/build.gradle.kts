plugins {
    id("java")
    id("jacoco")
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

description = "StreamConverter Web API module - Example implementation"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        // Automatically detect available Java 21 installations
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":streamconverter-core"))
    
    // Spring Boot Web dependencies
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.bootJar {
    archiveClassifier.set("boot")
}

tasks.jar {
    enabled = true
    archiveClassifier.set("")
}
