<!-- This body is a reviewer agent's primary context. Fill the fields; do not narrate.
     out_of_scope and review_focus bound what the reviewer must consider, and are the
     only fields that reduce its token spend. Spec: doc:05-authoring-for-agents. -->

## change

```
type:      feat|fix|docs|chore|refactor|test
scope:     <module or path>
bean:      NNNN
atomic:    true|false
size:      files=N lines=+A/-B
```

<!-- atomic: false requires one line here saying why the parts cannot ship separately. -->

## contract

```
before:        <observable state today>
after:         <observable state after merge>
out_of_scope:  <what the reviewer MUST NOT flag: pre-existing, owned elsewhere, deferred>
```

<!-- out_of_scope MUST NOT name any file or behaviour this diff changes; it may only cite
     pre-existing state or another named bean/PR's ownership. A reviewer MUST still
     evaluate every changed line regardless of out_of_scope. -->

## verify

<!-- One block per success criterion in the bean. Observed output is verbatim, never
     paraphrased and never claimed unseen (doc:00-constitution §3). -->

```
cmd:       <exact command>
expect:    <exact expected output>
observed:  <verbatim actual output>
```

## review_focus

<!-- Specific answerable questions. A reviewer answers these and stops. Empty is invalid. -->

- <question>

## refs

```
doc:   <doc:NN-slug#anchor, ...>
bean:  <bean:NNNN, ...>
adr:   <adr:NNNN, ...>
rule:  <rule:tool/name, ...>
```

<!-- The reviewer reads these documents and no others (AGENTS.md routing, last row). -->
