package com.example.injehealth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.adapter.WorkoutLogAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.WorkoutLog;
import com.example.injehealth.db.entity.WorkoutSession;
import com.example.injehealth.util.SystemBarHelper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class WorkoutCheckActivity extends AppCompatActivity {

    private int sessionId;
    private WorkoutLogAdapter adapter;
    private List<String> exerciseNames = new ArrayList<>();
    private Map<String, List<WorkoutLog>> groupedLogs = new LinkedHashMap<>();
    private TextView tvSetsProgress;
    private int totalSets = 0;
    private int completedSets = 0;
    private String bodyPart;
    private String startAy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_check);
        SystemBarHelper.applyPadding(this, R.id.main);

        bodyPart = getIntent().getStringExtra(RoutineSetupActivity.ROUTINE_NAME);


        //일단 이거 안씀.
        sessionId = getIntent().getIntExtra(RoutineSetupActivity.EXTRA_SESSION_ID, -1);

        tvSetsProgress = findViewById(R.id.tv_sets_progress);
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvBadge = findViewById(R.id.tv_body_part_badge);

        findViewById(R.id.btn_back).setOnClickListener(v -> showExitConfirmDialog());

        RecyclerView rv = findViewById(R.id.rv_exercises);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WorkoutLogAdapter(exerciseNames, groupedLogs, sessionId,new WorkoutLogAdapter.OnSetCheckedListener() {
            @Override
            public void onSetChecked(WorkoutLog log) {
                completedSets++;
                updateProgress();
                Executors.newSingleThreadExecutor().execute(() ->
                        AppDatabase.getInstance(WorkoutCheckActivity.this).workoutLogDao().update(log));
            }
            @Override
            public void onSetAdded(WorkoutLog newLog) {
                totalSets++;
                updateProgress();
                Executors.newSingleThreadExecutor().execute(() -> {
                    long id = AppDatabase.getInstance(WorkoutCheckActivity.this).workoutLogDao().insert(newLog);
                    newLog.id = (int) id;   // DB에서 발급된 id를 다시 채워줌 (나중에 update 시 필요)
                });
            }
            @Override
            public void onSetDeleted(WorkoutLog log) {
                totalSets--;
                updateProgress();
                if (log.id > 0) {
                    Executors.newSingleThreadExecutor().execute(() ->
                            AppDatabase.getInstance(WorkoutCheckActivity.this).workoutLogDao().deleteById(log.id));
                }
            }
            @Override public int getTotalSets()     { return totalSets; }
            @Override public int getCompletedSets() { return completedSets; }

        });
        rv.setAdapter(adapter);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmDialog();
            }
        });

        Button btnFinish = findViewById(R.id.btn_finish_workout);
        btnFinish.setOnClickListener(v -> showFinishConfirmDialog());

        loadWorkoutData(tvTitle, tvBadge);
    }

    private void loadWorkoutData(TextView tvTitle, TextView tvBadge) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            WorkoutSession session = db.workoutSessionDao().getById(sessionId);
            List<WorkoutLog> logs  = db.workoutLogDao().getBySession(sessionId);

            String bodyPart = session != null ? session.routine_name : "";
            Map<String, List<WorkoutLog>> grouped = new LinkedHashMap<>();
            for (WorkoutLog log : logs) {
                grouped.computeIfAbsent(log.exercise_name, k -> new ArrayList<>()).add(log);
            }

            runOnUiThread(() -> {
                tvTitle.setText(bodyPart + " 운동");
                tvBadge.setText(bodyPart);
                totalSets = logs.size();
                completedSets = 0;

                exerciseNames.clear();
                exerciseNames.addAll(grouped.keySet());
                groupedLogs.clear();
                groupedLogs.putAll(grouped);
                adapter.notifyDataSetChanged();
                updateProgress();
            });
        });
    }

    private void updateProgress() {
        tvSetsProgress.setText(completedSets + " / " + totalSets + " 세트 완료");
    }

    private void showFinishConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("운동 종료")
                .setMessage("운동을 종료하시겠습니까?")
                .setPositiveButton("종료", (d, w) -> finishWorkout())
                .setNegativeButton("취소", null)
                .show();
    }

    private void showExitConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("운동 중단")
                .setMessage("운동을 중단하시겠습니까? 기록은 저장되지 않습니다.")
                .setPositiveButton("중단", (d, w) -> {
                    deleteSessionAndLogs();
                    finish();
                })
                .setNegativeButton("계속", null)
                .show();
    }

    private void finishWorkout() {
        adapter.cancelAllTimers();
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            WorkoutSession session = db.workoutSessionDao().getById(sessionId);
            if (session != null) {
                session.done_at = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                db.workoutSessionDao().update(session);
            }
            runOnUiThread(() -> {
                Intent intent = new Intent(this, WorkoutDoneActivity.class);
                intent.putExtra(RoutineSetupActivity.EXTRA_SESSION_ID, sessionId);
                startActivity(intent);
                finish();
            });
        });
    }

    private void deleteSessionAndLogs() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.workoutLogDao().deleteBySession(sessionId);
            db.workoutSessionDao().deleteById(sessionId);
        });
    }

}
