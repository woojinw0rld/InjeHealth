package com.example.injehealth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class HistoryFragment extends Fragment {

    private static final String KEY_SEGMENT = "current_segment";
    private static final String SEGMENT_WORKOUT = "workout";
    private static final String SEGMENT_DIET = "diet";

    private String currentSegment = SEGMENT_WORKOUT;
    private TextView tabWorkout;
    private TextView tabDiet;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabWorkout = view.findViewById(R.id.tab_workout);
        tabDiet = view.findViewById(R.id.tab_diet);

        tabWorkout.setOnClickListener(v -> showWorkout());
        tabDiet.setOnClickListener(v -> showDiet());

        // 회전 등 세션 내 상태 복원 (앱 재시작 시 savedInstanceState=null → 운동 default)
        if (savedInstanceState != null) {
            currentSegment = savedInstanceState.getString(KEY_SEGMENT, SEGMENT_WORKOUT);
        }

        if (SEGMENT_DIET.equals(currentSegment)) {
            showDiet();
        } else {
            showWorkout();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SEGMENT, currentSegment);
    }

    private void showWorkout() {
        currentSegment = SEGMENT_WORKOUT;
        selectTab(tabWorkout, tabDiet);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.history_fragment_container, new HistoryWorkoutFragment())
                .commit();
    }

    private void showDiet() {
        currentSegment = SEGMENT_DIET;
        selectTab(tabDiet, tabWorkout);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.history_fragment_container, new HistoryDietFragment())
                .commit();
    }

    private void selectTab(TextView selected, TextView unselected) {
        selected.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_segment_selected));
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        selected.setTypeface(null, android.graphics.Typeface.BOLD);

        unselected.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_segment_unselected));
        unselected.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        unselected.setTypeface(null, android.graphics.Typeface.NORMAL);
    }
}
