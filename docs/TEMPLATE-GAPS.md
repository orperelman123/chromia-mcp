# Which classes have no template, and why that is the queue

Eight adversary rounds. **Every un-templated class attacked has drained.** That
is the whole basis for this file: the next drain is predictable from the
redirect map, not from the last report.

Do not round that up to "the templates are safe". The corpus records the
SHIPPED lending template being drained twice - `r7-lending-bad-debt-exit-race`
and `r8-lending-pool-cap-collateral-lever` - so having a template lowers the
odds, it does not close them. Both are fixed and both ship the original drain
as a must-fail test, which is the standard a fix has to meet here. The honest
claim is narrower and still worth acting on: a class with no template has
drained every single time it was attacked.

`closestTemplateNote()` answers an ask it has no template for by naming the
nearest one. GOAL.md already rules out the obvious defence - *"we never claimed
to cover streaming" is not a defence, because the agent asked this server how to
build and this server answered* - and round 8 showed the redirect itself is the
hazard: `scaffold_dapp template=amm` silently became `template=vault`, and the
AMM the agent then built was drained by a sandwich sized to fit inside a normal
2% tolerance. The note even said "an AMM's own invariant is yours to prove". The
agent proved nothing, because a disclaimer is not a guard.

So a redirect is a **debt**, and this is the ledger. Ordered by what an agent is
most likely to ask for next times how far the redirect target is from the ask.

**Closed so far:** `amm` / `dex` / `swap`, which used to land on `vault`. The
`amm` template ships the sandwich and JIT liquidity as unwritable rather than
detected, and the redirect now names it.

| Ask | Redirects to | What the target does NOT cover | Distinct exploit class |
|---|---|---|---|
| `stablecoin` | `vault` | the peg, the collateralisation ratio, liquidation of a CDP | a mint-against-collateral position is not a reserve row. Closest sibling is `lending`, not `vault`: it has a ratio that moves with a price, so it inherits every round-8 lesson about re-pricing history, and adds a peg that arbitrage is *supposed* to move |
| `exchange` | `amm` (was `vault`) | an order book: resting orders, partial fills, matching, cancellation | landing `amm` is closer than `vault` was, but an order book is not a curve. A resting order is a standing commitment at a stale price - the marketplace's timed-auction lesson (no mutable bid field; the standing bid is its own immutable escrow row) is the nearest precedent, and matching adds an ordering neither template has. The word "exchange" moving from one wrong template to a less wrong one is not the same as being covered |
| `subscription`, `allowance` | `streaming` | recurring PULL billing, where the payer is charged period after period | `streaming` is PREPAID to one named beneficiary and its safety rests on that: the money is already escrowed. A pull model has no escrow, so the failure is a charge that should not have happened - the opposite direction, and none of the streaming guards address it |
| bridge / cross-chain | *nothing* | everything | not in the redirect map at all, so it falls to the `else` branch, which at least says plainly that nothing covers it. Highest severity if built, lowest likelihood of being asked here |

## How to use this

Take the top row that is not in flight. Build the template the way the other
eight are built - the exploit made **unwritable**, not merely detected
(GOAL.md principle 4), every guard carrying a mutant that reddens a shipped
must-fail test *because the attack landed*, and the shape SHIPPED rather than
described.

That last point is not style. Rounds 7 and 8 were both topped by a defect in
our own prose - a prescribed holding period that WAS the vulnerability, and a
claim that a monotone counter "can never rewrite the past" when a monotone
subtrahend is a monotone clawback. A paragraph that describes a safe shape and
leaves the guards to the reader has now produced a drain twice.

**When a template lands, delete its row and fix its redirect in the same
commit.** A stale row here is the same defect as a stale sentence in a header.
