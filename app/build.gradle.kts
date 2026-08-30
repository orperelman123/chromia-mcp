import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mcpVersion = "0.7.7"
val ktorVersion = "3.2.3"
val postchainClientVersion = "3.36.0"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("com.gradleup.shadow") version "8.3.6"
    id("com.google.cloud.tools.jib") version "3.4.5"
    id("maven-publish")
}

group = "com.chromia"
version = project.findProperty("version")?.toString() ?: error("Version is not set")

// Health / MCP Implementation.version. Gradle `project.version`:
// gradle.properties pins 0.2.2 (latest official GitLab tag). Publish CI
// overrides with -Pversion=$CI_COMMIT_TAG.
val generateBuildInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/sources/buildInfo/kotlin")
    val projectVersion = project.version.toString()
    inputs.property("projectVersion", projectVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().asFile.resolve("org/chromia/BuildInfo.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package org.chromia

            /**
             * Server version from Gradle `project.version`.
             * Default is gradle.properties `version` (0.2.2, latest official GitLab tag).
             * Publish jobs override with -Pversion and the CI commit tag.
             */
            object BuildInfo {
                const val VERSION = "$projectVersion"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    sourceSets.getByName("main").kotlin.srcDir(generateBuildInfo)
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
    implementation("net.postchain.client:postchain-client:$postchainClientVersion")
    implementation("net.postchain.client:chromia-client:$postchainClientVersion")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("dev.langchain4j:langchain4j-easy-rag:1.8.0-beta15")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.1")
    implementation("org.apache.logging.log4j:log4j-core:2.25.1")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    // Umbrella kotlin-sdk only exposes the client at runtime; tests compile against Client/StdioClientTransport.
    testImplementation("io.modelcontextprotocol:kotlin-sdk-client:$mcpVersion")
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
compileKotlin.dependsOn(generateBuildInfo)

tasks.shadowJar {
    archiveBaseName.set("chromia-mcp-server")
    archiveClassifier.set("")
    // Docs, jib.yaml, and local java -jar examples use chromia-mcp-server.jar.
    // Version stays in the manifest Implementation-Version / BuildInfo.
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "org.chromia.AppKt"
        attributes["Implementation-Title"] = "chromia-mcp-server"
        attributes["Implementation-Version"] = project.version.toString()
    }
    mergeServiceFiles()
}

tasks.named("jar") {
    enabled = false
}

// jib and shadowJar both write under app/build/libs. Do not let Gradle run them concurrently.
listOf("jib", "jibDockerBuild", "jibBuildTar").forEach { name ->
    tasks.findByName(name)?.mustRunAfter(tasks.named("shadowJar"))
}

val localEmbeddingsFile = layout.buildDirectory.file("embeddings.json")

fun JavaExec.withLocalEmbeddingsPath() {
    environment("CHROMIA_EMBEDDINGS_PATH", localEmbeddingsFile.get().asFile.absolutePath)
}

tasks.register<JavaExec>("run") {
    dependsOn("shadowJar")
    group = "application"
    description = "Runs the Chromia MCP server in stdio mode"
    classpath = files(tasks.shadowJar.get().archiveFile)
    mainClass.set("org.chromia.AppKt")
    args = listOf("--stdio")
    standardInput = System.`in`
    standardOutput = System.out
    withLocalEmbeddingsPath()
}

tasks.register<JavaExec>("runSse") {
    dependsOn("shadowJar")
    group = "application"
    description = "Runs the Chromia MCP server in SSE mode on port 3001"
    classpath = files(tasks.shadowJar.get().archiveFile)
    mainClass.set("org.chromia.AppKt")
    args = listOf("--sse")
    withLocalEmbeddingsPath()
}

tasks.register<JavaExec>("generateEmbeddings") {
    dependsOn("shadowJar")
    group = "application"
    description = "Fetch documentation, create embeddings, persist locally, and upload embeddings.json to GitLab packages"
    classpath = files(tasks.shadowJar.get().archiveFile)
    mainClass.set("org.chromia.AppKt")
    args = listOf("--generate-embeddings")
    jvmArgs = listOf("-Xmx4g")
    withLocalEmbeddingsPath()
}

tasks.register<JavaExec>("generateEmbeddingsNoUpload") {
    dependsOn("shadowJar")
    group = "application"
    description = "Fetch documentation, create embeddings, and persist embeddings.json locally (no GitLab upload)"
    classpath = files(tasks.shadowJar.get().archiveFile)
    mainClass.set("org.chromia.AppKt")
    args = listOf("--generate-embeddings-no-upload")
    jvmArgs = listOf("-Xmx4g")
    withLocalEmbeddingsPath()
}

jib {
    from {
        image = "eclipse-temurin:21-jre-jammy@sha256:2843f155a9fe5aab6a73a71a9f65c38143e8e929366a1a7787f07c2a89c26887"
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

publishing {
    repositories {
        maven {
            name = "GitLab"
            url = uri("https://gitlab.com/api/v4/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
            credentials(HttpHeaderCredentials::class.java) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
    publications {
        create<MavenPublication>("chromia-mcp") {
            artifactId = "chromia-mcp"
            from(components["shadow"])
        }
    }
}
