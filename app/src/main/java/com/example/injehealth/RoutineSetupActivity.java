package com.example.injehealth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.adapter.RoutineAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.Exercise;
import com.example.injehealth.db.entity.Routine;
import com.example.injehealth.db.entity.WorkoutLog;
import com.example.injehealth.db.entity.WorkoutSession;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class RoutineSetupActivity extends AppCompatActivity {

    public static final String EXTRA_BODY_PART = "body_part";
    public static final String EXTRA_SESSION_ID = "session_id";

    private String bodyPart;
    private RoutineAdapter adapter;
    private final List<String> exerciseNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_setup);

        bodyPart = getIntent().getStringExtra(EXTRA_BODY_PART);
        if (bodyPart == null) bodyPart = "전체";

        TextView tvTitle = findViewById(R.id.tv_body_part_title);
        TextView tvBadge = findViewById(R.id.tv_body_part_badge);
        tvTitle.setText(bodyPart + " 운동 설정");
        tvBadge.setText(bodyPart);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_routines);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoutineAdapter(exerciseNames, position -> {
            exerciseNames.remove(position);
            adapter.notifyItemRemoved(position);
        });
        rv.setAdapter(adapter);

        loadSavedRoutine();

        findViewById(R.id.btn_add_exercise).setOnClickListener(v -> showExerciseSheet());

        Button btnStart = findViewById(R.id.btn_start_workout);
        btnStart.setOnClickListener(v -> startWorkout());
    }

    private void loadSavedRoutine() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Routine> saved = AppDatabase.getInstance(this).routineDao().getByBodyPart(bodyPart);
            runOnUiThread(() -> {
                for (Routine r : saved) {
                    if (!exerciseNames.contains(r.exercise_name)) {
                        exerciseNames.add(r.exercise_name);
                    }
                }
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void showExerciseSheet() {
        ExerciseSelectBottomSheet sheet = ExerciseSelectBottomSheet.newInstance(bodyPart);
        sheet.setOnExerciseSelectedListener(name -> {
            if (!exerciseNames.contains(name)) {
                exerciseNames.add(name);
                adapter.notifyItemInserted(exerciseNames.size() - 1);
            } else {
                Toast.makeText(this, "이미 추가된 종목입니다", Toast.LENGTH_SHORT).show();
            }
        });
        sheet.show(getSupportFragmentManager(), "exercise_select");
    }

    private void startWorkout() {
        if (exerciseNames.isEmpty()) {
            Toast.makeText(this, "종목을 추가해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            WorkoutSession session = new WorkoutSession();
            session.body_part  = bodyPart;
            session.date       = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            session.created_at = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            long sessionId = db.workoutSessionDao().insert(session);

            db.routineDao().deleteByBodyPart(bodyPart);

            List<Routine> routines = new ArrayList<>();
            for (String name : exerciseNames) {
                Exercise exercise = db.exerciseDao().getByName(name);
                Routine routine = new Routine();
                routine.body_part      = bodyPart;
                routine.exercise_name  = name;
                routine.exercise_id    = exercise != null ? exercise.id : 0;
                routine.default_sets   = 3;
                routine.default_reps   = 10;
                routine.default_weight = 0;
                routines.add(routine);
            }
            db.routineDao().insertAll(routines);

            List<WorkoutLog> logs = new ArrayList<>();
            for (String name : exerciseNames) {
                for (int s = 1; s <= 3; s++) {
                    WorkoutLog log = new WorkoutLog();
                    log.session_id     = (int) sessionId;
                    log.exercise_name  = name;
                    log.set_number     = s;
                    log.planned_sets   = 3;
                    log.planned_reps   = 10;
                    log.planned_weight = 0;
                    log.reps           = 0;
                    log.weight         = 0;
                    log.is_done        = 0;
                    logs.add(log);
                }
            }
            db.workoutLogDao().insertAll(logs);

            runOnUiThread(() -> {
                Intent intent = new Intent(this, WorkoutCheckActivity.class);
                intent.putExtra(EXTRA_SESSION_ID, (int) sessionId);
                startActivity(intent);
                finish();
            });
        });
    }
}
