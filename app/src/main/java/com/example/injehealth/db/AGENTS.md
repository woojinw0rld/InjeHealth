# DB LAYER (Room)

## OVERVIEW

Room persistence layer — 6 entities, 6 DAOs, 1 singleton database. All DB access goes through `AppDatabase.getInstance(ctx)`.

## SCHEMA

| Table | Entity | FK | Key Queries |
|-------|--------|----|-------------|
| `users` | User | — | `getUser()` (single row, LIMIT 1) |
| `exercises` | Exercise | — | `getByBodyPart()`, `getCustom()` (is_custom=1) |
| `routines` | Routine | exercise_id → exercises.id (CASCADE) | `getByBodyPart()`, `deleteByBodyPart()` |
| `workout_sessions` | WorkoutSession | — | `getByDate()`, `getById()`, `getAll() ORDER BY date DESC` |
| `workout_logs` | WorkoutLog | session_id → workout_sessions.id (CASCADE) | `getBySession() ORDER BY exercise_name, set_number` |
| `body_records` | BodyRecord | — | `getByDate()`, `getAll() ORDER BY date DESC` |

## CONVENTIONS

- **Fields**: `snake_case` everywhere (`body_part`, `set_number`, `is_done`).
- **Booleans**: Stored as `int` — 0=false, 1=true (`is_custom`, `is_done`).
- **Dates**: `String` type with `"yyyy-MM-dd"` format. `created_at`/`done_at` use ISO 8601.
- **PKs**: All `int id` with `autoGenerate = true`.
- **Return types**: All DAOs return `List<T>` or single entity. No `LiveData`/`Flow` — synchronous access.
- **Insert return**: Only `WorkoutSessionDao.insert()` returns `long` (session ID). Others return `void`.
- **FK strategy**: `onDelete = CASCADE` on all foreign keys. Indexed FK columns.

## ANTI-PATTERNS

- `AppDatabase` is missing `@Database(entities={...}, version=N)` annotation — **must be added**.
- No `@Database` export schema (`exportSchema` not set) — migrations will be harder to test.
- Synchronous DAO returns (`List<T>`) — will crash if called on main thread without `allowMainThreadQueries()`. Use `Executors` or switch to `LiveData`/`Flow`.
- `User` table assumes single user (`LIMIT 1`) — no multi-user support.
- `Routine.exercise_name` duplicates `Exercise.name` (denormalized) — can go stale if exercise renamed.

## SRS vs CODE GAPS

| Item | SRS v1.2 | Code | Action |
|------|----------|------|--------|
| `body_records` fields | `weight`, `memo` only | + `muscle_mass`, `body_fat_mass`, `body_fat_rate` | Code is ahead of SRS — update SRS or keep extended |
| `body_records` in `DB_ver0.1_health.txt` | `weight`, `memo` only | Same gap as above | Initial DB design didn't include body composition |
| `exercises.image_type` values | "ASSET / LOCAL" | "drawable" / "file" | Naming mismatch — align one way |
| `minSdk` | API 26 (Android 8.0) | 24 | Code more permissive than spec |

## WHERE TO LOOK

| Task | File(s) |
|------|---------|
| Add new table | Create `entity/NewEntity.java` + `dao/NewEntityDao.java`, add to `AppDatabase` abstract methods + `@Database(entities=...)` |
| Add query | Modify relevant `*Dao.java`, add `@Query` method |
| Seed data | Implement `AppDatabase.prepopulateCallback.onCreate()` body |
| Add migration | `AppDatabase.getInstance()` → `.addMigrations(MIGRATION_X_Y)` before `.build()` |
