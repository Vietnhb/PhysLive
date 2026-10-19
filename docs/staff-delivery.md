# Staff delivery — 19 October 2026

## Scope and source

The current local `physLive_preview` source is the read-only reference. Its committed tree and uncommitted Staff changes were compared before and after the port. Research prompts, provider transcripts, internal documents, generated caches and visual training experiments are not shipped as application source.

The unrelated `fix/ui-ux-logic-for-manager-and-admin` history contained the current application architecture. It was merged with the former main history. Superseded controllers, entities, frontend routes and tests were removed so Spring and React use one implementation. The newer account appeal/manager additions are retained; appeal persistence was moved into a service to satisfy the existing architecture tests.

## Delivered flows

- **V01–V05:** Staff workspace, text/image input, OCR confirmation, clarification/revision, signed planning, generation, recomputation, validation, responsive player and save/reopen. The old opt-in preview module and duplicate API tree are gone.
- **V06:** personal folders, move/rename/delete, cloning, school/public publication, discussions and reactions. Clones own their submission/specification/run chain and cannot mutate the source simulation.
- **V07:** assigned classes/students, prediction and completion, grading, return/reopen, activity logs and reports. Regression tests reject another teacher's grading/return and preserve attempt state when an assignment is closed.
- **V08:** JSON and CSV export of the displayed solved run, standalone HTML replay of the original scene and captured parameters/samples, and browser print/save-PDF. Offline replay is network-blocked and sandboxed; it does not calculate new physics or call a provider.
- **V09:** feedback/complaints, processing status and realtime integration from the shared modules. AI usage is accounted against the school's quota; repeated computations use bounded cache entries invalidated by approved-schema changes.
- **V10–V13:** department workspace, teacher/student assignment, transfer, school-wide assignment view and internal library moderation. Existing tests cover teaching-only, department-only and combined permissions and prevent cross-school access. Only School can create classes.
- **V14–V16:** one Flutter Android application using the responsive web workflows, with image capture/selection, navigation, network retry and export sharing. Debug and signed release APK builds succeed. Physical-device acceptance remains outstanding.

## Branches and commits

The branch stack retains each review boundary: integration → 05 workspace/storage → 06 library → 07 assignment acceptance → 08 contract acceptance → 09 export → 10 Android. Functionality already supplied by the merged branch is reused rather than implemented twice. New commit messages and added comments are short English.

## Verification

- Maven: 349 tests, zero failures/errors, four skipped. The skips are two PostgreSQL persistence cases requiring Docker and two live-provider cases requiring explicit credentials/configuration.
- Web: architecture check, ESLint, all 54 Node contract/unit tests and production build. Capability formula round trips run against the shipped catalog by default.
- Chromium: mobile Staff clarification, department-head denial of teaching access, and downloaded HTML replay with the browser offline. Browser API responses are mocked for UI acceptance; the actual offline renderer executes in Chromium.
- Flutter: analyzer clean, three policy tests pass, debug APK and release APK build. `apksigner verify` confirms the release APK's upload signature.
- No live-provider, PostgreSQL production-migration or physical-phone acceptance is claimed by these checks. CI now runs backend/web checks, Chromium and Android debug build.

## Deployment prerequisites

Use `backend/.env.example` for environment names and supply real provider/database configuration outside Git. The web uses `/api`; the supplied Nginx configuration now forwards it to the backend, including long-running AI/SSE requests. Compose contains only the actual PostgreSQL/backend/web services; stale microservice and Redis entries were removed.

The merged branch has a different Flyway/schema lineage from the former main. Do not run it against an existing shared database without backing up and comparing `flyway_schema_history` and schema state on staging. Never use `flyway repair`, `clean` or Hibernate `create` to silence a checksum/schema mismatch. The documented empty-database bootstrap applies only to a verified empty database. No shared database has been changed during this work.

Keep the local Android upload key and `key.properties` private and backed up. Release artifacts are local build output, not source commits.
