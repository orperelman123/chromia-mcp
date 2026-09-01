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
WORKDIR /app
COPY --from=build /src/app/build/libs/chromia-mcp-server.jar /app/chromia-mcp-server.jar

# Baked embeddings (may be an empty directory when the build-time download
# failed; RagStore then falls back to the GitLab download at boot).
COPY --from=build /embeddings/ /app/embeddings/
ENV CHROMIA_EMBEDDINGS_PATH=/app/embeddings/embeddings.json

# Compact tool advertisement by default for hosted agent use; override with
# CHROMIA_MCP_COMPACT_TOOLS=false for the full per-tool catalog.
ENV CHROMIA_MCP_COMPACT_TOOLS=true

EXPOSE 3001
# PORT is provided by most PaaS (Render/Heroku-style); default 3001.
# Container-aware heap: never exceed 70% of the cgroup limit (a fixed -Xmx above
# the instance RAM gets the container OOM-killed). 512MB instances survive light
# use; RAG + the Rell compiler under load want a 2GB instance.
CMD ["sh", "-c", "java -XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -jar /app/chromia-mcp-server.jar --sse --host 0.0.0.0 --port ${PORT:-3001}"]
