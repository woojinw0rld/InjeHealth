package com.example.injehealth;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.TimePickerDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.DietItem;
import com.example.injehealth.db.entity.DietLog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AddMealSheet extends BottomSheetDialogFragment {

    private String date;
    private Calendar selectedTime;
    private TextView tvSelectedTime;
    private LinearLayout containerFoodItems;
    private String photoPath = null;   // 내부 저장소 절대경로 (null이면 미선택)
    private Uri cameraUri = null;      // 카메라 촬영용 FileProvider Uri
    private ImageView ivPhotoPreview;
    private TextView btnPhotoRemove;

    // ─────────────────────────────────────────
    // cameraLauncher: 카메라 촬영 결과 처리
    // ─────────────────────────────────────────
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && photoPath != null) {
                    showPhoto(new File(photoPath));
                }
            }
    );

    // ─────────────────────────────────────────
    // galleryLauncher: 갤러리 선택 결과 처리
    // 선택한 Uri → 내부 저장소 복사 후 표시
    // ─────────────────────────────────────────
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK
                        && result.getData() != null) {
                    Uri selected = result.getData().getData();
                    if (selected != null) {
                        String saved = copyToInternalStorage(selected);
                        if (saved != null) {
                            photoPath = saved;
                            showPhoto(new File(photoPath));
                        }
                    }
                }
            }
    );

    public AddMealSheet() { }

    public static AddMealSheet newInstance(String date) {
        AddMealSheet sheet = new AddMealSheet();
        Bundle args = new Bundle();
        args.putString("date", date);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_meal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        date = getArguments() != null ? getArguments().getString("date") : null;
        if (date == null) {
            date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        selectedTime     = Calendar.getInstance();
        tvSelectedTime   = view.findViewById(R.id.tvSelectedTime);
        containerFoodItems = view.findViewById(R.id.containerFoodItems);
        ivPhotoPreview   = view.findViewById(R.id.ivPhotoPreview);
        btnPhotoRemove   = view.findViewById(R.id.btnPhotoRemove);

        // 회전 등 세션 내 상태 복원
        if (savedInstanceState != null) {
            photoPath = savedInstanceState.getString("photo_path");
            if (photoPath != null) {
                showPhoto(new File(photoPath));
            }
        }

        updateTimeDisplay();

        // 시간 변경
        view.findViewById(R.id.btnChangeTime).setOnClickListener(v ->
            new TimePickerDialog(requireContext(),
                    (tp, hour, minute) -> {
                        selectedTime.set(Calendar.HOUR_OF_DAY, hour);
                        selectedTime.set(Calendar.MINUTE, minute);
                        updateTimeDisplay();
                    },
                    selectedTime.get(Calendar.HOUR_OF_DAY),
                    selectedTime.get(Calendar.MINUTE),
                    true
            ).show()
        );

        // 카메라
        view.findViewById(R.id.btnPhotoCamera).setOnClickListener(v -> openCamera());
        // 갤러리
        view.findViewById(R.id.btnPhotoGallery).setOnClickListener(v -> openGallery());
        // 사진 제거
        btnPhotoRemove.setOnClickListener(v -> {
            photoPath = null;
            cameraUri = null;
            ivPhotoPreview.setVisibility(View.GONE);
            btnPhotoRemove.setVisibility(View.GONE);
            view.findViewById(R.id.llPhotoHint).setVisibility(View.VISIBLE);
        });

        // 음식 행 — 첫 행 자동 추가
        addFoodRow();
        view.findViewById(R.id.btnAddFood).setOnClickListener(v -> addFoodRow());

        // 닫기 / 취소
        view.findViewById(R.id.ivClose).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());

        // 저장
        view.findViewById(R.id.btnSave).setOnClickListener(v -> saveMeal(view));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoPath != null) outState.putString("photo_path", photoPath);
    }

    // ─────────────────────────────────────────
    // openCamera: 카메라 실행 (권한 확인 → FileProvider Uri 생성 → 촬영)
    // ─────────────────────────────────────────
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, 200);
            return;
        }
        File file = createPhotoFile();
        if (file == null) return;

        cameraUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", file);
        photoPath = file.getAbsolutePath();

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        cameraLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    // ─────────────────────────────────────────
    // openGallery: 갤러리 실행
    // ─────────────────────────────────────────
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // ─────────────────────────────────────────
    // createPhotoFile: 앱 내부 저장소에 사진 파일 생성
    // /files/meal_photos/meal_<timestamp>.jpg
    // ─────────────────────────────────────────
    private File createPhotoFile() {
        File dir = new File(requireContext().getFilesDir(), "meal_photos");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "meal_" + System.currentTimeMillis() + ".jpg");
    }

    // ─────────────────────────────────────────
    // copyToInternalStorage: 갤러리 Uri → 내부 저장소 복사
    // 외부 Uri는 앱 재시작 후 접근 불가 → 내부 복사 필수
    // ─────────────────────────────────────────
    private String copyToInternalStorage(Uri uri) {
        try {
            File dest = createPhotoFile();
            try (InputStream in  = requireContext().getContentResolver().openInputStream(uri);
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

    // ─────────────────────────────────────────
    // showPhoto: 사진 미리보기 표시 + 힌트 숨김
    // ─────────────────────────────────────────
    private void showPhoto(File file) {
        if (!isAdded()) return;
        Glide.with(this).load(file).fitCenter().into(ivPhotoPreview);
        ivPhotoPreview.setVisibility(View.VISIBLE);
        btnPhotoRemove.setVisibility(View.VISIBLE);
        View root = getView();
        if (root != null) root.findViewById(R.id.llPhotoHint).setVisibility(View.GONE);
    }

    private void updateTimeDisplay() {
        String time = String.format(Locale.getDefault(), "%02d:%02d",
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE));
        tvSelectedTime.setText(time);
    }

    private void addFoodRow() {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_food_input, containerFoodItems, false);
        containerFoodItems.addView(row);
    }

    private void saveMeal(View rootView) {
        EditText etMemo = rootView.findViewById(R.id.etMemo);
        String memo = etMemo.getText().toString().trim();

        // 음식 행 수집
        List<DietItem> foodItems = new ArrayList<>();
        for (int i = 0; i < containerFoodItems.getChildCount(); i++) {
            View row = containerFoodItems.getChildAt(i);
            EditText etName  = row.findViewById(R.id.etFoodName);
            EditText etKcal  = row.findViewById(R.id.etKcal);
            EditText etCarbs = row.findViewById(R.id.etCarbs);
            EditText etProt  = row.findViewById(R.id.etProtein);
            EditText etFat   = row.findViewById(R.id.etFat);

            String name = etName.getText().toString().trim();
            if (name.isEmpty()) continue;

            DietItem item = new DietItem();
            item.food_name = name;
            item.amount    = 0;
            item.unit      = "g";
            try { item.kcal    = Double.parseDouble(etKcal.getText().toString()); } catch (NumberFormatException e) { item.kcal = 0; }
            try { item.carbs   = Double.parseDouble(etCarbs.getText().toString()); } catch (NumberFormatException e) { item.carbs = 0; }
            try { item.protein = Double.parseDouble(etProt.getText().toString()); } catch (NumberFormatException e) { item.protein = 0; }
            try { item.fat     = Double.parseDouble(etFat.getText().toString()); } catch (NumberFormatException e) { item.fat = 0; }
            foodItems.add(item);
        }

        if (foodItems.isEmpty()) {
            Toast.makeText(requireContext(), R.string.diet_add_error_no_food, Toast.LENGTH_SHORT).show();
            return;
        }

        // eaten_at 조합
        String eatenAt = date + " " + String.format(Locale.getDefault(), "%02d:%02d",
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE));

        DietLog log = new DietLog();
        log.eatenAt   = eatenAt;
        log.memo      = memo.isEmpty() ? null : memo;
        log.photoPath = photoPath; // 내부 저장소 절대경로 or null

        Context appContext = requireContext().getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        Executors.newSingleThreadExecutor().execute(() -> {
            db.runInTransaction(() -> {
                long logId = db.dietLogDao().insert(log);
                for (DietItem item : foodItems) {
                    item.log_id = (int) logId;
                    db.dietItemDao().insert(item);
                }
            });
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;
                Activity activity = getActivity();
                if (activity instanceof DietDayActivity) {
                    ((DietDayActivity) activity).loadData();
                }
                dismiss();
            });
        });
    }
}
