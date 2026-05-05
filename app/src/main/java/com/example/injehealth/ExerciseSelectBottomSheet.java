package com.example.injehealth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.Exercise;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;


/**
 * 루틴에 운동을 선택하여 추가할 수 있는 바텀시트
 * 홈화면, 메뉴 --> 루틴설정 에서 사용함.
 * */
public class ExerciseSelectBottomSheet extends BottomSheetDialogFragment {

    public interface OnExerciseSelectedListener {
        void onSelected(String exerciseName);
    }

    private static final String ARG_BODY_PART = "body_part";

    private OnExerciseSelectedListener listener;

    public static ExerciseSelectBottomSheet newInstance(String bodyPart) {
        ExerciseSelectBottomSheet sheet = new ExerciseSelectBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_BODY_PART, bodyPart);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnExerciseSelectedListener(OnExerciseSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_exercise_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String bodyPart = getArguments() != null ? getArguments().getString(ARG_BODY_PART, "") : "";

        RecyclerView rv = view.findViewById(R.id.rv_exercises);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<String> names = new ArrayList<>();
        ExerciseListAdapter adapter = new ExerciseListAdapter(names, name -> {
            if (listener != null) listener.onSelected(name);
            dismiss();
        });
        rv.setAdapter(adapter);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Exercise> exercises = AppDatabase.getInstance(requireContext())
                    .exerciseDao().getByBodyPart(bodyPart);
            requireActivity().runOnUiThread(() -> {
                for (Exercise e : exercises) names.add(e.name);
                adapter.notifyDataSetChanged();
            });
        });
    }

    // 내부 어댑터
    private static class ExerciseListAdapter extends RecyclerView.Adapter<ExerciseListAdapter.VH> {

        interface OnClickListener { void onClick(String name); }

        private final List<String> names;
        private final OnClickListener clickListener;

        ExerciseListAdapter(List<String> names, OnClickListener clickListener) {
            this.names         = names;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_exercise_select, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.tvName.setText(names.get(position));
            holder.itemView.setOnClickListener(v -> clickListener.onClick(names.get(position)));
        }

        @Override
        public int getItemCount() { return names.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName;
            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_exercise_name);
            }
        }
    }
}
