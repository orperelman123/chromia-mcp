# Chromia MCP server - SSE deployment image
# Build: docker build -t chromia-mcp .
# Run:   docker run -p 3001:3001 chromia-mcp
#
# The RAG store downloads embeddings from the GitLab package registry at startup
# unless /app/embeddings.json is baked in or CHROMIA_EMBEDDINGS_PATH points elsewhere.

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
RUN timeout -k 60 2100 gradle :app:test :app:shadowJar --no-daemon --console=plain \
    -Dorg.gradle.internal.http.connectionTimeout=60000 \
    -Dorg.gradle.internal.http.socketTimeout=180000 \
    -Dorg.gradle.internal.repository.max.retries=6 \
    -Dorg.gradle.internal.repository.initial.backoff=1000

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /src/app/build/libs/chromia-mcp-server.jar /app/chromia-mcp-server.jar

# Compact tool advertisement by default for hosted agent use; override with
# CHROMIA_MCP_COMPACT_TOOLS=false for the full per-tool catalog.
ENV CHROMIA_MCP_COMPACT_TOOLS=true

EXPOSE 3001
# PORT is provided by most PaaS (Render/Heroku-style); default 3001.
# Container-aware heap: never exceed 70% of the cgroup limit (a fixed -Xmx above
# the instance RAM gets the container OOM-killed). 512MB instances survive light
# use; RAG + the Rell compiler under load want a 2GB instance.
CMD ["sh", "-c", "java -XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -jar /app/chromia-mcp-server.jar --sse --host 0.0.0.0 --port ${PORT:-3001}"]
