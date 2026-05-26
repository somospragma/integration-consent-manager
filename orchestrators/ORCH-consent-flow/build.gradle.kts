plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("jacoco")
    id("info.solidsoft.pitest") version "1.15.0"
}

group = "com.pragma.openfinance"
version = "1.0.0-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories { mavenCentral() }

val resilience4jVersion = "2.2.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.vavr:vavr:0.10.4")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.test { useJUnitPlatform(); finalizedBy(tasks.jacocoTestReport) }
jacoco { toolVersion = "0.8.12" }
tasks.jacocoTestReport { reports { xml.required = true; html.required = true } }
tasks.jacocoTestCoverageVerification {
    violationRules { rule { limit { minimum = "0.90".toBigDecimal() } } }
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }

pitest {
    junit5PluginVersion = "1.2.1"
    targetClasses = setOf("com.pragma.openfinance.orchestrator.domain.*")
    mutationThreshold = 80
    coverageThreshold = 90
    outputFormats = setOf("HTML", "XML")
}
