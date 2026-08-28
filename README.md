# Modus

A harness for agentic development.

Modus treats *the way you work* as a first-class, versioned artifact. Every domain
and every actor gets to define their own rules, their own modules, and their own
definition of done — and Modus enforces them mechanically.

- **Domain-scoped everything.** The root of the API is `/domains/{domainId}`.
- **Flat-file, durable.** Markdown and append-only logs are the source of truth.
- **AI-optimal, human-sanitised.** The data model is shaped for agents; the
  backoffice renders it for people.
- **Evidence-backed memory.** Durable memories at domain, epic and story level
  that may not be asserted without evidence.
- **Cost-conscious.** Every stage of every workflow carries an LLM spend figure.
- **Mechanically enforced style.** Linters, Detekt, ArchUnit — not review comments.

Status: bootstrapping. See `documentation/` for methodology and `beans/` for work items.
