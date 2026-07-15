# 19. Community & Sharing — P2

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 19: Community & Sharing (P2)". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** LATER (post-MVP) · **Primary modules:** (deferred) likely :presentation, :domain, :data + a `:community` feature module

## Purpose
Post-MVP social layer: 1-to-1 verified-teacher sessions, group Halaqa voice rooms, and social sharing of progress. **Not in MVP scope — brief only.**

## Must-know constraints
- **LATER / Phase 2.** Do not implement now. When built, follow the per-feature layered split (`:community:domain|data|presentation`) per AGENTS.md — never fold into `:app`.
- Live audio (Huffaz calls, Halaqa rooms) needs real-time infra (WebRTC/SFU) not yet in the mandated stack — requires a **constitution amendment** before adoption.
- Role-based permissions (moderator / reader / listener) and scheduling/rating systems are backend-heavy; depends on the unified user-data service (ARC-01) and cloud sync ([[17-offline-sync]]).
- All social/community UI must ship ar + en, RTL-aware, from day one; respect privacy (PRV-01..04) and store compliance (SEC-02) for user-generated content and audio.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| P2-HUF | Huffaz (1-to-1 Teacher) | LATER | Browse verified teachers, view profiles, book live evaluation sessions; live audio + scheduling + ratings. |
| P2-HAL | Halaqa (Group Sessions) | LATER | Group voice channels with role-based permissions (moderator/reader/listener). |
| P2-SHR | Social Sharing | LATER | Share progress, exam grades, annotated verses to social networks via native APIs. |

## Design system (`:designsystem`)
- Deferred. When designed, reuse existing primitives where possible (`SectionedCard`, `SettingsActionCard`, `PrimaryButton`/`SecondaryButton`, `TextField`, `StatusOverlay`, `NetworkErrorScreen`, `EmptyDataScreen`) and read all tokens via `Theme.*`.
- New surfaces likely needed — **needs new :designsystem components:** teacher profile card, live-call controls bar, participant/role list row, in-call audio indicators. Specify against the catalog at build time, not now.
- P2-SHR share entry points should route through the native share sheet already covered in [[18-platform-integrations]] (INT-04) rather than a bespoke UI.

## Data, stack & offline
- Deferred. Real-time transport (WebRTC/SFU + signaling) TBD and gated on a constitution amendment; Ktor/REST for teacher directory, scheduling, and ratings; Room for local caches of profiles/bookings.
- Community features are inherently online — degrade gracefully offline (AVL-02) and gate cleanly.

## Applicable NFRs
- NFR-LOC LOC-01/LOC-02 (ar+en, RTL). NFR-PRV PRV-01..04 (UGC + audio privacy). NFR-SEC SEC-01/SEC-02 (secure sessions, store compliance). NFR-ARC ARC-01 (unified user-data service).

## Related features
- [[18-platform-integrations]] — native share sheet for P2-SHR.
- [[17-offline-sync]] — cloud sync / user-data service dependency.
- [[13-progress-gamification]] / exam results — shared progress and grades.
