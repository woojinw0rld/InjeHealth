package com.example.injehealth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.adapter.RoutineAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.Routine;
import com.example.injehealth.db.entity.WorkoutLog;
import com.example.injehealth.db.entity.WorkoutSession;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 루틴 선택 및 운동 시작 화면
 * - 저장된 루틴 목록 표시
 * - 루틴 선택 후 운동 시작
 * - 운동 시작 전 이전 기록 기반으로 루틴 목표값 업데이트
 */
public class RoutineCheckActivity extends AppCompatActivity {

    // ─────────────────────────────────────────
    // 상수 및 멤버 변수
    // ─────────────────────────────────────────
    public static final String ROUTINE_NAME     = "ROUTINE_NAME";
    public static final String EXTRA_SESSION_ID = "session_id";

    private List<Routine> routineList;                      // 전체 루틴 목록
    private String myRoutine = "empty";                     // 선택된 루틴 이름
    private RoutineAdapter adapter;                         // 루틴 목록 어댑터
    private List<String> routineNames = new ArrayList<>();  // 루틴 이름 목록 (미사용 가능)

    // ─────────────────────────────────────────
    // onCreate: 화면 초기화
    // ─────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_list);

        // 상단 오늘 요일 뱃지 설정
        setTopTitle();

        // 뒤로가기 버튼
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 루틴 목록 RecyclerView 설정
        RecyclerView rv = findViewById(R.id.rv_routines);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoutineAdapter(routineName -> {
            // 루틴 선택 시 선택된 루틴 이름 저장
            myRoutine = routineName;
            Log.d("MYLOGCHECK", "myRoutine: " + myRoutine);
        });
        rv.setAdapter(adapter);

        // DB에서 저장된 루틴 불러오기
        loadSavedRoutine();

        // 운동 시작 버튼
        Button btnStart = findViewById(R.id.btn_start_workout);
        btnStart.setOnClickListener(v -> startWorkout());
    }

    // ─────────────────────────────────────────
    // setTopTitle: 상단 오늘 요일 뱃지 표시
    // ─────────────────────────────────────────
    private void setTopTitle() {
        TextView tvBadge = findViewById(R.id.tv_days_badge);
        String[] days = {"일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"};
        String today = days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1];
        tvBadge.setText(today);
    }

    // ─────────────────────────────────────────
    // loadSavedRoutine: DB에서 루틴 목록 불러와 어댑터에 전달
    // 운동 이름을 루틴 이름으로 그룹핑해서 표시
    // ─────────────────────────────────────────
    private void loadSavedRoutine() {
        Executors.newSingleThreadExecutor().execute(() -> {
            routineList = AppDatabase.getInstance(this).routineDao().getAll();

            // 루틴 이름 기준으로 운동 목록 그룹핑
            // 예: {"가슴 루틴": ["벤치프레스", "덤벨플라이"], "등 루틴": ["랫풀다운"]}
            LinkedHashMap<String, List<String>> grouped = new LinkedHashMap<>();
            for (Routine r : routineList) {
                grouped.computeIfAbsent(r.routine_name, k -> new ArrayList<>()).add(r.exercise_name);
            }

            runOnUiThread(() -> adapter.setData(grouped));
        });
    }

    // ─────────────────────────────────────────
    // startWorkout: 루틴 선택 확인 후 운동 시작
    // 이전 기록 기반으로 루틴 목표값 업데이트 후 WorkoutCheckActivity로 이동
    // ─────────────────────────────────────────
    private void startWorkout() {
        // 루틴 미선택 시 토스트 표시 후 리턴
        if (myRoutine.equals("empty")) {
            Toast.makeText(this, "루틴을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            // 이전 기록 기반으로 루틴 목표값 업데이트 (동기 실행)
            syncRoutineWithLastSession();

            runOnUiThread(() -> {
                // 루틴 업데이트 완료 후 운동 화면으로 이동
                Intent intent = new Intent(this, WorkoutCheckActivity.class);
                intent.putExtra(ROUTINE_NAME, myRoutine);
                startActivity(intent);
                finish();
            });
        });
    }

    // ─────────────────────────────────────────
    // syncRoutineWithLastSession: 이전 운동 기록 기반으로 루틴 목표값 업데이트
    // ─────────────────────────────────────────
    private void syncRoutineWithLastSession() {
        AppDatabase db = AppDatabase.getInstance(this);


        // 해당 루틴의 가장 최근 세션 조회
        WorkoutSession lastSession = db.workoutSessionDao().getByRoutineName(myRoutine);
        Log.d("MYLOGCHECK", "lastSession: " + (lastSession != null ? lastSession.id : "null")); // ← 여기
        // 최근 세션 있으면 로그 가져오고, 없으면 빈 리스트
        List<WorkoutLog> workoutLogs = lastSession != null ?
                db.workoutLogDao().getBySessionId(lastSession.id) : new ArrayList<>();
        Log.d("MYLOGCHECK", "workoutLogs size: " + workoutLogs.size()); // ← 여기

        for (Routine r : routineList) {
            int maxSetNumber = 0; // 완료된 세트 중 최대 번호
            Log.d("MYLOGCHECK", "루틴: " + r.routine_name+"in the sync");
            Log.d("MYLOGCHECK", "myRoutine: '" + myRoutine + "'");
            for (WorkoutLog log : workoutLogs) {
                Log.d("MYLOGCHECK", "로그확인: " + log.exercise_name+"in the sync");

                // 같은 운동이고 완료된 세트만 처리
                if (r.exercise_name.equals(log.exercise_name) && log.is_done == 1) {
                    // 완료된 세트 중 가장 높은 번호 → 실제 완료 세트 수
                    maxSetNumber     = Math.max(maxSetNumber, log.set_number);
                    Log.d("MYLOGCHECK", "set_number: " + log.set_number + "exerciseName: " + log.exercise_name);
                    // 이전 기록 중 더 높은 무게/횟수로 업데이트
                    r.default_weight = Math.max((int) log.weight, r.default_weight);
                    r.default_reps   = Math.max(log.reps, r.default_reps);
                }
            }

            // 이전 완료 기록 있으면 세트 수 업데이트, 없으면 기존 루틴 값 유지
            if (maxSetNumber > 0) {
                r.default_sets = maxSetNumber;
            }

            // 루틴 목표값 DB에 저장
            db.routineDao().update(r);
        }
    }
}