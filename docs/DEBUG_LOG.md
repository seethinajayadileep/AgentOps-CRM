# Debug Log

| Bug ID | Date | Error | Root Cause | Files Involved | Fix Applied | Why This Fix | Test Result | Status |
|--------|------|-------|------------|----------------|-------------|--------------|-------------|--------|
| B-001 | 2026-07-01 | `'dependencies.dependency.version' for org.flywaydb:flyway-database-postgresql:jar is missing` | Maven requires explicit version for dependency not managed by parent POM | backend/pom.xml | Added version 10.20.1 to flyway-database-postgresql | Simplest fix maintaining version consistency | Build passed | ✅ FIXED |
| B-003 | 2026-07-01 | `cannot find symbol: method setStatus()` and `TypeTag :: UNKNOWN` | Lombok annotation processing not working with Java 21 on this system | backend/pom.xml, backend/src/.../dto/*.java | Removed Lombok dependency, wrote plain Java getters/setters | Lombok requires specific configuration; plain Java is guaranteed to work | ✅ Backend compiles and runs | ✅ FIXED |
| B-004 | 2026-07-01 | `FATAL: role "postgres" does not exist` | Flyway auto-configures before PostgreSQL is fully ready | backend/src/main/resources/application*.properties | Set `spring.flyway.enabled=false` for Phase 0 | Flyway will be re-enabled in Phase 1 with proper database initialization | ✅ Health endpoint works | ✅ WORKAROUND |
| B-005 | 2026-07-03 | `new row for relation "agent_logs" violates check constraint "agent_logs_status_check"` when Evaluation Agent writes status=FALLBACK_USED | With `flyway.enabled=false` + `hibernate.ddl-auto=update`, Hibernate generated the CHECK constraint for the `status` enum when the table was first created and never alters it as new enum values are added; `FALLBACK_USED` was therefore rejected on existing DBs (same class as B-? / V6 voice_calls fix) | backend/src/main/resources/migration/V8__fix_agent_logs_status_constraint.sql; DB constraint `agent_logs_status_check` | Dropped and recreated the constraint to include all AgentActionStatus values (SUCCESS, PARTIAL, ERROR, FAILED, FALLBACK_USED); added V8 migration and applied the same SQL directly to the running Postgres | Mirrors the existing V6 voice_calls fix; keeps DB in sync with the enum while ddl-auto can't. The service already fail-safes (save wrapped in try/catch) so the API never broke, but the FALLBACK_ANSWER_USED/FALLBACK_USED audit rows were being lost | ✅ Re-ran all 4 evaluation tests + chat integration; AgentLog now shows FALLBACK_ANSWER_USED=5 (FALLBACK_USED) with zero constraint errors | ✅ FIXED |

## Bug Severity Levels
- **CRITICAL**: Application cannot start or core functionality broken
- **HIGH**: Major feature broken but app starts
- **MEDIUM**: Minor feature broken or workaround available
- **LOW**: Cosmetic issues or improvements

## Bug Status
- **OPEN**: Bug discovered, not yet fixed
- **IN PROGRESS**: Currently being worked on
- **FIXED**: Bug is resolved and tested
- **WORKAROUND**: Temporary fix, needs proper solution
- **BLOCKED**: Waiting on dependency or external factor

## Template for New Bugs

| Bug ID | Date | Error | Root Cause | Files Involved | Fix Applied | Why This Fix | Test Result | Status |
|--------|------|-------|------------|----------------|-------------|--------------|-------------|--------|
| B-XXX | YYYY-MM-DD | [Short error message] | [Root cause description] | [file1, file2, ...] | [Fix description] | [Why this fix was chosen] | [Test result] | [Status] |