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

**Not met.** Ten templates ship, and the record is still the same shape it has been since round 1: **every un-templated class attacked has drained** — nine rounds, nine classes — and four *shipped* templates now carry recorded drains (lending twice, staking, governance). Every one of those was certified `ok:true` with zero findings and every conservation invariant exact. The scoreboard is `app/src/test/resources/exploit-corpus/CORPUS.md`; read it rather than this paragraph.

Two findings outrank any single drain, and both were in the gate itself:

- **The gate disarmed on every real project.** Adding the `@test` module the scaffold ships - and `run_rell_tests` requires - reclassified `main` as test surface, downgraded every production HIGH to advisory and flipped `ok` to true. It was armed only for a `main.rell` submitted with no tests, the one shape a real project never has. Fixed, pinned in three shapes including the rename evasion.
- **An object field write was not a mutation.** `config.fee = f` matches none of `create|update|delete`, so an unauthenticated operation whose only write was an object assignment drew no finding - and the default template taught exactly that spelling. The corpus row for the class was `CAUGHT` the whole time, on the *other* spelling. **A corpus row proves the sample is caught, not the class.**

The drains that were not missing guards were wrong sentences. Rounds 7, 8 and 9 were each topped by a defect in our own prose - a prescribed holding period that *was* the vulnerability; a claim that a monotone counter "can never rewrite the past"; a seam citing a victim's 144 bps execution loss as the pool's 79 bps reserve movement. Three of the defects found on 2026-09-03 were introduced that same day while fixing others. A green suite cannot tell whether a sentence is true, so the arithmetic a header cites is now recomputed *in a test* from the template's own constants, and the honest rule is: **every claim in the guidance is either testable or deleted.**

Open and measured, not started: round 9's three prose drains (the AMM's "more than 1500" reads as a bound and is a floor with no ceiling; the lending residual's "only against an already-insolvent position" is false because a liquidation *creates* insolvency below 110% backing; seam 2 guards a confederate exiting when they only need to pay), the AMM's first-depositor claim, and the marketplace's miscounted guards. Round 10, found by reading the governance header, is closed in the template with replay and mutant.

What holds: each template makes its own exploit class unwritable rather than detected, and every guard has a mutant that reddens a shipped must-fail test *because the attack lands* - a mutant that goes red for any other reason proves nothing, and one was caught doing exactly that. `scripts/loop-gate.mjs` is the merge discipline as a command; `docs/ADVERSARY-ROUND-BRIEF.md`, `docs/TEMPLATE-GAPS.md` and `docs/AGENT-LANE-BRIEF.md` are the method, so a session can be picked up cold.
