# Chromia MCP server - SSE deployment image
# Build: docker build -t chromia-mcp .
# Run:   docker run -p 3001:3001 chromia-mcp
#
# The RAG store downloads embeddings from the GitLab package registry at startup
# unless /app/embeddings.json is baked in or CHROMIA_EMBEDDINGS_PATH points elsewhere.

FROM gradle:8.14-jdk21 AS build
WORKDIR /src
COPY . .
RUN gradle :app:shadowJar --no-daemon --console=plain

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /src/app/build/libs/chromia-mcp-server.jar /app/chromia-mcp-server.jar

# Compact tool advertisement by default for hosted agent use; override with
# CHROMIA_MCP_COMPACT_TOOLS=false for the full per-tool catalog.
ENV CHROMIA_MCP_COMPACT_TOOLS=true

EXPOSE 3001
# PORT is provided by most PaaS (Render/Heroku-style); default 3001.
CMD ["sh", "-c", "java -Xmx1536m -jar /app/chromia-mcp-server.jar --sse --host 0.0.0.0 --port ${PORT:-3001}"]
