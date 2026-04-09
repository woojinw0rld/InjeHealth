# PROJECT KNOWLEDGE BASE

**Generated:** 2026-04-07
**Commit:** 88c6459
**Branch:** feature/history

## OVERVIEW

Android health/fitness tracking app (InjeHealth). Java + Room (SQLite) + Gradle Kotlin DSL. Single `app` module — tracks workouts, routines, body composition, and exercises.

## STRUCTURE

```
InjeHealth/                     # Git repo root (NOT the parent Inje_health/)
├── app/
│   ├── build.gradle.kts        # Module build — deps, SDK levels, Room/Glide/MPChart
│   └── src/main/
│       ├── AndroidManifest.xml  # Single activity (MainActivity), no permissions declared
│       ├── java/.../injehealth/
│       │   ├── MainActivity.java   # Launcher — EdgeToEdge, empty shell (Hello World)
│       │   └── db/                 # See db/AGENTS.md
│       │       ├── AppDatabase.java
│       │       ├── entity/         # 6 Room @Entity classes
│       │       └── dao/            # 6 Room @Dao interfaces
│       └── res/
│           ├── layout/activity_main.xml  # ConstraintLayout, single TextView
│           └── values/, xml/, drawable/, mipmap-*/
├── build.gradle.kts            # Root — plugin alias only
├── settings.gradle.kts         # FAIL_ON_PROJECT_REPOS, jitpack.io for MPAndroidChart
├── gradle/libs.versions.toml   # Version catalog (AGP 9.1.0, appcompat 1.7.1, etc.)
└── 문서/                        # Sibling dir (outside git) — SRS docs
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Add new screen/Activity | `app/src/main/java/.../injehealth/` | Create Activity + layout XML, register in AndroidManifest |
| Add/modify DB table | `db/entity/` + `db/dao/` + `AppDatabase.java` | Bump DB version, add migration |
| Change dependencies | `app/build.gradle.kts` | Room/Glide/MPChart are hardcoded (not in version catalog) |
| Change SDK/Java version | `app/build.gradle.kts` | Java 11, compileSdk 36, minSdk 24 |
| Add repository | `settings.gradle.kts` | jitpack.io already present for MPAndroidChart |
| Layouts/UI resources | `app/src/main/res/` | Standard Android resource dirs |

## CONVENTIONS

- **Language**: Java only (no Kotlin app code). Build scripts are Kotlin DSL (.kts).
- **Build**: Gradle Kotlin DSL with version catalog (`libs.versions.toml`). Some deps (Room, RecyclerView, MPAndroidChart, Glide) are hardcoded in `app/build.gradle.kts` instead of catalog.
- **Package**: `com.example.injehealth` — sample namespace, not production-ready.
- **DB**: Room with singleton pattern (`AppDatabase.getInstance()`). DB name: `inje_health.db`.
- **Fields**: `snake_case` for entity fields (`body_part`, `set_number`, `is_done`). Boolean-as-int pattern (`is_custom`, `is_done`: 0/1).
- **Date strings**: Stored as `String` with format `"yyyy-MM-dd"` or ISO 8601. No typed date columns.
- **FK cascades**: `onDelete = CASCADE` on Routine->Exercise and WorkoutLog->WorkoutSession.
- **No architecture layers**: No ViewModel, Repository, or Service classes. Direct DAO access expected.
- **Centralized repos**: `FAIL_ON_PROJECT_REPOS` — all repos in `settings.gradle.kts`, not modules.

## ANTI-PATTERNS (THIS PROJECT)

- `AppDatabase` missing `@Database` annotation in source — **must add** `@Database(entities={...}, version=N)` for Room to compile.
- No Room migration strategy — adding/changing entities without migration will crash on DB upgrade.
- `prepopulateCallback.onCreate()` is empty stub — seed data not implemented.
- `local.properties` is committed (`.gitignore` has it but path mismatch may cause issues).
- `.idea/` partially tracked in git — leaks IDE settings.
- Release builds have `isMinifyEnabled = false` — no ProGuard/R8 shrinking.

## COMMANDS

```bash
# Run from InjeHealth/ (git repo root, NOT parent Inje_health/)
.\gradlew.bat assembleDebug       # Build debug APK
.\gradlew.bat test                # Unit tests (JVM)
.\gradlew.bat connectedAndroidTest # Instrumented tests (needs device/emulator)
.\gradlew.bat lint                # Android lint
.\gradlew.bat clean               # Clean build artifacts
```

## PRODUCT REQUIREMENTS (from 문서/)

Source documents (sibling `문서/` dir, outside git):
- `인제헬스_SRS_v1.2.docx` — Software Requirements Specification
- `인제헬스_기획서_v1.1.docx` — Service Plan (기획서)
- `인제헬스_기획발표_v3_디자인 수정.pptx` — Presentation deck
- `DB_ver0.1_health.txt` — Initial DB schema (DBML format)

### Target Users
헬스장을 규칙적으로 다니는 20~30대. 오프라인 전용, 광고 없는 깔끔한 UI.

### Planned Screens (13 total, SRS v1.2)

| Screen | Status | Description |
|--------|--------|-------------|
| LoginActivity | **NOT IMPL** | 최초 실행 — 몸무게/키/성별/나이 입력 |
| HomeActivity (toDay) | **NOT IMPL** | 운동 부위 선택 + 최근 기록 표시 |
| RoutineSetupActivity | **NOT IMPL** | 최초 부위 루틴 설정 |
| RoutineListActivity | **NOT IMPL** | 부위별 루틴 리스트 |
| ExerciseCatalogActivity | **NOT IMPL** | 종목 카탈로그 (부위별 카테고리) |
| WorkoutCheckActivity | **NOT IMPL** | 운동 체크리스트 (set/rep/무게, 타이머) |
| WorkoutDoneActivity | **NOT IMPL** | 운동 종료 + 눈바디 사진 등록 |
| HistoryListActivity | **NOT IMPL** | 과거 운동기록 리스트 + 눈바디 |
| HistoryDetailActivity | **NOT IMPL** | 특정 날짜 상세 (계획 vs 실제) |
| MyinbodyActivity | **NOT IMPL** | 신체 변동 기록 + 그래프 (MPAndroidChart) |
| SettingsActivity | **NOT IMPL** | 설정 화면 (v1.2 추가) |
| MyPageActivity | **NOT IMPL** | 개인정보 조회/수정 (v1.2 추가) |
| MenuActivity / DrawerMenu | **NOT IMPL** | 전체 메뉴 (설정, 탈퇴) |

> **현재 구현 상태**: MainActivity만 존재 (Hello World). 위 13개 화면 모두 미구현.

### Functional Requirements (FR-01 ~ FR-41)

| Group | IDs | Summary |
|-------|-----|---------|
| 초기 설정 | FR-01~03 | 최초 실행 시 로그인, 몸무게/키/성별/나이 입력 → Room 저장 |
| 부위 선택 / 루틴 | FR-04~08 | 부위 선택(복수 가능), 루틴 설정/수정, 기존 루틴 리스트 표시 |
| 운동 체크리스트 | FR-09~14 | 세트/횟수/무게 입력, 시작/종료 시간, 쉬는 시간 타이머, 세트 추가/삭제 |
| 눈바디 사진 | FR-15~18 | 운동 종료 후 카메라/갤러리 사진 등록 (선택 사항), 로컬 저장 |
| 기록 조회 | FR-19~22 | 최근 기록 홈 표시, 과거 리스트, 눈바디 확인, 계획 vs 실제 상세 |
| Myinbody | FR-23~26 | 날짜별 신체 데이터 입력, Room 저장, 그래프 시각화, 수정/삭제 |
| 메뉴 | FR-27~31 | toDay, 옛날운동기록, Myinbody, 설정, 회원탈퇴(데이터 삭제+초기화) |
| 종목 카탈로그 | FR-32~38 | 기본 종목 프리로드, 부위별 카테고리, 커스텀 종목 추가(갤러리 사진) |
| 마이페이지 | FR-39~41 | 개인정보 조회/수정 → Room users 테이블 업데이트 |

### Non-Functional Requirements
- 화면 전환 / DB 조회 1초 이내
- 모든 데이터 로컬 저장 (인터넷 불필요)
- Android 8.0 (API 26) 이상
- Material Design 가이드라인 준수

### Design Direction (기획서 v1.1)
- **Theme**: 다크 계열 UI, Material Design 3
- **Colors**: Primary `#1E88E5` (블루), Background `#121212`, Surface `#1E1E1E`, Accent `#FF7043` (오렌지)
- **Font**: Noto Sans KR (한글) + Roboto (영문/숫자)
- **UX**: 한 손 조작, 터치 타겟 ≥48dp, 주요 버튼 하단 배치, 다크 모드 기본

### Screen Flow
```
[최초 실행] LoginActivity → HomeActivity
[운동 세션] Home → RoutineList → (최초)RoutineSetup → ExerciseCatalog
           → WorkoutCheck → WorkoutDone → Home
[기록 조회] DrawerMenu → HistoryList → HistoryDetail
[Myinbody]  DrawerMenu → MyinbodyActivity
[설정]      DrawerMenu → Settings → MyPage
[탈퇴]      DrawerMenu → 데이터 삭제 + 초기화
```

### DB Schema Gap (SRS vs Code)
- **SRS `body_records`**: `weight`, `memo` only → **Code has extra fields**: `muscle_mass`, `body_fat_mass`, `body_fat_rate` (코드가 SRS보다 확장됨)
- **SRS `minSdk`**: API 26 → **Code**: minSdk 24 (불일치)
- **SRS `image_type`**: "ASSET / LOCAL" → **Code**: "drawable" / "file" (네이밍 차이)

### Development Assignment (개발 분담표)

| Stage | Milestone | Owner | Deliverables | Status |
|-------|-----------|-------|-------------|--------|
| 1 | 프로젝트 세팅 | (공통) | build.gradle, Entity 6개, DAO 6개, AppDatabase | ✅ 완료 |
| 2 | LoginActivity | **woojin** | LoginActivity, activity_login.xml, Manifest 등록 | ⬜ |
| 3 | HomeActivity + DrawerMenu | **woojin** | HomeActivity, activity_home.xml, nav_drawer.xml | ⬜ |
| 4 | 운동 플로우 | **woojin** | RoutineSetup/RoutineList/WorkoutCheck/WorkoutDone + adapters | ⬜ |
| 5 | 운동 기록 조회 | **hajun** | HistoryListActivity, HistoryDetailActivity, HistoryAdapter | ⬜ |
| 6 | Myinbody | **hajun** | MyinbodyActivity (입력 + MPAndroidChart 그래프) | ⬜ |
| 7 | 종목 카탈로그 | **hajun** | ExerciseCatalogActivity, ExerciseAdapter, prepopulate 콜백 | ⬜ |
| 8 | 마이페이지 / 설정 | **hajun** | MyPageActivity, SettingsActivity (조회/수정/탈퇴) | ⬜ |

> **이 프로젝트에서 AI 어시스턴트는 hajun 파트(5~8단계)만 담당합니다.**

### hajun 담당 상세 (Stage 5~8)

**Stage 5 — 운동 기록 조회**
- `HistoryListActivity.java` — 날짜별 운동 기록 리스트 (FR-19~21)
- `activity_history_list.xml` — 기록 목록 UI
- `adapter/HistoryAdapter.java` — RecyclerView 어댑터
- `HistoryDetailActivity.java` — 특정 날짜 상세: 계획 vs 실제 비교 (FR-22)
- `activity_history_detail.xml` — 상세 UI

**Stage 6 — Myinbody**
- `MyinbodyActivity.java` — 체중/근육량/체지방량/체지방률 입력 + 그래프 (FR-23~26)
- `activity_myinbody.xml` — 입력 폼 + MPAndroidChart 차트 UI

**Stage 7 — 종목 카탈로그**
- `ExerciseCatalogActivity.java` — 기본/커스텀 종목 관리 (FR-32~38)
- `activity_exercise_catalog.xml` — 카탈로그 UI
- `adapter/ExerciseAdapter.java` — RecyclerView 어댑터
- `db/AppDatabase.java` 수정 — 기본 종목 prepopulate 콜백 구현

**Stage 8 — 마이페이지 / 설정**
- `MyPageActivity.java` — 개인정보 조회/수정 (FR-39~41)
- `activity_my_page.xml` — 개인정보 UI
- `SettingsActivity.java` — 앱 설정 + 계정 탈퇴 (FR-30~31)
- `activity_settings.xml` — 설정 UI

### AndroidManifest 권한 (분담표 기준)
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

## NOTES

- **Git root is `InjeHealth/`**, not the parent `Inje_health/` folder. Run git commands from here.
- Only 1 Activity declared (MainActivity). **13 planned screens are all unimplemented.**
- Tests are Android Studio boilerplate only (`ExampleUnitTest`, `ExampleInstrumentedTest`).
- No CI/CD pipeline exists. No Dockerfile.
- MPAndroidChart pulled from jitpack.io — requires the maven repo in `settings.gradle.kts`.
- No mocking library in test deps (no Mockito/MockK).
- **Team**: woojin (Stage 2~4), hajun (Stage 5~8). Stage 1 완료.
