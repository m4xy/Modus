---
# modus-0100
title: A claim that arrives carrying someone's confidence is the one nobody checks
status: todo
type: task
priority: normal
order: CD
created_at: 2026-08-30T00:00:00Z
---

# A claim that arrives carrying someone's confidence is the one nobody checks

Six false claims survived a review round this sprint. Each was caught eventually, by somebody
opening the file and reading it. None was caught by the process meant to catch it, and every
brief in the sprint carried an instruction to verify what you are told.

They share one property, and it is not carelessness: **each claim arrived already carrying
someone's confidence**, so the reader's job appeared to be to build on it rather than to test
it. They differ in *whose* confidence, and that difference matters more than the count —
because the three mechanisms defeat "verify what you are told" in three different ways, and
therefore cannot share one remedy.

## The two properties that make this a finding rather than six anecdotes

### 1. Plausibility is inversely correlated with detectability

**A claim's survival time is a function of how good it sounds. That inverts what a reviewer's
intuition assumes.**

Nothing about the precedence gloss below looked wrong. It was well-formed, specific, in the
register of the surrounding rules, and — the decisive property — **it resolved the question it
was invoked for**. It read like a rule this repository would have, because the repository does
have rules of that shape.

An odd-sounding claim gets checked, because oddness is what prompts a reader to open the file.
A claim that sounds like the corpus is adopted, because discovering it is *not* in the corpus
costs a lookup the reader has no signal to make. So the defects that survive longest are
systematically the ones that look least like defects, and review attention — drawn to what
looks wrong — is aimed away from them.

The corollary is uncomfortable and is the point: **"it sounded right" is evidence that a claim
needs checking, not evidence that it does not.** Nobody can act on that by feeling more
suspicious; it has to become a rule about which claims get verified, by class rather than by
instinct.

### 2. Confidence detaches from its evidence and is renewed by every reader

The confidence attached to a claim propagates independently of the fact. Once attached it is
renewed by everyone who passes over it without objecting, because a claim that has survived a
review round reads as reviewed — whether or not any reviewer evaluated that particular
sentence. Ratification is inferred from proximity to things that were checked.

## The six instances, across three mechanisms

| # | claim | whose confidence | survived |
|---|---|---|---|
| 1 | a precedence clause quoted as `doc:00-constitution`'s rule, which is nowhere in that document | the orchestrator's, relayed in a brief | two agents, two beans, a PR body, a full review round |
| 2 | three divergences labelled `RESIDUAL` in `bean:0063` | the author's own label, then the reviewer's by ratification | one full review round |
| 3 | `IdGeneratorPort` must return `String` because `sharedKernelIsLeaf` would reject a typed return | the author's own, citing a real rule | one review round; disproved by a plant |
| 4 | `bean:0065`'s ready-list table, showing one substitution | the author's own, because it genuinely **was** measured | one review round |
| 5 | `bean:0096` cites four criteria that carry none of its claim | a reviewer's, **relayed as a correction** | refused and disproved by its author on receipt |
| 6 | the `$15/$75` pricing incident "was never true in committed history" | a reviewer's, **relayed as a correction** | accepted by two agents; one within an edit of committing it |

## Mechanism A — relayed between agents

**Recorded as the orchestrator's own finding, in its words.**

Both agents that adopted the precedence gloss had the file open. Both were briefed to verify
what they were told. Neither checked it.

The mechanism is not carelessness. **An orchestrator's assertions arrive already-decided.**
They are framed as context — the settled ground an agent builds on — rather than as claims the
agent is expected to test, and the brief format gives no place to mark which is which. The
orchestrator's account: *"I stated it as a clause when it was a paraphrase. Every brief I have
written this sprint mixes verified fact with my own inference in the same voice."*

That single voice is the defect. An agent receiving a brief cannot distinguish a fact the
orchestrator verified, a fact it read in an agent's report and did not reproduce
(`doc:80-agent-operating-procedure#reports-are-evidence` warns about exactly this direction),
and the orchestrator's own inference — because all three are written in the same register by
the party that decides what the agent does next. **The mechanism gets worse as the orchestrator
gets better**, because trusting a reliable source is the efficient choice, and it is invisible
from inside the loop.

### A relayed **correction** is the worst case of this mechanism

**A correction arrives with an implicit *someone already checked*, so it carries more authority
than the claim it corrects while being no better verified.** Two instances this sprint, both
relayed by the orchestrator, both wrong, and in both the receiving agent had the file open.

**Instance A1 — the criterion columns.** A reviewer reported that `bean:0096` cited four
criteria carrying none of the claim it made, and that the claim held exactly once elsewhere.
The orchestrator relayed it and instructed the author to replace the four citations with the
one. The author refused, reproduced it, and was right: the reviewer had read the `expectation`
column where the counters sit in `observed` — **one column to the right, in a five-column
table**. All four citations were correct and the true count was five cells across two beans.
`bean:0096` records it: *"A five-column evidence table is off by one between the two, and the
check reads the last of them."* The correction would have destroyed a correct paragraph and
replaced five true instances with one.

**Instance A2 — "it never happened".** A reviewer concluded the pricing incident was fabricated,
from a `git log -S` search empty across every ref. Relayed to two agents as established; **both
accepted it**, and one came within a single edit of committing it into two artefacts. The
incident is real, at `bean:0002`. A later reviewer then found the **search itself** defective:
`git log -S` is **last-wins, not AND**, so a command passing two `-S` strings silently searches
only the last. Reproduced here rather than taken on report —
`git log --all -S "outputPerMTok" -S "zzzznevermatches"` returns 0 while the same two strings
in the opposite order return 4.

**Why a correction is more dangerous than the claim it corrects, rather than merely equally
unverified:** in both instances the correction *had* been checked, carefully, by a competent
reviewer. The implicit reassurance was **earned** and still wrong. So the recipient's confidence
is not misplaced trust in a careless source; it is well-placed trust in a careful one, which no
amount of vigilance about sources would have corrected.

**And the grammar of a correction redirects the scrutiny away from itself.** An ordinary claim
invites *is this true?* A correction invites *what did I get wrong?* — a question about the
recipient's work rather than about the correction's. The recipient goes looking in the right
place for a defect and the wrong place for **this** defect. Instance A1 was caught only because
its author declined the instruction and re-derived the thing they had already derived.

### The control: the same mechanism, caught

While this bean was being written the orchestrator relayed a second claim for it — that a
documented pricing incident "was never true in committed history", supported by
`git log --all -S` returning nothing across every ref. It was **false**: the incident is real
and recorded at `.beans/modus-0002--backoffice-foundation.md`, including the ratio test that
detects it. Verified before writing, so it never entered the bean.

Two things make the near-miss worth keeping. It is the only instance in this sprint where the
remedy — verify before adopting — was actually applied to a relayed claim and worked, so it is
evidence the failure is not inevitable. And the claim would have been **a false statement about
an unverified claim, inside a bean about unverified claims**: the bean would have become its
own counter-example, which is the failure shape this sprint keeps producing.

## Mechanism B — a self-applied label, then ratified by review

`bean:0063` shipped three divergences labelled `RESIDUAL`. That label's entire content is the
claim *this divergence is acceptable*. All three passed a review round with the label attached.
In the next round, when a verdict assertion was finally demanded of them, **two changed the
outcome and one was a regression against the very code being replaced**. Nobody had examined
the label; what had been examined was the code beside it.

**A label borrows authority exactly as a relayed claim does, and once it survives a review
round it borrows the reviewer's authority too** — the label is then not merely asserted but
apparently ratified, though no reviewer ever evaluated it.

**The asymmetry is why this is its own mechanism rather than a special case of the relay.** A
relayed claim can be checked by whoever receives it, *because they know it was relayed* — the
brief has a sender. A self-applied label has no sender. In the artefact, a labelled residual
and a genuine one are identical, so **no reader can know there is anything to look at.** The
relay case has a tell; this one does not.

`bean:0063`'s own remedy is the model, because it is the one that worked: **require the label
to carry the assertion that justifies it.** A residual must ship with a verdict assertion
showing the outcome does not change; when it changes the outcome it was never a residual but a
defect. Then the label cannot be applied without producing its own evidence, and *"I called it
a residual"* stops being available as a substitute for having checked.

## Mechanism C — a claim that shows its work

Instances 3 and 4 were both produced by an author who did real work and stated it.

Instance 3 cited `rule:archunit/sharedKernelIsLeaf` as the reason a port could not return a
typed identifier. The rule exists, the citation resolved, and the reasoning was internally
sound. It was still wrong: that rule is scoped by an exact name set and cannot see the package
in question at all. Instance 4 published a ranked backlog table produced by genuinely replaying
the selection algorithm — **on the wrong tree**, where one bean's status differed, so the table
was accurate about something nobody had asked about.

**Showing work reads as having done the work, and a reader who can see a derivation stops
looking for the error in it.** A bare assertion invites the question *how do you know*; a
derivation answers that question before it is asked, and so suppresses it. Both instances were
caught only when someone re-ran the derivation rather than reading it — which is precisely the
labour the visible derivation appears to have made unnecessary.

This is distinct from B in what it hides. A label hides that there is a claim; a derivation
advertises the claim and hides that its **inputs** were wrong. And it is distinct from A in
having no external source to distrust: the author is the source, and the author checked.

## Why one remedy cannot cover three mechanisms

This bean's first draft assessed four candidate fixes as though this were one problem. That was
wrong, and the reason is precise: **all three mechanisms defeat "instruct people to verify",
but each defeats it differently.**

| mechanism | what verification would require | why the instruction fails |
|---|---|---|
| A — relayed | the receiver **distrusts a source** it depends on and is efficient to trust | the claim is framed as context rather than as a claim, and doubting the orchestrator is costly and usually wrong |
| B — self-applied label | a reader **notices there is anything to check** | the artefact gives no signal; a labelled residual and a real one are identical |
| C — shows its work | a reader **re-runs a derivation they can already see** | the visible derivation is what makes re-running it feel redundant |

So a remedy that helps one may do nothing for the others. Candidates, per mechanism, none
adopted here:

| for | candidate | assessment |
|---|---|---|
| A | mark each brief assertion `verified` or `asserted` | Most promising, on evidence rather than theory: the same distinction applied inside a bean already changed what an author wrote, when `bean:0065` was made to split three propositions into settled / observed / conjecture and doing so exposed a claim hedged while being relied on as settled thirty lines earlier. Unproven at brief scale; its cost lands on the orchestrator's context, which `doc:00-constitution#orchestrator` names as the scarcest resource in the system. |
| B | require every label to carry the assertion that justifies it | **Demonstrated**, in `bean:0063`, on the instance that motivated it. Generalising means deciding which labels are claims — `RESIDUAL`, `known limitation`, `out of scope`, `won't fix` all assert acceptability — and that enumeration is the work. |
| C | **state the inputs a derivation ran against, beside its result** | The only remedy in this bean a reader could adopt this afternoon, so it is stated as a rule rather than as a direction. **Any published derivation — a ranked list, a count, a diff stat, a measured table — names the tree, ref or command it was produced from, in the same block as the result.** Instance 4 published `| 2 | modus-0031 (AT) | modus-0066 (AQ) |` and nothing else; the adopted form is one line above it: `computed by replaying AGENTS.md step 1 over origin/main and over 1a2c138`. That line is where the error was — the derivation was run against the stack, where one bean is `in-progress`, rather than against the branch, where it is `todo`. It attacks the mechanism directly: a reader cannot re-run a derivation whose inputs are unstated, but can very often see that the stated inputs are the wrong ones — and seeing that costs a glance where re-running costs a session. Untested as a rule; the single case it is drawn from would have been caught by it. |
| all | instruct agents to verify what they are told | **Necessary and demonstrably not sufficient — that is the whole of its assessment.** It was in every brief this sprint and caught none of the six. An instruction to verify everything is an instruction to verify nothing in particular, and it competes directly with `doc:00-constitution#context-budget`, which tells the same agent not to re-read what it has been told. Keep it; do not count it as a defence. |
| all | a mechanical check | No proposal. Every instance was prose, and four of the six were paraphrases, corrections or derivations drawn from real sources. A check resolving quoted sentences against source files would have to distinguish a quotation from a gloss, which is the judgement in question rather than a way around it. |

## A related finding this bean does not carry

The near-miss above was reached through a search that returned nothing: `git log --all -S`
across every ref, run independently by three parties, all of whom concluded the incident never
happened. **None searched `.beans/`,** where this repository deliberately keeps its review
evidence and where `adr:0005-evidence-lives-in-the-work-item` says evidence lives.

That is a finding about **where evidence is searched**, not about whose confidence a claim
carries, and its remedy is concrete where this bean's are not — the incident was fixed
pre-merge, and a pre-merge fix is invisible to `git log -S` **by construction**, which
squash-merge makes the normal case rather than an edge one. The whole class of defects caught
in review is unfindable in committed history. It is recorded separately rather than as a fourth
mechanism here, because folding it in would weaken the shared property that makes the three
above one finding.

## Scope

Owned: this bean.

Not owned: `doc:80-agent-operating-procedure#orchestrating`, where an orchestration rule
belongs — whichever remedy is chosen for mechanism A lands there. `bean:0063`'s residual rule,
already stated in `tools/docs-lint-test.sh`'s header and needing only generalisation if that is
chosen. `bean:0068`'s retraction of the precedence gloss, which is done. The search-scope
finding above.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | The three mechanisms are stated as **distinct**, with the reason one remedy cannot cover them, wherever an orchestrator writing a brief will read it — not only in this bean | |
| 2 | A remedy is chosen per mechanism with the reason, or the mechanism is explicitly deferred. A remedy for A silently assumed to cover B and C repeats this bean's own first draft | |
| 3 | "Instruct agents to verify" is retained and explicitly **not counted as a defence**, with the observation that it was present throughout the sprint that produced all six instances | |
| 4 | If the `verified`/`asserted` marking is adopted, its cost to the orchestrator's context is stated, that being the resource `doc:00-constitution#orchestrator` names as scarcest | |
| 5 | Nothing in the fix claims to detect a plausible false claim automatically. Property 1 says the defect is invisible by construction, so a remedy promising detection contradicts the finding it implements | |
| 6 | If no remedy is adopted for a mechanism, the refusal is recorded with its reason. A finding closed by an explicit "won't do" is closed; silence does not close it | |
| 7 | `./gradlew qualityCheck` green | |

## Sequencing

Nothing blocks this and it blocks nothing. `priority: normal`, ordered behind the flat-file
store: it is a methodology finding, and displacing implementation work other beans depend on
would repeat a mistake this sprint already made and corrected (`bean:0065`, where two fix beans
that unblocked nothing were found demoting `bean:0017`).

It should be picked up before the next multi-agent sprint. All three mechanisms concern how
work is **briefed, labelled and derived** rather than how it is done, and a sprint run without
them repeats all six instances at no lower cost than the first time.
