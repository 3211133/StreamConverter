plugins {
    id("java")
}

description = "StreamConverter DB module - Database fetch rules for stream transformation"

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

    // Logging
    implementation("ch.qos.logback:logback-core:1.5.37")
    implementation("ch.qos.logback:logback-classic:1.5.37")

    // HikariCP connection pool
    implementation("com.zaxxer:HikariCP:7.0.2")

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mockito
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")

    // H2 in-memory database for testing
    testImplementation("com.h2database:h2:2.4.240")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("performance")
    }
    jvmArgs("-Xmx2g", "-Xms1g", "-Dfile.encoding=UTF-8")
}
