plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.serialization") version "2.1.0"
    id("org.sonarqube") version "6.2.0.5505"
    id("jacoco")
}

group = "id.co.bni"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    val mockkVersion = "1.14.5"
    val springMockkVersion = "4.0.2"
    val ktorVersion = "3.2.2"
    val openTelemetryVersion = "1.42.1"
    val openTelemetryInstrumentationVersion = "2.6.0"
    val slf4jVersion = "1.10.2"

    implementation(platform("io.opentelemetry:opentelemetry-bom:$openTelemetryVersion"))
    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:$openTelemetryInstrumentationVersion"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${slf4jVersion}")
    testImplementation("io.r2dbc:r2dbc-h2")
    testImplementation("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/domains/dtos/**",
                    "**/domains/entities/**",
                    "**/domains/producers/**",
                    "**/applications/resolvers/**",
                    "**/commons/configs/**",
                    "**/commons/exceptions/**",
                    "**/commons/errorhandlers/**",
                    "**/*Dto.class",
                    "**/*Config.class",
                    "**/*Request.class",
                    "**/*Response.class",
                    "**/*Req.class",
                    "**/*Resp.class",
                    "**/ShopeePayRepositoryImpl\$topUp*.class",
                    "**/ShopeePayRepositoryImpl\$getShopeePayBalance*.class",
                    "**/GopayRepositoryImpl\$topUp*.class",
                    "**/GopayRepositoryImpl\$getBalanceByWalletId*.class"
                )
            }
        })
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

sonar {
    properties {
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
    }
}
