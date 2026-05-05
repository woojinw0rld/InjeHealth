package com.example.injehealth.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.R;

import java.util.List;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    private final List<String> exerciseNames;
    private final OnDeleteListener deleteListener;

    public RoutineAdapter(List<String> exerciseNames, OnDeleteListener deleteListener) {
        this.exerciseNames  = exerciseNames;
        this.deleteListener = deleteListener;
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
        holder.tvExerciseName.setText(exerciseNames.get(position));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return exerciseNames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExerciseName;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExerciseName = itemView.findViewById(R.id.tv_exercise_name);
            btnDelete      = itemView.findViewById(R.id.btn_delete);
        }
    }
}
