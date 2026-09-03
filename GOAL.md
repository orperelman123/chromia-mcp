# The goal

**This MCP must make it as easy as possible for an agent to build secure Chromia dapps and chains — and the dapps it produces must be ones an auditor cannot drain.**

Everything else in this repo serves that. When a change is worth making, it is because it moves this; when a claim is worth doubting, it is because it only appears to.

## The bar: zero exploits

**A dapp built from this MCP must contain no exploit an auditor can land. Zero — not "fewer than last round".**

That is a number a round can fail, and most rounds have. It is not "the gate returned `ok:true`" — that is a linter result, and on 2026-09-02 it certified four agent-built dapps of which two were trivially drainable. The test is always:

> An agent builds a dapp using **only** this server's guidance, templates and gates. An independent hostile auditor then attacks it. Whatever they can drain is our failure, not the agent's.

**A round passes only when the auditor reports that they could drain nothing.** Anything less is a red round, however much coverage moved. Two things follow from that, and both have already bitten:

- **A drain in a class we have no template for is our failure too.** Every un-templated class attacked so far has drained. "We never claimed to cover streaming" is not a defence — the agent asked this server how to build, and this server answered.
- **The prose is part of the attack surface.** Round 7 drained a build whose author followed the template header exactly: the header prescribed a holding period against a step in pool value, and the attack was exit-only, so the advice was the vulnerability. A header sentence that is wrong is a defect of the same kind as a missing guard, and the residual list — where an auditor places the most trust — is the worst place to be wrong.

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

**Not met.** Round 7 drained the lending template as shipped, an extension written by following that template's own header, and an un-templated payment-streaming dapp — all three certified `ok:true` with zero findings. The first two are fixed; the third has no template yet. Earlier rounds drained every un-templated class they touched.

What holds: each template makes its own exploit class unwritable rather than merely detected, and the guards are load-bearing — every one has a mutant that goes red because the attack lands. The corpus at `app/src/test/resources/exploit-corpus/` records exactly which exploits are caught, which are open, and which are open on purpose because a precise rule is impossible. Read it rather than trusting this paragraph.
