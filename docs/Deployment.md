# Deployment & Hosting

## Environments

### Development (Local)

**Purpose:** Local development and testing by engineers.

**URL:** `http://127.0.0.1:3001` (when running locally)

**Characteristics:**
- Runs on developer's machine
- No external access
- Used for development, debugging, and local testing
- No deployment process - engineers run locally via `./gradlew :app:runSse` or `./gradlew :app:run`

**Configuration:**
- SSE defaults to `127.0.0.1:3001`; override with `--sse --host <host> --port <port>` (`parseSseArgs` in `Utils.kt`)
- CORS configured to allow all origins (for local development)
- Health check available at `/health` endpoint

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

`version` comes from Gradle `project.version` (generated `BuildInfo.VERSION` in `App.kt`). `gradle.properties` pins `0.2.2` (latest official GitLab tag). Tagged publish jobs pass `-Pversion=$CI_COMMIT_TAG`, so a released image reports the tag. This is not a hardcoded `0.0.1`.

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
