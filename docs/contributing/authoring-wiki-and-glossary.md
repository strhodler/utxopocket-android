# Authoring Wiki and Glossary

This guide explains how to propose and write Markdown articles for the built‑in wiki and glossary. All content must be layer‑1 Bitcoin (no Lightning/sidechains), in English, and tailored to UtxoPocket’s watch‑only, descriptor‑driven model.

## 1) Backlog first
- Open `/docs/contributing/wiki-glossary-backlog.md` and add a row under the table with:
  - ID and type (wiki `id` or glossary `slug`).
  - A 1–3 sentence summary rewritten in your own words.
  - Priority (High/Medium/Low).
  - Suggested `related` wiki IDs and glossary slugs.
- New contributors may also open an issue first and then submit a PR updating the backlog.

## 2) Article templates
Place wiki articles in `/docs/wiki` and glossary entries in `/docs/glossary`.

### 2.1 Wiki article template
Filename: `/docs/wiki/<id>.md`

```
---
id: <kebab-id>
title: <Title Case>
summary: <Short, one line>
category_id: privacy-toolkit
category_title: Privacy toolkit
category_description: Practical guides to reduce on‑chain exposure and keep compartments isolated.
related: [other-ids]
glossary_refs: [slugs]
keywords: [k, e, y]
---

## Why it matters
Explain the problem and relevance to a watch‑only, descriptor‑driven wallet.

## Core guidance
- Bullet actionable steps.
- Keep on-chain only and avoid vendor mentions unless necessary for definitions.

## Action checklist
- [ ] Keep items concise and testable.

```

Rules:
- English only; synthesize ideas (no copy/paste).
- Use `##` headings and `-` bullets; avoid raw HTML.
- Choose 2–6 `related` IDs; ensure those wiki files exist.
- List glossary slugs you actually reference in the text via `glossary_refs`.

### 2.2 Glossary entry template
Filename: `/docs/glossary/<slug>.md`

```
---
id: <slug>
title: <Title>
summary: <One sentence definition>
related: [wiki-ids]
keywords: [k, e, y]
---

Short paragraph with a clear, tool‑agnostic definition. Reference the wiki article ID(s) where readers should go for deeper guidance.

```

## 3) Cross‑linking
- The renderer auto‑links the first occurrence per section for any slug listed in `glossary_refs`.
- `related` shows topic cards at the end of the article. Keep it relevant and minimal.

## 4) Review checklist (for PRs)
- [ ] Added/updated a backlog row with status/links.
- [ ] Frontmatter validates: IDs/slugs, `related`, `glossary_refs` present where applicable.
- [ ] Content is in English, on-chain (L1), and rewritten in your words.
- [ ] No broken references; all `related` and `glossary_refs` exist.

## 5) Editorial workflow
Docs maintainers review backlog items, create or update Markdown under `/docs/wiki` and `/docs/glossary`, and keep cross-links/glossary references consistent. Mention in your PR description which backlog rows you addressed so reviewers can mark them complete.
