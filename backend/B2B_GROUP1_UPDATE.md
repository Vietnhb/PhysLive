# B2B group 1 backend update

- `GET /api/student/classes`: active classes in the student's current school, teachers and classmate count.
- `GET /api/assignments/mine/teacher/classes`: assigned active classes and active students in the teacher's current school. ADMIN is permitted but this is a personal listing, not a list of all schools.
- `POST /api/assignments`: retains the existing `specificationId` contract and now requires `classId` and at least one student. Every recipient must be enrolled in that class. The teacher must belong to its school and be assigned to it; ADMIN bypasses teacher assignment only, not class membership.
- Assignment responses include `classId`, `className`, `classGradeLevel`. Class association is fixed on creation. Updating recipients and submitting also validate membership in that class.
- Legacy assignments keep a nullable class link and their previous authorization behavior; no class is guessed or backfilled.
- Revenue totals use an aggregate database query, excluding unpaid amounts from gross paid revenue. Payment activation and IPN contracts are unchanged.

## Database

`V4__link_assignments_to_school_class.sql` adds a nullable foreign key and index after the existing V3 school schema. It has not been executed on PostgreSQL. This branch has no Flyway dependency; prepare/review the database migration separately before deploying. V2/V3 remain local as requested. Check database state/backups following the previously reported test incident before performing any external migration.

No frontend, onboarding approval/email, grading, submission-completion workflow or new physics solver was ported in this update. Existing clients creating assignments must supply `classId`; this backend-only update does not adapt their forms.
