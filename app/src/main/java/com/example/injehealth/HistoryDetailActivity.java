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

public class HistoryDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvBadge, tvDate;
    private TextView tvStatTime, tvStatSets, tvStatVolume, tvStatRate;
    private LinearLayout layoutPhoto, layoutExercises, layoutAnalysis;
    private ImageView ivPhoto, btnBack;
    private TextView tvAnalysisText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int sessionId = getIntent().getIntExtra("session_id", -1);
        if (sessionId == -1) {
            finish();
            return;
        }

        initViews();

        btnBack.setOnClickListener(v -> finish());

        Executors.newSingleThreadExecutor().execute(() -> {
            WorkoutSession session = AppDatabase.getInstance(this).workoutSessionDao().getById(sessionId);
            List<WorkoutLog> logs = AppDatabase.getInstance(this).workoutLogDao().getBySession(sessionId);
            if (session != null) {
                runOnUiThread(() -> bindData(session, logs));
            }
        });
    }

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

    private void bindData(WorkoutSession session, List<WorkoutLog> logs) {
        tvTitle.setText(String.format("%s 운동", session.body_part));
        tvBadge.setText(session.body_part);
        tvDate.setText(formatDate(session.date));

        long timeDiffMins = 0;
        if (session.created_at != null && session.done_at != null) {
            timeDiffMins = calculateTimeDiffMinutes(session.created_at, session.done_at);
        }
        tvStatTime.setText(String.format(Locale.getDefault(), "%d분", timeDiffMins));

        int totalSets = logs != null ? logs.size() : 0;
        tvStatSets.setText(String.valueOf(totalSets));

        double totalVolume = 0;
        int plannedRepsTotal = 0;
        int actualRepsTotal = 0;

        Map<String, List<WorkoutLog>> groupedLogs = new LinkedHashMap<>();

        if (logs != null) {
            for (WorkoutLog log : logs) {
                totalVolume += (log.reps * log.weight);
                plannedRepsTotal += log.planned_reps;
                actualRepsTotal += log.reps;

                if (!groupedLogs.containsKey(log.exercise_name)) {
                    groupedLogs.put(log.exercise_name, new ArrayList<>());
                }
                groupedLogs.get(log.exercise_name).add(log);
            }
        }

        tvStatVolume.setText(formatVolume(totalVolume));

        int completionRate = 0;
        if (plannedRepsTotal > 0) {
            completionRate = (int) (((double) actualRepsTotal / plannedRepsTotal) * 100);
        }
        tvStatRate.setText(String.format(Locale.getDefault(), "%d%%", completionRate));

        if (session.photo_path != null && !session.photo_path.isEmpty()) {
            layoutPhoto.setVisibility(View.VISIBLE);
            Glide.with(this).load(session.photo_path).into(ivPhoto);
        } else {
            layoutPhoto.setVisibility(View.GONE);
        }

        buildExerciseCards(groupedLogs);

        if (completionRate >= 100) {
            tvAnalysisText.setText(String.format(getString(R.string.detail_analysis_perfect), completionRate));
        } else if (completionRate >= 90) {
            tvAnalysisText.setText(String.format(getString(R.string.detail_analysis_great), completionRate));
        } else {
            tvAnalysisText.setText(String.format(getString(R.string.detail_analysis_normal), completionRate));
        }
    }

    private void buildExerciseCards(Map<String, List<WorkoutLog>> groupedLogs) {
        layoutExercises.removeAllViews();

        int surfaceColor = ContextCompat.getColor(this, R.color.surface);
        int surfaceVariantColor = ContextCompat.getColor(this, R.color.surface_variant);
        int textPrimaryColor = ContextCompat.getColor(this, R.color.text_primary);
        int textSecondaryColor = ContextCompat.getColor(this, R.color.text_secondary);
        int dividerColor = ContextCompat.getColor(this, R.color.divider);
        int successColor = ContextCompat.getColor(this, R.color.success);
        int accentColor = ContextCompat.getColor(this, R.color.accent);

        for (Map.Entry<String, List<WorkoutLog>> entry : groupedLogs.entrySet()) {
            String exerciseName = entry.getKey();
            List<WorkoutLog> exerciseLogs = entry.getValue();

            // Card Container
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

            // Title
            TextView tvExerciseTitle = new TextView(this);
            tvExerciseTitle.setText(exerciseName);
            tvExerciseTitle.setTextColor(textPrimaryColor);
            tvExerciseTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvExerciseTitle.setTypeface(null, Typeface.BOLD);
            tvExerciseTitle.setPadding(0, 0, 0, dpToPx(12));
            card.addView(tvExerciseTitle);

            // Table Header
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setPadding(0, 0, 0, dpToPx(8));

            TextView tvHeaderSet = createTableCell("세트", textSecondaryColor, 1);
            TextView tvHeaderPlanned = createTableCell("목표", textSecondaryColor, 2);
            TextView tvHeaderActual = createTableCell("실제", textSecondaryColor, 2);
            TextView tvHeaderStatus = createTableCell("", textSecondaryColor, 1);

            headerRow.addView(tvHeaderSet);
            headerRow.addView(tvHeaderPlanned);
            headerRow.addView(tvHeaderActual);
            headerRow.addView(tvHeaderStatus);
            card.addView(headerRow);

            double exerciseVolume = 0;

            // Set Rows
            for (WorkoutLog log : exerciseLogs) {
                exerciseVolume += (log.reps * log.weight);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dpToPx(4), 0, dpToPx(4));

                // Set Number Circle
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

                // Planned
                String plannedText = String.format(Locale.getDefault(), "%d회 × %.0fkg", log.planned_reps, log.planned_weight);
                TextView tvPlanned = createTableCell(plannedText, textPrimaryColor, 2);

                // Actual
                String actualText = String.format(Locale.getDefault(), "%d회 × %.0fkg", log.reps, log.weight);
                int actualColor = log.reps >= log.planned_reps ? successColor : accentColor;
                TextView tvActual = createTableCell(actualText, actualColor, 2);

                // Status Icon
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

            // Divider
            View divider = new View(this);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
            dividerParams.setMargins(0, dpToPx(12), 0, dpToPx(12));
            divider.setLayoutParams(dividerParams);
            divider.setBackgroundColor(dividerColor);
            card.addView(divider);

            // Footer (Volume)
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

    private TextView createTableCell(String text, int color, float weight) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        return tv;
    }

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

    private String formatVolume(double volume) {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(volume) + "kg";
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
