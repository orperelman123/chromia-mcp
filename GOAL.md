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

**Not met.** Round 8 attacked five builds and drained three. Every one of its top findings was a defect in our own prose rather than a missing guard, which is the second round running that this has been true:

- The streaming seam stated a safety property that is false — that a monotone paused-milliseconds counter "can never rewrite the past". A monotone *subtrahend* is a monotone *clawback*: raising it lowers what was earned at every past instant. Two builds followed that paragraph and differed by a single `require()` nobody had told them to write; the one missing it handed over 100% of a payroll escrow.
- The lending seam was scoped to "any new **operation** that moves pool value in a step". A utilisation rate curve adds no operation, satisfies every stated rule, and still let a lender's own withdrawal make a *healthy* borrower liquidatable at an unchanged oracle price. The correct fix — a checkpointed index — was the very shape the seam forbade, so the guidance pushed the author away from the only right answer.

Both are now fixed the same way: the shape is **shipped in the template** instead of described, because a paragraph that describes a safe shape and leaves the guards to the reader has now produced a drain twice. The pause/resume transitions, their two `require()`s, the checkpointed index and a per-position `recoverable_debt()` are all in the templates, each with a mutant that reddens a shipped must-fail test because the attack lands.

What is still open is the AMM. Round 8 drained one through a sandwich sized to fit inside a normal 2% tolerance, and it existed only because `scaffold_dapp template=amm` redirected to `template=vault`. Nothing in the source separates a sandwich from two honest trades, so that row stays a deliberate GAP and the answer is a template, in progress. **Eight rounds, eight un-templated classes attacked, eight drained** — that record, not any single finding, is the argument for building the template before the rule.

What holds: each template makes its own exploit class unwritable rather than merely detected, and the guards are load-bearing — every one has a mutant that goes red because the attack lands. The corpus at `app/src/test/resources/exploit-corpus/` records exactly which exploits are caught, which are open, and which are open on purpose because a precise rule is impossible. Read it rather than trusting this paragraph.
