package com.example.injehealth.adapter;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.R;
import com.example.injehealth.db.entity.Routine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RoutineManageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onAddExercise(String routineName);
        void onDeleteRoutine(String routineName);
        void onDeleteExercise(Routine routine);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM   = 1;

    private final List<Object> flatList = new ArrayList<>();
    private final Set<String> expandedGroups = new HashSet<>();
    private LinkedHashMap<String, List<Routine>> currentData = new LinkedHashMap<>();
    private final Listener listener;

    public RoutineManageAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(LinkedHashMap<String, List<Routine>> grouped) {
        currentData = grouped;
        rebuild();
    }

    private void rebuild() {
        flatList.clear();
        for (Map.Entry<String, List<Routine>> entry : currentData.entrySet()) {
            flatList.add(entry.getKey());
            if (expandedGroups.contains(entry.getKey())) {
                flatList.addAll(entry.getValue());
            }
        }
        notifyDataSetChanged();
    }

    private void toggle(String routineName) {
        if (expandedGroups.contains(routineName)) expandedGroups.remove(routineName);
        else expandedGroups.add(routineName);
        rebuild();
    }

    @Override
    public int getItemViewType(int position) {
        return flatList.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER)
            return new HeaderVH(inf.inflate(R.layout.item_routine_header, parent, false));
        return new ExerciseVH(inf.inflate(R.layout.item_routine_exercise, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderVH) {
            String name = (String) flatList.get(position);
            HeaderVH h = (HeaderVH) holder;
            h.tvName.setText(name);
            h.tvArrow.setText(expandedGroups.contains(name) ? "▼" : "▶");
            h.itemView.setOnClickListener(v -> toggle(name));
            h.btnAddExercise.setOnClickListener(v -> listener.onAddExercise(name));
            h.btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(holder.itemView.getContext())
                    .setMessage("\"" + name + "\" 루틴을 삭제할까요?")
                    .setPositiveButton("삭제", (d, w) -> listener.onDeleteRoutine(name))
                    .setNegativeButton("취소", null)
                    .show()
            );
        } else {
            Routine r = (Routine) flatList.get(position);
            ExerciseVH e = (ExerciseVH) holder;
            e.tvName.setText(r.exercise_name);
            e.btnDelete.setOnClickListener(v -> listener.onDeleteExercise(r));
        }
    }

    @Override
    public int getItemCount() { return flatList.size(); }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvName, tvArrow;
        ImageButton btnDelete, btnAddExercise;
        HeaderVH(@NonNull View v) {
            super(v);
            tvName        = v.findViewById(R.id.tv_routine_name);
            tvArrow       = v.findViewById(R.id.tv_arrow);
            btnDelete     = v.findViewById(R.id.btn_delete_routine);
            btnAddExercise = v.findViewById(R.id.btn_add_exercise);
        }
    }

    static class ExerciseVH extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageButton btnDelete;
        ExerciseVH(@NonNull View v) {
            super(v);
            tvName    = v.findViewById(R.id.tv_exercise_name);
            btnDelete = v.findViewById(R.id.btn_delete_exercise);
        }
    }
}
