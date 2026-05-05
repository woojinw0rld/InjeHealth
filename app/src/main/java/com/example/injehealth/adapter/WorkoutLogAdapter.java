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
import com.example.injehealth.db.entity.WorkoutLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WorkoutLogAdapter extends RecyclerView.Adapter<WorkoutLogAdapter.ViewHolder> {

    public interface OnSetCheckedListener {
        void onSetChecked(WorkoutLog log);
        int getTotalSets();
        int getCompletedSets();
        void onSetAdded(WorkoutLog newLog);
        void onSetDeleted(WorkoutLog log);
    }

    private final List<String> exerciseNames;
    private final Map<String, List<WorkoutLog>> groupedLogs;
    private final OnSetCheckedListener listener;
    private final Map<Integer, CountDownTimer> activeTimers = new HashMap<>();
    private final Map<Integer, Long> remainingMs = new HashMap<>();
    private final int sessionId;

    public WorkoutLogAdapter(List<String> exerciseNames,
                             Map<String, List<WorkoutLog>> groupedLogs,
                             int sessionId,
                             OnSetCheckedListener listener) {
        this.exerciseNames = exerciseNames;
        this.groupedLogs   = groupedLogs;
        this.listener      = listener;
        this.sessionId     = sessionId;
    }

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


        for (WorkoutLog log : sets) {
            View setRow = buildSetRow(holder.itemView.getContext(), log);
            holder.llSets.addView(setRow);
        }
        Button btnAddSet = new Button(holder.itemView.getContext());
        btnAddSet.setText("+ 세트 추가");
        btnAddSet.setTextColor(0xFF1E88E5);
        btnAddSet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnAddSet.setOnClickListener(v -> {
            List<WorkoutLog> currentSets = groupedLogs.get(exerciseName);
            WorkoutLog newLog = new WorkoutLog();
            newLog.session_id     = sessionId;
            newLog.exercise_name  = exerciseName;
            newLog.set_number     = currentSets.size() + 1;
            newLog.planned_sets   = currentSets.size() + 1;
            newLog.planned_reps   = currentSets.isEmpty() ? 10 : currentSets.get(0).planned_reps;
            newLog.planned_weight = currentSets.isEmpty() ? 0  : currentSets.get(0).planned_weight;
            newLog.reps           = 0;
            newLog.weight         = 0;
            newLog.is_done        = 0;

            currentSets.add(newLog);
            View setRow = buildSetRow(holder.itemView.getContext(), newLog);
            holder.llSets.addView(setRow, holder.llSets.getChildCount() - 1);

            listener.onSetAdded(newLog);
        });
        holder.llSets.addView(btnAddSet);
    }

    private View buildSetRow(Context ctx, WorkoutLog log) {
        View row = LayoutInflater.from(ctx).inflate(R.layout.item_workout_set, null);
        row.setTag(log);

        TextView tvSetNumber   = row.findViewById(R.id.tv_set_number);
        TextView tvPlanned     = row.findViewById(R.id.tv_planned);
        EditText etActualReps  = row.findViewById(R.id.et_actual_reps);
        EditText etActualWeight = row.findViewById(R.id.et_actual_weight);
        ImageButton btnCheck   = row.findViewById(R.id.btn_check);
        ImageButton btnDelete  = row.findViewById(R.id.btn_delete_set);
        ImageButton btnTimeAdd = row.findViewById(R.id.btn_time_add);
        LinearLayout llTimer   = row.findViewById(R.id.ll_rest_timer);
        TextView tvTimer       = row.findViewById(R.id.tv_rest_timer);

        tvSetNumber.setText(String.valueOf(log.set_number));
        tvPlanned.setText(log.planned_reps + "회 × " + formatWeight(log.planned_weight) + "kg");

        if (log.is_done == 1) {
            applyDoneState(row, etActualReps, etActualWeight, btnCheck);
            btnDelete.setVisibility(View.GONE);
            etActualReps.setText(String.valueOf(log.reps));
            etActualWeight.setText(formatWeight(log.weight));
        }

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
            startRestTimer(log.id, llTimer, tvTimer, 60_000);
        });
        btnTimeAdd.setOnClickListener(v -> {
            Long current = remainingMs.get(log.id);
            if (current != null && current > 0) {
                startRestTimer(log.id, llTimer, tvTimer, current + 10_000);
            }
        });

        btnDelete.setOnClickListener(v -> {
            LinearLayout llSets = (LinearLayout) row.getParent();
            llSets.removeView(row);
            groupedLogs.get(log.exercise_name).remove(log);
            listener.onSetDeleted(log);
            renumberSets(llSets);  // 삭제 후 번호 갱신
        });

        return row;
    }

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

    private void startRestTimer(int logId, LinearLayout llTimer, TextView tvTimer, long ms) {
        if (activeTimers.containsKey(logId)) {
            activeTimers.get(logId).cancel();
        }
        llTimer.setVisibility(View.VISIBLE);
        remainingMs.put(logId, ms);
        CountDownTimer timer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long msUntilFinished) {
                remainingMs.put(logId, msUntilFinished);
                long secs = msUntilFinished / 1000;
                tvTimer.setText("휴식 시간: " + secs / 60 + ":" + String.format("%02d", secs % 60));
            }
            @Override
            public void onFinish() {
                llTimer.setVisibility(View.GONE);
                activeTimers.remove(logId);
                remainingMs.remove(logId);
            }
        }.start();
        activeTimers.put(logId, timer);
    }

    public void cancelAllTimers() {
        for (CountDownTimer t : activeTimers.values()) t.cancel();
        activeTimers.clear();
        remainingMs.clear();
    }

    private String formatWeight(double w) {
        return w == (int) w ? String.valueOf((int) w) : String.valueOf(w);
    }

    @Override
    public int getItemCount() {
        return exerciseNames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExerciseName;
        LinearLayout llSets;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExerciseName = itemView.findViewById(R.id.tv_exercise_name);
            llSets         = itemView.findViewById(R.id.ll_sets);
        }
    }
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
}
