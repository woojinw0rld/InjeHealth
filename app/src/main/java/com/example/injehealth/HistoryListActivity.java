package com.example.injehealth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.adapter.HistoryAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.model.SessionSummary;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HistoryListActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private View layoutEmpty;
    private TextView tvHistorySubtitle;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvHistory = findViewById(R.id.rv_history);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvHistorySubtitle = findViewById(R.id.tv_history_subtitle);

        adapter = new HistoryAdapter(session -> {
            Intent intent = new Intent(HistoryListActivity.this, HistoryDetailActivity.class);
            intent.putExtra("session_id", session.id);
            startActivity(intent);
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        loadHistoryData();
    }

    private void loadHistoryData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SessionSummary> list = AppDatabase.getInstance(this).workoutSessionDao().getSessionSummaries();
            runOnUiThread(() -> {
                adapter.setItems(list);
                updateUI(list);
            });
        });
    }

    private void updateUI(List<SessionSummary> list) {
        if (list == null || list.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvHistorySubtitle.setText(String.format(Locale.getDefault(), getString(R.string.history_total_format), 0));
        } else {
            rvHistory.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            tvHistorySubtitle.setText(String.format(Locale.getDefault(), getString(R.string.history_total_format), list.size()));
        }
    }
}