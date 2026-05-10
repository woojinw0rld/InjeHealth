package com.example.injehealth;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.adapter.WorkoutLogAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.Routine;
import com.example.injehealth.db.entity.WorkoutLog;
import com.example.injehealth.db.entity.WorkoutSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 운동 진행 화면
 * - 루틴 기반으로 운동 세트 목록 표시
 * - 세트 완료 체크, 추가, 삭제 처리
 * - 휴식 타이머 종료 시 알림 발송
 * - 운동 종료 시 WorkoutDoneActivity로 이동
 */
public class WorkoutCheckActivity extends AppCompatActivity {

    // ─────────────────────────────────────────
    // 멤버 변수
    // ─────────────────────────────────────────
    private int sessionId;                                          // 현재 운동 세션 ID
    private WorkoutLogAdapter adapter;                             // 운동 로그 RecyclerView 어댑터
    private List<String> routines;                                 // 루틴 이름 목록 (미사용 가능성 있음)
    private List<String> exerciseNames = new ArrayList<>();        // 운동 이름 목록
    private Map<String, List<WorkoutLog>> groupedLogs             // 운동별 세트 로그 맵
            = new LinkedHashMap<>();
    private TextView tvSetsProgress;                               // "완료/전체 세트" 텍스트뷰
    private int totalSets     = 0;                                 // 전체 세트 수
    private int completedSets = 0;                                 // 완료된 세트 수
    private String routineName;                                    // 선택된 루틴 이름
    private RecyclerView rv;                                       // 운동 목록 RecyclerView

    // 알림 관련 상수
    private static final String CHANNEL_ID = "workout_channel";   // 알림 채널 ID
    private static final int    NOTIF_ID   = 1;                   // 알림 고유 ID
    private static final int    PERM_CODE  = 101;                 // 알림 권한 요청 코드

    // ─────────────────────────────────────────
    // onCreate: 화면 초기화
    // ─────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_check);

        // 이전 화면에서 넘어온 루틴 이름 수신
        routineName = getIntent().getStringExtra(RoutineSetupActivity.ROUTINE_NAME);

        // 뷰 바인딩
        tvSetsProgress = findViewById(R.id.tv_sets_progress);
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvBadge = findViewById(R.id.tv_body_part_badge);

        // 뒤로가기 버튼 → 중단 확인 다이얼로그
        findViewById(R.id.btn_back).setOnClickListener(v -> showExitConfirmDialog());

        // RecyclerView 설정
        rv = findViewById(R.id.rv_exercises);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // 운동 종료 버튼 → 종료 확인 다이얼로그
        Button btnFinish = findViewById(R.id.btn_finish_workout);
        btnFinish.setOnClickListener(v -> showFinishConfirmDialog());

        // 알림 초기화
        createNotificationChannel();
        requestNotificationPermission();

        // DB에서 운동 데이터 로드
        loadWorkoutData(tvTitle, tvBadge);
    }

    // ─────────────────────────────────────────
    // loadWorkoutData: DB에서 세션/루틴/로그 불러오기
    // 오늘 세션이 이미 있으면 재사용, 없으면 새로 생성
    // ─────────────────────────────────────────
    private void loadWorkoutData(TextView tvTitle, TextView tvBadge) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            // 오늘 날짜 + 루틴 이름으로 기존 세션 조회
            WorkoutSession existingSession = db.workoutSessionDao().getByRoutineNameAndDate(
                    routineName,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );

            if (existingSession != null) {
                // 이미 오늘 세션이 있으면 재사용 (알림 복귀 시 중복 생성 방지)
                sessionId = existingSession.id;
            } else {
                // 새 세션 생성 후 DB insert
                WorkoutSession session = new WorkoutSession();
                session.routine_name = routineName;
                session.created_at   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                session.date         = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                sessionId = (int) db.workoutSessionDao().insert(session);
            }

            // 루틴에 속한 운동 목록 조회
            List<Routine> myRoutines = db.routineDao().getByRoutineName(routineName);

            // 현재 세션의 기존 완료 로그 조회 (복귀 시 완료 상태 복원용)
            List<WorkoutLog> existingLogs = db.workoutLogDao().getBySessionId(sessionId);

            runOnUiThread(() -> {
                // 어댑터 초기화 및 RecyclerView에 연결
                setWorkoutlogAdapter();

                // 상단 제목/뱃지 설정
                tvTitle.setText(routineName + " 운동");
                tvBadge.setText(routineName);

                // 전체 세트 수 계산
                totalSets = myRoutines.stream().mapToInt(r -> r.default_sets).sum();

                // 기존 로그 중 완료된 세트 수 계산
                completedSets = (int) existingLogs.stream()
                        .filter(l -> l.is_done == 1).count();

                // 기존 로그 병합하여 완료 상태 유지한 채로 표시
                adapter.setRoutinesWithLogs(myRoutines, existingLogs);
                updateProgress();
            });
        });
    }

    // ─────────────────────────────────────────
    // updateProgress: 세트 진행 현황 텍스트 갱신
    // ─────────────────────────────────────────
    private void updateProgress() {
        tvSetsProgress.setText(completedSets + " / " + totalSets + " 세트 완료");
    }

    // ─────────────────────────────────────────
    // showFinishConfirmDialog: 운동 완료 확인 다이얼로그
    // ─────────────────────────────────────────
    private void showFinishConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("운동 완료")
                .setMessage("운동을 완료하시겠습니까?")
                .setPositiveButton("완료", (d, w) -> finishWorkout())
                .setNegativeButton("취소", null)
                .show();
    }

    // ─────────────────────────────────────────
    // showExitConfirmDialog: 운동 중단 확인 다이얼로그
    // 중단 선택 시 세션/로그 전부 삭제 후 화면 종료
    // ─────────────────────────────────────────
    private void showExitConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("운동 중단")
                .setMessage("운동을 중단하시겠습니까?")
                .setPositiveButton("중단", (d, w) -> {
                    //deleteSessionAndLogs(); // DB에서 이번 세션 기록 삭제
                    finish();
                })
                .setNegativeButton("계속", null)
                .show();
    }

    // ─────────────────────────────────────────
    // finishWorkout: 운동 정상 종료 처리
    // done_at 기록 후 WorkoutDoneActivity로 이동
    // ─────────────────────────────────────────
    private void finishWorkout() {
        // 진행 중인 휴식 타이머 전부 취소
        adapter.cancelAllTimers();

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            // 세션에 종료 시각 기록
            WorkoutSession session = db.workoutSessionDao().getById(sessionId);
            if (session != null) {
                session.done_at = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                db.workoutSessionDao().update(session);
            }

            runOnUiThread(() -> {
                // 운동 완료 화면으로 이동
                Intent intent = new Intent(this, WorkoutDoneActivity.class);
                intent.putExtra(RoutineSetupActivity.EXTRA_SESSION_ID, sessionId);
                startActivity(intent);
                finish();
            });
        });
    }

    // ─────────────────────────────────────────
    // deleteSessionAndLogs: 운동 중단 시 DB 기록 전부 삭제
    // ─────────────────────────────────────────
    private void deleteSessionAndLogs() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.workoutLogDao().deleteBySession(sessionId);   // 세트 로그 삭제
            db.workoutSessionDao().deleteById(sessionId);    // 세션 삭제
        });
    }

    // ─────────────────────────────────────────
    // setWorkoutlogAdapter: 어댑터 생성 및 이벤트 리스너 연결
    // ─────────────────────────────────────────
    private void setWorkoutlogAdapter() {
        adapter = new WorkoutLogAdapter(sessionId, new WorkoutLogAdapter.OnSetCheckedListener() {

            /** 세트 완료 체크: 완료 수 증가 + DB 저장 */
            @Override
            public void onSetChecked(WorkoutLog log) {
                completedSets++;
                updateProgress();
                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(WorkoutCheckActivity.this);
                    if (log.id == 0) {
                        // 아직 DB에 없는 세트 → insert
                        long id = db.workoutLogDao().insert(log);
                        log.id = (int) id;
                    } else {
                        // 이미 DB에 있는 세트 → update
                        db.workoutLogDao().update(log);
                    }
                });
            }

            /** 세트 추가: 전체 수 증가 + DB insert */
            @Override
            public void onSetAdded(WorkoutLog newLog) {
                totalSets++;
                updateProgress();
//                Executors.newSingleThreadExecutor().execute(() -> {
//                    long id = AppDatabase.getInstance(WorkoutCheckActivity.this)
//                            .workoutLogDao().insert(newLog);
//                    newLog.id = (int) id; // 이후 update 시 id 필요하므로 다시 세팅
//                });
            }

            /** 세트 삭제: 전체 수 감소 + DB delete */
            @Override
            public void onSetDeleted(WorkoutLog log) {
                totalSets--;
                updateProgress();
                if (log.id > 0) {
                    // DB에 저장된 세트만 삭제 (id가 0이면 아직 저장 안 된 것)
                    Executors.newSingleThreadExecutor().execute(() ->
                            AppDatabase.getInstance(WorkoutCheckActivity.this)
                                    .workoutLogDao().deleteById(log.id));
                }
            }

            @Override public int getTotalSets()     { return totalSets; }
            @Override public int getCompletedSets() { return completedSets; }

            /** 휴식 타이머 종료: 알림 발송 */
            @Override
            public void onRestTimerFinished() {
                showNotification();
            }
        });

        rv.setAdapter(adapter);

        // 시스템 뒤로가기 버튼 → 중단 확인 다이얼로그
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        showExitConfirmDialog();
                    }
                });
    }

    // ─────────────────────────────────────────
    // createNotificationChannel: 알림 채널 생성 (Android 8.0+ 필수)
    // 앱당 한 번만 등록하면 되며, 중복 등록해도 무시됨
    // ─────────────────────────────────────────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "운동 알림",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("휴식 타이머 종료 알림");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // ─────────────────────────────────────────
    // requestNotificationPermission: 알림 권한 요청 (Android 13+ 필수)
    // ─────────────────────────────────────────
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERM_CODE);
            }
        }
    }

    // ─────────────────────────────────────────
    // showNotification: 휴식 타이머 종료 알림 발송
    // 알림 클릭 시 현재 운동 화면으로 복귀
    // ─────────────────────────────────────────
    private void showNotification() {
        // 알림 클릭 시 기존 인스턴스를 앞으로 가져옴 (onCreate 재호출 없음)
        Intent intent = new Intent(this, WorkoutCheckActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(RoutineSetupActivity.ROUTINE_NAME, routineName);
        intent.putExtra(RoutineSetupActivity.EXTRA_SESSION_ID, sessionId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_target)
                .setContentTitle("인제헬스")
                .setContentText("휴식 끝! 다음 세트 시작하세요 💪")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // 알림 클릭 시 자동 삭제

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        // 권한 있을 때만 알림 발송
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            manager.notify(NOTIF_ID, builder.build());
        }
    }

    // ─────────────────────────────────────────
    // onNewIntent: singleTop 모드에서 알림 클릭으로 복귀할 때 호출
    // onCreate 재실행 없이 현재 화면 그대로 유지
    // ─────────────────────────────────────────
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 별도 처리 없음 — 현재 화면 상태 그대로 유지
    }
}