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
import com.example.injehealth.util.SystemBarHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class RoutineSetupActivity extends AppCompatActivity {
    private List<Routine> routineList;

    public static final String ROUTINE_NAME = "ROUTINE_NAME";
    public static final String EXTRA_SESSION_ID = "session_id";

    private String myRoutine = "empty";
    private RoutineAdapter adapter;
    private List<String> routineNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_setup);
        SystemBarHelper.applyPadding(this, R.id.main);

        loadSavedRoutine();

        /*상단 타이틀 변경*/
        setTopTitel();

        /*뒤로 돌아가기 버튼*/
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_routines);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoutineAdapter(routineNames, routineName -> {
            myRoutine = adapter.getSelectedName();
            Log.d("TAG", "myRoutine: " + myRoutine);
        });
        rv.setAdapter(adapter);

        Button btnStart = findViewById(R.id.btn_start_workout);
        btnStart.setOnClickListener(v -> startWorkout());
    }
    private void setTopTitel(){
        TextView tvBadge = findViewById(R.id.tv_days_badge);
        String[] days = {"일요일","월요일","화요일","수요일","목요일","금요일","토요일"};
        String today = days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1];
        tvBadge.setText(today);
    }

    /**저장된 루틴을 불러옮*/
    private void loadSavedRoutine() {
        Executors.newSingleThreadExecutor().execute(() -> {
            routineList = AppDatabase.getInstance(this).routineDao().getAll();
            for(Routine r : routineList){
                if (!routineNames.contains(r.routine_name)) {
                    routineNames.add(r.routine_name);
                }
            }
        });
    }
    /**운동추가 바텀 시트*/
//    private void showExerciseSheet() {
//        ExerciseSelectBottomSheet sheet = ExerciseSelectBottomSheet.newInstance(bodyPart);
//        sheet.setOnExerciseSelectedListener(name -> {
//            if (!exerciseNames.contains(name)) {
//                exerciseNames.add(name);
//                adapter.notifyItemInserted(exerciseNames.size() - 1);
//            } else {
//                Toast.makeText(this, "이미 추가된 종목입니다", Toast.LENGTH_SHORT).show();
//            }
//        });
//        sheet.show(getSupportFragmentManager(), "exercise_select");
//    }

    private void startWorkout() {
        if (routineList.isEmpty() && myRoutine.equals("empty")) {
            Toast.makeText(this, "종목을 추가해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        setRoutinDB();
        Intent intent = new Intent(this, WorkoutCheckActivity.class);
        intent.putStringArrayListExtra("routineNames_names", new ArrayList<>(routineNames));
        intent.putExtra(ROUTINE_NAME, myRoutine);
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
            WorkoutSession lastWorkoutSession = db.workoutSessionDao().getLastByRoutineName(myRoutine);
            //null 체크.
            List<WorkoutLog> workoutLogs = lastWorkoutSession != null ?
                    db.workoutLogDao().getBySession(lastWorkoutSession.id) : new ArrayList<>();

            for (Routine r : routineList) {
                for (WorkoutLog log : workoutLogs) {
                    if (r.exercise_name.equals(log.exercise_name)) {
                        r.default_weight = Math.max((int) log.weight, r.default_weight);
                        r.default_reps   = Math.max(log.reps, r.default_reps);
                        r.default_sets   = Math.max(log.set_number, r.default_sets);
                        break;
                    }
                }
                db.routineDao().update(r);
            }
        });
    }

}
