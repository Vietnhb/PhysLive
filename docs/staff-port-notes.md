# Staff port through branch 04

## Source and scope

Source: the local `physLive_preview` working tree, based on `cbf2856`, including its uncommitted physics and planning updates. Preview is read-only. Existing commits are not rewritten.

New commit messages and adapter comments use short English. Source comments are preserved when copying preview code.

## Branch stack

1. `feat/viet-01-backend-compile`: existing compile repair.
2. `feat/viet-02-staff-permissions`: audited grants, teaching boundary, account payloads, web guards.
3. `feat/viet-03-problem-understanding`: provider settings, transport, request contracts and prompts.
4. `feat/viet-04-simulation-runtime`: physics data, runtime, schema adapter, planning APIs and tests.

Review each branch against the previous branch. The fourth branch contains all four deliveries. Main is unchanged.

## Compatibility boundaries

- Existing Teacher and Admin routes keep their current behavior. Staff receives the legacy teacher authority only with a valid `TEACH` grant. A department-head grant alone cannot use the new teaching APIs.
- Permission entities follow preview, with package imports adapted to this repository. Migration V6 adds the Staff role and permission tables; it does not convert accounts or implement School's grant-management UI.
- Approved schema access uses the current repository and lifecycle strings. Disabled schemas and topics are excluded.
- The preview planning/runtime code is retained. Storage methods and endpoints are deferred to branch 05; no fake storage implementation is supplied.
- `LawGapSink` is an optional interface. The reviewer queue implementation is not installed yet.
- The new TypeScript API client is ready for the next workspace port. Existing screens still call legacy endpoints.

## Enablement

The module is off by default. Set `physlive.staff.preview-enabled=true` only in a configured environment.

Required environment settings are listed in `backend/src/main/resources/staff-preview.properties`: AI text/vision and visual provider settings, Jev credentials, and the existing JWT signing key. No credentials are committed.

Available endpoints when enabled:

- `POST /api/simulation/understand`: text or multipart image; image recognition requires confirmation before planning.
- `POST /api/simulation/revise`: revise the previous intent.
- `POST /api/simulation/generate`: generate from a signed plan.
- `POST /api/simulation/compute`: recompute permitted parameter changes.
- `POST /api/simulation/render-diagnostics`: record signed-plan render diagnostics.

All endpoints check teaching permission. Normal JWT account and license checks remain active.

## Database prerequisites

Schema resources are not automatically published to the database. `SchemaCatalogCompiler.loadFromClasspath` compiles the 21 preview topic manifests; a reviewed catalog rollout must store the compiled definitions and their law catalogues, link enabled topics, and approve the versions.

The current database model still has a global unique constraint on schema version. Preview needs uniqueness by schema ID and version. That change must be coordinated with the legacy `findByVersion` consumers and the reviewer module before bulk catalog installation. Do not import all manifests blindly into the old schema.

Migrations V5/V6 assume the existing database tables. They have not been applied to a shared database or verified on PostgreSQL staging.

## Verification

- Backend tests use isolated H2 databases; no shared database is modified.
- Provider transport tests mock HTTP, including malformed responses and provider errors.
- Physics regression uses preview law cases, rejected cases and recorded planning fixtures.
- Enabled-module tests load the actual runtime and compiler, seed one approved topic, and mock only the external AI client and current user. They cover teaching denial, OCR confirmation, unsigned/tampered plans and understanding → generation → recomputation.
- React TypeScript and production build pass; Vite reports existing large-bundle warnings.
- Live provider calls, PostgreSQL migration and browser end-to-end acceptance are not claimed.

## Next work

Complete the shared role/account migration with Hùng, coordinate catalog installation with Kiệt, then connect the preview workspace and saved-run flow in branch 05. Do not mark the full Staff flow accepted based only on these backend tests.
