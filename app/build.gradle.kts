import java.time.Duration
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mcpVersion = "0.15.0"
val ktorVersion = "3.5.1"
val postchainClientVersion = "3.39.1"

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
// gradle.properties holds this fork's release version (fallback for local
// builds). CI/release workflows override with -Pversion.
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
             * Default is gradle.properties `version` (this fork's release version).
             * CI and release jobs override with -Pversion.
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

/**
 * THE ROUND-18 EVASION PROBES: eight test doubles that are COMPILED AND NEVER RUN.
 *
 * `NoTestDoublesTest` used to be eight source regexes, and adversary round 18
 * (section 6) wrote eight doubles one token away from the spellings they looked
 * for: six were invisible. The scan is structural now - it reads the compiled
 * classes, where `by` delegation, nesting, import aliases and multi-line
 * supertypes are all the same thing - and these eight are what prove it catches
 * each shape.
 *
 * They therefore have to be COMPILED, and a double compiled into the test tree
 * would still be a double in the suite (Or's rule is zero, and the scan is the
 * proof). So they live in a source set of their own: it is not a test source set,
 * it declares no JUnit, nothing runs it, and nothing in app/src/test/kotlin
 * imports it. `test` depends on its compile task so the classes are on disk when
 * the scan looks, and the assertion over app/build/classes/kotlin/test is still
 * ZERO.
 */
val doubleProbes: SourceSet = sourceSets.create("doubleProbes")

// rell-api-gtx's postchain dependencies pull kotlin-stdlib 2.4.0, whose metadata
// our Kotlin 2.2 compiler rejects. Pin the stdlib to the project Kotlin version.
// postchain 3.49 (via rell-api-gtx) also constrains http4k to 6.53.x, which breaks
// postchain-client 3.36's runtime ABI (ClientFilters.AcceptGZip signature) - pin the
// whole http4k group to the version postchain-client is compiled against.
// NOTE (local_chain_up, 2026-09-02): the reverse also holds - postchain 3.49's own
// REST API (RestApi/ServerFilters.GZip) hard-crashes on 6.0.1.0 with NoSuchMethodError,
// and BOTH ABIs cannot be satisfied at once. The embedded local-chain node therefore
// runs with api.port disabled and LocalChain serves the REST subset itself over ktor
// (see LocalChain.kt) - do not re-enable postchain's RestApi while this pin stands.
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
        force("org.jetbrains.kotlin:kotlin-reflect:2.2.0")
        // postchain-client (3.36 and 3.39 alike) is built against http4k 6.0.1.0.
        eachDependency {
            if (requested.group == "org.http4k") {
                useVersion("6.0.1.0")
                because("postchain-client runtime ABI; 6.53 also carries Kotlin 2.4 metadata")
            }
        }
        force(
            "org.http4k:http4k-core:6.0.1.0",
            "org.http4k:http4k-client-apache:6.0.1.0",
            "org.http4k:http4k-format-core:6.0.1.0",
            "org.http4k:http4k-format-gson:6.0.1.0",
            "org.http4k:http4k-realtime-core:6.0.1.0"
        )
        // postchain 3.49 also constrains httpclient5 to 5.6.x, whose automatic content
        // decompression double-gunzips with http4k AcceptGZip ("Not in GZIP format" on
        // every chromia_dapp_query). Pin to the version postchain-client targets.
        force("org.apache.httpcomponents.client5:httpclient5:5.4.2")
    }
}

repositories {
    mavenCentral()
    maven("https://gitlab.com/api/v4/projects/50818999/packages/maven")
    maven("https://gitlab.com/api/v4/projects/32294340/packages/maven")
    maven("https://gitlab.com/api/v4/projects/46288950/packages/maven")
    // chromaway rell registry (rell-api-base / rell-base for the rell_check tool)
    maven("https://gitlab.com/api/v4/projects/32802097/packages/maven")
}

dependencies {
    // The umbrella `kotlin-sdk` artifact declares core/client/server at RUNTIME
    // scope only, so depend on the modules this server actually compiles against.
    implementation("io.modelcontextprotocol:kotlin-sdk-core:$mcpVersion")
    implementation("io.modelcontextprotocol:kotlin-sdk-server:$mcpVersion")
    // Streamable HTTP answers POSTs through call.respond(<JSONRPCMessage>), which
    // needs a server ContentNegotiation configured with McpJson (see App.installMcpJson).
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
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
    // In-process Rell compiler for the rell_check tool (agents' write→compile→fix loop)
    implementation("net.postchain.rell:rell-api-base:0.16.7")
    // In-process Rell test runner for the run_rell_tests tool.
    // http4k version is pinned group-wide above (postchain-client ABI).
    implementation("net.postchain.rell:rell-api-gtx:0.16.7")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.1")
    implementation("org.apache.logging.log4j:log4j-core:2.25.1")
    
    // The evasion probes compile against production and its dependencies only -
    // no JUnit, because nothing runs them.
    "doubleProbesImplementation"(sourceSets["main"].output)
    "doubleProbesImplementation"("io.modelcontextprotocol:kotlin-sdk-core:$mcpVersion")
    "doubleProbesImplementation"("dev.langchain4j:langchain4j-easy-rag:1.8.0-beta15")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    // UpstreamWarningGateTest runs ONE real live test through the REAL JUnit
    // launcher and lets the REAL report generator write its XML, then feeds that
    // XML to scripts/gate-tally.mjs. The alternative was a hand-written XML
    // string - a fixture of the exact artifact whose parsing is under test,
    // which is how a gate ends up green over a shape the real reporter does not
    // emit. Same JUnit, same version as the engine above; neither is a double.
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.1")
    testImplementation("org.junit.platform:junit-platform-reporting:1.10.1")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    // Ktor's mock engine artifact was here until 2026-09-07. It is a substitute for
    // the transport itself: with it on the classpath a test can replace the HTTP
    // client and still call itself an integration test. Every use is gone (real
    // embedded servers, real closed ports, the real explorer), and NoTestDoublesTest
    // refuses the dependency by name so it cannot come back quietly - which is why
    // this comment does not spell the artifact out.
    // Tests compile against Client/StdioClientTransport and the Streamable HTTP client transport.
    testImplementation("io.modelcontextprotocol:kotlin-sdk-client:$mcpVersion")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // NoTestDoublesTest reads the doubleProbes classes. The probes are compiled,
    // never run, and never on this task's classpath - so the directory is handed
    // over below rather than found on it.
    dependsOn(tasks.named("compileDoubleProbesKotlin"))

    // WHERE THE ZERO-DOUBLES SCAN LOOKS - decided by GRADLE, never by a literal
    // in a test.
    //
    // Until 2026-09-09 `NoTestDoublesTest.classesRoot` was the string
    // `app/build/classes/kotlin`, and adversary round 19 (finding r19d5) walked
    // straight past it: the test source set compiles Java too, into
    // `app/build/classes/java/test`, and `app/build/resources/test` is a
    // directory the test JVM loads from as well - both on THIS task's runtime
    // classpath and neither inside that string. A list a person maintains cannot
    // notice a source set added after it was written, so the task hands over the
    // four things the scan needs and the scan asserts it read every directory on
    // the classpath that is not production's own output or a dependency jar.
    // Resolved in doFirst: the classpath is resolved when the task runs, not when
    // the script is configured.
    val productionOutput = sourceSets["main"].output
    val doubleProbeClasses = doubleProbes.output.classesDirs
    val testRuntimeClasspath = classpath
    doFirst {
        val separator = java.io.File.pathSeparator
        systemProperty(
            "chromia.test.runtime.classpath",
            testRuntimeClasspath.files.joinToString(separator) { it.absolutePath }
        )
        systemProperty(
            "chromia.test.production.output",
            productionOutput.files.joinToString(separator) { it.absolutePath }
        )
        systemProperty(
            "chromia.test.production.classes",
            productionOutput.classesDirs.files.joinToString(separator) { it.absolutePath }
        )
        systemProperty(
            "chromia.test.doubleprobes.classes",
            doubleProbeClasses.files.joinToString(separator) { it.absolutePath }
        )
    }
    // Explicit bounds so constrained build containers fail fast instead of
    // thrashing or hanging (a Render Docker build sat 12h+ with no heap bound).
    maxHeapSize = "1280m"
    // 25 was not enough any more and the way it failed was misleading: the task
    // was KILLED mid-suite, so the run reported a partial tally (935 tests one
    // run, 588 the next) and Gradle removed the result XMLs on the way out -
    // which the merge gate then read as "the suite did not run". Round 9 added
    // four full dapp builds and five samples to the corpus, each analysed on
    // every run.
    //
    // 60 was not a budget, it was a coin flip, and it came up tails on
    // 2026-09-08. THE CHECK THE OLD COMMENT ASKS FOR, done before this number
    // moved - is it a hang or a long suite?  It is a long suite:
    //
    //   - the last run that COMPLETED (2026-09-07 17:43-18:42Z, 1534 tests, 0
    //     ignored) measured 58m54.93s. That is 65 seconds inside the old cap.
    //     Its report is the evidence: DappScaffoldSecureTemplatesTest alone is
    //     28m50.58s of it - 126 tests, each compiling and running a real Rell
    //     dapp - and the ten slowest classes are 50.3 of the 58.9 minutes;
    //   - the run that was killed made steady progress the whole hour. Its
    //     captured output carries 4453 timestamped lines spread evenly from
    //     23:39:52 to 00:39:10 with no stall longer than 220s (that one during
    //     the first dapp build), and it died INSIDE the template class, not
    //     waiting on anything;
    //   - it also shared the machine: the fix_round17_template worktree ran its
    //     own test task from 23:43 to 23:56 of that hour.
    //
    // So the suite needs an hour of real work and had no headroom for a second
    // JVM on the box. 90 gives it that headroom without hiding much: a genuine
    // hang still fails, and the gate prints the partial tally and names the
    // timeout, which is how this one was diagnosed.
    //
    // FLAG, not a claim: the old comment said "CI's own job budget is 50
    // minutes, so this stays inside it". A 59-minute suite does not stay inside
    // 50 minutes. Either the ubuntu-latest runner is materially faster than this
    // box or .github/workflows/ci.yml (timeout-minutes: 50, and that job also
    // builds the fat jar) is being killed - unverified from here, and worth
    // checking before the next release rather than assuming.
    timeout.set(Duration.ofMinutes(90))

    // Environment-gated tests (a PostgreSQL with C.UTF-8 collation, the live
    // testnet probes) skip unless their env vars are set - and a skip is a test
    // that silently did not run. Once a developer has provisioned the local
    // environment, record it in local-test-env.properties (gitignored) and it
    // applies to every run, so the default here is zero skips rather than a
    // green suite that quietly covered less than it claims. Real env vars still
    // win, so CI is unaffected.
    // Look in the main clone too, not just this checkout: the file is gitignored,
    // so `git worktree add` does not carry it over and every worktree build
    // silently went back to skipping - the exact failure this wiring exists to
    // prevent, hidden by the fact that the main clone looked fine. `git rev-parse
    // --git-common-dir` points at the main clone's .git for a worktree, and at
    // our own .git otherwise, so the fallback costs nothing in a normal clone.
    val localTestEnv = sequenceOf(
        rootProject.layout.projectDirectory.file("local-test-env.properties").asFile,
        runCatching {
            val commonGitDir = providers.exec {
                commandLine("git", "rev-parse", "--git-common-dir")
                workingDir = rootProject.layout.projectDirectory.asFile
            }.standardOutput.asText.get().trim()
            rootProject.layout.projectDirectory.file(commonGitDir).asFile
                .parentFile.resolve("local-test-env.properties")
        }.getOrNull()
    ).filterNotNull().firstOrNull { it.isFile }
    if (localTestEnv != null) {
        // NOT `java.util.Properties()`: inside this block `java` resolves to the
        // JavaPluginExtension and shadows the package - the script then fails to
        // compile ("Unresolved reference: util"), taking every Gradle task with it.
        val props = Properties()
        localTestEnv.inputStream().use(props::load)
        props.stringPropertyNames()
            .filter { System.getenv(it) == null }
            .forEach { environment(it, props.getProperty(it)) }

        // Self-heal the local test database. WSL stops with the session that
        // started it, taking the C.UTF-8 PostgreSQL with it - after a restart
        // every DB-backed test fails "connection refused" (12 failures on
        // 2026-09-02, all environmental) and "zero skips is structural" is only
        // true until the next reboot. If the properties file names a command
        // that brings the database up, run it once when the port is closed and
        // wait for the port. Opt-in and dev-local: the key only exists in the
        // gitignored file, so CI and plain clones are untouched.
        val ensureCmd = props.getProperty("CHROMIA_TEST_DB_ENSURE_CMD")
        val dbUrl = System.getenv("CHROMIA_TEST_DATABASE_URL") ?: props.getProperty("CHROMIA_TEST_DATABASE_URL")
        if (ensureCmd != null && dbUrl != null) {
            doFirst {
                val m = Regex("""//([^:/?]+):(\d+)""").find(dbUrl)
                val host = m?.groupValues?.get(1) ?: "localhost"
                val port = m?.groupValues?.get(2)?.toInt() ?: 5432
                fun open() = runCatching {
                    Socket().use { it.connect(InetSocketAddress(host, port), 1500) }
                    true
                }.getOrDefault(false)
                if (!open()) {
                    logger.lifecycle("test database $host:$port is down - running CHROMIA_TEST_DB_ENSURE_CMD")
                    ProcessBuilder("cmd", "/c", ensureCmd).inheritIO().start()
                    var waited = 0
                    while (!open() && waited < 90_000) { Thread.sleep(3000); waited += 3000 }
                    if (!open()) throw GradleException(
                        "test database $host:$port still unreachable ${waited / 1000}s after the ensure command - " +
                            "DB-backed tests would fail 'connection refused', not skip"
                    )
                    logger.lifecycle("test database $host:$port is up")
                }
            }
        }
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.dependsOn(generateBuildInfo)

tasks.shadowJar {
    // rell-api-gtx pushes the fat jar past 65535 entries.
    isZip64 = true
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
    // The jar is Java 21 bytecode (class file 65); these tasks used to run on
    // whatever JVM the Gradle daemon happened to be (a Temurin 17 daemon on the
    // dev box, 2026-09-04) and died with UnsupportedClassVersionError before
    // main - so `generateEmbeddingsNoUpload` could not regenerate the store.
    // Launch with the same toolchain that compiled it.
    javaLauncher.set(javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) })
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
