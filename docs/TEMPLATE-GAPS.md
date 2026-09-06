# Which classes have no template, and why that is the queue

Twelve adversary rounds. **Every un-templated class attacked has drained.** That
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

**Closed so far:** `exchange` / `order book` / `limit order` / `matching engine`.
That row was the top of this table and its answer was the drain: it said no
template covered an order book and offered two sentences instead - the
marketplace's immutable escrow row, and "an order that can be pulled in the block
it would have been filled in is not a commitment at all". Round 12 implemented
both literally and lost half a maker's inventory to a taker who bought one unit an
hour, because an order with no mutable field means a partial fill is
delete-and-recreate and the remainder's clock starts now. The `exchange` template
ships an order book whose resting TERMS are immutable and whose partial fill
writes ONE MONOTONE COUNTER (`filled`) and nothing else, so the row is never
re-created; whose matching no caller can reorder, because no operation names a
counterparty; and whose crossing orders are filled at the resting price in the
block they are signed. Both branches of the recreate are pinned drains, and the
grind ships as a must-fail test with a mutant.

`amm` / `dex` / `swap`, which used to land on `vault`. The
`amm` template ships the sandwich and JIT liquidity as unwritable rather than
detected, and the redirect now names it. `stablecoin` / `cdp` / `peg`, which
also landed on `vault` - round 9 built it there and drained it by redeeming at
par out of a shortfall (`r9-stablecoin-redemption-at-par-exit-race`). The
`stablecoin` template has no redeem-at-par to delete: the peg is the debtor's
burn against their OWN debt, an under-water position closes by PRO-RATA
liquidation *while the system as a whole is still worth its coin*, and an
insolvent system SETTLES into one pool every coin redeems the same share of.
That italicised clause is round 11's, and it is there because the per-position
pro-rata cap - the whole answer this paragraph used to give - held while the
drain went one level up: seized collateral leaves the common settlement reserve
faster than the coin it retires, so at 98% system backing the liquidator was
paid 104 tokens for liquidating-then-settling where settling first paid 89, and
7 of the 15 tokens that moved came from a holder who was party to no liquidation
at all. The round-9 and round-11 numbers ship as must-fail tests in both orders,
and the redirect now names it ahead of `lending` (which claims "debt").

| Ask | Redirects to | What the target does NOT cover | Distinct exploit class |
|---|---|---|---|
| `subscription`, `allowance` | `streaming` | recurring PULL billing, where the payer is charged period after period | `streaming` is PREPAID to one named beneficiary and its safety rests on that: the money is already escrowed. A pull model has no escrow, so the failure is a charge that should not have happened - the opposite direction, and none of the streaming guards address it |
| bridge / cross-chain | `ft4` (via `token`, `asset`, `transfer`) | everything - burn-proof authenticity, replay protection, destination-chain minting | **the earlier version of this row said bridge asks "fall to the `else` branch, which at least says plainly that nothing covers it". The route audit of 2026-09-03 disproved that: every realistic phrasing names a token or an asset, so "a cross-chain token bridge" is answered "Use `template=ft4`: it ships the conservation invariant tests" with no warning at all. Only the bare words reach `else`.** Highest severity if built; the ledger's own reason for deprioritising it was false |

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
