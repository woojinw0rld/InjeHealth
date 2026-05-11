package com.example.injehealth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.WorkoutLog;
import com.example.injehealth.db.entity.WorkoutSession;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 운동 완료 화면
 * - 운동 시간, 종목 수, 완료 세트 수 요약 표시
 * - 카메라/갤러리로 오늘의 눈바디 사진 등록
 * - 저장 또는 건너뛰기로 홈으로 이동
 */
public class WorkoutDoneActivity extends AppCompatActivity {

    // ─────────────────────────────────────────
    // 멤버 변수
    // ─────────────────────────────────────────
    private int sessionId;          // 완료된 운동 세션 ID
    private String photoPath = null; // 저장된 사진 경로 (null이면 사진 미선택)
    private Uri cameraUri = null;    // 카메라 촬영 결과 Uri (FileProvider 경로)
    private ImageView ivPhoto;       // 사진 미리보기 ImageView

    // ─────────────────────────────────────────
    // cameraLauncher: 카메라 촬영 결과 처리
    // 촬영 성공 시 ivPhoto에 사진 표시
    // ─────────────────────────────────────────
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && photoPath != null) {
                    ivPhoto.setImageURI(null);
                    // 알아서 크기 조절, 회전 보정, 백그라운드 로딩, 캐싱
                    Glide.with(this)
                            .load(new File(photoPath))
                            .into(ivPhoto);
                }
            }
    );

    // ─────────────────────────────────────────
    // galleryLauncher: 갤러리 선택 결과 처리
    // 선택한 이미지를 내부 저장소에 복사 후 ivPhoto에 표시
    // ─────────────────────────────────────────
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selected = result.getData().getData();
                    if (selected != null) {
                        // 외부 Uri는 앱 종료 후 접근 불가 → 내부 저장소에 복사
                        String saved = copyToInternalStorage(selected);
                        if (saved != null) {
                            photoPath = saved;
                            // 알아서 크기 조절, 회전 보정, 백그라운드 로딩, 캐싱
                            Glide.with(this)
                                    .load(new File(photoPath))
                                    .into(ivPhoto);
                        }
                    }
                }
            }
    );

    // ─────────────────────────────────────────
    // onCreate: 화면 초기화
    // ─────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_done);

        // 이전 화면(WorkoutCheckActivity)에서 넘어온 세션 ID 수신
        sessionId = getIntent().getIntExtra(RoutineSetupActivity.EXTRA_SESSION_ID, -1);
        ivPhoto = findViewById(R.id.iv_photo);

        // DB에서 운동 요약 데이터 로드
        loadSummary();

        // 버튼 클릭 이벤트 연결
        findViewById(R.id.btn_camera).setOnClickListener(v -> openCamera());   // 카메라 촬영
        findViewById(R.id.btn_gallery).setOnClickListener(v -> openGallery()); // 갤러리 선택
        findViewById(R.id.btn_skip).setOnClickListener(v -> goHome());          // 사진 없이 홈으로
        findViewById(R.id.btn_save).setOnClickListener(v -> saveAndGoHome());   // 사진 저장 후 홈으로
    }

    // ─────────────────────────────────────────
    // loadSummary: DB에서 운동 요약 정보 불러와 화면에 표시
    // 운동 시간 / 종목 수 / 완료 세트 수
    // ─────────────────────────────────────────
    private void loadSummary() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            WorkoutSession session = db.workoutSessionDao().getById(sessionId);
            List<WorkoutLog> logs  = db.workoutLogDao().getBySessionId(sessionId);

            // 세션이 없으면 홈으로 강제 이동
            if (session == null) { runOnUiThread(this::goHome); return; }

            // 운동 시간 계산 (created_at ~ done_at)
            String duration = "--";
            if (session.created_at != null && session.done_at != null) {
                LocalDateTime start = LocalDateTime.parse(session.created_at,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                LocalDateTime end = LocalDateTime.parse(session.done_at,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                long mins = ChronoUnit.MINUTES.between(start, end);
                duration = mins + "분";
            }

            // 종목 수: exercise_name 중복 제거 후 카운트
            long exCount  = logs.stream().map(l -> l.exercise_name).distinct().count();

            // 완료 세트 수: is_done == 1 인 것만 카운트
            long setCount = logs.stream().filter(l -> l.is_done == 1).count();

            final String d = duration;
            runOnUiThread(() -> {
                ((TextView) findViewById(R.id.tv_duration)).setText(d);
                ((TextView) findViewById(R.id.tv_exercise_count)).setText(exCount + "종목");
                ((TextView) findViewById(R.id.tv_set_count)).setText(setCount + "세트");
            });
        });
    }

    // ─────────────────────────────────────────
    // openCamera: 카메라 앱 실행
    // 권한 없으면 요청 후 리턴, 있으면 FileProvider로 Uri 생성 후 촬영
    // ─────────────────────────────────────────
    private void openCamera() {
        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
            return;
        }

        // 사진 저장할 파일 생성
        File file = createPhotoFile();
        if (file == null) return;

        // FileProvider를 통해 외부 앱(카메라)이 접근 가능한 Uri 생성
        cameraUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", file);
        photoPath = file.getAbsolutePath();

        // 카메라 앱 실행, 촬영 결과를 cameraUri 경로에 저장
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        cameraLauncher.launch(intent);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera(); // 권한 허용됐으면 바로 카메라 실행
        }
    }

    // ─────────────────────────────────────────
    // openGallery: 갤러리 앱 실행
    // 이미지 타입만 선택 가능하도록 필터 적용
    // ─────────────────────────────────────────
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*"); // 이미지 파일만 선택 가능
        galleryLauncher.launch(intent);
    }

    // ─────────────────────────────────────────
    // createPhotoFile: 내부 저장소에 사진 파일 생성
    // /files/photos/eyebody_{타임스탬프}.jpg 형태로 저장
    // ─────────────────────────────────────────
    private File createPhotoFile() {
        File dir = new File(getFilesDir(), "photos");
        if (!dir.exists()) dir.mkdirs(); // 디렉토리 없으면 생성
        return new File(dir, "eyebody_" + System.currentTimeMillis() + ".jpg");
    }

    // ─────────────────────────────────────────
    // copyToInternalStorage: 갤러리 Uri → 내부 저장소 복사
    // 외부 Uri는 앱 재실행 후 접근 불가하므로 내부에 복사해서 보관
    // 성공 시 저장된 경로 반환, 실패 시 null 반환
    // ─────────────────────────────────────────
    private String copyToInternalStorage(Uri uri) {
        try {
            File dest = createPhotoFile();
            try (InputStream in   = getContentResolver().openInputStream(uri);
                 OutputStream out = Files.newOutputStream(dest.toPath())) {
                byte[] buf = new byte[4096]; // 4KB 단위로 읽고 씀
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            return dest.getAbsolutePath();
        } catch (IOException e) {
            return null; // 복사 실패
        }
    }

    // ─────────────────────────────────────────
    // saveAndGoHome: 사진 경로 DB에 저장 후 홈으로 이동
    // 사진 미선택 시 토스트 메시지 표시 후 리턴
    // ─────────────────────────────────────────
    private void saveAndGoHome() {
        // 사진 선택 안 했으면 저장 불가
        if (photoPath == null) {
            Toast.makeText(this, "사진을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            WorkoutSession session = db.workoutSessionDao().getById(sessionId);
            if (session != null) {
                // 세션에 사진 경로 업데이트
                session.photo_path = photoPath;
                db.workoutSessionDao().update(session);
            }
            runOnUiThread(this::goHome);
        });
    }

    // ─────────────────────────────────────────
    // goHome: 홈(MainActivity)으로 이동
    // 백스택 정리 후 이동 (뒤로가기 시 이 화면으로 안 돌아오게)
    // ─────────────────────────────────────────
    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        // CLEAR_TOP: MainActivity 위의 스택 전부 제거
        // SINGLE_TOP: MainActivity 인스턴스 재사용
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}