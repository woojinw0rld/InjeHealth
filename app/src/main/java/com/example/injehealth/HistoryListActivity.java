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


/**
 * 운동 기록 목록 화면 (FR-19~21)
 *
 * - 날짜별 운동 세션을 RecyclerView로 표시
 * - 기록이 없을 경우 빈 상태(Empty State) UI 노출
 * - 아이템 클릭 시 HistoryDetailActivity로 이동
 */
public class HistoryListActivity extends AppCompatActivity {

    private RecyclerView rvHistory;       // 운동 기록 리스트
    private View layoutEmpty;             // 기록 없을 때 표시되는 빈 상태 레이아웃
    private TextView tvHistorySubtitle;   // "총 N개의 운동 기록" 서브타이틀
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_list);

        // 시스템 바(상태바, 네비게이션 바) 영역만큼 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvHistory = findViewById(R.id.rv_history);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvHistorySubtitle = findViewById(R.id.tv_history_subtitle);

        // 어댑터 초기화 — 아이템 클릭 시 상세 화면으로 session_id 전달
        adapter = new HistoryAdapter(session -> {
            Intent intent = new Intent(HistoryListActivity.this, HistoryDetailActivity.class);
            intent.putExtra("session_id", session.id);
            startActivity(intent);
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        loadHistoryData();
    }

    /**
     * 백그라운드 스레드에서 DB 조회 후 UI 갱신
     * WorkoutSession + WorkoutLog JOIN 쿼리로 요약 정보(SessionSummary) 획득
     */
    private void loadHistoryData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SessionSummary> list = AppDatabase.getInstance(this).workoutSessionDao().getSessionSummaries();
            runOnUiThread(() -> {
                adapter.setItems(list);
                updateUI(list);
            });
        });
    }

    /**
     * 조회 결과에 따라 리스트 / 빈 상태 전환
     * - 데이터 있음 → RecyclerView 표시
     * - 데이터 없음 → Empty State 표시
     */
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
