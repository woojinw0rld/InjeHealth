package com.example.injehealth;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.Exercise;
import com.example.injehealth.util.BodyPartLabels;
import com.example.injehealth.util.PhotoFileHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class AddCustomExerciseSheet extends BottomSheetDialogFragment {

    private Uri selectedImageUri = null;
    private String selectedBodyPart = null;

    private ImageView ivPreview;
    private LinearLayout llUploadHint;
    private EditText etExerciseName;
    private Button btnAdd;

    // 부위 버튼 목록
    private final List<TextView> bodyPartBtns = new ArrayList<>();
    private final String[] bodyPartKeys = {"chest", "back", "legs", "shoulders", "arms", "cardio"};

    // ─────────────────────────────────────────
    // galleryLauncher: 갤러리 선택 결과 처리
    // ─────────────────────────────────────────
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK
                        && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        selectedImageUri = uri;
                        llUploadHint.setVisibility(View.GONE);
                        ivPreview.setVisibility(View.VISIBLE);
                        Glide.with(this).load(uri).centerCrop().into(ivPreview);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_custom_exercise, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivPreview      = view.findViewById(R.id.ivPreview);
        llUploadHint   = view.findViewById(R.id.llUploadHint);
        etExerciseName = view.findViewById(R.id.etExerciseName);
        btnAdd         = view.findViewById(R.id.btnAdd);
        Button btnCancel  = view.findViewById(R.id.btnCancel);
        ImageView ivClose = view.findViewById(R.id.ivClose);
        View flImageUpload = view.findViewById(R.id.flImageUpload);

        // 닫기
        ivClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());

        // 이미지 업로드 영역 클릭 → 갤러리 실행
        flImageUpload.setOnClickListener(v -> openGallery());

        // 부위 버튼 설정
        int[] btnIds = {R.id.btnChest, R.id.btnBack, R.id.btnLegs,
                        R.id.btnShoulders, R.id.btnArms, R.id.btnCardio};
        for (int i = 0; i < btnIds.length; i++) {
            TextView btn = view.findViewById(btnIds[i]);
            bodyPartBtns.add(btn);
            final int idx = i;
            btn.setOnClickListener(v -> selectBodyPart(idx));
        }

        // 추가하기 버튼
        btnAdd.setOnClickListener(v -> onAddClicked());
    }

    /** 갤러리 실행 */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    /** 부위 선택 상태 갱신 */
    private void selectBodyPart(int idx) {
        selectedBodyPart = bodyPartKeys[idx];
        for (int i = 0; i < bodyPartBtns.size(); i++) {
            bodyPartBtns.get(i).setBackgroundResource(i == idx
                    ? R.drawable.bg_segment_selected
                    : R.drawable.bg_segment_unselected);
        }
    }

    /** 추가하기 클릭 처리 */
    private void onAddClicked() {
        String name = etExerciseName.getText().toString().trim();
        if (name.isEmpty()) {
            etExerciseName.setError("운동 이름을 입력하세요");
            return;
        }
        if (selectedBodyPart == null) {
            Toast.makeText(requireContext(), "부위를 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        Context ctx = requireContext().getApplicationContext();
        Uri imgUri  = selectedImageUri;
        String bodyPart = selectedBodyPart;

        btnAdd.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            String imageType;
            String imageRef;

            if (imgUri != null) {
                // 갤러리 사진 → getFilesDir()/exercise_photos/<uuid>.jpg 복사
                String fileName = UUID.randomUUID().toString() + ".jpg";
                try {
                    imageRef  = PhotoFileHelper.copyToSubdir(ctx, imgUri, "exercise_photos", fileName);
                    imageType = "file";
                } catch (IOException e) {
                    // 복사 실패 시 부위별 기본 이미지 fallback
                    imageRef  = BodyPartLabels.imageRef(bodyPart);
                    imageType = "drawable";
                }
            } else {
                imageType = "drawable";
                imageRef  = BodyPartLabels.imageRef(bodyPart);
            }

            Exercise ex = new Exercise();
            ex.name        = name;
            ex.body_part   = bodyPart;
            ex.image_type  = imageType;
            ex.image_ref   = imageRef;
            ex.is_custom   = 1;
            ex.description = null;

            AppDatabase.getInstance(ctx).exerciseDao().insert(ex);

            // UI 스레드: 시트 닫기 + 부모 새로고침
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) return;
                if (getParentFragment() instanceof ExerciseCatalogFragment) {
                    ((ExerciseCatalogFragment) getParentFragment()).onCustomExerciseAdded();
                }
                dismiss();
            });
        });
    }
}
