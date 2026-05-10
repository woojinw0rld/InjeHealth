package com.example.injehealth.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.R;

import java.util.List;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.ViewHolder> {

    public interface OnSelectListener {
        void onSelect(String routineName);
    }

    private final List<String> routineNames;
    private final OnSelectListener selectListener;
    private int selectedPosition = -1;

    public RoutineAdapter(List<String> routineNames, OnSelectListener selectListener) {
        this.routineNames   = routineNames;
        this.selectListener = selectListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_routine, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvExerciseName.setText(routineNames.get(position));
        holder.cbSelect.setChecked(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            selectListener.onSelect(routineNames.get(selectedPosition));
        });

        holder.cbSelect.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            selectListener.onSelect(routineNames.get(selectedPosition));
        });
    }

    @Override
    public int getItemCount() {
        return routineNames.size();
    }

    public String getSelectedName() {
        if (selectedPosition == -1) return null;
        return routineNames.get(selectedPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExerciseName;
        CheckBox cbSelect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExerciseName = itemView.findViewById(R.id.tv_exercise_name);
            cbSelect       = itemView.findViewById(R.id.cb_select);
        }
    }
}
