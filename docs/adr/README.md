# Architecture Decision Records

This folder collects Architecture Decision Records (ADRs) — short documents that
capture significant technical decisions made in this project, the context in
which they were made, and their consequences.

## Format

We use a lightweight variant of [Michael Nygard's ADR template][nygard]:

```markdown
# ADR-NNNN: Short title

- **Status:** Proposed | Accepted | Superseded by ADR-XXXX | Deprecated
- **Date:** YYYY-MM-DD

## Context

What is the problem, and what forces are at play?

## Decision

What did we decide, and what are we explicitly *not* doing?

## Consequences

What becomes easier? What becomes harder? What follow-ups does this create?
```

## Conventions

- Filename: `NNNN-kebab-case-title.md` (zero-padded, sequential).
- One decision per file. If a later ADR changes course, add a new ADR that
  references and supersedes the older one — do not edit history.
- Keep ADRs short. If you need more than two screens, link out instead of
  inlining.
- ADRs are written in [GitHub Flavored Markdown][gfm], consistent with the rest
  of the repository documentation.

## Index

| ID | Title | Status |
|----|-------|--------|
| [ADR-0001](0001-meilisearch-client.md) | Do not use the `meilisearch-java` SDK | Accepted |

[nygard]: https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions
[gfm]: https://github.github.com/gfm/