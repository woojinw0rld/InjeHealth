package com.example.injehealth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.User;
import com.example.injehealth.db.entity.WorkoutLog;
import com.example.injehealth.db.entity.WorkoutSession;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {



    /** 홈 화면 최상단 인삿말 */
    private TextView tvGreeting;
    /** 오늘 날짜 */
    private TextView tvDate;
    /** 이번 주 운동 횟수 */
    private TextView tvWeeklyCount;
    /** 연속 운동 일수 */
    private TextView tvStreakDays;
    /** 최근 운동 카드 (운동 기록 있을 때 표시) */
    private LinearLayout cardRecentWorkout;
    /** 최근 운동 없을 때 표시되는 안내 텍스트 */
    private TextView tvNoRecentWorkout;
    /** 최근 운동 부위 */
    private TextView tvRecentBodyPart;
    /** 최근 운동 날짜 */
    private TextView tvRecentDate;
    /** 최근 운동 소요 시간 */
    private TextView tvRecentDuration;
    /** 최근 운동 총 볼륨 */
    private TextView tvRecentVolume;
    /** 최근 운동 종목 목록 컨테이너 */
    private LinearLayout llRecentExercises;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,  @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupBodyPartChips(view);
        setDate();
        loadHomeData();
    }

    private void initViews(View view) {
        tvGreeting        = view.findViewById(R.id.tv_greeting);
        tvDate            = view.findViewById(R.id.tv_date);
        tvWeeklyCount     = view.findViewById(R.id.tv_weekly_count);
        tvStreakDays      = view.findViewById(R.id.tv_streak_days);
        cardRecentWorkout = view.findViewById(R.id.card_recent_workout);
        tvNoRecentWorkout = view.findViewById(R.id.tv_no_recent_workout);
        tvRecentBodyPart  = view.findViewById(R.id.tv_recent_body_part);
        tvRecentDate      = view.findViewById(R.id.tv_recent_date);
        tvRecentDuration  = view.findViewById(R.id.tv_recent_duration);
        tvRecentVolume    = view.findViewById(R.id.tv_recent_volume);
        llRecentExercises = view.findViewById(R.id.ll_recent_exercises);
    }

    private void setupBodyPartChips(View view) {
        int[] chipIds = {
                R.id.chip_chest, R.id.chip_back, R.id.chip_legs,
                R.id.chip_shoulders, R.id.chip_arms, R.id.chip_cardio
        };
        String[] parts = {"가슴", "등", "하체", "어깨", "팔", "유산소"};

        for (int i = 0; i < chipIds.length; i++) {
            String part = parts[i];
            view.findViewById(chipIds[i]).setOnClickListener(v ->
                    Toast.makeText(requireContext(), part + " 선택됨", Toast.LENGTH_SHORT).show());
        }
        /** bnt_exercise_catalog :: 전체 운동 리스트*/
        view.findViewById(R.id.btn_exercise_catalog).setOnClickListener(v ->
                ((MainActivity) requireActivity()).switchToTab(R.id.tab_exercise));
        /** btn_recent_detail :: 최근 운동기록 자세히 보기  추후 리팩토링 예정 mainactivity로 불러야함.*/
        view.findViewById(R.id.btn_recent_detail).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HistoryListActivity.class)));
    }

    // ── 인사말 / 날짜 ──────────────────────────────────────

    private void setGreeting(String name) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "좋은 아침이에요"
                        : hour < 18 ? "좋은 오후에요"
                        : "좋은 저녁이에요";
        tvGreeting.setText(greeting + ","+ name +"님! 👋");
    }

    private void setDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREAN);
        tvDate.setText(sdf.format(new Date()));
    }

    // ── DB 로드 ────────────────────────────────────────────
    private void loadHomeData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            User user = db.userDao().getUser();
            List<WorkoutSession> sessions = db.workoutSessionDao().getAll();

            WorkoutSession recent = sessions.isEmpty() ? null : sessions.get(0);
            List<WorkoutLog> recentLogs = recent != null ? db.workoutLogDao().getBySession(recent.id) : new ArrayList<>();

            int weekly = calcWeeklyCount(sessions);
            int streak = calcStreak(sessions);

            requireActivity().runOnUiThread(() -> {
                setGreeting(user != null ? user.name : "");
                tvWeeklyCount.setText(String.valueOf(weekly));
                tvStreakDays.setText(String.valueOf(streak));
                bindRecentWorkout(recent, recentLogs);
            });
        });
    }

    // ── 통계 계산 ──────────────────────────────────────────
    /**주간 운동 카운트*/
    private int calcWeeklyCount(List<WorkoutSession> sessions) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        String monday = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        int count = 0;
        for (WorkoutSession s : sessions) {
            if (s.date != null && s.date.compareTo(monday) >= 0) count++;
        }
        return count;
    }

    /**연속 운동 일수 카운트*/
    private int calcStreak(List<WorkoutSession> sessions) {
        if (sessions.isEmpty()) return 0;

        List<LocalDate> dates = new ArrayList<>();
        for (WorkoutSession s : sessions) {
            if (s.date == null) continue;
            LocalDate d = LocalDate.parse(s.date);
            if (!dates.contains(d)) dates.add(d);
        }
        if (dates.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        if (!dates.get(0).equals(today) && !dates.get(0).equals(today.minusDays(1))) return 0;

        int streak = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i - 1).minusDays(1).equals(dates.get(i))) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    // ── 최근 운동 카드 바인딩 ──────────────────────────────


    /**최근 운동 기록*/
    private void bindRecentWorkout(WorkoutSession session, List<WorkoutLog> logs) {
        if (session == null) {
            cardRecentWorkout.setVisibility(View.GONE);
            tvNoRecentWorkout.setVisibility(View.VISIBLE);
            return;
        }
        cardRecentWorkout.setVisibility(View.VISIBLE);
        tvNoRecentWorkout.setVisibility(View.GONE);

        tvRecentBodyPart.setText(session.body_part + " 운동");
        tvRecentDate.setText(formatDateKorean(session.date));
        tvRecentDuration.setText(calcDuration(session.created_at, session.done_at));
        tvRecentVolume.setText(formatVolume(logs));

        llRecentExercises.removeAllViews();
        Map<String, List<WorkoutLog>> grouped = groupByExercise(logs);
        int shown = 0;
        for (Map.Entry<String, List<WorkoutLog>> entry : grouped.entrySet()) {
            if (shown >= 3) break;
            llRecentExercises.addView(makeExerciseRow(entry.getKey(), entry.getValue()));
            shown++;
        }
    }

    private View makeExerciseRow(String name, List<WorkoutLog> logs) {
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = (int) (8 * dp);
        row.setLayoutParams(rowParams);

        TextView tvName = new TextView(requireContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvName.setText(name);
        tvName.setTextColor(0xFF9E9E9E);
        tvName.setTextSize(13f);

        TextView tvInfo = new TextView(requireContext());
        tvInfo.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvInfo.setText(summarizeLogs(logs));
        tvInfo.setTextColor(0xFFE0E0E0);
        tvInfo.setTextSize(13f);

        row.addView(tvName);
        row.addView(tvInfo);
        return row;
    }

    private String summarizeLogs(List<WorkoutLog> logs) {
        if (logs.isEmpty()) return "";
        double maxWeight = 0;
        int maxReps = 0;
        for (WorkoutLog l : logs) {
            if (l.weight > maxWeight) maxWeight = l.weight;
            if (l.reps > maxReps) maxReps = l.reps;
        }
        String wStr = maxWeight == (int) maxWeight
                ? String.valueOf((int) maxWeight) : String.valueOf(maxWeight);
        return wStr + "kg × " + maxReps + "회 × " + logs.size() + "세트";
    }


    /**운동 로그를 종목명 기준으로 묶음*/
    private Map<String, List<WorkoutLog>> groupByExercise(List<WorkoutLog> logs) {
        Map<String, List<WorkoutLog>> map = new LinkedHashMap<>();
        for (WorkoutLog l : logs) {
            if (!map.containsKey(l.exercise_name)) map.put(l.exercise_name, new ArrayList<>());
            map.get(l.exercise_name).add(l);
        }
        return map;
    }

    // ── 포맷 헬퍼 ─────────────────────────────────────────

    /**운동 시간 계산*/
    private String calcDuration(String createdAt, String doneAt) {
        if (createdAt == null || doneAt == null) return "-";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            long diffMs = sdf.parse(doneAt).getTime() - sdf.parse(createdAt).getTime();
            return (diffMs / 60000) + "분";
        } catch (ParseException e) {
            return "-";
        }
    }

    private String formatVolume(List<WorkoutLog> logs) {
        double total = 0;
        for (WorkoutLog l : logs) {
            if (l.is_done == 1) total += l.weight * l.reps;
        }
        return String.format(Locale.getDefault(), "%,.0fkg", total);
    }

    private String formatDateKorean(String dateStr) {
        if (dateStr == null) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN);
            return out.format(in.parse(dateStr));
        } catch (ParseException e) {
            return dateStr;
        }
    }
}
