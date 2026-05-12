package com.example.injehealth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.adapter.ExerciseAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.Exercise;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ExerciseCatalogFragment extends Fragment {

    private ExerciseAdapter adapter;
    private List<Exercise> allExercises = new ArrayList<>();
    private String selectedFilter = "all";

    private TextView tvExerciseCount;
    private TextView tvEmpty;
    private EditText etSearch;

    // 칩 뷰 목록
    private final List<TextView> chips = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_catalog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch       = view.findViewById(R.id.etSearch);
        tvExerciseCount = view.findViewById(R.id.tvExerciseCount);
        tvEmpty        = view.findViewById(R.id.tvEmpty);
        RecyclerView rv = view.findViewById(R.id.rvExercises);
        FloatingActionButton fab = view.findViewById(R.id.fabAdd);

        // RecyclerView 설정
        adapter = new ExerciseAdapter(new ArrayList<>(), exercise -> {
            // 운동 클릭 — 현재 스코프 밖 (다음 plan에서 상세화면 연결)
        });
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // 칩 설정
        TextView chipAll       = view.findViewById(R.id.chipAll);
        TextView chipChest     = view.findViewById(R.id.chipChest);
        TextView chipBack      = view.findViewById(R.id.chipBack);
        TextView chipLegs      = view.findViewById(R.id.chipLegs);
        TextView chipShoulders = view.findViewById(R.id.chipShoulders);
        TextView chipArms      = view.findViewById(R.id.chipArms);
        TextView chipCardio    = view.findViewById(R.id.chipCardio);

        chips.clear();
        chips.add(chipAll);
        chips.add(chipChest);
        chips.add(chipBack);
        chips.add(chipLegs);
        chips.add(chipShoulders);
        chips.add(chipArms);
        chips.add(chipCardio);

        String[] filterKeys = {"all", "가슴", "등", "하체", "어깨", "팔", "유산소"};
        for (int i = 0; i < chips.size(); i++) {
            final String key = filterKeys[i];
            chips.get(i).setOnClickListener(v -> {
                selectedFilter = key;
                updateChipStyles(chips, chips.get(chips.indexOf(v)));
                applyFilter();
            });
        }

        // 검색어 변경 리스너
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // clear 아이콘 클릭 → 검색어 초기화
        etSearch.setOnTouchListener((v, event) -> {
            if (etSearch.getCompoundDrawables()[2] != null) {
                if (event.getRawX() >= (etSearch.getRight() - etSearch.getCompoundDrawables()[2].getBounds().width() - etSearch.getPaddingEnd())) {
                    etSearch.setText("");
                    return true;
                }
            }
            return false;
        });

        // FAB 클릭 → 커스텀 운동 추가 시트
        fab.setOnClickListener(v -> {
            AddCustomExerciseSheet sheet = new AddCustomExerciseSheet();
            sheet.show(getChildFragmentManager(), "add_custom");
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExercises();
    }

    /** DB에서 전체 운동 목록 로드 */
    private void loadExercises() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Exercise> list = AppDatabase.getInstance(requireContext()).exerciseDao().getAll();
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                allExercises = list;
                applyFilter();
            });
        });
    }

    /** 검색어 + 부위 필터 적용 후 adapter 갱신 */
    private void applyFilter() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        List<Exercise> filtered = new ArrayList<>();
        for (Exercise ex : allExercises) {
            boolean matchFilter = "all".equals(selectedFilter) || selectedFilter.equals(ex.body_part);
            boolean matchSearch = query.isEmpty() || ex.name.toLowerCase().contains(query);
            if (matchFilter && matchSearch) filtered.add(ex);
        }
        adapter.updateData(filtered);
        tvExerciseCount.setText("운동 목록 (" + filtered.size() + ")");
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** 칩 선택 스타일 갱신 */
    private void updateChipStyles(List<TextView> all, TextView selected) {
        for (TextView chip : all) {
            chip.setBackgroundResource(chip == selected
                    ? R.drawable.bg_segment_selected
                    : R.drawable.bg_segment_unselected);
        }
    }

    /** AddCustomExerciseSheet에서 추가 완료 시 호출 */
    public void onCustomExerciseAdded() {
        loadExercises();
    }
}
