package com.example.injehealth;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.adapter.BodyRecordAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.BodyRecord;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Myinbody 화면 (FR-23~26)
 *
 * - 체중 / 근육량 / 체지방량 / 체지방률 4개 지표 입력 및 관리
 * - 탭 전환으로 원하는 지표의 그래프(MPAndroidChart) 시각화
 * - 통계 카드: 현재값, 전일 대비, 총 변화
 * - 기록 내역: RecyclerView (인라인 수정 / 삭제)
 * - 모든 데이터는 Room DB(body_records)에 로컬 저장
 */
public class MyinbodyActivity extends AppCompatActivity {

    // ── 선택 가능한 지표 탭 ──
    // 예: TAB_WEIGHT=0 → 체중 그래프, TAB_MUSCLE=1 → 근육량 그래프
    private static final int TAB_WEIGHT = 0;
    private static final int TAB_MUSCLE = 1;
    private static final int TAB_FAT_MASS = 2;
    private static final int TAB_FAT_RATE = 3;

    private int currentTab = TAB_WEIGHT; // 현재 선택된 탭

    // 탭 버튼 뷰 배열 (체중 / 근육량 / 체지방량 / 체지방률)
    private TextView[] tabViews;

    // 통계 카드
    private TextView tvStatCurrent, tvStatDaily, tvStatTotal;

    // MPAndroidChart 라인 차트
    private LineChart chart;

    // 입력 폼
    private EditText etWeight, etMuscle, etFatMass, etFatRate;
    private TextView tvTodayDate;

    // 기록 리스트
    private RecyclerView rvRecords;
    private View layoutEmpty;
    private BodyRecordAdapter adapter;

    // DB에서 조회한 전체 기록 (날짜 오름차순)
    // 예: [{date:"2026-04-01", weight:75.0, ...}, {date:"2026-04-02", weight:74.8, ...}]
    private List<BodyRecord> allRecords = new ArrayList<>();

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_myinbody);

        // 시스템 바 영역 패딩 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupTabs();
        setupChart();
        setupRecyclerView();

        // 오늘 날짜 표시
        tvTodayDate.setText(new SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault()).format(new Date()));

        // 추가 버튼
        findViewById(R.id.btn_add).setOnClickListener(v -> addRecord());

        // DB에서 기록 로드
        loadRecords();
    }

    /** 뷰 바인딩 초기화 */
    private void initViews() {
        tvStatCurrent = findViewById(R.id.tv_stat_current);
        tvStatDaily = findViewById(R.id.tv_stat_daily);
        tvStatTotal = findViewById(R.id.tv_stat_total);
        chart = findViewById(R.id.chart);

        etWeight = findViewById(R.id.et_weight);
        etMuscle = findViewById(R.id.et_muscle);
        etFatMass = findViewById(R.id.et_fat_mass);
        etFatRate = findViewById(R.id.et_fat_rate);
        tvTodayDate = findViewById(R.id.tv_today_date);

        rvRecords = findViewById(R.id.rv_records);
        layoutEmpty = findViewById(R.id.layout_empty);
    }

    /**
     * 지표 탭 초기화
     * - 4개 탭(체중/근육량/체지방량/체지방률) 클릭 리스너 설정
     * - 선택된 탭: primary 색상 + primary_20 배경
     * - 미선택 탭: text_secondary + surface_variant 배경
     */
    private void setupTabs() {
        tabViews = new TextView[]{
                findViewById(R.id.tab_weight),
                findViewById(R.id.tab_muscle),
                findViewById(R.id.tab_fat_mass),
                findViewById(R.id.tab_fat_rate)
        };

        for (int i = 0; i < tabViews.length; i++) {
            final int tabIndex = i;
            tabViews[i].setOnClickListener(v -> {
                currentTab = tabIndex;
                updateTabUI();
                updateChart();
                updateStats();
            });
        }
    }

    /** 탭 선택 상태 UI 갱신 */
    private void updateTabUI() {
        for (int i = 0; i < tabViews.length; i++) {
            if (i == currentTab) {
                // 선택된 탭: primary 색상 강조
                tabViews[i].setTextColor(ContextCompat.getColor(this, R.color.primary));
                tabViews[i].setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_20));
                tabViews[i].setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                // 미선택 탭: 기본 색상
                tabViews[i].setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                tabViews[i].setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.surface_variant));
                tabViews[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    /**
     * MPAndroidChart 초기 설정
     * - 다크 테마에 맞춰 배경/축/범례 색상 설정
     * - 터치 인터랙션 활성화, 범례 비활성화
     */
    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.getLegend().setEnabled(false);
        chart.setNoDataText("데이터가 없습니다");
        chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        // X축: 날짜 라벨 (하단)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // Y축 (좌): 수치
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        leftAxis.setTextSize(10f);
        leftAxis.setGridColor(Color.parseColor("#0FE0E0E0")); // divider_subtle 색상
        leftAxis.setDrawAxisLine(false);

        // Y축 (우): 비활성화
        chart.getAxisRight().setEnabled(false);
    }

    /** RecyclerView + 어댑터 초기화 */
    private void setupRecyclerView() {
        adapter = new BodyRecordAdapter(new BodyRecordAdapter.OnRecordActionListener() {
            @Override
            public void onUpdate(BodyRecord record) {
                // 백그라운드에서 DB 업데이트 → UI 갱신
                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase.getInstance(MyinbodyActivity.this).bodyRecordDao().update(record);
                    runOnUiThread(() -> loadRecords());
                });
            }

            @Override
            public void onDelete(BodyRecord record) {
                // 백그라운드에서 DB 삭제 → UI 갱신
                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase.getInstance(MyinbodyActivity.this).bodyRecordDao().delete(record);
                    runOnUiThread(() -> loadRecords());
                });
            }
        });

        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        rvRecords.setAdapter(adapter);
    }

    /**
     * 오늘 기록 추가
     * - 체중 필수, 나머지 선택
     * - 같은 날짜 기록이 있으면 덮어쓰기(update)
     */
    private void addRecord() {
        String weightStr = etWeight.getText().toString().trim();
        if (weightStr.isEmpty()) {
            Toast.makeText(this, "체중을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        BodyRecord record = new BodyRecord();
        record.date = dateFormat.format(new Date());
        record.weight = parseDouble(weightStr);
        record.muscle_mass = parseDouble(etMuscle.getText().toString().trim());
        record.body_fat_mass = parseDouble(etFatMass.getText().toString().trim());
        record.body_fat_rate = parseDouble(etFatRate.getText().toString().trim());

        Executors.newSingleThreadExecutor().execute(() -> {
            // 같은 날짜 기록이 이미 있으면 업데이트, 없으면 새로 삽입
            BodyRecord existing = AppDatabase.getInstance(this).bodyRecordDao().getByDate(record.date);
            if (existing != null) {
                record.id = existing.id;
                AppDatabase.getInstance(this).bodyRecordDao().update(record);
            } else {
                AppDatabase.getInstance(this).bodyRecordDao().insert(record);
            }

            runOnUiThread(() -> {
                // 입력 필드 초기화
                etWeight.setText("");
                etMuscle.setText("");
                etFatMass.setText("");
                etFatRate.setText("");
                Toast.makeText(this, "기록이 추가되었습니다", Toast.LENGTH_SHORT).show();
                loadRecords();
            });
        });
    }

    /**
     * DB에서 전체 기록 로드 → 차트/통계/리스트 갱신
     * - allRecords: 날짜 오름차순 (차트용)
     * - 어댑터에는 역순(최신 먼저) 전달
     */
    private void loadRecords() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // getAll()은 날짜 DESC → 차트용으로 뒤집어서 ASC로 보관
            List<BodyRecord> descList = AppDatabase.getInstance(this).bodyRecordDao().getAll();
            List<BodyRecord> ascList = new ArrayList<>(descList);
            Collections.reverse(ascList); // 오름차순 (오래된 것 → 최신)

            runOnUiThread(() -> {
                allRecords = ascList;
                adapter.setItems(descList); // 리스트는 최신 먼저
                updateEmptyState(descList);
                updateChart();
                updateStats();
            });
        });
    }

    /** 빈 상태 / 리스트 전환 */
    private void updateEmptyState(List<BodyRecord> list) {
        if (list == null || list.isEmpty()) {
            rvRecords.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvRecords.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    /**
     * 현재 선택된 탭 기준으로 MPAndroidChart 라인 차트 갱신
     *
     * allRecords(오름차순)를 순회하며:
     * - X축 인덱스 = 리스트 순서 (0, 1, 2, ...)
     * - X축 라벨 = "M/d" 형식 날짜
     * - Y축 값 = 선택된 지표 값 (체중/근육량/체지방량/체지방률)
     */
    private void updateChart() {
        if (allRecords.isEmpty()) {
            chart.clear();
            return;
        }

        // 차트 데이터 포인트 생성
        // entries: [(0, 75.0), (1, 74.8), (2, 75.2), ...]
        List<Entry> entries = new ArrayList<>();
        // labels: ["4/1", "4/2", "4/3", ...]
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < allRecords.size(); i++) {
            BodyRecord record = allRecords.get(i);
            // 현재 탭에 해당하는 값 추출
            float value = getValueForTab(record, currentTab);
            entries.add(new Entry(i, value));

            // 날짜 라벨: "yyyy-MM-dd" → "M/d"
            labels.add(formatShortDate(record.date));
        }

        // 라인 데이터셋 스타일 설정 (primary 블루 색상)
        LineDataSet dataSet = new LineDataSet(entries, "");
        int primaryColor = ContextCompat.getColor(this, R.color.primary);
        dataSet.setColor(primaryColor);
        dataSet.setCircleColor(primaryColor);
        dataSet.setCircleRadius(3f);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 부드러운 곡선

        // X축 라벨 설정 + 데이터 반영
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.setData(new LineData(dataSet));
        chart.invalidate(); // 차트 다시 그리기
    }

    /**
     * 통계 카드 갱신 (현재값 / 전일 대비 / 총 변화)
     *
     * allRecords 기준:
     * - 현재값 = 마지막 기록의 해당 지표
     * - 전일 대비 = 마지막 - 마지막에서 두번째
     * - 총 변화 = 마지막 - 첫번째
     */
    private void updateStats() {
        if (allRecords.isEmpty()) {
            tvStatCurrent.setText("—");
            tvStatDaily.setText("—");
            tvStatTotal.setText("—");
            return;
        }

        // 현재값 = allRecords 마지막 항목
        float current = getValueForTab(allRecords.get(allRecords.size() - 1), currentTab);
        tvStatCurrent.setText(formatStatValue(current));

        // 전일 대비 = 마지막 - (마지막-1)
        if (allRecords.size() >= 2) {
            float previous = getValueForTab(allRecords.get(allRecords.size() - 2), currentTab);
            float dailyChange = current - previous;
            tvStatDaily.setText(formatChangeValue(dailyChange));
            tvStatDaily.setTextColor(getChangeColor(dailyChange));
        } else {
            tvStatDaily.setText("—");
            tvStatDaily.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }

        // 총 변화 = 마지막 - 첫번째
        if (allRecords.size() >= 2) {
            float first = getValueForTab(allRecords.get(0), currentTab);
            float totalChange = current - first;
            tvStatTotal.setText(formatChangeValue(totalChange));
            tvStatTotal.setTextColor(getChangeColor(totalChange));
        } else {
            tvStatTotal.setText("—");
            tvStatTotal.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    /**
     * 현재 탭에 해당하는 지표 값 추출
     *
     * @param record  BodyRecord 데이터
     * @param tab     TAB_WEIGHT(0) / TAB_MUSCLE(1) / TAB_FAT_MASS(2) / TAB_FAT_RATE(3)
     * @return 해당 지표 float 값
     */
    private float getValueForTab(BodyRecord record, int tab) {
        switch (tab) {
            case TAB_MUSCLE:   return (float) record.muscle_mass;
            case TAB_FAT_MASS: return (float) record.body_fat_mass;
            case TAB_FAT_RATE: return (float) record.body_fat_rate;
            default:           return (float) record.weight;
        }
    }

    /**
     * "yyyy-MM-dd" → "M/d" 변환 (차트 X축 라벨용)
     * 예: "2026-04-15" → "4/15"
     */
    private String formatShortDate(String dateStr) {
        if (dateStr == null) return "";
        try {
            Date date = dateFormat.parse(dateStr);
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                return (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH);
            }
        } catch (Exception e) {
            // 파싱 실패 시 원본 반환
        }
        return dateStr;
    }

    /**
     * 수치 포맷: 단위 포함 (탭에 따라 kg 또는 %)
     * 예: 75.0 → "75.0kg", 20.3 → "20.3%"
     */
    private String formatStatValue(float value) {
        String unit = currentTab == TAB_FAT_RATE ? "%" : "kg";
        return String.format(Locale.getDefault(), "%.1f%s", value, unit);
    }

    /**
     * 변화량 포맷: 부호 + 단위
     * 예: +0.3 → "+0.3kg", -1.2 → "-1.2%"
     */
    private String formatChangeValue(float change) {
        String unit = currentTab == TAB_FAT_RATE ? "%" : "kg";
        String sign = change > 0 ? "+" : "";
        return String.format(Locale.getDefault(), "%s%.1f%s", sign, change, unit);
    }

    /**
     * 변화량에 따른 색상 반환
     * - 양수(증가): accent 오렌지 — 체중/체지방 증가는 부정적 의미
     * - 음수(감소): success 그린 — 체중/체지방 감소는 긍정적 의미
     * - 0(변화 없음): text_primary 기본 색상
     */
    private int getChangeColor(float change) {
        if (change > 0) return ContextCompat.getColor(this, R.color.accent);
        if (change < 0) return ContextCompat.getColor(this, R.color.success);
        return ContextCompat.getColor(this, R.color.text_primary);
    }

    /**
     * 문자열 → double 파싱 (빈 문자열이면 0 반환)
     */
    private double parseDouble(String text) {
        if (text == null || text.isEmpty()) return 0;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
