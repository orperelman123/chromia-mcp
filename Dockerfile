# Chromia MCP server - SSE deployment image
# Build: docker build -t chromia-mcp .
# Run:   docker run -p 3001:3001 chromia-mcp
#
# RAG embeddings are baked into the image at BUILD time (see the download step
# below): runtime loads /app/embeddings/embeddings.json from disk via
# CHROMIA_EMBEDDINGS_PATH, so a boot no longer downloads from GitLab. The
# boot-time download+parse spike OOM-crash-looped 512MB containers
# (-XX:MaxRAMPercentage=70 + ExitOnOutOfMemoryError). If the build-time
# download fails, the image still builds and runtime falls back to the GitLab
# download at boot, exactly as before.

FROM gradle:8.14-jdk21 AS build
WORKDIR /src
COPY . .
# Every image build resolves all dependencies from scratch, so one flaky Maven
# Central download fails the whole deploy (seen live: tika jar "Read timed out").
# Generous HTTP timeouts + retries with backoff make the build survive blips.
# Tests run in the SAME build as the jar - a deploy can never ship untested code.
# Explicit gradle heap + a hard timeout: a hung/thrashing suite must FAIL the
# build loudly (one unbounded build sat 12h+ "in progress"), never wedge it.
ENV GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
# Version truth: /health and MCP serverInfo report the real build, not the
# gradle.properties placeholder. Render exposes RENDER_GIT_COMMIT at build time.
ARG RENDER_GIT_COMMIT=""
RUN VERSION=$(git describe --tags --always 2>/dev/null || echo "${RENDER_GIT_COMMIT:-unknown}" | cut -c1-12); \
    timeout -k 60 2100 gradle :app:test :app:shadowJar --no-daemon --console=plain \
    -Pversion="${VERSION:-unknown}" \
    -Dorg.gradle.internal.http.connectionTimeout=60000 \
    -Dorg.gradle.internal.http.socketTimeout=180000 \
    -Dorg.gradle.internal.repository.max.retries=6 \
    -Dorg.gradle.internal.repository.initial.backoff=1000

# Bake the RAG embeddings into the image so boot never downloads them.
# Same registry URL as the runtime fallback (RagStore.PACKAGE_URL/FILE_NAME -
# DockerfileEmbeddingsBakeTest pins the two together) and the same
# retry/timeout hardening spirit as the Maven settings above.
# MUST NOT fail the image build when GitLab is unreachable: the file is
# downloaded into a directory so the later COPY succeeds even when empty,
# and the runtime GitLab fallback then still applies - a GitLab outage at
# image-build time must never brick a deploy.
RUN mkdir -p /embeddings && \
    EMB_URL="https://gitlab.com/api/v4/projects/71940508/packages/generic/embeddings/v1/embeddings.json"; \
    if command -v curl >/dev/null 2>&1; then \
        curl --fail --silent --show-error --location \
            --connect-timeout 15 --max-time 600 \
            --retry 6 --retry-delay 5 --retry-all-errors \
            -o /embeddings/embeddings.json "$EMB_URL" \
        || { echo "##### WARNING: embeddings.json build-time download FAILED - image ships without baked embeddings; runtime will download from GitLab at boot (memory-spike risk on small instances) #####" >&2; rm -f /embeddings/embeddings.json; }; \
    elif command -v wget >/dev/null 2>&1; then \
        wget --quiet --timeout=180 --tries=6 --waitretry=5 \
            -O /embeddings/embeddings.json "$EMB_URL" \
        || { echo "##### WARNING: embeddings.json build-time download FAILED - image ships without baked embeddings; runtime will download from GitLab at boot (memory-spike risk on small instances) #####" >&2; rm -f /embeddings/embeddings.json; }; \
    else \
        echo "##### WARNING: neither curl nor wget in build image - embeddings.json NOT baked; runtime will download from GitLab at boot #####" >&2; \
    fi; \
    if [ -f /embeddings/embeddings.json ]; then \
        echo "Baked embeddings.json into image ($(du -h /embeddings/embeddings.json | cut -f1))"; \
    fi

FROM eclipse-temurin:21-jre-jammy

# Non-root runtime user: a compromised tool call must not own the container.
# The app only writes to java.io.tmpdir (/tmp) at runtime (RagStore temp
# files); /app stays read-only for it by design.
RUN groupadd --gid 10001 app && \
    useradd --uid 10001 --gid app --home /home/app --create-home --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=build /src/app/build/libs/chromia-mcp-server.jar /app/chromia-mcp-server.jar

# Baked embeddings (may be an empty directory when the build-time download
# failed; RagStore then falls back to the GitLab download at boot).
COPY --from=build /embeddings/ /app/embeddings/
ENV CHROMIA_EMBEDDINGS_PATH=/app/embeddings/embeddings.json

# Compact tool advertisement by default for hosted agent use; override with
# CHROMIA_MCP_COMPACT_TOOLS=false for the full per-tool catalog.
ENV CHROMIA_MCP_COMPACT_TOOLS=true

# glibc malloc arena cap. The ONNX embedding model (langchain4j easy-rag
# BGE-small) allocates ~90-130MB natively OUTSIDE the JVM heap (invisible to
# NMT, measured 2026-09-02 as working set minus NMT committed). Uncapped,
# glibc creates up to 8*ncores malloc arenas and fragmentation inflates RSS
# on small instances; 2 arenas is plenty for this thread count (~40).
ENV MALLOC_ARENA_MAX=2

USER app
EXPOSE 3001

# JVM sizing - every flag below is backed by a measurement (2026-09-02, jar
# run with the hosted env: compact tools + heavy tools disabled, NMT summary
# + forced-GC working-set samples; full numbers in docs/Deployment.md):
#   live heap after RAG warmup ........ ~30MB
#   warmup transient (parse 18.8MB json) peaks ~100MB above steady state
#   metaspace ......................... ~30MB (reduced surface)
#   ONNX runtime + model (native) ..... ~90-130MB, untunable from the JVM
#   steady-state working set, tuned ... ~240MB; search-load peak ~250MB
#
# -XX:+UseG1GC          On <2GB / <2 CPU containers (Render starter: 512MB,
#                       0.5 CPU) the JVM defaults to SerialGC, which COMMITS
#                       THE ENTIRE Xmx up front and never gives it back -
#                       measured 378MB working set vs G1's 234MB for the same
#                       config. This single default explains most of the 84%
#                       idle memory seen on the live 512MB service. Explicit
#                       G1 grows/shrinks committed heap with actual use.
# -XX:MaxRAMPercentage=35   512MB box -> 179MB heap cap: 6x the measured live
#                       heap, and the warmup spike fit in 160MB in testing.
#                       (2GB box -> 716MB, ample for the full toolset.) The
#                       old 70% let committed heap balloon to ~358MB on
#                       starter - memory that G1 had no pressure to return.
# -XX:MinHeapFreeRatio=10 / -XX:MaxHeapFreeRatio=25
#                       Return heap to the OS promptly after spikes (warmup,
#                       big tool calls) instead of hoarding committed pages.
# -XX:G1PeriodicGCInterval=300000
#                       Idle-time concurrent cycle every 5min so a quiet
#                       server drifts back down to its floor.
# -XX:MaxMetaspaceSize=192m  6x measured reduced-surface metaspace; headroom
#                       for the Rell compiler classes on full deployments.
# -XX:MaxDirectMemorySize=64m  ktor CIO + ONNX direct buffers measured ~19MB;
#                       cap prevents unbounded direct-buffer growth.
# -XX:+ExitOnOutOfMemoryError  Deliberate for a supervised container: the
#                       platform restarts a dead container in seconds, while a
#                       half-OOMed JVM limps on serving corrupt sessions. Kept.
#
# Deliberately NOT set: -Xss (thread stacks measured only ~2.5MB committed -
# nothing to win, and shrinking stacks risks the Rell compiler's recursion on
# full-power deployments); AppCDS (base CDS archive already active; an app
# archive would complicate the build for ~1s boot win on a server that boots
# in ~4s).
#
# JAVA_OPTS overrides the whole flag set without an image rebuild.
# `exec` makes java PID 1 so SIGTERM reaches the JVM directly (shutdown hooks,
# SSE session teardown). dash usually tail-call-optimizes `sh -c "java ..."`
# into an exec anyway, but that is an implementation detail of the shell -
# the explicit exec makes signal delivery guaranteed, not incidental.
#
# No Docker HEALTHCHECK: the JRE image ships neither curl nor wget, and both
# Render and Kubernetes probe /health from the outside (healthCheckPath /
# livenessProbe). Compose users: probe /health from the host.
CMD ["sh", "-c", "exec java ${JAVA_OPTS:--XX:+UseG1GC -XX:MaxRAMPercentage=35 -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=25 -XX:G1PeriodicGCInterval=300000 -XX:MaxMetaspaceSize=192m -XX:MaxDirectMemorySize=64m -XX:+ExitOnOutOfMemoryError} -jar /app/chromia-mcp-server.jar --sse --host 0.0.0.0 --port ${PORT:-3001}"]
