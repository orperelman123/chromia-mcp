# Which classes have no template, and why that is the queue

Twenty adversary rounds. **Every un-templated class attacked has drained.** That
is the whole basis for this file: the next drain is predictable from the
redirect map, not from the last report.

Round 20 made the claim sharper and more uncomfortable at once. It attacked the
un-templated class at the top of this table **by building the design note this file
gives for it** - eleven guards, every one proved load-bearing on a real chain - and
drained it five of five draws on 4.76% of the stake with nothing forfeited. So the
row was right that the class drains; and **a row's design note is production advice
with no test behind it**, which is a second way for this file to be the hazard. A
note here should carry the INVARIANT it claims as something a chain can be made to
answer, not as a sentence. That row is now closed by `template=raffle`, and the
section below keeps the note and its counterexample together on purpose.

Round 18 added a second way to reach an un-templated class, and it did not need a
missing row: **an ask that names an uncovered class ALONGSIDE a covered one used to
be answered by the covered one alone.** "a lending pool that also runs a weekly
raffle for depositors" was scaffolded onto `lending` - three files, `ok:true`, and
1517 bytes about lazy interest accrual with NOT ONE WORD about the raffle, the class
this server declines by name when it is asked for on its own. The row below existed,
the refusal existed, and the ordered `when` reached `lend*` four branches before it
looked at `raffle*`. That is fixed (the uncovered classes are now a LIST read BEFORE
the routing, so the uncovered half is named first, the covered half is named by
template, and nothing is scaffolded) - but the lesson belongs at the top of this
file: **a row here is only worth what the routing does with an ask that mentions two
things.**

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
must-fail tests with mutants. Round 14 attacked that template the round it
shipped and found two more, both closed the same way: funding bought time that
had ALREADY PASSED, so a top-up after eleven unfunded months paid for the eleven
months in the block it landed (funding now buys time FORWARD); and the boundary
step is `amount_per_period * block_ms / period_ms` and not the streaming header's
one unit - measured at 2778 units, 27.8% of the funding, on the largest fee and
shortest period the module used to accept - so the fee is now bounded against the
period at one unit per second of it.

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

**Closed so far:** `insurance` / premiums and cover / a claims pool. That row was
the top of this table - it was the row that made the table stop being empty, added
on 2026-09-07 - and its answer was the drain, in the same sentence as the bridge's:
`closestTemplateNote("an insurance pool funded by premium payments")` matched the
`payment*` key, answered `template=ft4`, and said only that it "ships the
conservation, no-negative-balance and non-owner-must-fail invariant tests to copy
for your own economics". Round 17 built exactly that, carried every guard the answer
names - the holder is the SIGNER and never an argument, every amount bounded, the
premium at least 10% of the cover - drew `ok:true` with ZERO findings on all three
samples, kept the copied invariant EXACT at every step, and drained twice. A CANCEL
REFUNDED A SPENT PREMIUM: alice paid 100, took 300 of the other members' premiums on
a claim and got her 100 back, leaving the reserve at MINUS 100 with 2000 of cover
still written against it, and repeating the cycle took her to 2100 from 1000. AND
THE CLAIMS RACED: two holders covered for 1000 each against a reserve of 200 both
suffered a covered loss, and whoever landed first took the whole 200 while the other
was refused down to a single unit - 100 points on transaction order alone, where pro
rata is 100 each. The copied invariant could not see either one, because a drain in
an insurance pool is a REDISTRIBUTION: total value never moves. The `insurance`
template makes both unwritable: there is ONE HELPER THAT RETURNS A PREMIUM and every
exit path calls it, and what it returns is the premium LESS WHAT THE POLICY HAS
ALREADY BEEN PAID, so "refund the whole premium" has nowhere to be written; and
NOTHING IS PAID INSIDE A CLAIM - a CLAIM ROUND snapshots the reserve and the total
claimed and pays every claimant `reserve * claim / total_claimed`, the stablecoin's
shared-settlement shape, so transaction order moves nothing. A REFUND IS PRO RATA
for the same reason, because the exit race is one operation further along and is
reachable in any pool that has ever paid a claim larger than the claimant's own
premium. Cover is bounded by the reserve times a configured multiplier with a
positive default, the premium is a fraction of the cover, and the invariant it ships
is the one that is actually true for a pool: `premiums_in - claims_out - refunds_out
== reserve`. Both drains ship as must-fail tests, and all TWELVE guards carry a
mutant that reddens a shipped case because the attack landed.

Round 18 then attacked that template and found the race NEITHER promise names - a
cancel against the SETTLEMENT. Both pro-rata promises were true of what they said:
cancel-against-cancel and claim-against-claim. But a claim is public the moment it is
filed, settlement is REFUSED until `CLAIM_WINDOW_MS` (24 hours) has passed, and a
policy with no claim in the round could still walk out of it - so a loss-free member
had a protocol-guaranteed day, with somebody else's loss already on chain, to take
her premium out of the reserve that loss was about to be paid from. Measured on a
chain with three members, three policies of 400 cover for 100 premium (every purchase
exactly on the reserve bound) and a covered loss of 400: `alice=1000 bob=1000
eve=1000` against the honest `alice=1200 bob=900 eve=900` - **two hundred points on
transaction order alone**, `rell_security_check` ok:true with zero findings. Closed
the way the other two were, unwritable rather than checked: `retire_policy()` refuses
outright while a round is open, `leave_policy()` is the one place a policy leaves and
an in-round exit JOINS the round instead of racing it, and `settle_claim_round` pays
the claims, closes the round and only then drains the exit queue through the same
refund helper. `file_claim` refuses a policy that is leaving, which is the symmetric
half of the refusal that already existed. The drain and its control ship as cases
asserting ONE string from both transaction orders, and the mutant reddens with the
numbers above.

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

**THE TABLE IS NOT EMPTY, and the sentence that said it was cost a round.** It read
"THE TABLE IS EMPTY. For the first time since it was written, there is no ask in it
that no template covers", and the paragraph under it admitted the real limit -
*"naming is the part that has always lagged"*. Round 17 then measured thirty-four
ordinary asks through the shipped redirect
(`realworld/adversary-round17/redirect/raw.json`) and got seven classes it does not
cover, took the first of them - an insurance pool - built it from this server's own
answer, and drained it twice. An empty table was never a claim about the world; it
was a claim about what this file had got round to writing down. So the rows below
are the measurement, not a brainstorm, and the rule at the bottom of this file
applies to every one of them.

Ordered by what an agent is most likely to ask for next times how far the answer is
from the ask. Three of the seven now answer `(none)` because round 17's fix made
them honest refusals that NAME the missing guard - which is worth more than a
confident redirect to guards that do not cover the class (rounds 8 and 14) but is
still a gap, and is still where the next drain lands.

**THE TOP ROW IS THE RANDOMNESS CLASS, AND ROUND 18 DRAINED IT ON A CHAIN.** It is
first not because the refusal is dishonest - it names the missing guard - but because
it is the only row in this table with a MEASURED drain against it, and because it is
the row the laundering reached. `fixtures/raffle/` is the raffle half of "a lending
pool that also runs a weekly raffle for depositors", built from the answer that ask
received, carrying every guard that answer's own discipline implies: the depositor is
the SIGNER and never an argument, the prize is FUNDED before it is paid (a tenth of
every deposit), the draw is PERMISSIONLESS so no operator can withhold it, the week is
enforced against the BLOCK CLOCK and never against a caller's argument, a round settles
once, the winner is drawn in PROPORTION TO STAKE, every amount is bounded, and nothing
is minted anywhere. `rell_security_check`: **ok:true, ZERO findings.** `chr test`: 3 of
3 green. The draw's entropy is `op_context.last_block_time`, the timestamp of the block
ALREADY COMMITTED, so the attacker predicts nothing - she watches blocks land and
answers the one whose ticket is hers. Over five weeks with three depositors, trudy
staking 90 of 1890 (about one week in twenty-one at honest odds): **trudy wins 5 of 5
prizes and turns 100 into 400, while alice and bob each put in 1500 and end at 1350** -
300 points moved, and she never waits more than two minutes of blocks. The control, the
same five weeks drawn at the first second the operation allows, gives trudy ONE of the
five, which is what her stake is for. AND EVERY DRAINED DRAW IS LEGAL: in the block she
chose, alice and bob are EACH refused "you are not this week's winner", so no rule keyed
on the operation can tell that block from any other - which is why this is a template's
job and not a checker's.

**AND THE ONE RULE AIMED AT THIS CLASS DID NOT FIRE, WHICH IS THE SECOND HALF OF WHY IT
IS FIRST.** `rell_security_check` returned ok:true with ZERO findings on the fixture -
and it is not that no rule exists: `block-clock-randomness` was added in round 5 for
exactly this ("a raffle picks its winner with `op_context.last_block_time %
ticket_count`"), it is HIGH, and its own text says hashing the clock or routing it
through a helper does not change the answer. It stayed silent here. Reading the rule
beside the fixture, the shape it looks for is the clock reduced or compared and then
DECIDING A BENEFICIARY it can name; this draw computes `winner_at(op_context.
last_block_time)` inside a helper, then `require(winner == caller)` against the
caller's own parameter and pays the row `depositor @? { .account == winner }` - the
beneficiary the rule would have to name is reached through the parameter the attacker
supplied. That reading is a lead for the rules lane and not a measurement; what IS
measured is the verdict, ok:true with zero findings, recorded in
`realworld/adversary-round18/fixtures/raffle/`. The redirect used to hand the ask to
`lending` and say nothing about the raffle, and the fix for THAT (an uncovered class is
named first, the covered half is named by template, nothing is scaffolded) is a fix to
the ANSWER, not to the class. An agent that reads the refusal, builds the raffle anyway
and asks this server to check it still gets ok:true. Until a sixteenth template ships,
the honest position is that this server can tell an agent the guard is missing and
cannot tell it that the guard is absent from the code in front of it.

**WHY NOT IN THIS ROUND.** A commit-reveal raffle whose draw no caller can choose the
block for is the shape (an entry commits `hash(secret)`, the reveal is bounded by a
deadline with a forfeitable deposit, and the draw mixes only revealed secrets, so the
last revealer's choice is between revealing and forfeiting rather than between blocks).
It is a full template - main module, shipped suite, guard-by-guard mutants, redirect,
module args, corpus rows - built and measured on a real chain the way the other fifteen
were, and this lane's budget went to the fifteenth template's third drain and to the
laundering that put the raffle in front of an agent in the first place. The row stays,
at the top, with the numbers.

**ROUND 19: A SYNONYM WALKED AROUND THE WHOLE LIST, AND TWO REFUSALS WERE SAFE BY
ACCIDENT.** Round 18's fix - the uncovered classes read out of a list BEFORE the routing
`when` - was right, and round 19 measured what it is worth when the list is A LIST OF
WORDS. `a tombola for token holders that pays out weekly` was answered **ok:true,
`template=ft4`, FOUR FILES** and not one word about the draw: `tombola` is a raffle and
was not one of the six spellings, so `declined` came back empty, `token holders` sent the
ask to the token skeleton, and the agent got a guard-free skeleton for the class this
table has at its top with a MEASURED drain against it. That is round 18's laundering with
the uncovered half spelled by a synonym instead of hidden behind a second clause. And the
same ask in French and Spanish - `une loterie hebdomadaire pour les deposants`, `un sorteo
semanal de premios para los depositantes` - declined, but declined as "unknown template"
rather than "this class has no template": nothing matched at all, so the answer never
named the class or the missing guard, and an agent that reads it learns only that we have
no template by that name.

Both are fixed at the root and the rule generalises past this file: **an uncovered class
is recognised by CONCEPT, not by spelling.** Each class's vocabulary is built from what
the class IS - every ordinary English name for it plus French, Spanish, German,
Portuguese, Italian, Dutch and Polish - and an ask is FOLDED before it is tokenised
(`DappScaffold.foldAsk`), because the tokeniser is `[a-z0-9]+` and every accent used to be
a word boundary: `lotería` tokenised to `loter` + `a` and matched nothing at all. The
answer to a class that names nothing covered now carries the class and its missing guard
(`declinedOnlyTail`), which is what the two "safe by accident" refusals were missing.

**AND THE RULE THAT GOES WITH IT, because a vocabulary is a place to be careless:** every
stem was chosen against round 19's own clean pass, `a lending market for ABETTING
collateral positions`, which routes to `lending` because `abetting` does not START with
`betting`. There is no `bet*` (it takes `better`, `between`), no `pari*` (it takes
`parity`, a stablecoin word), no bare `signer*` (it takes a bridge's relayer signers), no
`pledge*` (it takes a lending pool's pledged collateral), and no `donation*` or `gaming*`
(those are two OTHER rows of this table, and answering them with the raffle's guard would
be this finding pointed the other way). `Round19TemplateSurfaceProbeTest` pins a
twenty-five-ask clean-pass corpus across the whole covered roster beside the multilingual
corpus, so a stem that starts firing inside a covered word reddens the class it steals
from.

**STILL OPEN AFTER ROUND 19, and written down rather than implied:**

- **A SCRIPT WITH NO LATIN TRANSLITERATION IS INVISIBLE TO EVERY KEY LIST, covered and
  uncovered alike.** `TOKEN` is `[a-z0-9]+`; folding reaches the Latin scripts and
  nothing else. Round 18 measured a Chinese insurance ask (`带索赔的保险池`) tokenising to
  NOTHING, and it falls to the roster - which is the honest answer for an unknown name and
  the WRONG answer for an uncovered class, because the roster does not name the missing
  guard. The fix is not more keys: it is a transliteration or an embedding step in front
  of the tokeniser, and it is a lane of its own.
- **TWO ROWS OF THE TABLE BELOW HAVE NO NAMED REFUSAL.** The loyalty/points/gaming-item
  row and the fee-splitter/charity/yield-aggregator row reach the generic roster, not a
  class paragraph with a missing guard, and `untemplatedClasses` has four entries where
  this table has six uncovered classes. They were deliberately NOT added in this lane -
  each one changes what three frozen redirect recordings say, and a wrong named class is
  worse than the roster - but they are the same defect as the two "safe by accident"
  refusals round 19 measured, one row down.
- **THE `describe_tool` LONG FORMS ARE NOT AUDITED SENTENCE BY SENTENCE.** Round 19's
  review measured what is derivable across all 71 tools - every tool has a full
  description, every tool is named by at least one test source, every
  `describe_tool{tool:"X"}` / `chromia_help{topic:"X"}` cross-reference resolves, every
  parameter a tool's own example names is declared by its schema, and every "Rell pin X"
  equals `DappScaffold.RELL_SOURCE_TAG` - and found one class of defect: **seven
  descriptions state "Rell pin 0.16.7" while `chromia.yml`'s `compile.rellVersion` is
  0.16.1, and nothing tied either number to a constant.** Both are right in their own
  place (0.16.7 is the source tag the help tools quote; CLI 0.33.x's SUPPORTED_VERSIONS
  stops at 0.16.1, so a project pinned to the tag fails `chr build`), and an agent that
  copies the pin it read into a chromia.yml gets the error. The measurement is added; the
  clarifying half-sentence in those seven descriptions is not, and neither is a
  claim-by-claim audit of the other sentences. That is the next item after the template.

## The commit-reveal raffle - BUILT, and first DRAINED FROM THIS SECTION

**STATUS, 2026-09-09: `template=raffle` ships, and the row above is retired.** Read the
rest of this section anyway, because it is the only place in this file where the ADVICE
was the attack surface.

Adversary round 20 did what this section asks a template lane to do: it built the design
below, exactly as written. Eleven honest guards went green on a real chain and every one
of them was proved load-bearing. Then an attacker holding **4.76% of the stake won FIVE OF
FIVE draws, with ZERO deposits forfeited and ZERO secrets withheld** - so the deposit, the
whole economic mechanism this section rests on, never came up. The sentence that failed is
in step 3 below: *"her choice is between REVEALING and FORFEITING her deposit"*. It is
true of ONE commitment and false of two, because the mix

    round.mixed = hash(round.mixed ++ secret)

folds the secrets in **REVEAL ORDER**. Six commitments are 720 orderings of the same
accumulator, every one of them reached by revealing everything and paying nothing extra.
She read the honest secrets off the chain, ran the module's own public `pick` over each
ordering of her own, and revealed in the one whose ticket was hers.

**WHAT THE SHIPPED TEMPLATE DOES INSTEAD**, and each of these is structural rather than a
check a later operation can forget:

1. **The seed folds the COMMITTED set, in COMMIT order.** `reveal` stores the secret and
   touches no accumulator at all; the seed is computed once, in `settle_round`, folding
   over `@sort .seq` - an immutable field written by `commit` before any secret in the
   round exists. There is no ordering left to choose.
2. **A round that is not complete does not draw.** `reveals == commits` or the round
   refunds, so the 2^m withholding subsets collapse into one choice: reveal everything, or
   there is nothing to win.
3. **A forfeited deposit is BURNED**, never added to the prize - the round-20 module paid
   forfeits into the pot, which pays the survivors for the denial.
4. **The stake is capped by the deposit**, so walking away from a losing draw never costs
   less than playing it. Round 20 sized its deposit against `prize / participants`, a
   fraction of one stake, which bounded nothing.

Measured on a chain, in `exploit-corpus/realworld/adversary-round20/raffle-fixed`: all 24
reveal orders of one round settle to the same seed and the same winner; and over FIFTY
rounds the same 720-permutation search still finds a winning ordering in 50 of 50 rounds
and wins **one round in fifty - 20000 ppm against a 47619 ppm stake share**, where the
drained module won every draw.

**THE LESSON THIS FILE OWES ITSELF:** a design note in this repository is read by agents
and built by them, so it is production advice with no test behind it. This one had eleven
provable guards and one false sentence, and the false sentence was the whole of it. A
future row here should carry the *invariant* it claims - "her expected value from
deviating is negative" - as something a chain can be made to answer, not as a sentence.

The original design follows, unchanged, so the note and its counterexample sit together.

**THE DRAIN IT HAS TO MAKE UNWRITABLE, in one sentence:** the entropy was
`op_context.last_block_time`, the timestamp of the block ALREADY COMMITTED, so the
attacker predicted nothing - she watched blocks land and answered the one whose ticket was
hers, winning 5 of 5 weekly draws on a 90-of-1890 stake and turning 100 into 400 while two
honest depositors went 1500 -> 1350. Every drained draw was LEGAL: in the block she chose,
the other two were each refused "you are not this week's winner", so no rule keyed on the
operation can tell that block from any other. **That is why this is a template's job and
not a checker's.**

**THE SHAPE.**

1. **COMMIT.** An entry is `commit(h: byte_array)` where `h = hash(secret ++ signer)`.
   The row is `(round, account, commitment)` with `key (round, account)` - one commitment
   per account per round, refused by the DATABASE rather than by a check somebody has to
   remember - and `commitment` is IMMUTABLE, so nothing can be swapped after the phase
   closes. The stake or ticket price moves INTO the module's own escrow in the same
   operation, the way `marketplace`'s escrow does; nothing is paid out of a pot that was
   never funded.
2. **THE PHASE BOUNDARY IS THE BLOCK CLOCK, WRITTEN ONCE.** `round.opened_at` is written
   by the operation that opens the round and by NOTHING else - the `streaming` template's
   "NO OPERATION IN IT WRITES A TIMESTAMP" guard, which exists because round 7's grief was
   an anchor a caller could advance. `COMMIT_MS` and `REVEAL_MS` are module args with
   positive defaults. Commit is refused at or after `opened_at + COMMIT_MS`; reveal is
   refused before it and at or after `opened_at + COMMIT_MS + REVEAL_MS`. The two
   comparisons must PARTITION the timeline exactly - that is `escrow`'s deadline pair, and
   the airdrop row of this table is here because a window whose comparisons overlap or
   leave a gap is its own exploit class.
3. **REVEAL, WITH A DEPOSIT THE REVEALER FORFEITS.** `reveal(secret: byte_array)` requires
   `hash(secret ++ signer) == commitment` and folds the secret into the round's
   accumulator - `round.mixed = hash(round.mixed ++ secret)`, a monotone write, one per
   commitment, refused twice by the same `key`. **THE GUARD THAT MAKES THE DRAW
   UNPREDICTABLE IS THE FORFEIT, NOT THE HASH:** the last revealer can always see the
   accumulator and compute whether her secret wins, so her choice is between REVEALING and
   FORFEITING her deposit, and the deposit is sized against the prize (`deposit >= prize /
   participants`, a module arg with a positive default) so that withholding costs more
   than the swing it buys. A design that hashes secrets and skips the deposit has moved the
   attack from choosing a BLOCK to choosing whether to SPEAK, which is the same attack.
4. **THE DRAW USES ONLY REVEALED SECRETS, AND NO BLOCK DATA AT ALL.** `settle_round()` is
   permissionless, refuses before the reveal window closes, refuses twice (`round.settled`
   leaves false exactly once), and picks the winner from `round.mixed` against the
   REVEALED entries in proportion to stake. It reads no timestamp, no block rid and no
   transaction hash: **on this chain a block's own data is visible to whoever chooses when
   to submit, so it is not entropy**, and a template that ships that sentence in its header
   and then reads the clock in its draw is the round-7 shape (our own prose being the
   vulnerability) all over again.
5. **THE TIMEOUT THAT REFUNDS.** If nobody reveals - or fewer than `MIN_REVEALS` do - the
   round has no entropy and MUST NOT fall back to anything. `refund_round()` is
   permissionless after the reveal window, returns every commit's stake and every
   forfeitable deposit that was revealed, and keeps the deposits of those who did not.
   There is no path that pays a prize out of a round with too few reveals, and no operation
   that extends a window: an extension is an option for whoever may take it, which is the
   `escrow` template's own admission and the shape of the round-7 grief.

**WHAT IT SHIPS WITH, or it is not a template here.** The round-18 drain as a must-fail
test with a mutant that reddens it BECAUSE THE ATTACK LANDED - drained-then-refused, the
standard every other guard in this repository meets - plus: an economic invariant
(`stakes_in == prizes_out + refunds_out + forfeits_retained`, true of a redistribution the
way `insurance`'s is), the block-clock draw as a mutant (delete the accumulator, read
`op_context.last_block_time`, and the round-18 five-of-five must come back), a
last-revealer test that measures the forfeit against the swing, an EXTENDING THIS TEMPLATE
section naming the seam (a second prize, a rollover, a referral - each is a second path
out of the pot and belongs INSIDE `settle_round()`, which is `insurance`'s exit queue one
class along), the redirect moved in the same commit, and this row deleted in that commit.

**AND THE HALF NO TEMPLATE CLOSES, stated in the header rather than discovered later:** a
commit-reveal raffle with ONE participant is decided by that participant, and one with two
colluding participants is decided by whichever of them reveals last. The forfeit bounds
what that is worth; it does not remove it. A template that does not say so is the round-8
disclaimer ("an AMM's own invariant is yours to prove") that a build then proved nothing
about.

| Ask | Redirects to | What the target does NOT cover | Distinct exploit class |
|---|---|---|---|
| **a wager somebody takes the other side of** - a prediction market, a sportsbook, a casino game | `(none)` - a NAMED refusal, and since 2026-09-09 a refusal that points at `raffle` for the DRAW half and says why a book is not one | SOLVENCY FOR EVERY OUTCOME AT ONCE. A raffle pays one winner out of a pot the entrants funded, so the pot is always exactly what was staked; a book takes a position against every bettor and must be able to pay whichever side wins. `template=raffle` has no reserve, no odds and no worst case | a reserve sized against the EXPECTED outcome rather than the worst one, and a settlement source that is the house's own opinion. `insurance` is the nearest shipped shape - cover bounded by the reserve that backs it, every payout pro rata |
| a token airdrop with a claim window | `staking` | a CLAIM WINDOW. Its guards cover the mint - a reward paid out of a pool nobody funded is round 4 - but not an allocation that expires, and not where the unclaimed remainder goes | a deadline whose two comparisons do not partition the timeline, and a remainder that stays claimable for ever. `escrow`'s deadline pair is the shape to copy |
| a payment channel, a state channel | `(none)` - it reached `ft4` on the word `payment` until 2026-09-07 | a CHANNEL. A token ledger has no sequence number, no off-chain state and no dispute window | the CLOSE: a stale state posted by whoever profits from it, and a dispute window somebody has to be online to watch. `escrow` covers value locked between two named parties; nothing covers the close |
| a multisig wallet, a threshold account | `(none)` - it reached `ft4` on the word `wallet` until 2026-09-07 | a SIGNER SET. The ft4 skeleton has one auth descriptor | who may add or remove a key, whether a signature counts once, and whether the set can be closed. FT4's account model has multi-signature auth descriptors and `bridge` ships M-of-N over one ACTION; neither is a template for an account |
| a crowdfunding campaign with refunds | `(none)` - a NAMED refusal since 2026-09-08, where it used to fall to the roster | a goal, a deadline and an all-or-nothing refund | the refund race a `subscription`-style escrow does not have: many backers against one pot, so a partial refund is `insurance`'s pro-rata problem with a deadline attached |
| a loyalty programme, points | `(none)` - a NAMED refusal since 2026-09-09, where it fell to the roster before; **a gaming item shop still falls to the roster and is the row this one was split off from** | an issuer who can mint the points at will | the round-4 unbacked mint in a class where minting is the POINT, so `staking`'s "every credit is a pool debit" cannot simply be carried over |
| a fee splitter, a charity or donation pool, a yield aggregator | `(none)` - a NAMED refusal since 2026-09-09, where it fell to the roster before | a share of an incoming stream, split by weights that can move | a weight changed between the accrual and the withdrawal - the lending template's stale-share-price drain in a class with no share price |

That is not a claim that no other class is missing. Every drain in this project
landed in a class this file had either not thought of or had ranked below the one
that got built - and the bridge row is the sharpest case, because it carried a
reason for its own deprioritisation that was not true until round 14 tested the
claim and drained the build. So the next drain is still predictable from the
redirect map rather than from this table. When you find a class with no template,
put it here FIRST, before you build anything.

## How to use this

Take the top row that is not in flight. Build the template the way the other
fourteen are built - the exploit made **unwritable**, not merely detected
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

**And a redirect is not fixed until a TWO-CLASS ask is fixed.** Round 18's laundering
is the general form: an ask that names the new template's class alongside an uncovered
one must still answer the uncovered one FIRST. That is a list read before the routing
(`untemplatedClasses` in `DappScaffold.kt`), the covered roster is derived from the
routing's own key lists (`templateKeys`, one copy, no second list to keep in step), and
`Round18TemplateRedirectProbeTest` crosses every uncovered class token with every
covered one and freezes the answers. A new template adds its keys to that map and the
matrix grows in the same commit.
