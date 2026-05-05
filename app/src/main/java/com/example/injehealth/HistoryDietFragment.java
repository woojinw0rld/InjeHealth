package com.example.injehealth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.adapter.DietDayAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.model.DietDaySummary;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HistoryDietFragment extends Fragment {

    private RecyclerView recyclerDiet;
    private View dietEmptyState;
    private DietDayAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_diet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerDiet = view.findViewById(R.id.recyclerDiet);
        dietEmptyState = view.findViewById(R.id.dietEmptyState);

        adapter = new DietDayAdapter(date -> {
            Intent intent = new Intent(requireActivity(), DietDayActivity.class);
            intent.putExtra("date", date);
            startActivity(intent);
        });

        recyclerDiet.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerDiet.setAdapter(adapter);

        FloatingActionButton fabAddDay = view.findViewById(R.id.fabAddDay);
        fabAddDay.setOnClickListener(v -> {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Intent intent = new Intent(requireActivity(), DietDayActivity.class);
            intent.putExtra("date", today);
            startActivity(intent);
        });

        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<DietDaySummary> list = AppDatabase.getInstance(requireContext())
                    .dietLogDao().getDaySummaries();
            requireActivity().runOnUiThread(() -> {
                adapter.setItems(list);
                updateEmptyState(list);
            });
        });
    }

    private void updateEmptyState(List<DietDaySummary> list) {
        if (list == null || list.isEmpty()) {
            recyclerDiet.setVisibility(View.GONE);
            dietEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerDiet.setVisibility(View.VISIBLE);
            dietEmptyState.setVisibility(View.GONE);
        }
    }
}
