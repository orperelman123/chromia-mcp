import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mcpVersion = "0.4.0"
val ktorVersion = "3.1.1"
val postchainClientVersion = "3.36.0"

plugins {
    kotlin("plugin.serialization") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.6"
    alias(libs.plugins.kotlin.jvm)
    id("com.google.cloud.tools.jib") version "3.4.5"
    id("maven-publish")
}

group = "com.chromia"
version = project.findProperty("version")?.toString() ?: error("Version is not set")

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

tasks.register<JavaExec>("run") {
    dependsOn("shadowJar")
    group = "application"
    description = "Runs the Chromia MCP server in stdio mode"
    classpath = files(tasks.shadowJar.get().archiveFile)
    mainClass.set("org.chromia.AppKt")
    args = listOf("--stdio")
    standardInput = System.`in`
    standardOutput = System.out
}

tasks.register<JavaExec>("runSse") {
    dependsOn("shadowJar")
    group = "application"
    description = "Runs the Chromia MCP server in SSE mode on port 3001"
    classpath = files(tasks.shadowJar.get().archiveFile)
    mainClass.set("org.chromia.AppKt")
    args = listOf("--sse")
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
            shadow.component(this)
        }
    }
}
