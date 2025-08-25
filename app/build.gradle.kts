import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mcpVersion = "0.4.0"
val ktorVersion = "3.1.1"
val postchainClientVersion = "3.36.0"

plugins {
    kotlin("plugin.serialization") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    alias(libs.plugins.kotlin.jvm)
    id("com.google.cloud.tools.jib") version "3.4.5"
}

repositories {
    mavenCentral()
    maven("https://gitlab.com/api/v4/projects/50818999/packages/maven")
    maven("https://gitlab.com/api/v4/projects/32294340/packages/maven")
    maven("https://gitlab.com/api/v4/projects/46288950/packages/maven")
}

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:$mcpVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-sse:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
    implementation("net.postchain.client:postchain-client:$postchainClientVersion")
    implementation("net.postchain.client:chromia-client:${postchainClientVersion}")
    implementation("com.google.code.gson:gson:latest")
    implementation(libs.guava)
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.1")
    implementation("org.apache.logging.log4j:log4j-core:2.25.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-XXLanguage:+MultiDollarInterpolation"))
}

tasks.shadowJar {
    archiveBaseName.set("chromia-mcp-server")
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "org.chromia.AppKt"
    }
    mergeServiceFiles()
}

tasks.named("jar") {
    enabled = false
}

jib {
    from {
        image = "registry.gitlab.com/chromaway/core-tools/chromia-images/java21:1.0.8@sha256:82d2e91e86908fb1095ff4bf5b42c2412f280a91362d2d216f89bd51fa48c80c"
        if (System.getenv("CI_REGISTRY_IMAGE") != null) {
            platforms {
                platform {
                    architecture = "amd64"
                    os = "linux"
                }
                platform {
                    architecture = "arm64"
                    os = "linux"
                }
            }
        }
    }
    to {
        if (System.getenv("CI_REGISTRY_IMAGE") == null) {
            image = "chromia-mcp"
        } else {
            image = "${System.getenv("CI_REGISTRY_IMAGE")}/chromia-mcp"
            auth {
                username = System.getenv("CI_REGISTRY_USER")
                password = System.getenv("CI_REGISTRY_PASSWORD")
            }
        }
        if (System.getenv("CI_COMMIT_TAG") != null) {
            tags = setOf(System.getenv("CI_COMMIT_TAG"))
        }
    }
}
