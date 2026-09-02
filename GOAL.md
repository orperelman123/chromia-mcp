# The goal

**This MCP must make it as easy as possible for an agent to build secure Chromia dapps and chains — and the dapps it produces must be ones an auditor cannot drain.**

Everything else in this repo serves that. When a change is worth making, it is because it moves this; when a claim is worth doubting, it is because it only appears to.

## The acceptance test

Not "the gate returned `ok:true`" — that is a linter result, and on 2026-09-02 it certified four agent-built dapps of which two were trivially drainable. The test is always:

> An agent builds a dapp using **only** this server's guidance, templates and gates. An independent hostile auditor then attacks it. Whatever they can drain is our failure, not the agent's.

Run it as a round: build, attack, pin what got through, fix, re-attack. `app/src/test/resources/exploit-corpus/` is the standing scoreboard — every exploit found becomes a sample with a pinned verdict, so coverage is measured on every build instead of claimed in a report.

## Four principles, in priority order

1. **Guidance must not manufacture bugs.** A template that teaches an insecure pattern outranks any missed detection: it *creates* the hole. Defaults are what agents copy, so the default must be the safe one. (The scaffold once shipped `add_auth_handler(flags = [])` as the golden pattern; FT4 resolves flags with `contains_all()`, and `contains_all([])` is always true, so every value-moving operation copied from it silently dropped the Transfer flag.)
2. **Detection must not be evadable.** A rule keyed on anything the attacker chooses is not a boundary — key on **type and use, never on names**, and attack your own rule before shipping it. (`authorization-not-bound-to-caller` once matched a parameter-name allowlist: HIGH as `from`, silent as `victim`, same drain. The same evasion class then appeared three more times, and was finally systemic in a shared helper.)
3. **The gate must not cry wolf.** A gate agents route around is worse than no gate. A rule that cannot be made precise is **dropped, with the reasoning stated** — not shipped noisy. Undecidable properties (conservation, quorum) stay MEDIUM advisories and never make `ok:false`.
4. **What cannot be caught statically needs a different answer** — a template that makes the bug unwritable, a test the scaffold ships, or a runtime check. Never a regex pretending. The `governance`, `vault` and `staking` templates exist because their exploits are undecidable from syntax; each ships the original drain as a must-fail test.

## Never claim more than is true

`ok:true` reads to an agent as an audit pass. The notes must state the boundary, gaps must be named rather than rounded up, and a lane stays red rather than being closed by softening its assertion. **A fake green is worse than a red** — every one this project shipped came from evidence that looked like proof: a cached build that ran nothing, an empty results directory read as zero failures, a mutant that "went red" because the chain failed to start, a rule that passed every test its author wrote.

`scripts/loop-gate.mjs` is that discipline as a command. Use it instead of reading a build log.

## Where things stand

Honest, and not yet done. Faithful builds on the templates have survived hostile review; hand-written operations that deviate from a template's structure, and value classes with no template, are where drains still land. The corpus records exactly which — read it rather than trusting this paragraph.
