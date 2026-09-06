# Running an adversary round

The standing method. Eight rounds have used it; it is written down because it
kept living in one session's head, and a loop that dies with its context is not
a loop.

## The test

> An agent builds a dapp using **only** this server's guidance, templates and
> gates. An independent hostile auditor then attacks it. Whatever they can
> drain is our failure, not the agent's.

**A round passes only when the auditor reports they could drain nothing.**
Not "fewer than last round". Coverage moving is not a pass.

## Two rules that decide most arguments

- **A drain in a class we have no template for is our failure too.** Eight
  un-templated classes have been attacked and eight drained. "We never claimed
  to cover streaming" is not a defence: the agent asked this server how to
  build and this server answered. `docs/TEMPLATE-GAPS.md` is the ledger of
  which asks currently get answered by a template that does not cover them.
- **The prose is in scope.** Rounds 7, 8 and 9 were each topped by a defect in
  our own text, not by a missing guard: a prescribed holding period that WAS
  the vulnerability; a claim that a monotone paused-milliseconds counter "can
  never rewrite the past" when a monotone subtrahend is a monotone clawback;
  and a seam citing a victim's 144 bps execution loss as though it were the
  pool's 79 bps reserve movement, which pointed at a safe band width in a seam
  whose rule is that no width is safe. **A header sentence that is wrong is a
  defect of the same kind as a missing guard**, and the residual list - where
  an auditor places the most trust - is the worst place to be wrong.

## Shape of a round

1. **Build.** Several dapps, using only the MCP. Include at least one
   **templated** class and one **un-templated** one - the templated builds are
   where a regression would hide, and the shipped lending template has itself
   been drained twice (`r7-lending-bad-debt-exit-race`,
   `r8-lending-pool-cap-collateral-lever`), so "it has a template" is not cover.
   Also build at least one dapp by following a template's own EXTENDING seams
   literally: round 7 and round 8 both drained a build whose author did exactly
   what the header said.
2. **Attack.** Measure. A drain is numbers - what went in, what came out, whose
   balance moved - not an argument that something looks unsafe.
   **A tool is attacked the round it ships.** `verify_guards` tells an agent
   whether a guard is load-bearing; an agent will trust that verdict the way
   it trusts `ok:true`. So a FALSE verdict - `load_bearing` for a guard that
   is not, or `vacuous` for one that is - is a finding of the same rank as a
   drain, and the round's builds should try to produce one: a guard string
   that also appears in the test module, an `attackLanded` fragment that
   matches an unrelated error, a test that fails for a second reason once the
   guard is gone, module_args that make every mutant fail alike.
   **And a tool is attacked the round AFTER its fix**, at the input the fix
   did not touch: round 11 hardened the search and the error, round 12 found
   the substitution; round 12 hardened the substitution, round 13 found that
   the error rule was a list of message sources (the guard's, then the
   replacement's) with a next item (any OTHER production line, or
   `module_args`). The rule is now structural - a refused transaction is
   never the attack landing - so round 14 attacks the structure: an error
   where the attack DID land but the chain's half still contains a production
   literal or an `Operation '...' failed` from a LATER operation in the test;
   a production literal short enough (under four characters) to slip the
   filter, or one that also names the test's assertion; a test whose only
   evidence of the attack is a query result rather than a transaction.
   Round 14 found three of those (a later operation's refusal, a test-side
   `require()` in production words, a nested `but was <...>`), all in the
   CONSERVATIVE direction - a load-bearing guard reported `still_refused`,
   whose "fix" is to weaken the test until the tool agrees. The rule now
   compares the runner's failing FRAME with the declaration the guard lives
   in and consults no string literal at all. So round 15 attacks the frame:
   a guard in a `function` called by several operations (any refusal counts
   there - is that exploitable?); an operation name that is a suffix of
   another (`take` / `retake`); a refusal from the guard's own operation in
   a LATER transaction of the test after the attack landed in an earlier one;
   a query-side guard; a test that never runs the attacked operation at all.
   Round 15 found four of those, one of them `ok:true`, and the fix stopped
   patching the comparison: the guard's own declarations are RESOLVED THROUGH
   CALLERS (a guard in a `function` belongs to the operations and queries that
   reach it, transitively), frames are compared WITH THEIR MODULE, a refusal by
   the guard's own declaration counts only from the FIRST statement of the test
   that invokes it, and what the tool cannot tell apart is a new
   `ambiguous_refusal` with ok:false. So round 16 attacks those three: the
   CALLER RESOLUTION (a helper called through an interface or an `@extend`, a
   same-named helper in two modules, a caller the bare-name graph misses); the
   STATEMENT-ORDER rule (an attack and a later refusal inside ONE statement, a
   test whose attack is in a loop or a helper, a multi-line statement whose
   span swallows the later one); and the QUERY FRAME (a query refusal whose
   frame names a file the submission spells differently, a refusal from inside
   a library, an error with two frames of which the first is the innermost).
   And `ambiguous_refusal` is itself new surface: a verdict an agent can make
   the tool return on purpose is a verdict an agent can learn to ignore.
3. **Pin.** Every exploit becomes a row in
   `app/src/test/resources/exploit-corpus/` with a verdict (`MUST_FLAG` /
   `MUST_STAY_CLEAN`) and a status (`CAUGHT` / `GAP` / `CLEAN` /
   `FALSE_POSITIVE`). Coverage is then measured on every build instead of
   claimed in a report.
   **A tool-verdict finding is pinned as a test, not a corpus row.** The
   scoreboard's rows are "what `analyze` should say about a sample"; a wrong
   answer from `verify_guards` has no row shape there, which round 11 found
   when it had six of them and nowhere to put them. Its probes live under
   `realworld/adversary-round11/vg/` with their raw verdicts, and
   `VerifyGuardsProbeTest` runs the same probes pinned to the TRUE verdict, so
   a regression in the tool goes red the way a regression in a rule does. A
   new verification tool ships with its own `*ProbeTest`, and the round that
   attacks it adds to that class.
4. **Answer.** Choose deliberately, and say which you chose:
   - a **rule**, only if it can be keyed on type and use rather than names, and
     survives you attacking it yourself;
   - a **template** that makes the class unwritable, when no precise rule
     exists (principle 4) - the answer for AMM, since nothing in the source
     distinguishes a sandwich from two honest trades;
   - a **documented GAP**, when a rule would fire on correct code. A gate
     agents route around is worse than no gate (principle 3). Say so in the
     row; do not ship a noisy rule to make a number look better.
5. **Prove.** Every guard needs a mutant that turns a shipped must-fail test
   red **because the attack landed** - the error text must be the attack
   succeeding (`run_must_fail: Transaction did not fail`), never a chain that
   failed to start or an unrelated `require()`.

## Verifying the round's own report

Do not merge on a lane's word - re-run `scripts/loop-gate.mjs` yourself in its
worktree, with `--expect-min` set to the **previously verified** count and never
an estimate. A lane has reported a build as "still running" when it had failed
seven minutes earlier, on a branch with five red tests. Recompute any number a
report gives you that an argument rests on; that is how the 144 bps error was
caught, and it had already passed a lane's own green gate.
