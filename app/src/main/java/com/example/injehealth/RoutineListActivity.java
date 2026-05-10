package com.example.injehealth;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.adapter.RoutineManageAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.Exercise;
import com.example.injehealth.db.entity.Routine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executors;


public class RoutineListActivity extends AppCompatActivity {
    private RecyclerView rv;
    private TextView tvEmpty;
    private RoutineManageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_list);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_routines);
        tvEmpty = findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoutineManageAdapter(new RoutineManageAdapter.Listener() {
            @Override
            public void onAddExercise(String routineName) {
                showExerciseSelect(routineName);
            }

            @Override
            public void onDeleteRoutine(String routineName) {
                deleteRoutine(routineName);
            }

            @Override
            public void onDeleteExercise(Routine routine) {
                deleteExercise(routine);
            }
        });        rv.setAdapter(adapter);

        loadRoutines();
        findViewById(R.id.btn_add_routine).setOnClickListener(v -> showAddRoutineDialog());
    }

    private void loadRoutines(){
        Executors.newSingleThreadExecutor().execute(()->{
            List<Routine> allRoutine = AppDatabase.getInstance(this).routineDao().getAll();
            LinkedHashMap<String, List<Routine>> groupedRoutine = new LinkedHashMap<>();
            for(Routine r : allRoutine){
                groupedRoutine.computeIfAbsent(r.routine_name, k -> new ArrayList<>()).add(r);
            }
            runOnUiThread(()->{
                if(groupedRoutine.isEmpty()){
                    rv.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                }else{
                    tvEmpty.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                    adapter.setData(groupedRoutine);
                }
            });
        });
    }
    private void showAddRoutineDialog(){
        EditText input = new EditText(this);
        input.setHint("루틴 이름");
        new AlertDialog.Builder(this)
                .setTitle("루틴 추가")
                .setView(input)
                .setPositiveButton("추가", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) showExerciseSelect(name);
                })
                .setNegativeButton("취소", null)
                .show();
    }
    private void showExerciseSelect(String routineName) {
        ExerciseSelectBottomSheet sheet = ExerciseSelectBottomSheet.newInstance(routineName);
        sheet.setOnExerciseSelectedListener(name -> addExercise(routineName, name));
        sheet.show(getSupportFragmentManager(), "exercise_select");
    }
    private void addExercise(String routineName, String exerciseName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            for (Routine r : db.routineDao().getByRoutineName(routineName)) {
                if (r.exercise_name.equals(exerciseName)) return;
            }
            Exercise ex = db.exerciseDao().getByName(exerciseName);
            Routine routine = new Routine();
            routine.routine_name = routineName;
            routine.exercise_name = exerciseName;
            routine.exercise_id = ex != null ? ex.id : 0;
            routine.default_sets = 0;
            routine.default_reps = 0;
            routine.default_weight = 0;
            db.routineDao().insert(routine);
            runOnUiThread(this::loadRoutines);
        });
    }
    private void deleteExercise(Routine routine) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).routineDao().delete(routine);
            runOnUiThread(this::loadRoutines);
        });
    }

    private void deleteRoutine(String routineName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).routineDao().deleteRoutine(routineName);
            runOnUiThread(this::loadRoutines);
        });
    }

}
