# Contributing to UtxoPocket

Thanks for helping keep this privacy-first watch-only wallet moving. This document summarizes the workflow so newcomers can plug in quickly.

## 1. Before You Start
- **Discuss first**: open a GitHub issue (English only) or comment on an existing one before writing code. Use the issue to capture acceptance criteria, UX copy, and any design references.
- **Stay watch-only**: never introduce private key handling. All descriptors must remain public (receive + change branches).
- **Tooling**: install JDK 21, Android Studio (SDK 36 + NDK), and clone the repo following `docs/project-setup.md`.

## 2. Branch & Commit Style
- Branch from `main` using `feature/<descriptor>` (e.g., `feature/tor-bootstrap-toast`).
- Keep history linear (rebase before pushing) and delete remote branches after merge.
- Commits follow **Conventional Commits** `type(scope): description`, e.g., `feat(wallets): add testnet selector`.
- When possible, squash to a descriptive commit before merge so documentation reviewers can reuse the summary.

## 3. Coding Standards
- MVVM + Clean Architecture. New features belong under `app/src/main/java/com/strhodler/utxopocket` within the existing module structure.
- Kotlin style: JetBrains defaults (see IDE tips in `docs/project-setup.md`).
- UI strings live in `app/src/main/res/values/strings.xml` (English source). Avoid inlined text.
- Security-sensitive paths (Tor, PIN, SQLCipher) must keep parity with the guarantees documented in `README.md`. If behavior changes, update the docs in the same PR.

## 4. Documentation & Wiki
- Every user-facing change must update the relevant docs in the same PR (README, `docs/` guides, wiki Markdown, release notes).
- Mention any new onboarding copy, glossary entries, or wiki topics in the PR template so the docs team can audit terminology quickly.

### 4.1 Contributing Wiki/Glossary Content
You can contribute articles to our built-in wiki (`/docs/wiki`) and glossary (`/docs/glossary`).

Quick start:
- Add or update backlog items in `/docs/contributing/wiki-glossary-backlog.md` (English only). Each item should include a short summary, priority, and suggested `related`/`glossary` refs.
- Author the Markdown using the templates in `/docs/contributing/authoring-wiki-and-glossary.md`.
- Follow the frontmatter schema exactly; the app’s loader parses these fields.
- Cross-link topics via `related` and list glossary slugs in `glossary_refs`.
- In your PR, mention the new files and paste screenshots if rendering is relevant.

Files to read before submitting:
- `docs/contributing/authoring-wiki-and-glossary.md`
- `docs/contributing/wiki-glossary-backlog.md`

### 4.2 Translations
- English (`values/strings.xml`) is the canonical source. Update English first, then mirror the entries in the localized files (`values-es/strings.xml`, etc.).
- Android Studio’s *Open Translation Editor* (right-click the `values` folder ⇒ **Open Translation Editor**) is the fastest way to add or review localized strings. It keeps keys aligned, shows missing translations, and prevents formatting mistakes.
- When adding a string:
  1. Add it in English with a descriptive name.
  2. Open the Translation Editor, filter for “Untranslated”, and fill in the other locales. Preserve placeholders (`%1$s`, `%d`), HTML markup, and newline escapes.
  3. For strings that intentionally remain English (brand names, technical terms), mark them with `translatable="false"` so Android Studio stops flagging them.
- Keep character length and tone consistent with the source; prefer concise, neutral language. If nuance is unclear, leave a comment in the PR so the docs team can review.

## 5. Pull Request Checklist
1. Reference the issue ID and summarize the change in English.
2. Paste the output of the required Gradle commands.
3. Note manual test steps (Tor bootstrap, panic wipe, descriptor import, etc.) so testers can reproduce them.
4. Highlight any docs updated (`README.md`, `docs/...`, strings) and attach screenshots for UI tweaks.
5. Confirm no private keys or telemetry were introduced.

## 7. Reporting Bugs & Security Concerns
- File bugs via GitHub issues with repro steps, logs, and environment details.
- Critical security findings (Tor offline, panic wipe failure, coverage < 80%) should also be tagged `security-alert`.
- For sensitive disclosures, reach out privately before filing a public issue.

By following this workflow we keep development, QA, release, and documentation efforts aligned, ship reproducible builds, and maintain the open-source trust model outlined in the README. Happy contributing!
