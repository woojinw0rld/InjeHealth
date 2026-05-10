package com.example.injehealth.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RoutineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnSelectListener {
        void onSelect(String routineName);
    }
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM   = 1;

    private final List<Object> flatList = new ArrayList<>();
    private final Set<String> expandedGroups = new HashSet<>();
    private LinkedHashMap<String, List<String>> currentData = new LinkedHashMap<>();
    private String selectedRoutine = null;
    private final OnSelectListener selectListener;
    private int selectedPosition = -1;

    public RoutineAdapter(OnSelectListener selectListener) {
        this.selectListener = selectListener;
    }

    public void setData(LinkedHashMap<String, List<String>> data){
        currentData = data;
        rebuild();
    }
    private void rebuild() {
        flatList.clear();
        for (Map.Entry<String, List<String>> e : currentData.entrySet()) {
            flatList.add(e.getKey());
            if (expandedGroups.contains(e.getKey())) flatList.addAll(e.getValue());
        }
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderVH) {
            String name = (String) flatList.get(position);
            HeaderVH h = (HeaderVH) holder;
            h.tvName.setText(name);
            h.tvArrow.setText(expandedGroups.contains(name) ? "▼" : "▶");
            h.itemView.setBackgroundColor(name.equals(selectedRoutine) ? 0xFF1A3A5C : 0xFF1E1E1E);
            h.btnAdd.setVisibility(View.GONE);
            h.btnDelete.setVisibility(View.GONE);
            h.itemView.setOnClickListener(v -> {
                selectedRoutine = name;
                selectListener.onSelect(name);
                // 펼치기/접기
                if (expandedGroups.contains(name)) expandedGroups.remove(name);
                else expandedGroups.add(name);
                rebuild();
            });
        } else {
            ExerciseVH e = (ExerciseVH) holder;
            e.tvName.setText((String) flatList.get(position));
            e.btnDelete.setVisibility(View.GONE);
        }
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER)
            return new HeaderVH(inf.inflate(R.layout.item_routine_header, parent, false));
        return new ExerciseVH(inf.inflate(R.layout.item_routine_exercise, parent, false));
    }


    @Override
    public int getItemViewType(int position) {
        return flatList.get(position) instanceof String &&
                currentData.containsKey(flatList.get(position))
                ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return flatList.size();
    }

    public String getSelectedName() {
        return selectedRoutine;
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvName, tvArrow;
        ImageButton btnAdd, btnDelete;
        HeaderVH(@NonNull View v) {
            super(v);
            tvName   = v.findViewById(R.id.tv_routine_name);
            tvArrow  = v.findViewById(R.id.tv_arrow);
            btnAdd   = v.findViewById(R.id.btn_add_exercise);
            btnDelete = v.findViewById(R.id.btn_delete_routine);
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
