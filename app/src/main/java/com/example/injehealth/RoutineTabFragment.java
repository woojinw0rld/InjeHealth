package com.example.injehealth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.Exercise;
import com.example.injehealth.db.entity.Routine;

import java.util.List;
import java.util.concurrent.Executors;

public class RoutineTabFragment extends Fragment {

    private static final String ARG_BODY_PART = "body_part";

    public static RoutineTabFragment newInstance(String bodyPart) {
        RoutineTabFragment f = new RoutineTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BODY_PART, bodyPart);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_routine_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String bodyPart = getArguments() != null ? getArguments().getString(ARG_BODY_PART, "") : "";

        RecyclerView rv = view.findViewById(R.id.rv_routines);
        TextView tvEmpty = view.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadRoutines(bodyPart, rv, tvEmpty);

        view.findViewById(R.id.btn_add_exercise).setOnClickListener(v -> {
            ExerciseSelectBottomSheet sheet = ExerciseSelectBottomSheet.newInstance(bodyPart);
            sheet.setOnExerciseSelectedListener(name -> addExerciseToRoutine(name, bodyPart, rv, tvEmpty));
            sheet.show(getChildFragmentManager(), "exercise_select");
        });
    }

    private void loadRoutines(String bodyPart, RecyclerView rv, TextView tvEmpty) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Routine> routines = AppDatabase.getInstance(requireContext())
                    .routineDao().getByBodyPart(bodyPart);
            requireActivity().runOnUiThread(() -> {
                if (routines.isEmpty()) {
                    rv.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rv.setVisibility(View.VISIBLE);
                    rv.setAdapter(new Adapter(routines, routine ->
                            deleteRoutine(routine, bodyPart, rv, tvEmpty)));
                }
            });
        });
    }

    private void addExerciseToRoutine(String name, String bodyPart, RecyclerView rv, TextView tvEmpty) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<Routine> existing = db.routineDao().getByBodyPart(bodyPart);
            for (Routine r : existing) {
                if (r.exercise_name.equals(name)) return;
            }
            Exercise exercise = db.exerciseDao().getByName(name);
            Routine routine = new Routine();
            routine.body_part     = bodyPart;
            routine.exercise_name = name;
            routine.exercise_id   = exercise != null ? exercise.id : 0;
            routine.default_sets  = 3;
            routine.default_reps  = 10;
            routine.default_weight = 0;
            db.routineDao().insert(routine);
            requireActivity().runOnUiThread(() -> loadRoutines(bodyPart, rv, tvEmpty));
        });
    }

    private void deleteRoutine(Routine routine, String bodyPart, RecyclerView rv, TextView tvEmpty) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(requireContext()).routineDao().delete(routine);
            requireActivity().runOnUiThread(() -> loadRoutines(bodyPart, rv, tvEmpty));
        });
    }

    private static class Adapter extends RecyclerView.Adapter<Adapter.VH> {

        interface OnDeleteListener { void onDelete(Routine routine); }

        private final List<Routine> routines;
        private final OnDeleteListener listener;

        Adapter(List<Routine> routines, OnDeleteListener listener) {
            this.routines = routines;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_routine, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Routine r = routines.get(position);
            holder.tvName.setText(r.exercise_name);
            holder.btnDelete.setOnClickListener(v -> listener.onDelete(r));
        }

        @Override
        public int getItemCount() { return routines.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageButton btnDelete;
            VH(@NonNull View itemView) {
                super(itemView);
                tvName    = itemView.findViewById(R.id.tv_exercise_name);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}
