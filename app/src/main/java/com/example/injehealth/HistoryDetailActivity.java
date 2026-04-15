package com.example.injehealth;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.WorkoutLog;
import com.example.injehealth.db.entity.WorkoutSession;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 운동 기록 상세 화면 (FR-22)
 *
 * - 특정 세션의 운동 상세 정보 표시 (계획 vs 실제 비교)
 * - 통계: 운동 시간, 총 세트, 총 볼륨, 달성률
 * - 눈바디 사진 표시 (Glide)
 * - 종목별 세트 카드 동적 생성
 * - 달성률 기반 운동 분석 텍스트 제공
 */
public class HistoryDetailActivity extends AppCompatActivity {

    // 헤더 영역
    private TextView tvTitle, tvBadge, tvDate;

    // 통계 영역 (운동 시간 / 총 세트 / 총 볼륨 / 달성률)
    private TextView tvStatTime, tvStatSets, tvStatVolume, tvStatRate;

    // 눈바디 사진 영역
    private LinearLayout layoutPhoto;
    private ImageView ivPhoto;

    // 종목별 운동 상세 카드 컨테이너
    private LinearLayout layoutExercises;

    // 운동 분석 영역
    private LinearLayout layoutAnalysis;
    private TextView tvAnalysisText;

    // 뒤로가기 버튼
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_detail);

        // 시스템 바 영역 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Intent에서 session_id 추출 — 없으면 화면 종료
        int sessionId = getIntent().getIntExtra("session_id", -1);
        if (sessionId == -1) {
            finish();
            return;
        }

        initViews();

        btnBack.setOnClickListener(v -> finish());

        // 백그라운드에서 세션 + 로그 데이터 조회
        Executors.newSingleThreadExecutor().execute(() -> {
            WorkoutSession session = AppDatabase.getInstance(this).workoutSessionDao().getById(sessionId);
            List<WorkoutLog> logs = AppDatabase.getInstance(this).workoutLogDao().getBySession(sessionId);
            if (session != null) {
                runOnUiThread(() -> bindData(session, logs));
            }
        });
    }

    /** 뷰 바인딩 초기화 */
    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvBadge = findViewById(R.id.tv_badge);
        tvDate = findViewById(R.id.tv_date);

        tvStatTime = findViewById(R.id.tv_stat_time);
        tvStatSets = findViewById(R.id.tv_stat_sets);
        tvStatVolume = findViewById(R.id.tv_stat_volume);
        tvStatRate = findViewById(R.id.tv_stat_rate);

        layoutPhoto = findViewById(R.id.layout_photo);
        ivPhoto = findViewById(R.id.iv_photo);
        layoutExercises = findViewById(R.id.layout_exercises);
        layoutAnalysis = findViewById(R.id.layout_analysis);
        tvAnalysisText = findViewById(R.id.tv_analysis_text);
    }

    /**
     * 세션 + 로그 데이터를 UI에 바인딩
     *
     * @param session 해당 날짜의 운동 세션
     * @param logs    세션에 속한 세트별 운동 로그 목록
     */
    private void bindData(WorkoutSession session, List<WorkoutLog> logs) {
        // 헤더: 부위명 + 날짜
        tvTitle.setText(String.format("%s 운동", session.body_part));
        tvBadge.setText(session.body_part);
        tvDate.setText(formatDate(session.date));

        // 운동 시간 계산 (created_at ~ done_at 차이, 분 단위)
        long timeDiffMins = 0;
        if (session.created_at != null && session.done_at != null) {
            timeDiffMins = calculateTimeDiffMinutes(session.created_at, session.done_at);
        }
        tvStatTime.setText(String.format(Locale.getDefault(), "%d분", timeDiffMins));

        // 총 세트 수 = 로그 개수
        int totalSets = logs != null ? logs.size() : 0;
        tvStatSets.setText(String.valueOf(totalSets));

        // 볼륨 / 달성률 계산 + 종목별 그룹핑
        double totalVolume = 0;
        int plannedRepsTotal = 0;
        int actualRepsTotal = 0;

        // 종목명 기준으로 로그를 그룹핑 (LinkedHashMap → 삽입 순서 유지)
        // 예: {"벤치프레스" → [세트1, 세트2, 세트3], "인클라인 덤벨" → [세트1, 세트2]}
        Map<String, List<WorkoutLog>> groupedLogs = new LinkedHashMap<>();

        if (logs != null) {
            for (WorkoutLog log : logs) {
                // 볼륨 = 실제 횟수 × 무게 (세트마다 누적)
                totalVolume += (log.reps * log.weight);
                plannedRepsTotal += log.planned_reps;   // 계획 횟수 누적 (달성률 분모)
                actualRepsTotal += log.reps;             // 실제 횟수 누적 (달성률 분자)

                // 종목별 그룹핑 — 키가 없으면 빈 리스트 생성 후 로그 추가
                if (!groupedLogs.containsKey(log.exercise_name)) {
                    groupedLogs.put(log.exercise_name, new ArrayList<>());
                }
                groupedLogs.get(log.exercise_name).add(log);
            }
        }

        tvStatVolume.setText(formatVolume(totalVolume));

        // 달성률 = (실제 총 횟수 / 계획 총 횟수) × 100
        int completionRate = 0;
        if (plannedRepsTotal > 0) {
            completionRate = (int) (((double) actualRepsTotal / plannedRepsTotal) * 100);
        }
        tvStatRate.setText(String.format(Locale.getDefault(), "%d%%", completionRate));

        // 눈바디 사진: 경로 있으면 Glide로 로드, 없으면 숨김
        if (session.photo_path != null && !session.photo_path.isEmpty()) {
            layoutPhoto.setVisibility(View.VISIBLE);
            Glide.with(this).load(session.photo_path).into(ivPhoto);
        } else {
            layoutPhoto.setVisibility(View.GONE);
        }

        // 종목별 운동 카드 동적 생성
        buildExerciseCards(groupedLogs);

        // 달성률에 따른 분석 메시지
        if (completionRate >= 100) {
            tvAnalysisText.setText(String.format(getString(R.string.detail_analysis_perfect), completionRate));
        } else if (completionRate >= 90) {
            tvAnalysisText.setText(String.format(getString(R.string.detail_analysis_great), completionRate));
        } else {
            tvAnalysisText.setText(String.format(getString(R.string.detail_analysis_normal), completionRate));
        }
    }

    /**
     * 종목별 운동 카드를 동적으로 생성하여 layoutExercises에 추가
     *
     * 카드 구조:
     *   [종목명]
     *   세트 | 목표       | 실제       | 상태
     *    1   | 10회×60kg  | 10회×60kg  |  ✓
     *    2   | 10회×60kg  |  8회×60kg  |  ✗
     *   ──────────────────────────────────
     *   운동 볼륨                  1,200kg
     *
     * @param groupedLogs 종목명 → 로그 리스트 맵
     */
    private void buildExerciseCards(Map<String, List<WorkoutLog>> groupedLogs) {
        layoutExercises.removeAllViews();

        // 디자인 시스템 색상 로드
        int surfaceColor = ContextCompat.getColor(this, R.color.surface);
        int surfaceVariantColor = ContextCompat.getColor(this, R.color.surface_variant);
        int textPrimaryColor = ContextCompat.getColor(this, R.color.text_primary);
        int textSecondaryColor = ContextCompat.getColor(this, R.color.text_secondary);
        int dividerColor = ContextCompat.getColor(this, R.color.divider);
        int successColor = ContextCompat.getColor(this, R.color.success);
        int accentColor = ContextCompat.getColor(this, R.color.accent);

        // 그룹핑된 맵을 순회하며 종목별 카드 생성
        // entry.getKey() = 종목명, entry.getValue() = 해당 종목의 세트 로그 리스트
        for (Map.Entry<String, List<WorkoutLog>> entry : groupedLogs.entrySet()) {
            String exerciseName = entry.getKey();          // 예: "벤치프레스"
            List<WorkoutLog> exerciseLogs = entry.getValue(); // 예: [세트1, 세트2, 세트3]

            // ── 카드 컨테이너 ──
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dpToPx(16));
            card.setLayoutParams(cardParams);
            card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(surfaceColor);
            cardBg.setCornerRadius(dpToPx(12));
            cardBg.setStroke(dpToPx(1), dividerColor);
            card.setBackground(cardBg);

            // ── 종목명 타이틀 ──
            TextView tvExerciseTitle = new TextView(this);
            tvExerciseTitle.setText(exerciseName);
            tvExerciseTitle.setTextColor(textPrimaryColor);
            tvExerciseTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvExerciseTitle.setTypeface(null, Typeface.BOLD);
            tvExerciseTitle.setPadding(0, 0, 0, dpToPx(12));
            card.addView(tvExerciseTitle);

            // ── 테이블 헤더 (세트 / 목표 / 실제 / 상태) ──
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setPadding(0, 0, 0, dpToPx(8));

            headerRow.addView(createTableCell("세트", textSecondaryColor, 1));
            headerRow.addView(createTableCell("목표", textSecondaryColor, 2));
            headerRow.addView(createTableCell("실제", textSecondaryColor, 2));
            headerRow.addView(createTableCell("", textSecondaryColor, 1));
            card.addView(headerRow);

            double exerciseVolume = 0;

            // ── 세트별 행 ──
            for (WorkoutLog log : exerciseLogs) {
                exerciseVolume += (log.reps * log.weight);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dpToPx(4), 0, dpToPx(4));

                // 세트 번호 (원형 배경)
                TextView tvSetNum = new TextView(this);
                tvSetNum.setText(String.valueOf(log.set_number));
                tvSetNum.setTextColor(textSecondaryColor);
                tvSetNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                tvSetNum.setGravity(Gravity.CENTER);

                GradientDrawable circleBg = new GradientDrawable();
                circleBg.setShape(GradientDrawable.OVAL);
                circleBg.setColor(surfaceVariantColor);
                tvSetNum.setBackground(circleBg);

                LinearLayout.LayoutParams setNumParams = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
                setNumParams.weight = 0;
                setNumParams.gravity = Gravity.CENTER;

                LinearLayout setNumContainer = new LinearLayout(this);
                setNumContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                setNumContainer.setGravity(Gravity.CENTER);
                setNumContainer.addView(tvSetNum, setNumParams);

                // 목표 (계획 횟수 × 계획 무게)
                String plannedText = String.format(Locale.getDefault(), "%d회 × %.0fkg", log.planned_reps, log.planned_weight);
                TextView tvPlanned = createTableCell(plannedText, textPrimaryColor, 2);

                // 실제 (실제 횟수 × 실제 무게) — 달성 여부에 따라 색상 분기
                String actualText = String.format(Locale.getDefault(), "%d회 × %.0fkg", log.reps, log.weight);
                int actualColor = log.reps >= log.planned_reps ? successColor : accentColor;
                TextView tvActual = createTableCell(actualText, actualColor, 2);

                // 상태 아이콘 (✓ 또는 ✗)
                ImageView ivStatus = new ImageView(this);
                int iconRes = log.reps >= log.planned_reps ? R.drawable.ic_check : R.drawable.ic_close;
                ivStatus.setImageResource(iconRes);
                ivStatus.setColorFilter(actualColor);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
                iconParams.gravity = Gravity.CENTER;

                LinearLayout statusContainer = new LinearLayout(this);
                statusContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                statusContainer.setGravity(Gravity.CENTER);
                statusContainer.addView(ivStatus, iconParams);

                row.addView(setNumContainer);
                row.addView(tvPlanned);
                row.addView(tvActual);
                row.addView(statusContainer);
                card.addView(row);
            }

            // ── 구분선 ──
            View divider = new View(this);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
            dividerParams.setMargins(0, dpToPx(12), 0, dpToPx(12));
            divider.setLayoutParams(dividerParams);
            divider.setBackgroundColor(dividerColor);
            card.addView(divider);

            // ── 푸터: 종목별 볼륨 합계 ──
            LinearLayout footer = new LinearLayout(this);
            footer.setOrientation(LinearLayout.HORIZONTAL);
            footer.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvVolumeLabel = new TextView(this);
            tvVolumeLabel.setText("운동 볼륨");
            tvVolumeLabel.setTextColor(textSecondaryColor);
            tvVolumeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvVolumeLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            footer.addView(tvVolumeLabel);

            TextView tvVolumeValue = new TextView(this);
            tvVolumeValue.setText(formatVolume(exerciseVolume));
            tvVolumeValue.setTextColor(textPrimaryColor);
            tvVolumeValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvVolumeValue.setTypeface(null, Typeface.BOLD);
            footer.addView(tvVolumeValue);

            card.addView(footer);
            layoutExercises.addView(card);
        }
    }

    /**
     * 테이블 셀 생성 헬퍼
     *
     * @param text   셀 텍스트
     * @param color  텍스트 색상
     * @param weight layout_weight 비율
     */
    private TextView createTableCell(String text, int color, float weight) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        return tv;
    }

    /**
     * "yyyy-MM-dd" → "yyyy년 M월 d일" 변환
     */
    private String formatDate(String dateStr) {
        if (dateStr == null) return "";
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inFormat.parse(dateStr);
            SimpleDateFormat outFormat = new SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault());
            if (date != null) {
                return outFormat.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateStr;
    }

    /**
     * ISO 8601 시간 두 개의 차이를 분 단위로 계산
     *
     * @param createdAt 운동 시작 시간 (yyyy-MM-dd'T'HH:mm:ss)
     * @param doneAt    운동 종료 시간
     * @return 차이(분), 음수면 0 반환
     */
    private long calculateTimeDiffMinutes(String createdAt, String doneAt) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date start = format.parse(createdAt);
            Date end = format.parse(doneAt);
            if (start != null && end != null) {
                long diffMillis = end.getTime() - start.getTime();
                return Math.max(0, diffMillis / (1000 * 60));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 볼륨 값을 천 단위 구분 + "kg" 형식으로 포맷
     * 예: 4500.0 → "4,500kg"
     */
    private String formatVolume(double volume) {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(volume) + "kg";
    }

    /** dp → px 변환 */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
