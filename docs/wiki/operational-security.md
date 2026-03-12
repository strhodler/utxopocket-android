---
id: operational-security
title: Operational security for watch-only workflows
summary: Threat-driven practices for device safety, transport privacy, and incident response without introducing key material.
category_id: safety-operations
category_title: Safety and operations
category_description: Operational practices for resilient, private wallet use.
related: [watch-only-threat-model, why-tor, backup-recovery, operational-hygiene]
glossary_refs: [watch-only, tor, electrum-server]
keywords: [opsec, threat model, incident response]
---

## Start from threats
Define what you defend against: device loss, coercion, network observers, or malicious infrastructure. Controls should map directly to those risks.

## Key controls
- Keep signing and monitoring separated.
- Keep Tor mode as the default transport posture for wallet traffic.
- Use strict fail-closed behavior for critical dependencies.
- Prepare panic and recovery procedures in advance.

## Incident mindset
When anomalies appear, freeze risky actions, collect evidence, and verify from an independent path before resuming normal operations.

## Action checklist
- [ ] Keep an incident runbook with clear stop conditions.
- [ ] Rehearse device-loss and restore scenarios.
- [ ] Verify no telemetry tooling is introduced in your stack.
