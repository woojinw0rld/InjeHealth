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

import com.example.injehealth.adapter.HistoryAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.model.SessionSummary;

import java.util.List;
import java.util.concurrent.Executors;

public class HistoryWorkoutFragment extends Fragment {

    private RecyclerView rvHistory;
    private View layoutEmpty;
    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_workout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvHistory = view.findViewById(R.id.rv_history);
        layoutEmpty = view.findViewById(R.id.layout_empty);

        adapter = new HistoryAdapter(session -> {
            Intent intent = new Intent(requireActivity(), HistoryDetailActivity.class);
            intent.putExtra("session_id", session.id);
            startActivity(intent);
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);

        loadHistoryData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistoryData();
    }

    private void loadHistoryData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SessionSummary> list = AppDatabase.getInstance(requireContext())
                    .workoutSessionDao().getSessionSummaries();
            requireActivity().runOnUiThread(() -> {
                adapter.setItems(list);
                updateUI(list);
            });
        });
    }

    private void updateUI(List<SessionSummary> list) {
        if (list == null || list.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvHistory.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }
}
