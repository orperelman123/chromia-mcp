# Release checklist — v0.5.0

Everything is prepared locally; these are the remaining owner steps, in order.

1. **Push main**
   ```bash
   git push origin main
   ```
   This triggers CI and auto-deploys the Render service from the new commit.

2. **Verify CI + deploy**
   - GitHub Actions: `CI` workflow green (unit suite + e2e sweep + stdio smoke).
   - `curl https://chromia-mcp.onrender.com/health` — `version` should now show the
     new build (git describe of the pushed commit; after step 4's tag it shows `0.5.0`
     on the next deploy).

3. **Render dashboard** (one-time settings; the Blueprint declares them but existing
   services don't pick plan changes up automatically)
   - Upgrade plan **starter → standard** (2GB; the server needs ~1.5GB — starter idles
     at ~98% memory and OOMs under load).
   - Set **Health Check Path** to `/health`.
   - Once on 2GB: clear `CHROMIA_MCP_DISABLE_TOOLS` if you want the full toolset
     (Rell compiler tools + on-chain queries) hosted.

4. **Tag the release**
   ```bash
   git tag v0.5.0
   git push --tags
   ```
   `release.yml` runs the full test suite, builds the fat jar, and creates the GitHub
   Release `v0.5.0` with `chromia-mcp-server.jar` attached.

5. **Publish the npm launcher**
   ```bash
   cd packages/npm
   npm publish
   ```
   Needs `npm login` first. **Constraint**: the launcher downloads the jar from the
   `v0.5.0` GitHub Release anonymously — the repo/release must be **public**, or
   users must set `CHROMIA_MCP_JAR` to a local jar. Package version (0.5.0) and tag
   (v0.5.0) must match — they do.

6. **Optional — upstream MR**: open a merge request to
   `gitlab.com/chromaway/core-tools/chromia-mcp` from the company account using the
   patch-ready notes in [docs/UPSTREAM.md](docs/UPSTREAM.md) (11 findings).
