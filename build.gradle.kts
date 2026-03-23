plugins {
    java
    kotlin("jvm") version "2.1.10"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.retry:spring-retry:2.0.6")
    implementation("org.springframework:spring-aspects:6.2.12")

    implementation ("org.apache.httpcomponents.client5:httpclient5")
    implementation("com.google.transit:gtfs-realtime-bindings:0.0.4")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks.test {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.wrapper {
    gradleVersion = "9.0.0"
}
