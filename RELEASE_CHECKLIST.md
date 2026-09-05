# Release checklist — v0.5.0

Everything is prepared locally; these are the remaining owner steps, in order.

1. **Push main**
   ```bash
   git push origin main
   ```
   This triggers CI.

2. **Verify CI**
   - GitHub Actions: `CI` workflow green (unit suite + e2e sweep + stdio smoke + the
     launcher's release download + the fresh-install boot against the published index).

3. **Hosted service** — none since 2026-09-05 (the Render service is suspended; the
   product is the local install). Nothing to do here.

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
