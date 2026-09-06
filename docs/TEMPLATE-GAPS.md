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

**Closed so far:** `bridge` / `cross-chain` / `wrapped` asset. That row was the
top of this table and the highest-severity entry it has ever carried, and its own
stated reason for being deprioritised was FALSE: the earlier text said bridge asks
"fall to the `else` branch, which at least says plainly that nothing covers it",
and the route audit of 2026-09-03 disproved it - every realistic phrasing names a
token or an asset, so "a cross-chain token bridge" was answered "Use
`template=ft4`: it ships the conservation ... invariant tests to copy for your own
economics" with no warning at all. Round 14 built a receiver on that answer,
carried every guard this server ships, drew ok:true with ZERO findings, and lost
TEN TIMES its backing: nothing recorded which burns had already been paid, so one
burn of 1000 submitted ten times minted 10000, and three attestations quoting one
source transaction paid three accounts 1000, 5000 and 250000. The prose defect
underneath both is the invariant the redirect handed over:
`test_transfer_conserves_total_points` compares balances against a counter THE
MINTING OPERATION RAISES ITSELF, so it was exact at every step of the 10x mint - a
TRANSFER-conservation test is structurally blind to a mint. The `bridge` template
makes all of it unwritable: the PROCESSED-BURNS REGISTRY is keyed by the burn's
identity on the source chain, so a repeat is refused by the database rather than
by a check somebody has to remember; the ROW BINDS WHAT THE BURN PAYS, with
recipient and amount written once by the attestation that opens it and read from
the row by the mint; one relayer counts once per burn; the counter EQUALS the
threshold in exactly one transaction, so the mint happens once with no flag to
forget; the relayer set is configuration, shut before anything may be attested;
the mint is capped per period and in total; and the invariant compares what was
MINTED against the burns it ACCEPTED. Both drains ship as must-fail tests, each
with a mutant that reddens it because the attack lands.

**Closed so far:** `subscription` / `allowance` / recurring PULL billing. That row
was the top of this table and its answer was the drain: it sent the ask to
`streaming`, which is PREPAID to one named beneficiary and every guard of which
rests on the money already being escrowed. Round 13 built from that redirect,
carried over every structural guard that CAN be carried, and was drained twice at
HIGH: the claim was on the payer's ACCOUNT rather than on an escrow, so one
permissionless charge() eighty-three years later took all 9990 points she held
and every point that arrived afterwards; and streaming's `cancellable = false`,
which there protects a vesting grant, here meant NEITHER PARTY could end it. Its
period boundary was a whole fee rather than the one-unit staircase the streaming
header bounds it at - two identical subscribers cancelling ten minutes either
side of a boundary in a thirty-day period paid 1000 and 2000. The `subscription`
template makes all three unwritable: a merchant's whole claim is the escrow the
payer funded, the fee accrues PRO RATA so nothing is billed in advance and no
boundary is worth straddling, and either party may ALWAYS cancel - there is no
`cancellable` term, because a pull authorisation that cannot be revoked is a
standing claim on a person rather than a right over a sum. Both drains ship as
must-fail tests with mutants.

`exchange` / `order book` / `limit order` / `matching engine`.
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

**THE TABLE IS EMPTY.** For the first time since it was written, there is no ask
in it that no template covers.

| Ask | Redirects to | What the target does NOT cover | Distinct exploit class |
|---|---|---|---|
| _(none open)_ | - | - | - |

That is not a claim that no class is missing. It is a claim that no class we have
NAMED is missing, and naming is the part that has always lagged: every drain in
this project landed in a class this file had either not thought of or had ranked
below the one that got built - and the bridge row is the sharpest case, because it
sat at the top for four rounds carrying a reason for its own deprioritisation that
was not true. So the next drain is still predictable from the redirect map rather
than from this table. When you find a class with no template, put it here FIRST,
before you build anything.

## How to use this

Take the top row that is not in flight. Build the template the way the other
twelve are built - the exploit made **unwritable**, not merely detected
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
