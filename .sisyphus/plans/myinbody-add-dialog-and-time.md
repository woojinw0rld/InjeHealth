# Myinbody — Add Dialog + Time + Recent Comparison

## TL;DR
> 입력 폼을 BottomSheet 다이얼로그로 분리하고, 기록에 시간(HH:mm)을 추가해 같은 날 덮어쓰기 문제를 해결하고, "전일 대비"를 "최근 대비"로 변경.

---

## Context

### User Requests (verbatim)
1. "+ 버튼 누르면 추가 다이얼로그 (디자인은 가져가면서)" → 운동기록 리스트만 밑에 남게
2. "시간까지 기록하게 하고싶어. 이게 안되니깐 하루에 2번 추가하니 덮어씌워지더라"
3. "전일 대비에서 최근 대비로 바꾸고 싶어"
4. "db 바뀐거 db스키마txt에 반영해달라고도 해줘"
5. "이번 md는 간단하게끔 짜자"

### Decisions (A/A/A 확정)
- **시간 입력**: 기본 = 현재 시각 자동, "시간 변경" 버튼으로 TimePicker 선택 (AddMealSheet 패턴 그대로)
- **충돌 정책**: 같은 시각 기록도 그냥 새로 추가 (덮어쓰기 없음)
- **DB 마이그레이션**: 기존 v4 데이터 보존 (`date || ' 00:00'` → `recorded_at`)

### 참고 패턴 (이미 코드베이스에 존재)
- `AddMealSheet.java` (BottomSheetDialogFragment + TimePicker) — 그대로 본떠서 `AddBodyRecordSheet` 작성
- `bottom_sheet_add_meal.xml` — 레이아웃 구조 참고
- `MIGRATION_3_4` (`AppDatabase.java:91-118`) — 테이블 재생성 + 데이터 보존 마이그 패턴

---

## Work Objectives

### Core Objective
Myinbody의 인라인 입력 폼을 BottomSheet 다이얼로그로 분리하고, 기록 단위를 날짜 → 시각(분 단위)로 바꿔 하루 여러 번 기록 가능하게 만든다.

### Deliverables
- DB v4 → v5 마이그레이션 (`recorded_at TEXT NOT NULL`)
- `AddBodyRecordSheet.java` + `bottom_sheet_add_body_record.xml` 신규
- `MyinbodyFragment` 리팩터: 입력 폼 제거, "+" 버튼으로 Sheet 호출
- 어댑터 표시에 시간 포함
- "전일 대비" → "최근 대비" 라벨 변경
- `Docs/인제 헬스 - DB Schema v5.txt` 갱신

### Must NOT
- 기존 v1~v3 마이그레이션 수정 금지
- AddMealSheet 직접 재사용 금지 (별도 Sheet 작성, BodyRecord용)
- ViewModel/LiveData 도입 금지 (프로젝트 컨벤션)
- 차트/통계 카드 디자인 변경 금지 (라벨 텍스트만 변경)

---

## Verification Strategy
- **자동 테스트**: 없음 (프로젝트에 테스트 인프라 없음)
- **빌드 검증**: `gradlew :app:assembleDebug` BUILD SUCCESSFUL
- **수동 QA**: 빌드 후 실기기에서 시나리오 확인 (사용자 직접 또는 다음 세션)

---

## TODOs

- [ ] 1. **BodyRecord 엔티티 + DAO 시간 컬럼 추가**

  **What to do**:
  - `BodyRecord.java`: `String date` 제거, `String recorded_at` 추가 ("yyyy-MM-dd HH:mm")
  - `BodyRecordDao.java`:
    - `getByDate(String)` 제거 (충돌 정책 = 항상 추가이므로 불필요)
    - `getAll()` ORDER BY → `recorded_at DESC`
    - `@Insert`는 그대로
  - `MyinbodyFragment.addRecord()`의 upsert 분기 제거 → 단순 insert

  **Files**:
  - `app/src/main/java/com/example/injehealth/db/entity/BodyRecord.java`
  - `app/src/main/java/com/example/injehealth/db/dao/BodyRecordDao.java`

  **Acceptance**:
  - [ ] `getByDate` 호출처 0건 (`grep -r "getByDate" app/src`)
  - [ ] 빌드 성공

---

- [ ] 2. **AppDatabase v4 → v5 마이그레이션**

  **What to do**:
  - `@Database(version = 5)`
  - `MIGRATION_4_5` 추가 (테이블 재생성 + 데이터 보존):
    ```sql
    CREATE TABLE body_records_new (
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      recorded_at TEXT NOT NULL,
      weight REAL NOT NULL,
      muscle_mass REAL NOT NULL,
      body_fat_mass REAL NOT NULL,
      body_fat_rate REAL NOT NULL,
      memo TEXT
    );
    INSERT INTO body_records_new (id, recorded_at, weight, muscle_mass, body_fat_mass, body_fat_rate, memo)
      SELECT id, date || ' 00:00', weight, muscle_mass, body_fat_mass, body_fat_rate, memo
      FROM body_records;
    DROP TABLE body_records;
    ALTER TABLE body_records_new RENAME TO body_records;
    CREATE INDEX index_body_records_recorded_at ON body_records(recorded_at);
    ```
  - `addMigrations(..., MIGRATION_4_5)` 등록

  **Files**: `app/src/main/java/com/example/injehealth/db/AppDatabase.java`

  **Reference**: `MIGRATION_3_4` (line 91-118) 패턴 그대로

  **Acceptance**:
  - [ ] 빌드 성공
  - [ ] Room 컴파일 에러 없음 (스키마 일치)

---

- [ ] 3. **AddBodyRecordSheet + 레이아웃 신규 작성**

  **What to do**:
  - `bottom_sheet_add_body_record.xml`:
    - 시간 표시 + 변경 버튼 (AddMealSheet의 `tvSelectedTime` / `btnChangeTime` 그대로 모방)
    - 4개 EditText (체중/근육량/체지방량/체지방률) — `fragment_myinbody.xml:276-422` 입력 폼 디자인 그대로 이식
    - 저장/취소 버튼
  - `AddBodyRecordSheet.java extends BottomSheetDialogFragment`:
    - `selectedTime = Calendar.getInstance()` (현재 시각 default)
    - `btnChangeTime` → `TimePickerDialog`
    - 저장 시 `recorded_at = "yyyy-MM-dd HH:mm"` 조합 후 단순 `bodyRecordDao().insert()`
    - 부모 Fragment 갱신: `if (getParentFragment() instanceof MyinbodyFragment) ((MyinbodyFragment) getParentFragment()).loadRecords();`

  **Files (신규)**:
  - `app/src/main/res/layout/bottom_sheet_add_body_record.xml`
  - `app/src/main/java/com/example/injehealth/AddBodyRecordSheet.java`

  **Reference**: `AddMealSheet.java` 전체, `bottom_sheet_add_meal.xml`

  **Acceptance**:
  - [ ] 빌드 성공
  - [ ] Sheet show → 4필드 입력 → 시간 표시 → 저장 → DB insert 동작

---

- [ ] 4. **MyinbodyFragment 리팩터 + "최근 대비" 변경**

  **What to do**:
  - `fragment_myinbody.xml`:
    - "오늘 기록 입력 폼" 섹션 전체 제거 (line 229-437 근처)
    - "기록 내역" 헤더 옆에 "+" 아이콘 버튼 추가 (`@drawable/ic_add` 또는 텍스트 버튼)
    - "전일 대비" 관련 주석 정리
  - `MyinbodyFragment.java`:
    - `etWeight/etMuscle/etFatMass/etFatRate/tvTodayDate` 관련 필드 + initViews 라인 제거
    - `addRecord()` 메서드 제거
    - "+" 버튼 click → `new AddBodyRecordSheet().show(getChildFragmentManager(), "AddBodyRecord")`
    - `loadRecords()`는 public 또는 package-private (Sheet에서 호출)
    - `BodyRecord.date` 참조를 `recorded_at`로 일괄 변경 (formatShortDate, allRecords 정렬 등)
    - 통계 라벨 주석 "전일 대비" → "최근 대비" (line 45, 360, 364, 379)
  - `strings.xml`:
    - `myinbody_stat_daily_change`: "전일 대비" → "최근 대비"
    - `myinbody_input_title` / `myinbody_btn_add` / `myinbody_label_*` / `myinbody_hint_*` 등 입력폼 전용 string은 새 Sheet 레이아웃에서 재사용 (삭제 X)

  **Files**:
  - `app/src/main/res/layout/fragment_myinbody.xml`
  - `app/src/main/java/com/example/injehealth/MyinbodyFragment.java`
  - `app/src/main/res/values/strings.xml`

  **Acceptance**:
  - [ ] 빌드 성공
  - [ ] `grep "전일 대비" app/src` → 0건
  - [ ] `grep "BodyRecord.date\|record\.date" app/src` → 0건

---

- [ ] 5. **BodyRecordAdapter 시간 표시**

  **What to do**:
  - `dateFormatIn` 패턴: `"yyyy-MM-dd"` → `"yyyy-MM-dd HH:mm"`
  - `dateFormatOut` 패턴: `"yyyy년 M월 d일"` → `"yyyy년 M월 d일 HH:mm"` (또는 별도 라인 표시)
  - `record.date` → `record.recorded_at` 일괄 변경
  - `formatDate()` 호출부 그대로

  **Files**: `app/src/main/java/com/example/injehealth/adapter/BodyRecordAdapter.java`

  **Acceptance**:
  - [ ] 빌드 성공
  - [ ] 리스트 아이템에 "2026년 4월 15일 14:30" 형식 표시

---

- [ ] 6. **DB 스키마 문서 갱신**

  **What to do**:
  - 파일 rename: `Docs/인제 헬스 - DB Schema v4.txt` → `Docs/인제 헬스 - DB Schema v5.txt`
  - 내용 갱신:
    - body_records 스키마: `date TEXT` 제거, `recorded_at TEXT NOT NULL` 추가
    - DB version: 4 → 5
    - 마이그레이션 히스토리에 "4→5: BodyRecord에 recorded_at 시각 컬럼 도입 (date 단일 → 분 단위), 같은 날 다중 기록 지원" 추가
  - `app/src/main/java/com/example/injehealth/AGENTS.md` (DB 섹션):
    - DB version 4 → 5
    - body_records Key Fields에 `recorded_at` 추가
  - `app/src/main/java/com/example/injehealth/db/AGENTS.md`:
    - version 4 → 5
    - migrations 목록에 3→4, 4→5 추가
    - BodyRecord 행 갱신
  - `Docs/개발분담표.md`:
    - 6단계 Myinbody 테이블 갱신: `MyinbodyActivity` → `MyinbodyFragment` (Fragment 전환 완료 반영)
    - `AddBodyRecordSheet.java`, `bottom_sheet_add_body_record.xml` 행 추가 (✅)
    - `BodyRecordAdapter.java` 행 추가 (✅)
    - AppDatabase v4 → v5 행 갱신
    - 1단계 AppDatabase 행: "AppDatabase v4" → "AppDatabase v5 (MIGRATION_4_5 포함)"

  **Files**:
  - `Docs/인제 헬스 - DB Schema v4.txt` → `v5.txt` (rename + edit)
  - `app/src/main/java/com/example/injehealth/AGENTS.md`
  - `app/src/main/java/com/example/injehealth/db/AGENTS.md`
  - `Docs/개발분담표.md`

  **Acceptance**:
  - [ ] v5.txt 파일 존재, v4.txt 미존재
  - [ ] 두 AGENTS.md 모두 version 5 표기
  - [ ] 개발분담표.md 6단계 Myinbody 섹션이 현재 구조 반영

---

- [ ] 7. **빌드 + 매뉴얼 QA**

  **What to do**:
  - `gradlew :app:assembleDebug` BUILD SUCCESSFUL
  - 실기기/에뮬레이터:
    1. 앱 실행 → Myinbody 탭 진입 (마이그 자동 적용)
    2. "+" 버튼 → BottomSheet 표시 → 4필드 입력 → 저장 → 리스트에 즉시 표시 (시간 포함)
    3. 같은 날 두 번 추가 → 두 항목 모두 보존 ✅
    4. 시간 변경 버튼 → TimePicker 동작
    5. 통계 카드 라벨이 "최근 대비"로 표시
    6. 기존 v4 데이터 있는 경우 마이그 후 "00:00"으로 표시되는지

  **Acceptance**:
  - [ ] BUILD SUCCESSFUL
  - [ ] 위 6개 시나리오 모두 통과

---

## Commit Strategy
- Task 1+2 묶음: `refactor(db): add recorded_at to BodyRecord, migrate v4→v5`
- Task 3: `feat(myinbody): add AddBodyRecordSheet bottom sheet dialog`
- Task 4+5: `refactor(myinbody): replace inline form with sheet, show time, rename to 최근 대비`
- Task 6: `docs: update DB schema to v5`

---

## Success Criteria
- [ ] 모든 Task 완료
- [ ] BUILD SUCCESSFUL
- [ ] grep "전일 대비" → 0건
- [ ] grep "getByDate" → 0건
- [ ] DB Schema v5.txt 존재
