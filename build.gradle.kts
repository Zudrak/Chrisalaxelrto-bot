import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.7.9"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.spring") version "1.7.21"
}

group = "dev.arbjerg"
version = "0.1"

repositories {
    mavenCentral()
    maven { url = uri("https://m2.dv8tion.net/releases") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://maven.lavalink.dev/snapshots") }
    maven { url = uri("https://maven.lavalink.dev/releases") }
}

dependencies {
    implementation("net.dv8tion:JDA:5.1.1")
    implementation("dev.arbjerg:lavaplayer:2.2.2")
    implementation("dev.lavalink.youtube:v2:1.10.2")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    runtimeOnly("com.h2database:h2")
    implementation("io.r2dbc:r2dbc-h2")
    implementation("org.flywaydb:flyway-core")
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.6")

    implementation(group = "com.microsoft.azure.cognitiveservices", name = "azure-cognitiveservices-computervision", version = "1.0.9-beta")

    //Tokenizer
    implementation("com.google.api-client:google-api-client:1.32.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.tomcat:tomcat-util:11.0.0-M4")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.4.0")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    
    // import Kotlin API client BOM
    implementation(platform("com.aallam.openai:openai-client-bom:3.2.0"))

// define dependencies without versions
    implementation("com.aallam.openai:openai-client")
    implementation("io.ktor:ktor-client-okhttp")


}

tasks.withType<BootJar> {
    archiveFileName.set("ukulele.jar")
    doLast {
        //copies the jar into a place where the Dockerfile can find it easily (and users maybe too)
        copy {
            from("build/libs/ukulele.jar")
            into(".")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}
