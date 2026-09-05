# Deployment & Hosting

**Local is the path.** This server is designed to run on the developer's own
machine — full 70-tool catalog, no memory constraints, `local_chain_up` fully usable, no
hosting cost. Hosting (Render, Kubernetes) remains an *option* for sharing a reduced
docs/analytics endpoint; the fork's own Render service was **retired (suspended) on
2026-09-05** — see [Hosted (Render)](#hosted-render--retired-2026-09-05) below for what it
was and the measurements that would apply to a redeploy.

The documentation index a local install answers from is cached on disk after the first
download: `$CHROMIA_MCP_HOME/embeddings.bin` + `embeddings.cache.json` (default
`~/.chromia-mcp/`; the downloaded JSON is re-encoded once into a flat binary that reads in
0.65 s instead of the 5-11 s JSON parse, taking initialize → first answer from 16.9 s to
3.7 s), reused while younger than 7 days, then refreshed from the weekly release asset — and
still served if that refresh fails. `CHROMIA_EMBEDDINGS_CACHE=off` disables the cache; `CHROMIA_EMBEDDINGS_PATH`
bypasses it entirely.

## Environments

### Local (primary)

**Purpose:** The normal way to run the server — daily use *and* development.

**Two shapes, one jar** (`app/build/libs/chromia-mcp-server.jar`, built by
`.\gradlew.bat :app:shadowJar`):

1. **stdio** — what Claude Code and most MCP clients use. Registered once via
   `claude mcp add chromia ... -- java -jar <jar> --stdio`; the client starts and stops the
   process itself. See "Run it locally" in the [README](../README.md) for the exact
   registration (including the `CHROMIA_TEST_DATABASE_URL` env var).
2. **local SSE server** — for clients that connect by URL (ChatGPT-style connectors,
   browser clients, other tools on the machine/LAN). Start with **`.\serve-local.ps1`**
   (repo root; `serve-local.cmd` for double-click). It auto-picks a free port from 3001,
   forces the full toolset, uses a fixed `-Xmx2g` heap (no container limit locally;
   measured steady state ~1.5 GB), waits for `/health`, prints the URL, and shuts down
   cleanly on Ctrl+C. Gradle equivalent for development: `./gradlew :app:runSse`.

**Requirements:**
- Java 21+ (only hard requirement — docs RAG, analytics, compiler tools all work with it alone)
- PostgreSQL, only for DB-backed tools (`run_rell_tests` with entities, `local_chain_up`)
  via `CHROMIA_TEST_DATABASE_URL`; without it those tools refuse cleanly, the rest works

**Configuration:**
- SSE defaults to `127.0.0.1:3001`; override with `--sse --host <host> --port <port>`
  (`parseSseArgs` in `Utils.kt`) — `serve-local.ps1` handles this for you
- Binding beyond localhost (`-BindHost 0.0.0.0`) should be paired with
  `CHROMIA_MCP_AUTH_TOKEN` (bearer auth; `/health` stays open)
- CORS allows all origins by default (restrict with `CHROMIA_MCP_ALLOWED_ORIGINS`)
- Health check at `/health`

#### Optional: auto-start the SSE server on login (Windows)

Nothing is installed by default. If you want the local SSE endpoint always available,
create a Scheduled Task once (regular user, no admin needed):

```powershell
schtasks /Create /TN "chromia-mcp-sse" /SC ONLOGON /RL LIMITED `
  /TR "powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File C:\Users\Orpe7\chromia-mcp\serve-local.ps1 -Port 3001"
```

- Pin the port (`-Port 3001`) so clients get a stable URL across logins.
- Check it: `schtasks /Query /TN "chromia-mcp-sse"`; run it now without re-login:
  `schtasks /Run /TN "chromia-mcp-sse"`; remove it: `schtasks /Delete /TN "chromia-mcp-sse" /F`.
- Alternative without admin/schtasks: put a shortcut to `serve-local.cmd` in
  `shell:startup` (Win+R → `shell:startup`) — it opens a visible console window you can
  Ctrl+C.
- The stdio shape needs no auto-start: the MCP client launches the process on demand.

### Hosted (Render) — retired 2026-09-05

The service (`https://chromia-mcp.onrender.com`, Render **starter**, Frankfurt) ran from
2026-08-30 to 2026-09-05 and is now **suspended, not deleted**. It stopped picking up
deploys with `autoDeploy: true` (47 commits behind `main`, answering from the 2025-10-21
GitLab package while every check in this repo was green), and the product is the local
install. Everything below is kept as the record of how it was shaped and measured, so a
redeploy — Render or anything else running the `Dockerfile` — starts from numbers, not
guesses. The hosted-only monitoring (`scripts/hosted-check.mjs`, `hosted-check.yml`) was
removed with the service; the fresh-install boot check in CI (`rag-eval.mjs
--production-shaped`) is what survives of it.

The service was deliberately a **reduced surface**: docs search (`search`/`fetch_docs`/`fetch`),
network analytics, help/reference (`chromia_help`), scaffolding, validation, preflight,
onboarding, and error translation — ~35 tools. The heavy tools stay **local by design**:

| Tool | Why hosted-off | Where it runs |
|---|---|---|
| `rell_check`, `rell_security_check` | in-process Rell compiler, measured past 512 MB | local install |
| `run_rell_tests` | compiler + needs PostgreSQL for entity tests | local install |
| `chromia_dapp_query` | postchain client memory spike measured past 512 MB | local install |
| `local_chain_up` | embedded node + PostgreSQL | local install |

This is a split, not a workaround: agents get an always-on shared docs/analytics
endpoint, and the write→compile→test loop runs next to the developer's code with no
memory ceiling. Disabled tools refuse calls with a pointer to the local install.

#### Memory: measured, not guessed (2026-09-02)

Measured by running the release jar with the exact hosted env (compact tools + the
disable list above), NMT + forced-GC working-set sampling, plus a 10-query docs-search
load over a real SSE session:

| Component | Measured |
|---|---|
| JVM + app baseline (no RAG): classes, code cache, symbols, threads | ~106 MB |
| RAG embeddings index on heap (from the 18.8 MB baked JSON) | ~11 MB |
| Live heap total after warmup (yes, really) | ~30 MB |
| Metaspace (reduced surface) | ~30 MB |
| ONNX runtime + BGE-small model, **native, outside the JVM** | ~90–130 MB |
| **Steady-state working set, tuned flags** | **~240 MB (47% of 512 MB)** |
| Peak during boot RAG warmup (transient JSON parse) | ~340–370 MB (66–72%), returns to ~240 MB after GC |
| Peak under docs-search load | ~250 MB (49%) |

(Measured on Windows working-set; Linux RSS in the container will differ by a few
percent — `MALLOC_ARENA_MAX=2` in the image keeps glibc from inflating it.)

**2026-09-04 update — the table above was measured against the 18.8 MB / 3208-segment GitLab
package, which turned out to be the 2025-10-21 build.** The current store (published as the
`embeddings` GitHub release asset) is 150 MB / 25 823 segments and changes the picture:

| Component | Measured (current store) |
|---|---|
| Live heap after load (vectors ~40 MB, text ~25 MB, lowercased lexical index ~25 MB) | ~105–130 MB |
| Heap floor for load + first search | between 200 MB (OOM) and 224 MB (ok) |
| `-Xmx179m` (the old `MaxRAMPercentage=35` on 512 MB) | loads, then `ExitOnOutOfMemoryError` on the first search |
| `-Xmx256m` (`MaxRAMPercentage=50` on 512 MB): 30 searches / 6 concurrent | median 119 ms / 772 ms, 0 errors |
| Working set at `-Xmx256m` after the burst | ~425 MB |

The file is no longer parsed whole: `InMemoryEmbeddingStore.fromFile` needed a 150 MB direct
buffer (`Files.readAllBytes`) and died under `-XX:MaxDirectMemorySize=64m`; `EmbeddingStoreJson`
streams it entry by entry, so there is no boot transient above the live footprint. A 512 MB
instance still fits, with the store's growth as the only margin; **1 GB is the comfortable size
for the full index.**

**Why the live service used to read 84% idle / 95.7% peak:** the old
`-XX:MaxRAMPercentage=70` with no GC flag. On a sub-2 GB container the JVM silently
defaults to **SerialGC, which commits the entire ~358 MB heap up front and never
returns it** — measured side by side: 378 MB working set (SerialGC) vs 234 MB (G1)
for the same workload, with only ~30 MB of live objects. The Dockerfile now pins
explicit G1, a heap cap (35% then; **50% since 2026-09-04** for the full index, see the
table above), heap-shrink ratios, and a periodic idle GC; every flag is annotated with its
measurement in the Dockerfile itself. Expected live effect with the current store: ~425 MB
working set (~83%) after a search burst — starter fits, 1 GB is comfortable.

#### Owner options

- **(a) Leave as-is — recommended.** Starter + reduced surface + tuned image. The
  service is not Blueprint-managed; `render.yaml` documents the intended shape and
  the dashboard stays authoritative. Nothing to do besides deploying the tuned image.
- **(b) Adopt into the Blueprint later.** Dashboard → New + → Blueprint → this repo;
  Render adopts the existing service by matching the `chromia-mcp` name. Gains:
  `healthCheckPath: /health` finally applies (deploys gate on real health, not
  port-open), and config becomes reviewable in git. Note: adoption applies the plan
  in the file (starter today = no cost change), and later plan edits in the file
  change the bill on sync. Full steps are in `render.yaml`'s comment block.
- **(c) Upgrade to standard (2 GB) only if the heavy tools must run hosted.** That
  unlocks `rell_check`/`rell_security_check`/`chromia_dapp_query` hosted (still not
  `run_rell_tests` entity tests or `local_chain_up` — those need PostgreSQL). It
  roughly triples the monthly cost and duplicates what every local install already
  does better. Not recommended without a concrete consumer.

#### Ops runbook (when it misbehaves)

- **First look:** Render dashboard → Metrics (memory %, restarts) and Logs. The
  server logs one structured line per tool call — spot the tool that was running
  when memory climbed.
- **Memory pressure before OOM:** watch for working set trending above ~70%
  (>360 MB) at idle — that is drift, not load, and worth a restart + investigation.
  `-XX:+ExitOnOutOfMemoryError` is set deliberately: a heap OOM exits the container
  and Render restarts it in seconds, which beats a wedged half-dead JVM. A restart
  loop (3+ in an hour) means a real leak or a config regression — roll back first.
- **Boot:** healthy boot logs the RAG load from the baked file and
  `docs warmup done in ~Ns`. A boot that logs the GitLab download instead means the
  image was built while GitLab was unreachable (the bake step warns loudly in the
  build log) — rebuild/redeploy to restore the baked path.
- **Proposal for the app owner (not implemented here, touches `app/src`):** extend
  `/health` with `heapUsedMb`, `heapMaxMb`, `rssAnonMb` (from
  `/proc/self/status` VmRSS), `toolCallsTotal`, and `openSseSessions`. Cost is a few
  lines; it turns "is memory creeping?" into a curl instead of a dashboard dig, and
  any uptime monitor can alert on it.

The rest of this document describes the hosted pipelines (Render blueprint and the
upstream GitLab → Kubernetes flow).

### Production

**Purpose:** Live production environment serving AI assistant clients.

**URL:** `https://mcp.chromia.dev`

**Health Check:** `https://mcp.chromia.dev/health`

**Characteristics:**
- Serves production traffic from AI assistant clients
- Deployed to Kubernetes cluster
- Accessible via HTTPS
- Deployed from version tags (format: `X.Y.Z`)

**Deployment Method:**
- Automated deployment via GitLab CI/CD pipeline
- Deploys when version tag is created
- Uses Docker image from GitLab Container Registry

## Deployment Flow

### Overview

Deployment is **semi-automated** with manual tag creation:

1. **Code Push** → Triggers CI/CD pipeline (build and test)
2. **Version Tag Creation** → Manual trigger (creates version tag)
3. **Docker Build** → Automatic (on version tag)
4. **Docker Push** → Automatic (pushes to GitLab Container Registry)
5. **Kubernetes Deployment** → Automatic (on version tag)

### Detailed Deployment Process

#### Step 1: Code Push to Repository

**Trigger:** Developer pushes code to repository (any branch).

**What happens:**
- GitLab CI/CD pipeline is triggered automatically
- Pipeline runs on GitLab runners
- Build stage executes: compiles code, runs tests, creates fat JAR

**Branches:**
- Any branch → Build and test only
- Version tags (format: `X.Y.Z`) → Full deployment pipeline

#### Step 2: Build Stage

**Job:** `build` (stage: `build`)

**When it runs:**
- Automatically on push to any branch
- Automatically on merge request events

**What it does:**
1. Installs OpenJDK 21
2. Runs tests: `./gradlew check --info`
3. Builds fat JAR: `./gradlew :app:shadowJar`
4. Stores artifacts for 1 week

**Artifacts:**
- `app/build/libs/chromia-mcp-server.jar` - Fat JAR with all dependencies

**Cache:**
- Gradle cache is stored and reused across builds
- Build artifacts are cached per branch

#### Step 3: Version Tag Creation (Manual)

**Automatic Tag Creation (Optional):**
- `release-patch` job: Creates patch version tag (increments patch number)
- `release-minor` job: Creates minor version tag (increments minor number)
- Only runs on `dev` branch
- Requires manual trigger in GitLab CI/CD

**What happens after tag creation:**
- Tag push triggers pipeline automatically
- Pipeline runs with `CI_COMMIT_TAG` variable set to tag value

#### Step 4: Docker Image Build and Push

**Job:** `release-image` (stage: `release`)

**When it runs:**
- Automatically on version tags (format: `^[0-9]+\.[0-9]+\.[0-9]+$`)

**What it does:**
1. Installs kubectl, OpenJDK 21, wget, unzip
2. Downloads Jib CLI tool (v0.13.0)
3. Builds application JAR sequentially: `./gradlew :app:shadowJar` then `./gradlew jib` (do not run them as concurrent siblings; both write under `app/build/libs`)
4. Fetches dependencies for `claude-code-chromia` subproject
5. Builds Docker image using Jib:
   - Base image: `eclipse-temurin:21-jre-jammy`
   - Image name: `${CI_REGISTRY_IMAGE}/chromia-mcp`
   - Tag: `${CI_COMMIT_TAG}` (version tag)
   - Multi-platform: amd64 and arm64 (if `CI_REGISTRY_IMAGE` is set)
6. Pushes image to GitLab Container Registry

**Image Details:**
- **Registry:** GitLab Container Registry
- **Image path:** `registry.gitlab.com/chromaway/core-tools/chromia-mcp/chromia-mcp`
- **Tag format:** Semantic version (e.g., `1.2.3`)
- **Platforms:** Linux amd64 and arm64

**Docker Build Process:**
- Uses Jib to build Docker image directly from Gradle
- No Dockerfile required (Jib generates image)
- Creates minimal JRE-based image
- Copies JAR to `/app/app.jar`
- Sets entrypoint to run JAR with `--sse` argument

#### Step 5: Maven Publishing (Optional)

**Job:** `publish` (stage: `release`)

**When it runs:**
- Automatically on version tags (format: `^[0-9]+\.[0-9]+\.[0-9]+$`)

**What it does:**
1. Publishes fat JAR to GitLab Maven repository
2. Uses version from git tag: `-Pversion=$CI_COMMIT_TAG`
3. Authenticates using `CI_JOB_TOKEN`

**Artifact:**
- Group: `com.chromia`
- Artifact: `chromia-mcp`
- Version: Git tag value (e.g., `1.2.3`)

**Maven Repository:**
- `https://gitlab.com/api/v4/projects/{CI_PROJECT_ID}/packages/maven`

#### Step 6: Kubernetes Deployment

**Job:** `deploy-kubernetes` (stage: `deploy`)

**When it runs:**
- Automatically on version tags (format: `^[0-9]+\.[0-9]+\.[0-9]+$`)

**What it does:**
1. Installs kubectl
2. Decodes base64-encoded `KUBECONFIG` variable to temporary file
3. Updates Kubernetes deployment:
   ```bash
   kubectl --kubeconfig="$KUBE_FILE" set image \
     deployment/chromia-mcp \
     chromia-mcp="${CI_REGISTRY_IMAGE}/chromia-mcp:${CI_COMMIT_TAG}"
   ```
4. Waits for rollout to complete:
   ```bash
   kubectl --kubeconfig="$KUBE_FILE" rollout status deployment/chromia-mcp
   ```

**Deployment Strategy:**
- Kubernetes rolling update (default)
- Old pods are terminated only after new pods are healthy
- Zero-downtime deployment (if configured correctly in Kubernetes)

## CI/CD Pipelines

### Pipeline Definition Location

**File:** `.gitlab-ci.yml` in repository root

**CI/CD Platform:** GitLab CI/CD

**Pipeline Stages (in order):**
1. `build` - Compile, test, and build artifacts
2. `release` - Build Docker image, publish to Maven
3. `deploy` - Deploy to Kubernetes

### Pipeline Triggers

**Automatic triggers:**
- Push to any branch → Build stage only
- Merge request to any branch → Build stage only
- Git tag matching `X.Y.Z` pattern → Full pipeline (build, release, deploy)

**Manual triggers:**
- `release-patch` job → Create patch version tag (on `dev`)
- `release-minor` job → Create minor version tag (on `dev`)

### Required CI/CD Variables

**GitLab CI/CD Variables (stored in GitLab project settings):**

- `KUBECONFIG` - Base64-encoded Kubernetes configuration file - system admin responsibility
- `CI_REGISTRY_USER` - GitLab Container Registry username (automatically provided)
- `CI_REGISTRY_PASSWORD` - GitLab Container Registry password (automatically provided)
- `CI_JOB_TOKEN` - GitLab job token for Maven publishing (automatically provided)

**Note:** These are stored as protected/masked variables in GitLab. Values are not documented here for security.

**For debugging or deployment issues with Kubernetes, contact the system administrator.**

## Docker Image Details

### Image Build Process

**Build tool:** Jib (Google Container Tools)

**Base image:** `eclipse-temurin:21-jre-jammy@sha256:2843f155a9fe5aab6a73a71a9f65c38143e8e929366a1a7787f07c2a89c26887`

**Build configuration:**
- Defined in `app/build.gradle.kts` - `jib` block
- Uses Jib Gradle plugin
- Builds directly from Gradle (no Dockerfile)

**Image structure:**
- Minimal JRE image (not full JDK)
- Application JAR copied to `/app/app.jar`
- Runs as non-root user (if configured)
- Entrypoint: `java -jar /app/app.jar --sse`

**Multi-platform support:**
- Builds for both `amd64` and `arm64` architectures (if `CI_REGISTRY_IMAGE` is set)
- Enables deployment on different hardware architectures

### Image Configuration

**Environment variables:**
- No environment variables are set in Docker image
- Application uses hardcoded configuration

### Image Registry

**Registry:** GitLab Container Registry

**Full image path:** `registry.gitlab.com/chromaway/core-tools/chromia-mcp/chromia-mcp:latest`

**Tagging strategy:**
- Semantic version tags (e.g., `1.2.3`)
- Tags are created from git tags
- `latest` tag is not automatically created

## Kubernetes Deployment

### Deployment Configuration

**Kubernetes deployment details are not in repository.**

**Note:** For Kubernetes deployment configuration or access issues, contact the system administrator.

- External access via `https://mcp.chromia.dev`

## Release Process

**Tag format:** Semantic versioning (`MAJOR.MINOR.PATCH`)

**Release workflow:**

1. **Code is merged to target branch** (`dev`)

2. **Trigger version tagging job in GitLab CI/CD :**
   - Go to GitLab CI/CD → Pipelines
   - Click on the `release-patch` or `release-minor` job:
     - `release-patch`: Increments patch version (e.g., `1.2.3` → `1.2.4`)
     - `release-minor`: Increments minor version (e.g., `1.2.3` → `1.3.0`)
   - The job automatically creates and pushes the version tag

3. **Pipeline triggers automatically after tag is created:**
   - Build stage: Compiles and tests code
   - Release stage: Builds Docker image and publishes to Maven
   - Deploy stage: Updates Kubernetes deployment

4. **Deployment completes:**
   - New pods are created with new image
   - Old pods are terminated after new pods are healthy
   - Service routes traffic to new pods

**Note:** Version tagging jobs are only available on `dev` branch or `support/*` branches and require manual trigger in GitLab CI/CD UI.

## Monitoring and Health Checks

### Health Check Endpoint

**Endpoint:** `/health`

**Production URL:** `https://mcp.chromia.dev/health`

**Response format:**
```json
{
  "status": "healthy",
  "server": "chromia-mcp-server",
  "version": "<Gradle project.version>"
}
```

`version` comes from Gradle `project.version` (generated `BuildInfo.VERSION` in `App.kt`). `gradle.properties` holds this fork's release version (`0.5.0`) as the local-build fallback. Tagged release builds pass `-Pversion=${TAG#v}`, so a released jar reports the tag; CI builds report `git describe`. This is not a hardcoded `0.0.1`.

**Use cases:**
- Kubernetes liveness/readiness probes
- External monitoring systems
- Manual health verification

### Logging

**Logging framework:** Log4j2

**Configuration:** `app/src/main/resources/log4j2.properties`

**Log output:**
- Server logs all tool requests and responses
- Errors are logged with stack traces
- Log level can be configured via properties file

### RAG Store Initialization

**Risk:** RAG store loads local `embeddings.json` first, then downloads from GitLab packages at startup.

**Impact:** If neither the local file nor GitLab packages are available, documentation search will not work (but server continues running).

**Mitigation:** Server continues running even if RAG store fails. Local no-upload ingest writes `app/build/embeddings.json` (or `CHROMIA_EMBEDDINGS_PATH`) so a developer machine does not need the GitLab package.
