package com.example.injehealth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.WorkoutLog;
import com.example.injehealth.db.entity.WorkoutSession;
import com.example.injehealth.util.SystemBarHelper;

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

public class WorkoutDoneActivity extends AppCompatActivity {

    private int sessionId;
    private String photoPath = null;
    private Uri cameraUri = null;
    private ImageView ivPhoto;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && cameraUri != null) {
                    ivPhoto.setImageURI(null);
                    ivPhoto.setImageURI(cameraUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selected = result.getData().getData();
                    if (selected != null) {
                        String saved = copyToInternalStorage(selected);
                        if (saved != null) {
                            photoPath = saved;
                            ivPhoto.setImageURI(Uri.fromFile(new File(saved)));
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_done);
        SystemBarHelper.applyPadding(this, R.id.main);

        sessionId = getIntent().getIntExtra(RoutineSetupActivity.EXTRA_SESSION_ID, -1);
        ivPhoto = findViewById(R.id.iv_photo);

        loadSummary();

        findViewById(R.id.btn_camera).setOnClickListener(v -> openCamera());
        findViewById(R.id.btn_gallery).setOnClickListener(v -> openGallery());
        findViewById(R.id.btn_skip).setOnClickListener(v -> goHome());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveAndGoHome());
    }

    private void loadSummary() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            WorkoutSession session = db.workoutSessionDao().getById(sessionId);
            List<WorkoutLog> logs = db.workoutLogDao().getBySession(sessionId);

            if (session == null) { runOnUiThread(this::goHome); return; }

            String duration = "--";
            if (session.created_at != null && session.done_at != null) {
                LocalDateTime start = LocalDateTime.parse(session.created_at,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                LocalDateTime end = LocalDateTime.parse(session.done_at,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                long mins = ChronoUnit.MINUTES.between(start, end);
                duration = mins + "분";
            }

            long exCount  = logs.stream().map(l -> l.exercise_name).distinct().count();
            long setCount = logs.stream().filter(l -> l.is_done == 1).count();

            final String d = duration;
            runOnUiThread(() -> {
                ((TextView) findViewById(R.id.tv_duration)).setText(d);
                ((TextView) findViewById(R.id.tv_exercise_count)).setText(exCount + "종목");
                ((TextView) findViewById(R.id.tv_set_count)).setText(setCount + "세트");
            });
        });
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
            return;
        }
        File file = createPhotoFile();
        if (file == null) return;
        cameraUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", file);
        photoPath = file.getAbsolutePath();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        cameraLauncher.launch(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private File createPhotoFile() {
        File dir = new File(getFilesDir(), "photos");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "eyebody_" + System.currentTimeMillis() + ".jpg");
    }

    private String copyToInternalStorage(Uri uri) {
        try {
            File dest = createPhotoFile();
            try (InputStream in  = getContentResolver().openInputStream(uri);
                 OutputStream out = Files.newOutputStream(dest.toPath())) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            return dest.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private void saveAndGoHome() {
        if (photoPath == null) {
            Toast.makeText(this, "사진을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            WorkoutSession session = db.workoutSessionDao().getById(sessionId);
            if (session != null) {
                session.photo_path = photoPath;
                db.workoutSessionDao().update(session);
            }
            runOnUiThread(this::goHome);
        });
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
