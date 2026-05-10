package com.example.injehealth.adapter;

import android.content.Context;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.R;
import com.example.injehealth.db.entity.Routine;
import com.example.injehealth.db.entity.WorkoutLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkoutLogAdapter extends RecyclerView.Adapter<WorkoutLogAdapter.ViewHolder> {

    // ─────────────────────────────────────────
    // 인터페이스: 어댑터 → 액티비티로 이벤트 위임
    // ─────────────────────────────────────────
    public interface OnSetCheckedListener {
        void onSetChecked(WorkoutLog log);      // 세트 완료 체크
        int getTotalSets();                      // 전체 세트 수
        int getCompletedSets();                  // 완료 세트 수
        void onSetAdded(WorkoutLog newLog);      // 세트 추가
        void onSetDeleted(WorkoutLog log);       // 세트 삭제
        void onRestTimerFinished();              // 휴식 타이머 종료 → 알림은 액티비티가 처리
    }

    // ─────────────────────────────────────────
    // 멤버 변수
    // ─────────────────────────────────────────
    private final List<String>                   exerciseNames;
    private final Map<String, List<WorkoutLog>>  groupedLogs;
    private final OnSetCheckedListener           listener;
    private final Map<Integer, CountDownTimer>   activeTimers = new HashMap<>();
    private final Map<Integer, Long>             remainingMs  = new HashMap<>();
    private final int                            sessionId;

    // ─────────────────────────────────────────
    // 생성자: Context 받지 않음 (메모리 누수 방지)
    // ─────────────────────────────────────────
    public WorkoutLogAdapter(int sessionId, OnSetCheckedListener listener) {
        this.exerciseNames = new ArrayList<>();
        this.groupedLogs   = new LinkedHashMap<>();
        this.sessionId     = sessionId;
        this.listener      = listener;
    }

    // ─────────────────────────────────────────
    // RecyclerView 기본 메서드
    // ─────────────────────────────────────────
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String exerciseName = exerciseNames.get(position);
        List<WorkoutLog> sets = groupedLogs.get(exerciseName);

        holder.tvExerciseName.setText(exerciseName);
        holder.llSets.removeAllViews();

        // 기존 세트 행 추가
        for (WorkoutLog log : sets) {
            holder.llSets.addView(buildSetRow(holder.itemView.getContext(), log));
        }

        // "+ 세트 추가" 버튼
        Button btnAddSet = holder.itemView.findViewById(R.id.btn_add_set);
        btnAddSet.setOnClickListener(v ->
                addSetListener(exerciseName, holder.llSets, holder.itemView.getContext()));
    }

    @Override
    public int getItemCount() {
        return exerciseNames.size();
    }

    // ─────────────────────────────────────────
    // 세트 추가
    // ─────────────────────────────────────────
    private void addSetListener(String exerciseName, LinearLayout llSets, Context ctx) {
        List<WorkoutLog> currentSets = groupedLogs.get(exerciseName);

        WorkoutLog newLog = new WorkoutLog();
        newLog.session_id     = sessionId;
        newLog.exercise_name  = exerciseName;
        newLog.set_number     = currentSets.size() + 1;
        newLog.planned_sets   = currentSets.size() + 1;
        newLog.planned_reps   = currentSets.isEmpty() ? 10 : currentSets.get(0).planned_reps;
        newLog.planned_weight = currentSets.isEmpty() ? 0  : currentSets.get(0).planned_weight;
        newLog.is_done        = 0;

        currentSets.add(newLog);
        llSets.addView(buildSetRow(ctx, newLog));
        listener.onSetAdded(newLog);
    }

    // ─────────────────────────────────────────
    // 루틴 → WorkoutLog 변환
    // ─────────────────────────────────────────
    public void setRoutines(List<Routine> routines) {
        exerciseNames.clear();
        groupedLogs.clear();

        for (Routine r : routines) {
            if (!exerciseNames.contains(r.exercise_name)) {
                exerciseNames.add(r.exercise_name);
                groupedLogs.put(r.exercise_name, new ArrayList<>());
            }
            List<WorkoutLog> sets = groupedLogs.get(r.exercise_name);
            for (int i = 0; i < r.default_sets; i++) {
                WorkoutLog log = new WorkoutLog();
                log.session_id     = sessionId;
                log.exercise_name  = r.exercise_name;
                log.set_number     = i + 1;
                log.planned_sets   = r.default_sets;
                log.planned_reps   = r.default_reps;
                log.planned_weight = r.default_weight;
                log.is_done        = 0;
                sets.add(log);
            }
        }
        notifyDataSetChanged();
    }

    // ─────────────────────────────────────────
    // 세트 행(row) 하나 생성
    // ─────────────────────────────────────────
    private View buildSetRow(Context ctx, WorkoutLog log) {
        // log.id가 0일 수 있으므로 객체 주소값을 타이머 키로 사용
        int timerKey = System.identityHashCode(log);

        View row = LayoutInflater.from(ctx).inflate(R.layout.item_workout_set, null);
        row.setTag(log);

        TextView     tvSetNumber    = row.findViewById(R.id.tv_set_number);
        TextView     tvPlanned      = row.findViewById(R.id.tv_planned);
        EditText     etActualReps   = row.findViewById(R.id.et_actual_reps);
        EditText     etActualWeight = row.findViewById(R.id.et_actual_weight);
        ImageButton  btnCheck       = row.findViewById(R.id.btn_check);
        ImageButton  btnDelete      = row.findViewById(R.id.btn_delete_set);
        ImageButton  btnTimeAdd     = row.findViewById(R.id.btn_time_add);
        LinearLayout llTimer        = row.findViewById(R.id.ll_rest_timer);
        TextView     tvTimer        = row.findViewById(R.id.tv_rest_timer);

        // 초기값 표시
        tvSetNumber.setText(String.valueOf(log.set_number));
        tvPlanned.setText(log.planned_reps + "회 × " + formatWeight(log.planned_weight) + "kg");
        etActualReps.setText(String.valueOf(log.planned_reps));
        etActualWeight.setText(formatWeight(log.planned_weight));

        // 이미 완료된 세트면 완료 상태로 표시
        if (log.is_done == 1) {
            applyDoneState(row, etActualReps, etActualWeight, btnCheck);
            btnDelete.setVisibility(View.GONE);
            etActualReps.setText(String.valueOf(log.reps));
            etActualWeight.setText(formatWeight(log.weight));
        }

        // 체크 버튼: 세트 완료 처리
        btnCheck.setOnClickListener(v -> {
            String repsStr   = etActualReps.getText().toString().trim();
            String weightStr = etActualWeight.getText().toString().trim();
            if (repsStr.isEmpty() || weightStr.isEmpty()) return;

            log.reps    = Integer.parseInt(repsStr);
            log.weight  = Double.parseDouble(weightStr);
            log.is_done = 1;
            listener.onSetChecked(log);

            applyDoneState(row, etActualReps, etActualWeight, btnCheck);
            btnDelete.setVisibility(View.GONE);
            startRestTimer(timerKey, llTimer, tvTimer, 10_000); // 60초 휴식 시작
        });

        // +10초 버튼
        btnTimeAdd.setOnClickListener(v -> {
            Long current = remainingMs.get(timerKey);
            if (current != null && current > 0) {
                startRestTimer(timerKey, llTimer, tvTimer, current + 10_000);
            }
        });

        // 삭제 버튼
        btnDelete.setOnClickListener(v -> {
            LinearLayout llSets = (LinearLayout) row.getParent();
            llSets.removeView(row);
            groupedLogs.get(log.exercise_name).remove(log);
            listener.onSetDeleted(log);
            renumberSets(llSets);
        });

        return row;
    }

    // ─────────────────────────────────────────
    // 완료 상태 UI 적용
    // ─────────────────────────────────────────
    private void applyDoneState(View row, EditText etReps, EditText etWeight, ImageButton btnCheck) {
        etReps.setEnabled(false);
        etWeight.setEnabled(false);
        etReps.setAlpha(0.5f);
        etWeight.setAlpha(0.5f);
        btnCheck.setBackgroundResource(R.drawable.bg_icon_blue_circle);
        btnCheck.setImageResource(R.drawable.ic_check);
        btnCheck.setColorFilter(0xFF1E88E5);
        btnCheck.setEnabled(false);
    }

    // ─────────────────────────────────────────
    // 휴식 타이머
    // ─────────────────────────────────────────
    private void startRestTimer(int timerKey, LinearLayout llTimer, TextView tvTimer, long ms) {
        // 기존 타이머 있으면 취소 (중복 방지)
        if (activeTimers.containsKey(timerKey)) {
            activeTimers.get(timerKey).cancel();
        }

        llTimer.setVisibility(View.VISIBLE);
        remainingMs.put(timerKey, ms);

        CountDownTimer timer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long msUntilFinished) {
                remainingMs.put(timerKey, msUntilFinished);
                long secs = msUntilFinished / 1000;
                tvTimer.setText("휴식 시간: " + secs / 60 + ":" + String.format("%02d", secs % 60));
            }

            @Override
            public void onFinish() {
                llTimer.setVisibility(View.GONE);
                activeTimers.remove(timerKey);
                remainingMs.remove(timerKey);
                listener.onRestTimerFinished(); // 알림은 액티비티에서 처리
            }
        }.start();

        activeTimers.put(timerKey, timer);
    }

    // ─────────────────────────────────────────
    // 타이머 전체 취소 (화면 나갈 때 호출)
    // ─────────────────────────────────────────
    public void cancelAllTimers() {
        for (CountDownTimer t : activeTimers.values()) t.cancel();
        activeTimers.clear();
        remainingMs.clear();
    }

    // ─────────────────────────────────────────
    // 세트 번호 재정렬 (삭제 후 호출)
    // ─────────────────────────────────────────
    private void renumberSets(LinearLayout llSets) {
        int setNum = 1;
        for (int i = 0; i < llSets.getChildCount(); i++) {
            View child = llSets.getChildAt(i);
            TextView tvNum = child.findViewById(R.id.tv_set_number);
            if (tvNum != null) {
                tvNum.setText(String.valueOf(setNum));
                if (child.getTag() instanceof WorkoutLog) {
                    ((WorkoutLog) child.getTag()).set_number = setNum;
                }
                setNum++;
            }
        }
    }
    // ─────────────────────────────────────────
    //  기존에 있는 로그 적용
    // ─────────────────────────────────────────
    public void setRoutinesWithLogs(List<Routine> routines, List<WorkoutLog> existingLogs) {
        exerciseNames.clear();
        groupedLogs.clear();

        // 기존 로그를 "운동이름_세트번호" 키로 맵 만들기
        // 예: "벤치프레스_1" → WorkoutLog 객체
        // 나중에 루틴 순회할 때 O(1)로 빠르게 찾기 위함
        Map<String, WorkoutLog> logMap = new HashMap<>();
        for (WorkoutLog log : existingLogs) {
            logMap.put(log.exercise_name + "_" + log.set_number, log);
        }

        for (Routine r : routines) {
            // 처음 나오는 운동이면 목록과 빈 세트 리스트 추가
            if (!exerciseNames.contains(r.exercise_name)) {
                exerciseNames.add(r.exercise_name);
                groupedLogs.put(r.exercise_name, new ArrayList<>());
            }

            List<WorkoutLog> sets = groupedLogs.get(r.exercise_name);

            for (int i = 0; i < r.default_sets; i++) {
                String key = r.exercise_name + "_" + (i + 1);
                WorkoutLog existing = logMap.get(key);

                if (existing != null) {
                    // DB에 기존 로그 있음 → 그대로 사용
                    // is_done, reps, weight 전부 유지되므로 완료 상태 보존됨
                    sets.add(existing);
                } else {
                    // DB에 없음 → 새 로그 생성 (미완료 상태로 초기화)
                    WorkoutLog log = new WorkoutLog();
                    log.session_id     = sessionId;
                    log.exercise_name  = r.exercise_name;
                    log.set_number     = i + 1;
                    log.planned_sets   = r.default_sets;
                    log.planned_reps   = r.default_reps;
                    log.planned_weight = r.default_weight;
                    log.is_done        = 0;
                    sets.add(log);
                }
            }
        }

        // 데이터 변경 알림 → 화면 갱신
        notifyDataSetChanged();
    }

    // ─────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────
    private String formatWeight(double w) {
        return w == (int) w ? String.valueOf((int) w) : String.valueOf(w);
    }

    // ─────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView     tvExerciseName;
        LinearLayout llSets;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExerciseName = itemView.findViewById(R.id.tv_exercise_name);
            llSets         = itemView.findViewById(R.id.ll_sets);
        }
    }
}