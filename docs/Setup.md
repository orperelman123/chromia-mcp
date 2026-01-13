# Setup & Local Development

## Prerequisites

### Required Tools and Versions

**Java Development Kit (JDK) 21 or higher**
- **Required version:** Java 21 or higher (JDK, not JRE). Java is backward compatible, so newer versions (22, 23, etc.) will also work.
- **Purpose:** Compile and run Kotlin code
- **How to verify:** Run `java -version` - should show version 21.x.x or higher
- **Installation:**
  - **macOS:** `brew install openjdk@21` (or newer version) or download from [Adoptium](https://adoptium.net/)
  - **Linux:** `sudo apt-get install openjdk-21-jdk` (or newer version) (Ubuntu/Debian) or use package manager
  - **Windows:** Download Java 21 or higher from [Adoptium](https://adoptium.net/) and set JAVA_HOME environment variable

**Gradle**
- **Version:** 8.14.3 (managed via Gradle Wrapper)
- **Purpose:** Build automation and dependency management
- **How to verify:** Run `./gradlew --version` (after cloning repository)
- **Installation:** Not required - project includes Gradle Wrapper (`gradlew` script)

### Optional Tools

**Docker** (Optional)
- **Purpose:** Run application in containerized environment or build Docker images
- **Required only if:** Using Docker-based deployment or development

**MCP Inspector** (Optional, for debugging)
- **Purpose:** Web-based tool for testing and debugging MCP servers
- **Installation:** `npm install -g @modelcontextprotocol/inspector` or use `npx`
- **Documentation:** https://www.npmjs.com/package/@modelcontextprotocol/inspector

**Environment Variables:**
- No environment variables are required for local development
- Optional: `GITLAB_ACCESS_TOKEN` - Only needed if generating RAG embeddings locally (currently not used)

## Step-by-Step Setup Instructions

### 1. Clone the Repository

```bash
git clone https://gitlab.com/chromaway/core-tools/chromia-mcp.git
cd chromia-mcp
```

### 2. Verify Java Installation

```bash
java -version
```

**Expected output:** Should show `openjdk version "21"` or higher (e.g., "22", "23").

**If Java is not found or wrong version:**
- Install Java 21 or higher (see Prerequisites)
- Set `JAVA_HOME` environment variable to Java installation directory
- Verify: `echo $JAVA_HOME` (macOS/Linux) or `echo %JAVA_HOME%` (Windows)

### 3. Make Gradle Wrapper Executable (macOS/Linux)

```bash
chmod +x gradlew
```

**Windows:** Skip this step - `gradlew.bat` is already executable.

### 4. Build the Project

**First build (downloads dependencies and compiles code):**
```bash
./gradlew build
```

### 5. Create the Fat JAR

**Why a fat JAR is needed:** A fat JAR bundles all project dependencies into a single JAR file. This makes it easy to distribute and run the application without needing to manage classpath dependencies separately. You can run the application with just `java -jar` without specifying additional classpath entries.

**Build fat JAR (includes all dependencies):**
```bash
./gradlew :app:shadowJar
```

**What this does:**
- Creates a single JAR file with all dependencies included
- Output: `app/build/libs/chromia-mcp-server.jar`

**Verify build success:**
```bash
ls -la app/build/libs/
```

**Expected:** Should contain `chromia-mcp-server.jar`.

## Running Locally

### Option 1: Run via Gradle (Recommended for Development)

#### SSE Mode (HTTP Server)

**Start the server:**
```bash
./gradlew :app:runSse
```

**What this does:**
- Starts HTTP server on `127.0.0.1:3001`
- Exposes MCP endpoint at `http://127.0.0.1:3001`
- Health check available at `http://127.0.0.1:3001/health`

**Default configuration:**
- **Host:** `127.0.0.1`
- **Port:** `3001`
- **Endpoint:** `http://127.0.0.1:3001`
- **Health check:** `http://127.0.0.1:3001/health`

**Verify it's running:**
```bash
curl http://127.0.0.1:3001/health
```

**Expected response:**
```json
{
  "status": "healthy",
  "server": "chromia-mcp-server",
  "version": "0.0.1"
}
```

#### Stdio Mode (Subprocess)

**Start the server:**
```bash
./gradlew :app:run
```

**What this does:**
- Starts server in stdio mode
- Reads from `stdin` and writes to `stdout`
- Used when MCP client spawns server as subprocess

**Note:** In stdio mode, the server reads from `stdin` and writes to `stdout`. This is typically used when the MCP client launches the server as a subprocess.

### Option 2: Run from Built JAR

**Build fat JAR:**
```bash
./gradlew :app:shadowJar
```

**Run JAR in SSE mode:**
```bash
java -jar app/build/libs/chromia-mcp-server.jar --sse
```

**Run JAR in stdio mode:**
```bash
java -jar app/build/libs/chromia-mcp-server.jar --stdio
```

### Running from IDE

**IntelliJ IDEA:**
1. Open the project in IntelliJ IDEA
2. Navigate to `app/src/main/kotlin/org/chromia/App.kt`
3. Right-click on `main()` function → Run
4. Edit run configuration to add program arguments:
   - For SSE mode: `--sse`
   - For stdio mode: `--stdio`

## Testing

### Running Tests

```bash
./gradlew test
```

**What this does:**
- Runs all unit tests in `app/src/test/kotlin/`
- Reports test results
- Fails build if tests fail

### Manual Testing

**1. Use MCP Inspector (Recommended for tool testing)**

**In SSE mode:**
1. Start server: `./gradlew :app:runSse`
2. In another terminal, start inspector:
   ```bash
   npx @modelcontextprotocol/inspector
   ```
3. Open browser to URL shown by inspector (usually `http://localhost:5173`)
4. In the MCP Inspector interface:
   - Choose the transport type to be **SSE**
   - Write the MCP server endpoint: `http://127.0.0.1:3001`
   - Press **Connect**
5. Test tools in the web interface 

**2. Test with actual MCP client**

Configure your MCP client (Cursor, Claude Desktop, etc.) to use local server:
- SSE mode: `http://127.0.0.1:3001`
- Stdio mode: Configure command as `java -jar /path/to/chromia-mcp-server.jar --stdio`

**Note:** Testing with actual MCP clients can be costly due to repeated query testing.

## Common Setup Pitfalls and Fixes

### Issue: Gradle wrapper not executable

**Symptoms:** `Permission denied` when running `./gradlew`

**Fix:**
```bash
chmod +x gradlew
```

### Issue: Port 3001 already in use

**Symptoms:** Server fails to start with "Address already in use" error.

**Fix:**
- Find process using port: `lsof -i :3001` (macOS/Linux) or `netstat -ano | findstr :3001` (Windows)
- Kill the process or use a different port (requires code modification)

### Issue: RAG store fails to load

**Symptoms:** Documentation search returns errors.

**Fix:**
- Check internet connection (embeddings are downloaded from GitLab packages)
- Verify GitLab packages are accessible
- Check server logs for download errors
- If embeddings are unavailable, documentation search will not work (but server continues running)

### Issue: MCP Inspector cannot connect

**Symptoms:** Inspector shows connection errors.

**Fix:**
- Verify server is running: `curl http://127.0.0.1:3001/health`
- Check server logs for errors
- Verify correct endpoint URL: `http://127.0.0.1:3001` (not `/mcp` or other paths)
- Check firewall settings if using remote server

## Development Workflow

### Making Code Changes

1. **Edit code** in `app/src/main/kotlin/`
2. **Rebuild** (if needed): `./gradlew build`
3. **Restart server** to see changes:
   - Stop server (`Ctrl+C`)
   - Start again: `./gradlew :app:runSse`
4. **Test changes** using MCP Inspector or MCP client

### Adding New Tools

See [Architecture.md](./Architecture.md) for detailed instructions on adding new tools.

**Quick summary:**
1. Define tool in `McpTools.kt`
2. Create strategy class in `ToolExecutor.kt`
3. Register tool in `App.kt`
4. Register strategy in `ToolExecutor.kt`
5. Add repository method (if needed)
6. Test with MCP Inspector

### Debugging

**Enable debug logging:**
- Edit `app/src/main/resources/log4j2.properties`
- Set log level to `DEBUG` for desired packages
- Restart server to see debug logs

**Use MCP Inspector:**
- Provides web interface to test tools
- Shows request/response details
- Helps identify parameter issues

**Check server logs:**
- Server logs all tool requests and responses
- Look for error messages in console output
- Check for exception stack traces

## Additional Development Resources

- **MCP Protocol Specification:** https://modelcontextprotocol.io/
- **Kotlin MCP SDK:** https://github.com/modelcontextprotocol/kotlin-sdk
- **Kotlin MCP SDK Samples:** https://github.com/modelcontextprotocol/kotlin-sdk/tree/main/samples - Contains sample implementations that demonstrate how MCP applications can be implemented using Kotlin SDK
- **Ktor Documentation:** https://ktor.io/docs/welcome.html
