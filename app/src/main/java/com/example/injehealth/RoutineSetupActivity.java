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

import java.util.ArrayList;
import java.util.List;
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

        /*intent로 HomeFragment에서 운동 명 받아옴*/
        bodyPart = getIntent().getStringExtra(EXTRA_BODY_PART);
        if (bodyPart == null) bodyPart = "전체";

        /*상단 타이틀 변경*/
        TextView tvTitle = findViewById(R.id.tv_body_part_title);
        TextView tvBadge = findViewById(R.id.tv_body_part_badge);
        tvTitle.setText(bodyPart + " 운동 설정");
        tvBadge.setText(bodyPart);

        /*뒤로 돌아가기 버튼*/
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

    /**저장된 루틴을 불러옮*/
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
    /**운동추가 바텀 시트*/
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
        setRoutinDB();
        Intent intent = new Intent(this, WorkoutCheckActivity.class);
        intent.putStringArrayListExtra("exercise_names", new ArrayList<>(exerciseNames));
        intent.putExtra(EXTRA_BODY_PART, bodyPart);
        startActivity(intent);
        finish();
    }

    /**
     * 현재 루틴을 세팅할 때 과거 운동기록을 참고하여
     * 루틴 목표 값에 저장.
     * */
    private void setRoutinDB(){
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            //db에서 이전 세션들 불러옴.
            List<WorkoutSession> workoutSessionsList = db.workoutSessionDao().getAll();
            WorkoutSession lasteWorkoutSession = null;
            for (WorkoutSession s : workoutSessionsList){ // 최근 같은 bodyPart 저장.
                if (bodyPart.equals(s.body_part)){
                    lasteWorkoutSession = s;
                    break;
                }
            }
            //null 체크.
            List<WorkoutLog> workoutLogs = lasteWorkoutSession != null ?
                    db.workoutLogDao().getBySession(lasteWorkoutSession.id) : new ArrayList<>();

            List<Routine> routines = new ArrayList<>();
            for (String name : exerciseNames) {
                Exercise exercise = db.exerciseDao().getByName(name);

                int defaultWeight = 0, defaultReps = 0, defaultSets = 3;
                for (WorkoutLog log : workoutLogs){  //각 운동들 가져와서 저장.
                    if(name.equals(log.exercise_name)){
                        defaultWeight = Math.max((int)log.weight, defaultWeight);
                        defaultReps = Math.max(log.reps, defaultReps);
                        defaultSets = Math.max(log.set_number,defaultSets);
                        break;
                    }
                }
                Routine routine = new Routine();
                routine.body_part = bodyPart;
                routine.exercise_name = name;
                routine.exercise_id = exercise != null ? exercise.id : 0;
                routine.default_sets = defaultSets;
                routine.default_reps = defaultReps;
                routine.default_weight = defaultWeight;
                routines.add(routine);
            }
            db.routineDao().insertAll(routines);
        });

    }

}
